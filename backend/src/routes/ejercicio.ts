import type { Env } from "../index";

// Las fotos de los ejercicios del banco global. Cada una vive en `ejercicios/{uid del autor}/{id}`: el uid sale de la sesión de
// quien sube, así nadie puede pisar la foto de otro autor aunque adivine el id de su ejercicio.
const TIPOS = new Set(["image/jpeg", "image/png", "image/webp"]);
const MAX_BYTES = 3 * 1024 * 1024;

// Guarda la imagen y devuelve la versión (la hora de la subida): la app la apunta en el ejercicio y la usa en la dirección.
// ponytail: sin límite de subidas por usuario ni comprobación de que sea un entrenador (eso lo hacen las reglas de Firestore al
// publicar); añadir un límite cuando haya más rutas de subida.
export async function subirFotoEjercicio(req: Request, env: Env, uid: string, id: string): Promise<Response> {
  const contentType = req.headers.get("Content-Type") ?? "";
  if (!TIPOS.has(contentType)) return new Response("Tipo no permitido", { status: 415 });
  if (Number(req.headers.get("Content-Length") ?? 0) > MAX_BYTES) return new Response("Demasiado grande", { status: 413 });

  const cuerpo = await req.arrayBuffer();
  if (cuerpo.byteLength === 0 || cuerpo.byteLength > MAX_BYTES) return new Response("Tamaño no válido", { status: 413 });

  await env.FOTOS.put(`ejercicios/${uid}/${id}`, cuerpo, { httpMetadata: { contentType } });
  return Response.json({ version: Date.now() });
}

// Pública, como las fotos de perfil: la ve quien tenga la dirección, que lleva el uid del autor, el id y la versión. Con `?v=` nunca
// cambia de contenido y se guarda para siempre.
export async function leerFotoEjercicio(req: Request, env: Env, autor: string, id: string): Promise<Response> {
  const versionada = new URL(req.url).searchParams.has("v");
  const objeto = await env.FOTOS.get(`ejercicios/${autor}/${id}`, { onlyIf: req.headers });
  if (!objeto) return new Response("Sin foto", { status: 404 });

  const cabeceras = new Headers();
  objeto.writeHttpMetadata(cabeceras);
  cabeceras.set("ETag", objeto.httpEtag);
  cabeceras.set("X-Content-Type-Options", "nosniff");
  cabeceras.set("Cache-Control", versionada ? "public, max-age=31536000, immutable" : "no-cache");
  if (!cabeceras.has("Content-Type")) cabeceras.set("Content-Type", "image/jpeg");

  if (!("body" in objeto)) return new Response(null, { status: 304, headers: cabeceras });
  return new Response(objeto.body, { headers: cabeceras });
}
