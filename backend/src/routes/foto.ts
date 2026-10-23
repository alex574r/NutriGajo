import type { Env } from "../index";

// Cada tipo de foto tiene su carpeta en el bucket: `portadas/{uid}` y `avatares/{uid}`.
export const CARPETAS = { portada: "portadas", avatar: "avatares" } as const;
export type Tipo = keyof typeof CARPETAS;

const TIPOS = new Set(["image/jpeg", "image/png", "image/webp"]);
const MAX_BYTES = 3 * 1024 * 1024;

// Guarda la imagen en la clave de su dueño y devuelve la versión (la hora de la subida): la app la apunta en Firestore
// y la usa en la dirección para no ver una foto vieja.
// ponytail: sin límite de subidas por usuario; añadir uno cuando haya más rutas de subida.
export async function subirFoto(req: Request, env: Env, uid: string, tipo: Tipo): Promise<Response> {
  const contentType = req.headers.get("Content-Type") ?? "";
  if (!TIPOS.has(contentType)) return new Response("Tipo no permitido", { status: 415 });
  if (Number(req.headers.get("Content-Length") ?? 0) > MAX_BYTES) return new Response("Demasiado grande", { status: 413 });

  const cuerpo = await req.arrayBuffer();
  if (cuerpo.byteLength === 0 || cuerpo.byteLength > MAX_BYTES) return new Response("Tamaño no válido", { status: 413 });

  await env.FOTOS.put(`${CARPETAS[tipo]}/${uid}`, cuerpo, { httpMetadata: { contentType } });
  return Response.json({ version: Date.now() });
}

// Con `?v=` (la versión que dice el perfil de su dueño) la dirección nunca cambia de contenido y se guarda para siempre.
// Sin ella (un nutriólogo viendo a sus pacientes, que no conoce la versión) la respuesta lleva un ETag y la app pregunta
// con If-None-Match: si la foto no cambió contesta 304 sin cuerpo.
export async function leerFoto(req: Request, env: Env, uid: string, tipo: Tipo): Promise<Response> {
  const versionada = new URL(req.url).searchParams.has("v");
  const objeto = await env.FOTOS.get(`${CARPETAS[tipo]}/${uid}`, { onlyIf: req.headers });
  if (!objeto) return new Response("Sin foto", { status: 404 });

  const cabeceras = new Headers();
  objeto.writeHttpMetadata(cabeceras);
  cabeceras.set("ETag", objeto.httpEtag);
  cabeceras.set("X-Content-Type-Options", "nosniff");
  cabeceras.set("Cache-Control", versionada ? "public, max-age=31536000, immutable" : "no-cache");
  if (!cabeceras.has("Content-Type")) cabeceras.set("Content-Type", "image/jpeg");

  // la condición no se cumplió: R2 devuelve el objeto sin cuerpo
  if (!("body" in objeto)) return new Response(null, { status: 304, headers: cabeceras });
  return new Response(objeto.body, { headers: cabeceras });
}
