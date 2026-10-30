import { tokenDeServicio } from "../auth/google";
import type { Env } from "../index";
import { enviarPush, escribir, ID, leer, texto } from "../lib/firestore";
import { relacion } from "./plan";

// Chat entre el nutriólogo y su paciente. Los mensajes viven en Firestore (pacientes/{id}/mensajes/{mensajeId}, con sus reglas);
// aquí queda lo que Firestore no guarda: el archivo de las fotos (R2) y el aviso push a la otra persona. Como en los planes, nada se
// sirve a quien tenga la dirección: cada petición lleva la sesión y el Worker comprueba en Firestore que es una de las dos personas.
const MAX_BYTES = 3 * 1024 * 1024;
const VISTA_PREVIA = 100;
const clave = (pacienteId: string, mensajeId: string) => `chat/${pacienteId}/${mensajeId}.jpg`;

// `PUT /chat/{pacienteId}/{mensajeId}` (image/jpeg): quien escribe el mensaje sube su foto, una sola vez. La app la reduce antes
// (1280 px, JPEG), así que 3 MB sobra.
// ponytail: sin límite de subidas por persona; añadir uno si algún día hay abuso.
export async function subirFotoChat(req: Request, env: Env, uid: string, pacienteId: string, mensajeId: string): Promise<Response> {
  if (req.headers.get("Content-Type") !== "image/jpeg") return new Response("Tipo no permitido", { status: 415 });
  if (Number(req.headers.get("Content-Length") ?? 0) > MAX_BYTES) return new Response("Demasiado grande", { status: 413 });
  if (!env.FIREBASE_SERVICE_ACCOUNT) return new Response("Chat sin configurar", { status: 503 });

  try {
    const token = await tokenDeServicio(env);
    const { paciente, quien } = await relacion(env, token, pacienteId, uid);
    if (!paciente) return new Response("No encontrado", { status: 404 });
    if (!quien) return new Response("No es tu chat", { status: 403 });
    if (await env.FOTOS.head(clave(pacienteId, mensajeId))) return new Response("La foto ya se subió", { status: 409 });

    const cuerpo = await req.arrayBuffer();
    if (cuerpo.byteLength === 0 || cuerpo.byteLength > MAX_BYTES) return new Response("Tamaño no válido", { status: 413 });
    const [a, b, c] = new Uint8Array(cuerpo.slice(0, 3));
    if (a !== 0xff || b !== 0xd8 || c !== 0xff) return new Response("No es un JPEG", { status: 400 });

    await env.FOTOS.put(clave(pacienteId, mensajeId), cuerpo, { httpMetadata: { contentType: "image/jpeg" } });
    return Response.json({ ok: true });
  } catch (e) {
    console.error("subirFotoChat:", String(e));
    return new Response("No se pudo guardar la foto", { status: 502 });
  }
}

// `GET /chat/{pacienteId}/{mensajeId}`: las dos personas del chat. La foto no cambia, así que el teléfono la guarda.
export async function leerFotoChat(env: Env, uid: string, pacienteId: string, mensajeId: string): Promise<Response> {
  if (!env.FIREBASE_SERVICE_ACCOUNT) return new Response("Chat sin configurar", { status: 503 });
  try {
    const token = await tokenDeServicio(env);
    const { paciente, quien } = await relacion(env, token, pacienteId, uid);
    if (!paciente) return new Response("No encontrado", { status: 404 });
    if (!quien) return new Response("Sin acceso", { status: 403 });

    const objeto = await env.FOTOS.get(clave(pacienteId, mensajeId));
    if (!objeto) return new Response("Sin foto", { status: 404 });
    return new Response(objeto.body, {
      headers: {
        "Content-Type": "image/jpeg",
        "Cache-Control": "private, max-age=31536000, immutable",
        "X-Content-Type-Options": "nosniff",
      },
    });
  } catch (e) {
    console.error("leerFotoChat:", String(e));
    return new Response("No se pudo leer la foto", { status: 502 });
  }
}

// `POST /mensajes/notificar {pacienteId, mensajeId}`: quien escribió el mensaje pide avisar a la otra persona del chat. El Worker lee
// el mensaje en Firestore y solo avisa si existe, es de quien pide y no se avisó antes; el dispositivo sale del perfil del destinatario
// (usuarios/{uid}.fcmToken), nunca del cuerpo de la petición. El texto del aviso es un trozo del mensaje o «foto».
// ponytail: un aviso por mensaje, sin agrupar; si alguien escribe muchos seguidos llegan todos.
export async function avisarMensaje(req: Request, env: Env, uid: string): Promise<Response> {
  const { pacienteId, mensajeId } = (await req.json().catch(() => ({}))) as { pacienteId?: unknown; mensajeId?: unknown };
  if (typeof pacienteId !== "string" || !ID.test(pacienteId) || typeof mensajeId !== "string" || !ID.test(mensajeId)) {
    return new Response("Mensaje no válido", { status: 400 });
  }
  if (!env.FIREBASE_SERVICE_ACCOUNT) return new Response("Avisos sin configurar", { status: 503 });

  try {
    const token = await tokenDeServicio(env);
    const { paciente, quien } = await relacion(env, token, pacienteId, uid);
    if (!paciente) return new Response("No encontrado", { status: 404 });
    if (!quien) return new Response("No es tu chat", { status: 403 });
    const mensaje = await leer(env, token, `pacientes/${pacienteId}/mensajes/${mensajeId}`);
    if (!mensaje) return new Response("Mensaje no encontrado", { status: 404 });
    if (texto(mensaje, "autorId") !== uid) return new Response("No es tu mensaje", { status: 403 });
    if (mensaje.fields?.notificadaEn) return Response.json({ enviado: false, motivo: "ya avisado" });

    // le llega a la otra persona del chat
    const destinatario = quien === "nutriologo" ? texto(paciente, "usuarioId") : texto(paciente, "nutriologoId");
    if (!destinatario || !ID.test(destinatario)) return new Response("Chat no válido", { status: 400 });
    const dispositivo = texto(await leer(env, token, `usuarios/${destinatario}`), "fcmToken");
    if (!dispositivo) return Response.json({ enviado: false, motivo: "sin dispositivo" });

    const remitente = texto(await leer(env, token, `usuarios/${uid}`), "nombre") ?? (quien === "nutriologo" ? "Tu nutriólogo" : "Tu paciente");
    const escrito = (texto(mensaje, "texto") ?? "").trim();
    const cuerpo = escrito ? (escrito.length > VISTA_PREVIA ? `${escrito.slice(0, VISTA_PREVIA)}…` : escrito) : "Te envió una foto";
    const envio = await enviarPush(env, token, {
      dispositivo,
      canal: "mensajes",
      titulo: remitente,
      cuerpo,
      datos: { tipo: "MENSAJE_NUEVO", pacienteId },
    });
    if (envio === "caducado") {
      await escribir(env, token, `usuarios/${destinatario}`, ["fcmToken"], {});
      return Response.json({ enviado: false, motivo: "dispositivo caducado" });
    }
    if (envio === "error") return new Response("FCM no aceptó el aviso", { status: 502 });

    await escribir(env, token, `pacientes/${pacienteId}/mensajes/${mensajeId}`, ["notificadaEn"], { notificadaEn: { integerValue: String(Date.now()) } });
    return Response.json({ enviado: true });
  } catch (e) {
    console.error("avisarMensaje:", String(e));
    return new Response("No se pudo avisar", { status: 502 });
  }
}
