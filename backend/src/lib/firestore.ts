import type { Env } from "../index";

// Lo que el Worker hace con la cuenta de servicio de Firebase: leer y escribir Firestore sin pasar por las reglas
// y mandar mensajes de FCM. Cada ruta comprueba antes, con lo que lee, que quien pide tiene derecho a lo que pide.

// Firestore devuelve cada campo con su tipo: { stringValue } o { integerValue }.
export type Documento = { fields?: Record<string, { stringValue?: string; integerValue?: string; booleanValue?: boolean }> };

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

const ENCABEZADOS = (token: string) => ({ Authorization: `Bearer ${token}`, "Content-Type": "application/json" });

// Los documentos de una colección cuyo `campo` (texto) vale `valor`, con su ruta completa (`coleccion/{id}`). Sin coincidencias, una lista vacía.
export async function consultar(env: Env, token: string, coleccion: string, campo: string, valor: string): Promise<{ ruta: string; doc: Documento }[]> {
  const respuesta = await fetch(`${documentos(env)}:runQuery`, {
    method: "POST",
    headers: ENCABEZADOS(token),
    body: JSON.stringify({
      structuredQuery: { from: [{ collectionId: coleccion }], where: { fieldFilter: { field: { fieldPath: campo }, op: "EQUAL", value: { stringValue: valor } } } },
    }),
  });
  if (!respuesta.ok) throw new Error(`Firestore respondió ${respuesta.status}`);
  const filas = (await respuesta.json()) as { document?: { name: string; fields?: Documento["fields"] } }[];
  // sin coincidencias Firestore contesta un renglón sin `document`
  return filas.flatMap((f) => (f.document ? [{ ruta: f.document.name.split("/documents/")[1], doc: { fields: f.document.fields } }] : []));
}

// Borra un documento; que ya no exista cuenta como hecho.
export async function borrar(env: Env, token: string, ruta: string): Promise<void> {
  const respuesta = await fetch(`${documentos(env)}/${ruta}`, { method: "DELETE", headers: { Authorization: `Bearer ${token}` } });
  if (!respuesta.ok && respuesta.status !== 404) throw new Error(`Firestore respondió ${respuesta.status}`);
}

// Borra todos los documentos directos de una colección o subcolección (`usuarios/{uid}/carreras`); vacía o inexistente cuenta como hecho.
// Va por lotes de `commit` (hasta 400 borrados por petición) y no uno por uno: Workers limita las subpeticiones de cada invocación.
// ponytail: no baja a las subcolecciones de esos documentos; quien las tenga las borra antes.
export async function borrarColeccion(env: Env, token: string, ruta: string): Promise<void> {
  for (;;) {
    const lista = await fetch(`${documentos(env)}/${ruta}?pageSize=400`, { headers: { Authorization: `Bearer ${token}` } });
    if (lista.status === 404) return;
    if (!lista.ok) throw new Error(`Firestore respondió ${lista.status}`);
    const { documents = [] } = (await lista.json()) as { documents?: { name: string }[] };
    if (documents.length === 0) return;
    const lote = await fetch(`${documentos(env)}:commit`, {
      method: "POST",
      headers: ENCABEZADOS(token),
      body: JSON.stringify({ writes: documents.map((d) => ({ delete: d.name })) }),
    });
    if (!lote.ok) throw new Error(`Firestore respondió ${lote.status}`);
  }
}

// Como `escribir`, pero si Firestore no acepta el cambio lanza el error: para lo que no se puede dar por hecho sin comprobarlo (el borrado de cuentas).
export async function actualizar(env: Env, token: string, ruta: string, campos: string[], valores: Documento["fields"]): Promise<void> {
  const mascara = campos.map((c) => `updateMask.fieldPaths=${c}`).join("&");
  const respuesta = await fetch(`${documentos(env)}/${ruta}?${mascara}`, {
    method: "PATCH",
    headers: ENCABEZADOS(token),
    body: JSON.stringify({ fields: valores }),
  });
  if (!respuesta.ok) throw new Error(`Firestore respondió ${respuesta.status}`);
}

// Borra la cuenta de Firebase Auth (API de administración, con la cuenta de servicio); que ya no exista cuenta como hecho.
export async function borrarCuentaDeAuth(env: Env, token: string, uid: string): Promise<void> {
  const respuesta = await fetch(`https://identitytoolkit.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/accounts:delete`, {
    method: "POST",
    headers: ENCABEZADOS(token),
    body: JSON.stringify({ localId: uid }),
  });
  if (respuesta.ok) return;
  const cuerpo = await respuesta.text().catch(() => "");
  if (respuesta.status === 400 && cuerpo.includes("USER_NOT_FOUND")) return;
  throw new Error(`Auth respondió ${respuesta.status}`);
}

export interface Push {
  dispositivo: string;
  // canal de notificaciones de la app (se crean en Avisos.crearCanales)
  canal: "citas" | "planes" | "mensajes";
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
