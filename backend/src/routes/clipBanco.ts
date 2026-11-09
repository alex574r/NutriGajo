import type { Env } from "../index";

// Los clips animados de los ejercicios que crea un entrenador: un MP4 corto por ejercicio (la app lo reproduce en bucle). Cada uno vive en
// `ejercicios/{uid del autor}/{id}/clip-{versión}`, con las mismas protecciones que la foto (fotoBanco.ts): el uid sale de la sesión de quien
// sube y la versión (la hora de la subida) hace que una subida nueva nunca pise a la que el ADMIN aprobó.
// ponytail: el tope de 2 MiB y el tipo se comprueban aquí; la duración (≤ 5 s) la comprueba la app antes de subir, el Worker no decodifica
// video. Las versiones viejas no se borran, como las de las fotos.
const TIPO = "video/mp4";
const MAX_BYTES = 2 * 1024 * 1024;
const VERSION = /^[0-9]{1,16}$/;

// Guarda el clip bajo su versión y la devuelve.
export async function subirClipBanco(req: Request, env: Env, uid: string, id: string): Promise<Response> {
  if ((req.headers.get("Content-Type") ?? "") !== TIPO) return new Response("Tipo no permitido", { status: 415 });
  if (Number(req.headers.get("Content-Length") ?? 0) > MAX_BYTES) return new Response("Demasiado grande", { status: 413 });

  const cuerpo = await req.arrayBuffer();
  if (cuerpo.byteLength === 0 || cuerpo.byteLength > MAX_BYTES) return new Response("Tamaño no válido", { status: 413 });

  const version = Date.now();
  await env.FOTOS.put(`ejercicios/${uid}/${id}/clip-${version}`, cuerpo, { httpMetadata: { contentType: TIPO } });
  return Response.json({ version });
}

// Pública, como las fotos: la dirección lleva el uid del autor, el id y la versión. Solo se sirve con `?v=` numérico (lo versionado nunca
// cambia) y acepta `Range`, para que el reproductor pida pedazos y repita el bucle sin bajarlo entero cada vez.
export async function leerClipBanco(req: Request, env: Env, autor: string, id: string): Promise<Response> {
  const version = new URL(req.url).searchParams.get("v");
  if (version === null || !VERSION.test(version)) return new Response("Sin clip", { status: 404 });

  const objeto = await env.FOTOS.get(`ejercicios/${autor}/${id}/clip-${version}`, { onlyIf: req.headers, range: req.headers });
  if (!objeto) return new Response("Sin clip", { status: 404 });

  const cabeceras = new Headers();
  objeto.writeHttpMetadata(cabeceras);
  cabeceras.set("ETag", objeto.httpEtag);
  cabeceras.set("X-Content-Type-Options", "nosniff");
  cabeceras.set("Cache-Control", "public, max-age=31536000, immutable");
  cabeceras.set("Accept-Ranges", "bytes");
  if (!cabeceras.has("Content-Type")) cabeceras.set("Content-Type", TIPO);

  if (!("body" in objeto)) return new Response(null, { status: 304, headers: cabeceras });

  const cuerpo = objeto as R2ObjectBody;
  if (req.headers.has("Range") && cuerpo.range) {
    const rango = cuerpo.range;
    // R2 devuelve el rango como {offset, length}, solo uno de los dos o {suffix} (los últimos bytes)
    const inicio = "suffix" in rango ? cuerpo.size - rango.suffix : (rango.offset ?? 0);
    const largo = "suffix" in rango ? rango.suffix : (rango.length ?? cuerpo.size - inicio);
    cabeceras.set("Content-Range", `bytes ${inicio}-${inicio + largo - 1}/${cuerpo.size}`);
    return new Response(cuerpo.body, { status: 206, headers: cabeceras });
  }
  return new Response(cuerpo.body, { headers: cabeceras });
}
