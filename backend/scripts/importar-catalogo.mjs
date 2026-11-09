#!/usr/bin/env node
// Importa el catálogo de openGym a una carpeta lista para subir a R2 (las reglas de cada campo están en catalogo.lib.mjs):
//
//   node scripts/importar-catalogo.mjs <repo de openGym> <carpeta de salida> [--version N]
//
// Escribe en la salida (fuera del repo de nutriGajo: los medios tienen licencia propia y no van a git):
//   ficha/{id}.json   foto/{id}.webp   clip/{id}.mp4   indice-{N}.json   y, al final de todo, version.json ({ version, total })
// Solo entran las entradas principales (sin `variantOf`) que tienen foto y clip; el resto se anota como saltado. `N` es la versión del
// catálogo (por defecto la fecha como YYYYMMDD): el índice la lleva en el nombre y `version.json` se escribe al final, así una corrida a
// medias nunca deja a un teléfono con una versión sin su índice.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { crearIndice, fichaDe } from "./catalogo.lib.mjs";

const RAIZ_NUTRIGAJO = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const argumentos = process.argv.slice(2);
const posicionales = argumentos.filter((a, i) => !a.startsWith("--") && argumentos[i - 1] !== "--version");
const [origen, salida] = posicionales;
const iVersion = argumentos.indexOf("--version");
const version = iVersion >= 0 ? Number(argumentos[iVersion + 1]) : Number(new Date().toISOString().slice(0, 10).replaceAll("-", ""));

const fallar = (mensaje) => {
  console.error(mensaje);
  process.exit(1);
};
if (!origen || !salida) fallar("Uso: node scripts/importar-catalogo.mjs <repo de openGym> <carpeta de salida> [--version N]");
if (!Number.isInteger(version) || version < 1 || version > 999_999_999) fallar("La versión tiene que ser un entero de 1 a 9 dígitos.");
const destino = path.resolve(salida);
if (destino === RAIZ_NUTRIGAJO || destino.startsWith(RAIZ_NUTRIGAJO + path.sep)) fallar("La carpeta de salida no puede estar dentro del repo de nutriGajo (los medios no van a git).");

const catalogo = path.join(origen, "catalogue");
const carpetaEntradas = path.join(catalogo, "exercises");
if (!fs.existsSync(carpetaEntradas)) fallar(`No encuentro ${carpetaEntradas}: ¿es el repo de openGym?`);
const leerJson = (ruta) => JSON.parse(fs.readFileSync(ruta, "utf8"));
const es = fs.existsSync(path.join(catalogo, "i18n", "es.json")) ? leerJson(path.join(catalogo, "i18n", "es.json")) : {};

for (const sub of ["ficha", "foto", "clip"]) fs.mkdirSync(path.join(destino, sub), { recursive: true });

const fichas = [];
const saltados = [];
let fotos = 0;
let clips = 0;
for (const archivo of fs.readdirSync(carpetaEntradas).sort()) {
  if (!/^\d{4,5}\.json$/.test(archivo)) continue;
  const ficha = fichaDe(leerJson(path.join(carpetaEntradas, archivo)), es);
  if (!ficha) continue;
  const foto = path.join(catalogo, "media", "still", `${ficha.id}.webp`);
  const clip = path.join(catalogo, "media", "clip", `${ficha.id}.mp4`);
  if (!fs.existsSync(foto) || !fs.existsSync(clip)) {
    saltados.push(ficha.id);
    continue;
  }
  fs.writeFileSync(path.join(destino, "ficha", `${ficha.id}.json`), JSON.stringify(ficha));
  fs.copyFileSync(foto, path.join(destino, "foto", `${ficha.id}.webp`));
  fs.copyFileSync(clip, path.join(destino, "clip", `${ficha.id}.mp4`));
  fotos++;
  clips++;
  fichas.push(ficha);
}
if (fichas.length === 0) fallar("No se escribió ninguna ficha.");

fs.writeFileSync(path.join(destino, `indice-${version}.json`), JSON.stringify(crearIndice(fichas)));
// lo último: con esto el catálogo «existe» para quien lo consulte
fs.writeFileSync(path.join(destino, "version.json"), JSON.stringify({ version, total: fichas.length }));

console.log(`Fichas: ${fichas.length} · fotos: ${fotos} · clips: ${clips} · versión ${version}`);
console.log(saltados.length ? `Saltados por falta de foto o clip (${saltados.length}): ${saltados.join(", ")}` : "Saltados: ninguno");
console.log(`Salida: ${destino}`);
