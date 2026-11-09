// Prueba de las rutas de actualización de la app (GET /app/ultima y GET /app/apk/{versionCode}) con un R2 de mentira: `npm run test:app`.
// Empaqueta el Worker con esbuild (ya viene con wrangler) y lo llama como lo haría Cloudflare, así también comprueba el enrutado.
import assert from "node:assert/strict";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { pathToFileURL } from "node:url";
import { build } from "esbuild";

const salida = join(mkdtempSync(join(tmpdir(), "app-")), "index.mjs");
await build({ entryPoints: ["src/index.ts"], bundle: true, format: "esm", platform: "node", outfile: salida, logLevel: "silent" });
const { default: worker } = await import(pathToFileURL(salida).href);

class R2Falso {
  objetos = new Map();
  lecturas = [];
  async get(clave, opciones) {
    this.lecturas.push(clave);
    const o = this.objetos.get(clave);
    if (!o) return null;
    const etag = `"${clave}"`;
    const base = {
      httpEtag: etag,
      size: o.cuerpo.length,
      writeHttpMetadata(cabeceras) {
        if (o.tipo) cabeceras.set("Content-Type", o.tipo);
      },
    };
    // como R2: si la condición dice que no cambió, devuelve el objeto sin cuerpo
    if (opciones?.onlyIf?.get?.("If-None-Match") === etag) return base;
    return { ...base, body: o.cuerpo };
  }
}

const r2 = new R2Falso();
const env = { FOTOS: r2, FIREBASE_PROJECT_ID: "nutrigajo" };
const pedir = (metodo, ruta, cabeceras = {}) => worker.fetch(new Request("http://x" + ruta, { method: metodo, headers: cabeceras }), env);

const manifiesto = JSON.stringify({ versionCode: 2, versionName: "1.1", sha256: "a".repeat(64), bytes: 11, notas: ["Una nota"], minVersionCode: 0, publicadaEn: 1 });

// ---- /app/ultima
let r = await pedir("GET", "/app/ultima");
assert.equal(r.status, 404, "sin nada publicado");

r2.objetos.set("apk/ultima.json", { cuerpo: manifiesto, tipo: "binary/octet-stream" });
r = await pedir("GET", "/app/ultima");
assert.equal(r.status, 200, "con el manifiesto publicado");
assert.equal(await r.text(), manifiesto, "se sirve tal cual");
assert.equal(r.headers.get("Content-Type"), "application/json", "siempre como JSON, sea lo que sea que diga R2");
assert.equal(r.headers.get("Cache-Control"), "public, max-age=300", "se guarda cinco minutos, no para siempre");
assert.equal(r.headers.get("X-Content-Type-Options"), "nosniff");

// ---- /app/apk/{versionCode}
r = await pedir("GET", "/app/apk/2");
assert.equal(r.status, 404, "un APK que no está publicado");

r2.objetos.set("apk/2.apk", { cuerpo: "APK-DOS-BYTES", tipo: "application/octet-stream" });
r = await pedir("GET", "/app/apk/2");
assert.equal(r.status, 200, "un APK publicado");
assert.equal(await r.text(), "APK-DOS-BYTES");
assert.equal(r.headers.get("Content-Type"), "application/vnd.android.package-archive");
assert.equal(r.headers.get("Content-Length"), String("APK-DOS-BYTES".length), "con su tamaño, para el avance de la descarga");
assert.equal(r.headers.get("Cache-Control"), "public, max-age=31536000, immutable", "el número va en la dirección: nunca cambia");
assert.equal(r.headers.get("ETag"), '"apk/2.apk"');
assert.equal(r.headers.get("X-Content-Type-Options"), "nosniff");

r = await pedir("GET", "/app/apk/2", { "If-None-Match": '"apk/2.apk"' });
assert.equal(r.status, 304, "con el ETag que ya tiene, no se vuelve a mandar");
assert.equal(r.headers.get("ETag"), '"apk/2.apk"');

// ---- lo que no es una ruta de lectura de las dos: ni toca R2
r2.lecturas.length = 0;
for (const ruta of ["/app/apk/abc", "/app/apk/1234567890", "/app/apk/..%2Fultima", "/app/apk/2/extra", "/app/apk/", "/app/apk", "/app/otra"]) {
  assert.equal((await pedir("GET", ruta)).status, 404, `GET ${ruta}`);
}
assert.equal((await pedir("POST", "/app/ultima", {})).status, 404, "no se publica por la red");
assert.equal((await pedir("PUT", "/app/ultima")).status, 404);
assert.equal((await pedir("PUT", "/app/apk/2")).status, 404, "no se sube un APK por la red");
assert.equal((await pedir("DELETE", "/app/apk/2")).status, 404);
assert.deepEqual(r2.lecturas, [], "ninguna de esas peticiones llegó a R2");

console.log("app.mjs: ok");
