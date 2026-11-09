// Prueba de los clips de los ejercicios (MP4 cortos en R2) con un R2 de mentira: `npm run test:clip` (Node 22 o más nuevo, que lee
// TypeScript directamente). Comprueba que un clip subido nunca pisa a otro, que solo se lee con su versión y que se sirve por rangos.
import assert from "node:assert/strict";
import { leerClipBanco, subirClipBanco } from "../src/routes/clipBanco.ts";

class R2Falso {
  objetos = new Map();
  async put(clave, cuerpo, opciones) {
    this.objetos.set(clave, { cuerpo: new Uint8Array(cuerpo), tipo: opciones?.httpMetadata?.contentType });
  }
  async get(clave, opciones) {
    const o = this.objetos.get(clave);
    if (!o) return null;
    const pedido = opciones?.range?.get?.("Range")?.match(/^bytes=(\d+)-(\d+)$/);
    const offset = pedido ? Number(pedido[1]) : undefined;
    const length = pedido ? Number(pedido[2]) - Number(pedido[1]) + 1 : undefined;
    return {
      body: pedido ? o.cuerpo.slice(offset, offset + length) : o.cuerpo,
      size: o.cuerpo.byteLength,
      range: pedido ? { offset, length } : undefined,
      httpEtag: `"${clave}"`,
      writeHttpMetadata(cabeceras) {
        if (o.tipo) cabeceras.set("Content-Type", o.tipo);
      },
    };
  }
}

const env = { FOTOS: new R2Falso() };
let ahora = 1000;
Date.now = () => ahora;

const MAX = 2 * 1024 * 1024;
const bytes = (n, relleno = 7) => new Uint8Array(n).fill(relleno);
const peticion = (cuerpo, tipo = "video/mp4", largo = cuerpo.byteLength) =>
  new Request("http://x/ejercicio/e1/clip", { method: "PUT", body: cuerpo, headers: { "Content-Type": tipo, "Content-Length": String(largo) } });
const subir = async (uid, id, cuerpo) => (await subirClipBanco(peticion(cuerpo), env, uid, id)).json();
const leer = (autor, id, version, cabeceras = {}) =>
  leerClipBanco(new Request(`http://x/ejercicio/${autor}/${id}/clip${version === undefined ? "" : `?v=${version}`}`, { headers: cabeceras }), env, autor, id);

const autor = "A".repeat(28);

// subir devuelve la versión (la hora de la subida) y el clip queda bajo uid/id/clip-versión
assert.deepEqual(await subir(autor, "e1", bytes(100)), { version: 1000 }, "la versión es la hora de la subida");
assert.ok(env.FOTOS.objetos.has(`ejercicios/${autor}/e1/clip-1000`), "queda bajo la carpeta del autor");

// se lee con su versión, como mp4, para siempre y sin que el navegador adivine el tipo
let r = await leer(autor, "e1", 1000);
assert.equal(r.status, 200);
assert.equal(r.headers.get("Content-Type"), "video/mp4");
assert.match(r.headers.get("Cache-Control"), /immutable/, "con versión se guarda para siempre");
assert.equal(r.headers.get("X-Content-Type-Options"), "nosniff");
assert.equal(r.headers.get("Accept-Ranges"), "bytes", "avisa que acepta rangos");
assert.equal((await r.arrayBuffer()).byteLength, 100);

// una segunda subida no pisa la primera
ahora = 2000;
assert.deepEqual(await subir(autor, "e1", bytes(50, 9)), { version: 2000 });
assert.equal((await (await leer(autor, "e1", 1000)).arrayBuffer()).byteLength, 100, "la versión aprobada no cambia");
assert.equal((await (await leer(autor, "e1", 2000)).arrayBuffer()).byteLength, 50);

// sin versión, con una que no existe o con basura, no se sirve nada
assert.equal((await leer(autor, "e1")).status, 404, "sin ?v no hay clip");
assert.equal((await leer(autor, "e1", 3000)).status, 404, "una versión que no existe");
assert.equal((await leer(autor, "e1", "abc")).status, 404, "una versión que no es número");
assert.equal((await leer(autor, "e1", "1000/../2000")).status, 404, "ni una que intenta cambiar la ruta");
assert.equal((await leer("B".repeat(28), "e1", 1000)).status, 404, "ni bajo otro autor");

// por rangos: el reproductor pide un pedazo y recibe 206 con su Content-Range
r = await leer(autor, "e1", 1000, { Range: "bytes=0-9" });
assert.equal(r.status, 206, "un rango responde 206");
assert.equal(r.headers.get("Content-Range"), "bytes 0-9/100");
assert.equal((await r.arrayBuffer()).byteLength, 10);

// los límites: solo mp4, hasta 2 MiB
assert.equal((await subirClipBanco(peticion(bytes(10), "image/jpeg"), env, autor, "e2")).status, 415, "tipo no permitido");
assert.equal((await subirClipBanco(peticion(bytes(10), "video/quicktime"), env, autor, "e2")).status, 415, "mov tampoco");
assert.equal((await subirClipBanco(peticion(bytes(10), "video/mp4", MAX + 1), env, autor, "e2")).status, 413, "demasiado grande por Content-Length");
assert.equal((await subirClipBanco(peticion(bytes(MAX + 1)), env, autor, "e2")).status, 413, "demasiado grande por el cuerpo");
assert.equal((await subirClipBanco(peticion(bytes(0)), env, autor, "e2")).status, 413, "vacío");
assert.equal((await subirClipBanco(peticion(bytes(MAX)), env, autor, "e2")).status, 200, "justo 2 MiB sí");

console.log("clipBanco: todas las pruebas pasaron");
