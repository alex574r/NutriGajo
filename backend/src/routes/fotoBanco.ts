import type { Env } from "../index";

// Las fotos de los bancos globales: los ejercicios que crea un entrenador y los platillos que crea un nutriólogo o un entrenador. Cada
// una vive en `{carpeta}/{uid del autor}/{id}/{versión}`: el uid sale de la sesión de quien sube, así nadie puede pisar la foto de otro
// autor aunque adivine el id; y la versión (la hora de la subida) hace que una subida nueva nunca pise a la anterior, de modo que la foto
// que el ADMIN aprobó con su versión no cambia aunque el autor suba otra después.
// Lo subido antes de que hubiera versiones vive en `{carpeta}/{uid}/{id}` y se sigue sirviendo, pero ya nada lo escribe.
// ponytail: las versiones viejas no se borran (no hay ruta para quitarlas); limpiar las que ningún documento apunta cuando pese.
export type Carpeta = "ejercicios" | "platillos";

const TIPOS = new Set(["image/jpeg", "image/png", "image/webp"]);
const MAX_BYTES = 3 * 1024 * 1024;
const VERSION = /^[0-9]{1,16}$/;

// Guarda la imagen bajo su versión y la devuelve: la app la apunta en el documento y la usa en la dirección.
// ponytail: sin límite de subidas por usuario ni comprobación de que sea un autor verificado (eso lo hacen las reglas de Firestore al
// publicar); añadir un límite cuando haya más rutas de subida.
export async function subirFotoBanco(req: Request, env: Env, uid: string, id: string, carpeta: Carpeta): Promise<Response> {
  const contentType = req.headers.get("Content-Type") ?? "";
  if (!TIPOS.has(contentType)) return new Response("Tipo no permitido", { status: 415 });
  if (Number(req.headers.get("Content-Length") ?? 0) > MAX_BYTES) return new Response("Demasiado grande", { status: 413 });

  const cuerpo = await req.arrayBuffer();
  if (cuerpo.byteLength === 0 || cuerpo.byteLength > MAX_BYTES) return new Response("Tamaño no válido", { status: 413 });

  const version = Date.now();
  await env.FOTOS.put(`${carpeta}/${uid}/${id}/${version}`, cuerpo, { httpMetadata: { contentType } });
  return Response.json({ version });
}

// La foto bajo su versión o, si no está (o la versión no es un número, que no se usa para armar una clave), la que se subió antes de
// que hubiera versiones.
async function buscar(env: Env, carpeta: Carpeta, autor: string, id: string, version: string | null, cabeceras: Headers) {
  if (version !== null && VERSION.test(version)) {
    const nueva = await env.FOTOS.get(`${carpeta}/${autor}/${id}/${version}`, { onlyIf: cabeceras });
    if (nueva) return nueva;
  }
  return env.FOTOS.get(`${carpeta}/${autor}/${id}`, { onlyIf: cabeceras });
}

// Pública, como las fotos de perfil: la ve quien tenga la dirección, que lleva el uid del autor, el id y la versión. Con `?v=` nunca
// cambia de contenido y se guarda para siempre.
export async function leerFotoBanco(req: Request, env: Env, autor: string, id: string, carpeta: Carpeta): Promise<Response> {
  const version = new URL(req.url).searchParams.get("v");
  const objeto = await buscar(env, carpeta, autor, id, version, req.headers);
  if (!objeto) return new Response("Sin foto", { status: 404 });

  const cabeceras = new Headers();
  objeto.writeHttpMetadata(cabeceras);
  cabeceras.set("ETag", objeto.httpEtag);
  cabeceras.set("X-Content-Type-Options", "nosniff");
  cabeceras.set("Cache-Control", version !== null ? "public, max-age=31536000, immutable" : "no-cache");
  if (!cabeceras.has("Content-Type")) cabeceras.set("Content-Type", "image/jpeg");

  if (!("body" in objeto)) return new Response(null, { status: 304, headers: cabeceras });
  return new Response((objeto as R2ObjectBody).body, { headers: cabeceras });
}
