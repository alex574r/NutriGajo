import { tokenDeServicio } from "../auth/google";
import type { Env } from "../index";
import { avisoDeAgendada, avisoDeCancelacion, avisoDeConfirmacion, zonaValida } from "../lib/aviso";
import { enviarPush, escribir, ID, leer, texto } from "../lib/firestore";

// El nutriólogo de la cita pide avisar a su paciente: `POST /citas/notificar {citaId}` con su sesión de Firebase. El aviso depende de la
// cita, no de lo que pida la app: confirmada (la pidió el paciente), agendada (`origen` NUTRIOLOGO: ya nace confirmada) o cancelada.
// El Worker no se fía de lo que manda la app: lee la cita en Firestore y solo avisa si es de ese nutriólogo, está confirmada o cancelada
// y no se avisó ya de su último cambio: tras avisar se guarda en `notificadaPara` el `actualizadaEn` avisado, y «ya avisado» es que siga
// siendo el mismo (una cita que se mueve y se vuelve a confirmar cambia `actualizadaEn` y avisa otra vez; no se comparan relojes, porque
// `actualizadaEn` lo pone el teléfono). Las citas avisadas antes de existir `notificadaPara` solo traen `notificadaEn` y cuentan como avisadas.
// El dispositivo del paciente sale de su perfil (usuarios/{uid}.fcmToken), nunca del cuerpo de la petición.
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
    const estado = texto(cita, "estado");
    if (estado !== "CONFIRMADA" && estado !== "CANCELADA") return new Response("La cita no está confirmada ni cancelada", { status: 409 });
    const cambio = cita.fields?.actualizadaEn?.integerValue;
    const avisadoPara = cita.fields?.notificadaPara?.integerValue;
    const yaAvisado = avisadoPara !== undefined ? avisadoPara === cambio : cita.fields?.notificadaEn !== undefined;
    if (yaAvisado) return Response.json({ enviado: false, motivo: "ya avisado" });

    const usuarioId = texto(cita, "usuarioId");
    if (!usuarioId || !ID.test(usuarioId)) return new Response("Cita no válida", { status: 400 });
    const paciente = await leer(env, token, `usuarios/${usuarioId}`);
    const dispositivo = texto(paciente, "fcmToken");
    if (!dispositivo) return Response.json({ enviado: false, motivo: "sin dispositivo" });

    const consultorio = await leer(env, token, `directorio/${uid}`);
    const nutriologo = texto(cita, "nutriologoNombre") ?? "Tu nutriólogo";
    const inicio = Number(cita.fields?.inicio?.integerValue);
    const zona = zonaValida(texto(paciente, "fcmZona"));
    const direccion = texto(consultorio, "direccion");
    const agendada = texto(cita, "origen") === "NUTRIOLOGO";
    const aviso =
      estado === "CANCELADA"
        ? avisoDeCancelacion(nutriologo, inicio, zona)
        : agendada
          ? avisoDeAgendada(nutriologo, inicio, zona, direccion)
          : avisoDeConfirmacion(nutriologo, inicio, zona, direccion);
    const tipo = estado === "CANCELADA" ? "CITA_CANCELADA" : agendada ? "CITA_AGENDADA" : "CITA_CONFIRMADA";
    const envio = await enviarPush(env, token, {
      dispositivo,
      canal: "citas",
      titulo: aviso.titulo,
      cuerpo: aviso.cuerpo,
      datos: { tipo, citaId },
    });
    if (envio === "caducado") {
      // el teléfono ya no existe para FCM: se quita del perfil para no insistir
      await escribir(env, token, `usuarios/${usuarioId}`, ["fcmToken"], {});
      return Response.json({ enviado: false, motivo: "dispositivo caducado" });
    }
    if (envio === "error") return new Response("FCM no aceptó el aviso", { status: 502 });

    await escribir(env, token, `citas/${citaId}`, ["notificadaEn", "notificadaPara"], {
      notificadaEn: { integerValue: String(Date.now()) },
      ...(cambio !== undefined ? { notificadaPara: { integerValue: cambio } } : {}),
    });
    return Response.json({ enviado: true });
  } catch {
    return new Response("No se pudo avisar", { status: 502 });
  }
}
