import type { Env } from "../index";

// Lo que el Worker hace con la cuenta de servicio de Firebase: leer y escribir Firestore sin pasar por las reglas
// y mandar mensajes de FCM. Cada ruta comprueba antes, con lo que lee, que quien pide tiene derecho a lo que pide.

// Firestore devuelve cada campo con su tipo: { stringValue } o { integerValue }.
export type Documento = { fields?: Record<string, { stringValue?: string; integerValue?: string }> };

export const ID = /^[A-Za-z0-9_-]{1,128}$/;

const documentos = (env: Env) => `https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents`;

export const texto = (d: Documento | null, campo: string) => d?.fields?.[campo]?.stringValue;

export async function leer(env: Env, token: string, ruta: string): Promise<Documento | null> {
  const respuesta = await fetch(`${documentos(env)}/${ruta}`, { headers: { Authorization: `Bearer ${token}` } });
  if (respuesta.status === 404) return null;
  if (!respuesta.ok) throw new Error(`Firestore respondió ${respuesta.status}`);
  return respuesta.json();
}

// Cambia solo los campos nombrados; con `valores` vacío para un campo, lo borra.
export async function escribir(env: Env, token: string, ruta: string, campos: string[], valores: Documento["fields"]) {
  const mascara = campos.map((c) => `updateMask.fieldPaths=${c}`).join("&");
  await fetch(`${documentos(env)}/${ruta}?${mascara}`, {
    method: "PATCH",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ fields: valores }),
  });
}

export interface Push {
  dispositivo: string;
  // canal de notificaciones de la app (se crean en Avisos.crearCanales)
  canal: "citas" | "planes";
  titulo: string;
  cuerpo: string;
  // FCM solo acepta textos
  datos: Record<string, string>;
}

// "ok", "caducado" (el teléfono ya no existe para FCM: desinstalada o sesión cerrada) o "error".
export async function enviarPush(env: Env, token: string, push: Push): Promise<"ok" | "caducado" | "error"> {
  const respuesta = await fetch(`https://fcm.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/messages:send`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      message: {
        token: push.dispositivo,
        notification: { title: push.titulo, body: push.cuerpo },
        data: push.datos,
        android: { priority: "HIGH", notification: { channel_id: push.canal } },
      },
    }),
  });
  if (respuesta.status === 404) return "caducado";
  return respuesta.ok ? "ok" : "error";
}
