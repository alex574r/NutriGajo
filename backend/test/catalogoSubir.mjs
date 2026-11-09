// Prueba del plan de subida del catálogo a R2 (`node test/catalogoSubir.mjs`, Node 22): qué archivos de la carpeta del importador se suben, con qué
// clave y tipo, y en qué orden. Lo que importa: `version.json` va siempre al final (con él el catálogo «existe» para los teléfonos).
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { planDeSubida } from "../scripts/catalogo.lib.mjs";

const carpeta = fs.mkdtempSync(path.join(os.tmpdir(), "catalogo-"));
const poner = (ruta, contenido = "x") => {
  fs.mkdirSync(path.dirname(path.join(carpeta, ruta)), { recursive: true });
  fs.writeFileSync(path.join(carpeta, ruta), contenido);
};
for (const id of ["0025", "0026", "10189"]) {
  poner(`ficha/${id}.json`);
  poner(`foto/${id}.webp`);
  poner(`clip/${id}.mp4`);
}
poner("indice-20261107.json");
poner("version.json", '{"version":20261107,"total":3}');
// lo ajeno no se sube: notas, el registro de lo ya subido, un json suelto y un archivo con extensión parecida
poner("notas.txt");
poner("subidos.txt");
poner("raro.json");
poner("foto/0025.png");
poner("ficha/abc.json");

const plan = planDeSubida(carpeta);
assert.equal(plan.length, 11, "9 por ejercicio, el índice y la versión");
assert.equal(plan.at(-1).clave, "catalogo/version.json", "la versión va al final");
assert.equal(plan.at(-2).clave, "catalogo/indice-20261107.json", "el índice, justo antes");
assert.ok(plan.every((p) => p.clave.startsWith("catalogo/")), "todo cuelga de catalogo/");
const foto = plan.find((p) => p.clave === "catalogo/foto/0025.webp");
assert.equal(foto.tipo, "image/webp");
assert.equal(foto.archivo, path.join(carpeta, "foto", "0025.webp"));
assert.equal(plan.find((p) => p.clave === "catalogo/clip/10189.mp4").tipo, "video/mp4");
assert.equal(plan.find((p) => p.clave === "catalogo/ficha/0026.json").tipo, "application/json");
for (const ajeno of ["catalogo/notas.txt", "catalogo/subidos.txt", "catalogo/raro.json", "catalogo/foto/0025.png", "catalogo/ficha/abc.json"]) {
  assert.ok(!plan.some((p) => p.clave === ajeno), `${ajeno} no se sube`);
}
assert.equal(new Set(plan.map((p) => p.clave)).size, plan.length, "sin claves repetidas");

fs.rmSync(carpeta, { recursive: true, force: true });
console.log("catalogoSubir: todas las pruebas pasaron");
