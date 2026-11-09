import type { Env } from "../index";

// El catálogo de ejercicios (fichas, fotos y clips animados) vive en R2 bajo `catalogo/`: lo sube el script `subir-catalogo.mjs`, no la app. Esta
// ruta solo lo lee, y quien la llama (index.ts) ya exigió una sesión de Firebase: son medios con licencia, no se sirven a quien no inició sesión.
// Solo se sirven las cinco formas de clave de abajo; cualquier otra ruta es 404 sin preguntar al R2.
// ponytail: sin límite de lecturas por usuario; añadirlo si el catálogo se vuelve un blanco de descargas masivas.
const ID = "[0-9]{4,5}";
const FORMAS: Array<{ patron: RegExp; tipo: string; rangos: boolean; cache: string }> = [
  { patron: /^version\.json$/, tipo: "application/json", rangos: false, cache: "private, no-cache" },
  { patron: /^indice-[0-9]{1,9}\.json$/, tipo: "application/json", rangos: false, cache: "private, max-age=31536000, immutable" },
  { patron: new RegExp(`^ficha/${ID}\\.json$`), tipo: "application/json", rangos: false, cache: "private, max-age=31536000, immutable" },
  { patron: new RegExp(`^foto/${ID}\\.webp$`), tipo: "image/webp", rangos: false, cache: "private, max-age=31536000, immutable" },
  { patron: new RegExp(`^clip/${ID}\\.mp4$`), tipo: "video/mp4", rangos: true, cache: "private, max-age=31536000, immutable" },
];

// `ruta` es lo que sigue a `/catalogo/` en la dirección.
export async function leerCatalogo(req: Request, env: Env, ruta: string): Promise<Response> {
  const forma = FORMAS.find((f) => f.patron.test(ruta));
  if (!forma) return new Response("No encontrado", { status: 404 });

  const objeto = await env.FOTOS.get(`catalogo/${ruta}`, forma.rangos ? { onlyIf: req.headers, range: req.headers } : { onlyIf: req.headers });
  if (!objeto) return new Response("No encontrado", { status: 404 });

  const cabeceras = new Headers();
  objeto.writeHttpMetadata(cabeceras);
  cabeceras.set("ETag", objeto.httpEtag);
  cabeceras.set("X-Content-Type-Options", "nosniff");
  cabeceras.set("Cache-Control", forma.cache);
  if (forma.rangos) cabeceras.set("Accept-Ranges", "bytes");
  if (!cabeceras.has("Content-Type")) cabeceras.set("Content-Type", forma.tipo);

  if (!("body" in objeto)) return new Response(null, { status: 304, headers: cabeceras });

  const cuerpo = objeto as R2ObjectBody;
  if (forma.rangos && req.headers.has("Range") && cuerpo.range) {
    const rango = cuerpo.range;
    // R2 devuelve el rango como {offset, length}, solo uno de los dos o {suffix} (los últimos bytes)
    const inicio = "suffix" in rango ? cuerpo.size - rango.suffix : (rango.offset ?? 0);
    const largo = "suffix" in rango ? rango.suffix : (rango.length ?? cuerpo.size - inicio);
    cabeceras.set("Content-Range", `bytes ${inicio}-${inicio + largo - 1}/${cuerpo.size}`);
    return new Response(cuerpo.body, { status: 206, headers: cabeceras });
  }
  return new Response(cuerpo.body, { headers: cabeceras });
}
