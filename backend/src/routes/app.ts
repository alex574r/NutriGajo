import type { Env } from "../index";

// Las actualizaciones de la app: el último APK publicado y un `apk/ultima.json` que dice cuál es. Los sube `scripts/publicar-apk.mjs` desde la
// computadora (con la cuenta de Cloudflare); este Worker solo los LEE, y no hay ruta de subida: nadie publica un APK por la red.
// Públicas, sin sesión: quien no puede iniciar sesión por un fallo tiene que poder actualizar justo para arreglarlo. Lo que protege al
// teléfono no es esto sino la firma del APK, que Android comprueba al instalar.
const CODIGO = /^[0-9]{1,9}$/;

// El manifiesto de la última versión (versionCode, versionName, sha256, bytes, notas…), siempre como JSON y por poco tiempo en caché.
export async function leerUltima(env: Env): Promise<Response> {
  const objeto = await env.FOTOS.get("apk/ultima.json");
  if (!objeto) return new Response("Sin versión publicada", { status: 404 });
  return new Response(objeto.body, {
    headers: { "Content-Type": "application/json", "Cache-Control": "public, max-age=300", "X-Content-Type-Options": "nosniff" },
  });
}

// El APK de esa versión. El número es solo dígitos (no se arma una clave de R2 con otra cosa) y, como va en la dirección, el archivo nunca
// cambia y se guarda para siempre.
export async function leerApk(req: Request, env: Env, versionCode: string): Promise<Response> {
  if (!CODIGO.test(versionCode)) return new Response("Sin versión", { status: 404 });
  const objeto = await env.FOTOS.get(`apk/${versionCode}.apk`, { onlyIf: req.headers });
  if (!objeto) return new Response("Sin versión", { status: 404 });

  const cabeceras = new Headers({
    "Content-Type": "application/vnd.android.package-archive",
    ETag: objeto.httpEtag,
    "X-Content-Type-Options": "nosniff",
    "Cache-Control": "public, max-age=31536000, immutable",
  });
  if (!("body" in objeto)) return new Response(null, { status: 304, headers: cabeceras });
  // con su tamaño: la app enseña el avance de la descarga
  cabeceras.set("Content-Length", String(objeto.size));
  return new Response((objeto as R2ObjectBody).body, { headers: cabeceras });
}
