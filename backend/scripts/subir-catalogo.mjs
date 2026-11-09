#!/usr/bin/env node
// Sube a R2 la carpeta que escribe `importar-catalogo.mjs`:
//
//   node scripts/subir-catalogo.mjs <carpeta> [--remote] [--concurrencia 8]
//
// Sin `--remote` solo imprime el plan (cuántos archivos y los primeros y últimos): no toca nada. Con `--remote` sube cada archivo con
// `wrangler r2 object put appsmobile/{clave} --remote`, en paralelo, y anota cada clave subida en `<carpeta>/subidos.txt`: si se corta, se
// vuelve a correr y se salta lo ya subido. `version.json` va siempre al final y solo si todo lo anterior salió bien.
// ponytail: un `wrangler` por archivo (esta versión no trae `r2 bulk`): unos 2 s cada uno, así que unos 17.000 archivos con 8 a la vez tardan
// cerca de una hora. Con credenciales S3 de R2 (rclone) sería mucho más rápido.
import { execFile } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { planDeSubida } from "./catalogo.lib.mjs";

const BACKEND = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const BUCKET = "appsmobile";
const argumentos = process.argv.slice(2);
const remoto = argumentos.includes("--remote");
const iConc = argumentos.indexOf("--concurrencia");
const concurrencia = iConc >= 0 ? Number(argumentos[iConc + 1]) : 8;
const carpeta = argumentos.find((a, i) => !a.startsWith("--") && argumentos[i - 1] !== "--concurrencia");
if (!carpeta || !fs.existsSync(carpeta)) {
  console.error("Uso: node scripts/subir-catalogo.mjs <carpeta del importador> [--remote] [--concurrencia 8]");
  process.exit(1);
}
if (!Number.isInteger(concurrencia) || concurrencia < 1 || concurrencia > 32) {
  console.error("La concurrencia tiene que ser un entero de 1 a 32.");
  process.exit(1);
}

const plan = planDeSubida(carpeta);
if (plan.at(-1)?.clave !== "catalogo/version.json") {
  console.error("La carpeta no trae version.json: corre primero el importador.");
  process.exit(1);
}
const registro = path.join(carpeta, "subidos.txt");
const hechos = new Set(fs.existsSync(registro) ? fs.readFileSync(registro, "utf8").split("\n").filter(Boolean) : []);
const pendientes = plan.filter((p) => !hechos.has(p.clave));
const version = pendientes.find((p) => p.clave === "catalogo/version.json");
const resto = pendientes.filter((p) => p !== version);

console.log(`Plan: ${plan.length} archivos · ya subidos: ${plan.length - pendientes.length} · pendientes: ${pendientes.length}`);
console.log(`Primero: ${plan[0].clave} · último: ${plan.at(-1).clave}`);
if (!remoto) {
  console.log("Simulacro: no se sube nada (usa --remote para subir).");
  process.exit(0);
}

const subir = (p) =>
  new Promise((resolver) => {
    execFile(
      "npx",
      ["wrangler", "r2", "object", "put", `${BUCKET}/${p.clave}`, "--file", p.archivo, "--content-type", p.tipo, "--remote"],
      { cwd: BACKEND, shell: true, windowsHide: true },
      (error, _salida, errores) => resolver(error ? { p, error: String(errores || error).trim().split("\n").pop() } : { p }),
    );
  });

let subidos = 0;
const fallos = [];
let siguiente = 0;
const trabajador = async () => {
  while (siguiente < resto.length && fallos.length === 0) {
    const p = resto[siguiente++];
    // un corte de red suelto no debe detener 17 mil archivos: hasta 3 intentos por archivo (subir de nuevo el mismo objeto no daña nada)
    let r = await subir(p);
    for (let intento = 1; r.error && intento < 3; intento++) r = await subir(p);
    if (r.error) fallos.push(r);
    else {
      fs.appendFileSync(registro, p.clave + "\n");
      if (++subidos % 200 === 0) console.log(`  ${subidos}/${resto.length}`);
    }
  }
};
await Promise.all(Array.from({ length: concurrencia }, trabajador));

if (fallos.length) {
  console.error(`Se detuvo en el primer fallo (${fallos[0].p.clave}): ${fallos[0].error}`);
  console.error("version.json NO se subió. Vuelve a correr el mismo comando para continuar.");
  process.exit(1);
}
if (version) {
  const r = await subir(version);
  if (r.error) {
    console.error(`No se pudo subir version.json: ${r.error}`);
    process.exit(1);
  }
  fs.appendFileSync(registro, version.clave + "\n");
}
console.log(`Listo: ${subidos} archivos y version.json subidos a ${BUCKET}/catalogo/.`);
