// Prueba de las rutas /catalogo/* del Worker con un R2 de mentira: `npm run test:catalogoRutas` (Node 22). El enrutado exige la sesión antes
// de llegar aquí (index.ts); esta prueba cubre qué claves se sirven, con qué tipo y caché, los rangos y que nada raro llegue al R2.
import assert from "node:assert/strict";
import { leerCatalogo } from "../src/routes/catalogo.ts";

class R2Falso {
  objetos = new Map();
  lecturas = [];
  async get(clave, opciones) {
    this.lecturas.push(clave);
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

const r2 = new R2Falso();
const env = { FOTOS: r2 };
const poner = (clave, texto, tipo) => r2.objetos.set(clave, { cuerpo: new TextEncoder().encode(texto), tipo });
poner("catalogo/version.json", '{"version":3,"total":2}', "application/json");
poner("catalogo/indice-3.json", "[]", "application/json");
poner("catalogo/ficha/0025.json", '{"id":"0025"}', "application/json");
poner("catalogo/foto/0025.webp", "webp", "image/webp");
poner("catalogo/clip/0025.mp4", "0123456789abcdef", "video/mp4");
poner("catalogo/clip/10189.mp4", "x", undefined); // sin tipo guardado

const leer = (ruta, cabeceras = {}) => leerCatalogo(new Request(`http://x/catalogo/${ruta}`, { headers: cabeceras }), env, ruta);

// la versión se pregunta siempre: no se guarda
let r = await leer("version.json");
assert.equal(r.status, 200);
assert.equal(r.headers.get("Content-Type"), "application/json");
assert.match(r.headers.get("Cache-Control"), /private/);
assert.match(r.headers.get("Cache-Control"), /no-cache/);
assert.equal(r.headers.get("X-Content-Type-Options"), "nosniff");
assert.deepEqual(await r.json(), { version: 3, total: 2 });

// lo demás no cambia: privado (solo el teléfono) y para siempre
for (const [ruta, tipo] of [["indice-3.json", "application/json"], ["ficha/0025.json", "application/json"], ["foto/0025.webp", "image/webp"], ["clip/0025.mp4", "video/mp4"]]) {
  r = await leer(ruta);
  assert.equal(r.status, 200, ruta);
  assert.equal(r.headers.get("Content-Type"), tipo, ruta);
  assert.match(r.headers.get("Cache-Control"), /private, max-age=31536000, immutable/, ruta);
  assert.equal(r.headers.get("X-Content-Type-Options"), "nosniff", ruta);
}

// el clip se sirve por rangos; lo demás no promete rangos
r = await leer("clip/0025.mp4", { Range: "bytes=0-9" });
assert.equal(r.status, 206);
assert.equal(r.headers.get("Content-Range"), "bytes 0-9/16");
assert.equal(r.headers.get("Accept-Ranges"), "bytes");
assert.equal((await r.text()).length, 10);
assert.equal((await leer("foto/0025.webp")).headers.get("Accept-Ranges"), null);

// sin tipo guardado cae al de la ruta
assert.equal((await leer("clip/10189.mp4")).headers.get("Content-Type"), "video/mp4");

// lo que no existe es 404
assert.equal((await leer("ficha/9999.json")).status, 404);
assert.equal((await leer("indice-99.json")).status, 404);

// lo mal formado es 404 y ni se pregunta al R2
const antes = r2.lecturas.length;
for (const mala of ["ficha/../x.json", "ficha/ab.json", "ficha/123456.json", "ficha/00a5.json", "clip/0025.mp4/extra", "indice-abc.json", "indice-1234567890.json", "foto/0025.png", "foto/0025.webp?x", "../secreto", "", "version.json/", "clip/0025.MP4", "ficha/0025.json "]) {
  assert.equal((await leer(mala)).status, 404, `«${mala}»`);
}
assert.equal(r2.lecturas.length, antes, "una ruta mal formada no llega al R2");

console.log("catalogoRutas: todas las pruebas pasaron");
