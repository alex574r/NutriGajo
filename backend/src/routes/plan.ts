import { tokenDeServicio } from "../auth/google";
import type { Env } from "../index";
import { enviarPush, escribir, ID, leer, texto } from "../lib/firestore";

// Planes de alimentación y ejercicio: el PDF que arma el nutriólogo para un paciente suyo. Son datos de salud, así que, a
// diferencia de las fotos, no se sirven a quien tenga la dirección: cada petición lleva la sesión de Firebase y el Worker
// comprueba en Firestore (pacientes/{id}) quién es quien pide. Un plan enviado no cambia: cada plan nuevo es otro `planId`.
const MAX_BYTES = 8 * 1024 * 1024;
const clave = (pacienteId: string, planId: string) => `planes/${pacienteId}/${planId}.pdf`;

type Relacion = "nutriologo" | "paciente" | null;

// Quién es `uid` respecto al paciente: su nutriólogo, el propio paciente o nadie; null en `paciente` si no existe.
async function relacion(env: Env, token: string, pacienteId: string, uid: string) {
  const paciente = await leer(env, token, `pacientes/${pacienteId}`);
  let quien: Relacion = null;
  if (texto(paciente, "nutriologoId") === uid) quien = "nutriologo";
  else if (texto(paciente, "usuarioId") === uid) quien = "paciente";
  return { paciente, quien };
}

// `PUT /plan/{pacienteId}/{planId}` (application/pdf): solo el nutriólogo del paciente, y solo una vez por plan.
export async function subirPlan(req: Request, env: Env, uid: string, pacienteId: string, planId: string): Promise<Response> {
  if (req.headers.get("Content-Type") !== "application/pdf") return new Response("Tipo no permitido", { status: 415 });
  if (Number(req.headers.get("Content-Length") ?? 0) > MAX_BYTES) return new Response("Demasiado grande", { status: 413 });
  if (!env.FIREBASE_SERVICE_ACCOUNT) return new Response("Planes sin configurar", { status: 503 });

  try {
    const token = await tokenDeServicio(env);
    const { paciente, quien } = await relacion(env, token, pacienteId, uid);
    if (!paciente) return new Response("No encontrado", { status: 404 });
    if (quien !== "nutriologo") return new Response("No es tu paciente", { status: 403 });
    if (await leer(env, token, `pacientes/${pacienteId}/planes/${planId}`)) return new Response("El plan ya se envió", { status: 409 });

    const cuerpo = await req.arrayBuffer();
    if (cuerpo.byteLength === 0 || cuerpo.byteLength > MAX_BYTES) return new Response("Tamaño no válido", { status: 413 });
    if (new TextDecoder().decode(cuerpo.slice(0, 5)) !== "%PDF-") return new Response("No es un PDF", { status: 400 });

    await env.FOTOS.put(clave(pacienteId, planId), cuerpo, { httpMetadata: { contentType: "application/pdf" } });
    return Response.json({ ok: true });
  } catch (e) {
    console.error("subirPlan:", String(e));
    return new Response("No se pudo guardar el plan", { status: 502 });
  }
}

// `GET /plan/{pacienteId}/{planId}`: el nutriólogo y el paciente. El plan no cambia, así que el teléfono lo guarda.
export async function leerPlan(env: Env, uid: string, pacienteId: string, planId: string): Promise<Response> {
  if (!env.FIREBASE_SERVICE_ACCOUNT) return new Response("Planes sin configurar", { status: 503 });
  try {
    const token = await tokenDeServicio(env);
    const { paciente, quien } = await relacion(env, token, pacienteId, uid);
    if (!paciente) return new Response("No encontrado", { status: 404 });
    if (!quien) return new Response("Sin acceso", { status: 403 });

    const objeto = await env.FOTOS.get(clave(pacienteId, planId));
    if (!objeto) return new Response("Sin plan", { status: 404 });
    return new Response(objeto.body, {
      headers: {
        "Content-Type": "application/pdf",
        "Content-Disposition": 'inline; filename="plan-nutrigajo.pdf"',
        "Cache-Control": "private, max-age=31536000, immutable",
        "X-Content-Type-Options": "nosniff",
      },
    });
  } catch (e) {
    console.error("leerPlan:", String(e));
    return new Response("No se pudo leer el plan", { status: 502 });
  }
}

// `POST /planes/notificar {pacienteId, planId}`: el nutriólogo avisa a su paciente de que ya puede ver el plan que le envió.
// Solo avisa si el plan existe en Firestore, lo envió ese nutriólogo y no se avisó antes.
export async function avisarPlan(req: Request, env: Env, uid: string): Promise<Response> {
  const { pacienteId, planId } = (await req.json().catch(() => ({}))) as { pacienteId?: unknown; planId?: unknown };
  if (typeof pacienteId !== "string" || !ID.test(pacienteId) || typeof planId !== "string" || !ID.test(planId)) {
    return new Response("Plan no válido", { status: 400 });
  }
  if (!env.FIREBASE_SERVICE_ACCOUNT) return new Response("Avisos sin configurar", { status: 503 });

  try {
    const token = await tokenDeServicio(env);
    const { paciente, quien } = await relacion(env, token, pacienteId, uid);
    if (!paciente) return new Response("No encontrado", { status: 404 });
    if (quien !== "nutriologo") return new Response("No es tu paciente", { status: 403 });
    const plan = await leer(env, token, `pacientes/${pacienteId}/planes/${planId}`);
    if (!plan) return new Response("Plan no encontrado", { status: 404 });
    if (texto(plan, "enviadoPor") !== uid) return new Response("No es tu plan", { status: 403 });
    if (plan.fields?.notificadaEn) return Response.json({ enviado: false, motivo: "ya avisado" });

    const usuarioId = texto(paciente, "usuarioId");
    if (!usuarioId || !ID.test(usuarioId)) return new Response("Paciente no válido", { status: 400 });
    const dispositivo = texto(await leer(env, token, `usuarios/${usuarioId}`), "fcmToken");
    if (!dispositivo) return Response.json({ enviado: false, motivo: "sin dispositivo" });

    const envio = await enviarPush(env, token, {
      dispositivo,
      canal: "planes",
      titulo: "Tu plan ya está listo",
      cuerpo: `${texto(plan, "nutriologoNombre") ?? "Tu nutriólogo"} te envió tu plan de alimentación y ejercicio. Ábrelo para verlo.`,
      datos: { tipo: "PLAN_NUEVO", pacienteId, planId },
    });
    if (envio === "caducado") {
      await escribir(env, token, `usuarios/${usuarioId}`, ["fcmToken"], {});
      return Response.json({ enviado: false, motivo: "dispositivo caducado" });
    }
    if (envio === "error") return new Response("FCM no aceptó el aviso", { status: 502 });

    await escribir(env, token, `pacientes/${pacienteId}/planes/${planId}`, ["notificadaEn"], { notificadaEn: { integerValue: String(Date.now()) } });
    return Response.json({ enviado: true });
  } catch (e) {
    console.error("avisarPlan:", String(e));
    return new Response("No se pudo avisar", { status: 502 });
  }
}
