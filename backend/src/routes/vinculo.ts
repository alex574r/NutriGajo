import { tokenDeServicio } from "../auth/google";
import type { Env } from "../index";
import { avisoDeBaja } from "../lib/aviso";
import { enviarPush, escribir, ID, leer, texto } from "../lib/firestore";

// Quien cerró un vínculo (el paciente al desvincularse o el nutriólogo al dar de baja) pide avisar a la otra persona:
// `POST /vinculos/notificar {pacienteId}` con su sesión de Firebase. El Worker no se fía de lo que manda la app: lee el registro en Firestore
// y solo avisa si quien pregunta es de ese vínculo y fue quien lo cerró (`cerradoPor`), si de verdad está cerrado y si no se avisó ya.
// El dispositivo de la otra persona sale de su perfil (usuarios/{uid}.fcmToken), nunca del cuerpo de la petición. Un vínculo que cerró el
// borrado de una cuenta (`CUENTA_ELIMINADA`) lo avisa ese borrado, no la app.
// ponytail: un solo dispositivo por persona (el último que abrió la app); sin reintentos si FCM falla.
export async function avisarBaja(req: Request, env: Env, uid: string): Promise<Response> {
  const { pacienteId } = (await req.json().catch(() => ({}))) as { pacienteId?: unknown };
  if (typeof pacienteId !== "string" || !ID.test(pacienteId)) return new Response("Vínculo no válido", { status: 400 });
  if (!env.FIREBASE_SERVICE_ACCOUNT) return new Response("Avisos sin configurar", { status: 503 });

  try {
    const token = await tokenDeServicio(env);
    const registro = await leer(env, token, `pacientes/${pacienteId}`);
    if (!registro) return new Response("No encontrado", { status: 404 });
    const paciente = texto(registro, "usuarioId");
    const nutriologo = texto(registro, "nutriologoId");
    if (uid !== paciente && uid !== nutriologo) return new Response("No es tu vínculo", { status: 403 });
    if (registro.fields?.activo?.booleanValue !== false) return new Response("El vínculo sigue activo", { status: 409 });
    const por = texto(registro, "cerradoPor");
    if (por !== "PACIENTE" && por !== "NUTRIOLOGO") return new Response("Ese cierre no se avisa desde aquí", { status: 409 });
    if ((por === "PACIENTE" && uid !== paciente) || (por === "NUTRIOLOGO" && uid !== nutriologo)) {
      return new Response("Solo avisa quien cerró el vínculo", { status: 403 });
    }
    if (registro.fields?.notificadaEn !== undefined) return Response.json({ enviado: false, motivo: "ya avisado" });

    const destino = por === "PACIENTE" ? nutriologo : paciente;
    if (!destino || !ID.test(destino)) return new Response("Vínculo no válido", { status: 400 });
    const dispositivo = texto(await leer(env, token, `usuarios/${destino}`), "fcmToken");
    if (!dispositivo) return Response.json({ enviado: false, motivo: "sin dispositivo" });

    const quien = por === "PACIENTE" ? (texto(registro, "nombre") ?? "Tu paciente") : (texto(registro, "nutriologoNombre") ?? "Tu nutriólogo");
    const aviso = avisoDeBaja(quien, por);
    // ponytail: sin canal propio; el canal de citas ya existe en la app y es el que se usa para los demás avisos de la relación
    const envio = await enviarPush(env, token, {
      dispositivo,
      canal: "citas",
      titulo: aviso.titulo,
      cuerpo: aviso.cuerpo,
      datos: { tipo: "VINCULO_CERRADO", pacienteId },
    });
    if (envio === "caducado") {
      // el teléfono ya no existe para FCM: se quita del perfil para no insistir
      await escribir(env, token, `usuarios/${destino}`, ["fcmToken"], {});
      return Response.json({ enviado: false, motivo: "dispositivo caducado" });
    }
    if (envio === "error") return new Response("FCM no aceptó el aviso", { status: 502 });

    await escribir(env, token, `pacientes/${pacienteId}`, ["notificadaEn"], { notificadaEn: { integerValue: String(Date.now()) } });
    return Response.json({ enviado: true });
  } catch {
    return new Response("No se pudo avisar", { status: 502 });
  }
}
