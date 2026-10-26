import { tokenDeServicio } from "../auth/google";
import type { Env } from "../index";
import { avisoDeConfirmacion, zonaValida } from "../lib/aviso";

// Firestore devuelve cada campo con su tipo: { stringValue } o { integerValue }.
type Documento = { fields?: Record<string, { stringValue?: string; integerValue?: string }> };

const ID = /^[A-Za-z0-9_-]{1,128}$/;
const documentos = (env: Env) => `https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents`;
const texto = (d: Documento | null, campo: string) => d?.fields?.[campo]?.stringValue;

async function leer(env: Env, token: string, ruta: string): Promise<Documento | null> {
  const respuesta = await fetch(`${documentos(env)}/${ruta}`, { headers: { Authorization: `Bearer ${token}` } });
  if (respuesta.status === 404) return null;
  if (!respuesta.ok) throw new Error(`Firestore respondió ${respuesta.status}`);
  return respuesta.json();
}

// Cambia solo los campos nombrados; con `valores` vacío para un campo, lo borra.
async function escribir(env: Env, token: string, ruta: string, campos: string[], valores: Documento["fields"]) {
  const mascara = campos.map((c) => `updateMask.fieldPaths=${c}`).join("&");
  await fetch(`${documentos(env)}/${ruta}?${mascara}`, {
    method: "PATCH",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ fields: valores }),
  });
}

// El nutriólogo de la cita confirmada pide avisar a su paciente: `POST /citas/notificar {citaId}` con su sesión de Firebase.
// El Worker no se fía de lo que manda la app: lee la cita en Firestore y solo avisa si es de ese nutriólogo, está confirmada
// y no se avisó antes. El dispositivo del paciente sale de su perfil (usuarios/{uid}.fcmToken), nunca del cuerpo de la petición.
// ponytail: un solo dispositivo por paciente (el último que abrió la app); sin reintentos si FCM falla.
export async function avisarConfirmacion(req: Request, env: Env, uid: string): Promise<Response> {
  const { citaId } = (await req.json().catch(() => ({}))) as { citaId?: unknown };
  if (typeof citaId !== "string" || !ID.test(citaId)) return new Response("Cita no válida", { status: 400 });
  if (!env.FIREBASE_SERVICE_ACCOUNT) return new Response("Avisos sin configurar", { status: 503 });

  try {
    const token = await tokenDeServicio(env);
    const cita = await leer(env, token, `citas/${citaId}`);
    if (!cita) return new Response("No encontrada", { status: 404 });
    if (texto(cita, "nutriologoId") !== uid) return new Response("No es tu cita", { status: 403 });
    if (texto(cita, "estado") !== "CONFIRMADA") return new Response("La cita no está confirmada", { status: 409 });
    if (cita.fields?.notificadaEn) return Response.json({ enviado: false, motivo: "ya avisado" });

    const usuarioId = texto(cita, "usuarioId");
    if (!usuarioId || !ID.test(usuarioId)) return new Response("Cita no válida", { status: 400 });
    const paciente = await leer(env, token, `usuarios/${usuarioId}`);
    const dispositivo = texto(paciente, "fcmToken");
    if (!dispositivo) return Response.json({ enviado: false, motivo: "sin dispositivo" });

    const consultorio = await leer(env, token, `directorio/${uid}`);
    const aviso = avisoDeConfirmacion(
      texto(cita, "nutriologoNombre") ?? "Tu nutriólogo",
      Number(cita.fields?.inicio?.integerValue),
      zonaValida(texto(paciente, "fcmZona")),
      texto(consultorio, "direccion"),
    );
    const envio = await fetch(`https://fcm.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/messages:send`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        message: {
          token: dispositivo,
          notification: { title: aviso.titulo, body: aviso.cuerpo },
          data: { tipo: "CITA_CONFIRMADA", citaId },
          android: { priority: "HIGH", notification: { channel_id: "citas" } },
        },
      }),
    });
    if (envio.status === 404) {
      // el teléfono ya no existe para FCM (desinstalada o sesión cerrada): se quita del perfil para no insistir
      await escribir(env, token, `usuarios/${usuarioId}`, ["fcmToken"], {});
      return Response.json({ enviado: false, motivo: "dispositivo caducado" });
    }
    if (!envio.ok) return new Response("FCM no aceptó el aviso", { status: 502 });

    await escribir(env, token, `citas/${citaId}`, ["notificadaEn"], { notificadaEn: { integerValue: String(Date.now()) } });
    return Response.json({ enviado: true });
  } catch {
    return new Response("No se pudo avisar", { status: 502 });
  }
}
