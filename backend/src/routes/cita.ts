import { tokenDeServicio } from "../auth/google";
import type { Env } from "../index";
import { avisoDeConfirmacion, zonaValida } from "../lib/aviso";
import { enviarPush, escribir, ID, leer, texto } from "../lib/firestore";

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
    const envio = await enviarPush(env, token, {
      dispositivo,
      canal: "citas",
      titulo: aviso.titulo,
      cuerpo: aviso.cuerpo,
      datos: { tipo: "CITA_CONFIRMADA", citaId },
    });
    if (envio === "caducado") {
      // el teléfono ya no existe para FCM: se quita del perfil para no insistir
      await escribir(env, token, `usuarios/${usuarioId}`, ["fcmToken"], {});
      return Response.json({ enviado: false, motivo: "dispositivo caducado" });
    }
    if (envio === "error") return new Response("FCM no aceptó el aviso", { status: 502 });

    await escribir(env, token, `citas/${citaId}`, ["notificadaEn"], { notificadaEn: { integerValue: String(Date.now()) } });
    return Response.json({ enviado: true });
  } catch {
    return new Response("No se pudo avisar", { status: 502 });
  }
}
