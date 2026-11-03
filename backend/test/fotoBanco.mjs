// Prueba de las fotos de los bancos globales (ejercicios y platillos) con un R2 de mentira: `npm run test:foto` (Node 22 o más nuevo, que
// lee TypeScript directamente). Comprueba lo que protege lo ya revisado: una foto subida nunca pisa a otra y solo se lee con su versión.
import assert from "node:assert/strict";
import { leerFotoBanco, subirFotoBanco } from "../src/routes/fotoBanco.ts";

class R2Falso {
  objetos = new Map();
  async put(clave, cuerpo, opciones) {
    this.objetos.set(clave, { cuerpo, tipo: opciones?.httpMetadata?.contentType });
  }
  async get(clave) {
    const o = this.objetos.get(clave);
    if (!o) return null;
    return {
      body: o.cuerpo,
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

const peticion = (texto, tipo = "image/jpeg", largo = texto.length) =>
  new Request("http://x/platillo/p1", { method: "PUT", body: texto, headers: { "Content-Type": tipo, "Content-Length": String(largo) } });
const subir = async (uid, id, texto, carpeta = "platillos") => (await subirFotoBanco(peticion(texto), env, uid, id, carpeta)).json();
const leer = (autor, id, version, carpeta = "platillos") =>
  leerFotoBanco(new Request(`http://x/platillo/${autor}/${id}${version === undefined ? "" : `?v=${version}`}`), env, autor, id, carpeta);
const texto = async (respuesta) => await respuesta.text();

const autor = "A".repeat(28);

// subir devuelve la versión y esa versión lee la foto
ahora = 1000;
assert.deepEqual(await subir(autor, "p1", "foto-1"), { version: 1000 }, "la versión es la hora de la subida");
let r = await leer(autor, "p1", 1000);
assert.equal(r.status, 200, "se lee con su versión");
assert.equal(await texto(r), "foto-1");
assert.match(r.headers.get("Cache-Control"), /immutable/, "con versión se guarda para siempre");

// una segunda subida no pisa la primera: lo que el admin aprobó con la versión 1000 sigue siendo esa foto
ahora = 2000;
assert.deepEqual(await subir(autor, "p1", "foto-2"), { version: 2000 });
assert.equal(await texto(await leer(autor, "p1", 1000)), "foto-1", "la versión aprobada no cambia aunque el autor suba otra");
assert.equal(await texto(await leer(autor, "p1", 2000)), "foto-2", "la nueva se lee con su versión");

// sin versión no hay nada que servir de lo nuevo, y una versión que no existe tampoco
assert.equal((await leer(autor, "p1")).status, 404, "sin ?v no se sirve lo versionado");
assert.equal((await leer(autor, "p1", 3000)).status, 404, "una versión que no existe");
assert.equal((await leer(autor, "p1", "../../otro")).status, 404, "una versión con basura no sale de la carpeta");
assert.equal((await leer(autor, "p1", "1000/../2000")).status, 404, "ni una que intenta cambiar la ruta");

// las carpetas y los autores no se mezclan
assert.equal((await leer(autor, "p1", 1000, "ejercicios")).status, 404, "lo de platillos no aparece en ejercicios");
assert.equal((await leer("B".repeat(28), "p1", 1000)).status, 404, "ni bajo otro autor");

// una foto subida antes de que hubiera versiones (la clave vieja) se sigue sirviendo y no se puede pisar
env.FOTOS.objetos.set(`ejercicios/${autor}/e1`, { cuerpo: "vieja", tipo: "image/jpeg" });
assert.equal(await texto(await leer(autor, "e1", 555, "ejercicios")), "vieja", "la foto anterior se sirve con cualquier versión");
assert.equal(await texto(await leer(autor, "e1", undefined, "ejercicios")), "vieja", "y sin versión, como siempre");
ahora = 4000;
await subir(autor, "e1", "nueva", "ejercicios");
assert.equal(env.FOTOS.objetos.get(`ejercicios/${autor}/e1`).cuerpo, "vieja", "subir una nueva no toca la clave vieja");
assert.equal(await texto(await leer(autor, "e1", 4000, "ejercicios")), "nueva");

// los límites de siempre
assert.equal((await subirFotoBanco(peticion("x", "text/html"), env, autor, "p2", "platillos")).status, 415, "tipo no permitido");
assert.equal((await subirFotoBanco(peticion("x", "image/jpeg", 4 * 1024 * 1024), env, autor, "p2", "platillos")).status, 413, "demasiado grande");

console.log("fotoBanco: todas las pruebas pasaron");
