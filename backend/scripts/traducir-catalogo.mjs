#!/usr/bin/env node
// Traduce al español la descripción y los pasos de las fichas del catálogo (lo que dejó `importar-catalogo.mjs`) con la Batches API de Claude;
// las reglas (lotes, petición, validación de la respuesta) están en traducir.lib.mjs:
//
//   node scripts/traducir-catalogo.mjs <carpeta del importador> <repo de openGym> <carpeta de salida> [--modelo ID] [--lotes N] [--version N] [--enviar | --recoger]
//
// Sin `--enviar` ni `--recoger` es un simulacro: cuenta, estima tokens y costo por modelo, y no llama a la API ni escribe nada.
// `--enviar` crea el lote en la API (hasta `--lotes N` peticiones, para una prueba) y anota su id en `<salida>/traduccion.json`.
// `--recoger` consulta el lote y, si ya terminó, escribe en `<salida>`: ficha/{id}.json de TODAS las fichas (traducida o, si falló, tal cual
// estaba), indice-{versión}.json (copia del índice, que no cambia), fallidos.json, muestra.md (20 al azar, inglés contra español) y, al final,
// version.json. Siempre parte de la carpeta del importador, nunca de la salida: se puede repetir. `subir-catalogo.mjs <salida>` sube esa carpeta.
// Sin clave de API: `--exportar` escribe cada lote en `<salida>/entrada/` (con INSTRUCCIONES.md), subagentes de Claude Code escriben su
// traducción en `<salida>/traducido/` y `--importar` la valida y escribe la misma salida que `--recoger`. `--reintentar` (con `--exportar`)
// exporta solo los lotes con ejercicios fallidos.
// Solo viaja texto (título como contexto, descripción y pasos): nunca las fotos ni los clips, que no pueden alimentar a una IA. La clave de API
// la toma el SDK de ANTHROPIC_API_KEY o del perfil de `ant auth login`.
import Anthropic from "@anthropic-ai/sdk";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { INSTRUCCIONES, PRECIOS, aplicarTraduccion, costoDeLote, crearLotes, itemsPorTraducir, leerResultadoDeLote, pedidoDeLote, resultadoDeTexto, tokensAprox } from "./traducir.lib.mjs";

const RAIZ_NUTRIGAJO = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const argumentos = process.argv.slice(2);
const conValor = ["--modelo", "--lotes", "--desde", "--version"];
const valorDe = (bandera) => {
  const i = argumentos.indexOf(bandera);
  return i >= 0 ? argumentos[i + 1] : undefined;
};
const [importador, openGym, salida] = argumentos.filter((a, i) => !a.startsWith("--") && !conValor.includes(argumentos[i - 1]));
// Haiku 5.5 por decisión del usuario (12 nov 2026): es traducción mecánica y sale en menos de un dólar; con --modelo se prueba otro.
const modelo = valorDe("--modelo") ?? "claude-haiku-5-5";
const maxLotes = valorDe("--lotes") === undefined ? Infinity : Number(valorDe("--lotes"));
const version = valorDe("--version") === undefined ? Number(new Date().toISOString().slice(0, 10).replaceAll("-", "")) : Number(valorDe("--version"));
const enviar = argumentos.includes("--enviar");
const recoger = argumentos.includes("--recoger");
const exportar = argumentos.includes("--exportar");
const importar = argumentos.includes("--importar");
const reintentar = argumentos.includes("--reintentar");

const fallar = (mensaje) => {
  console.error(mensaje);
  process.exit(1);
};
if (!importador || !openGym || !salida) fallar("Uso: node scripts/traducir-catalogo.mjs <carpeta del importador> <repo de openGym> <carpeta de salida> [--modelo ID] [--lotes N] [--version N] [--enviar | --recoger]");
if ([enviar, recoger, exportar, importar].filter(Boolean).length > 1) fallar("Elige solo uno: --enviar, --recoger, --exportar o --importar.");
if (!Number.isInteger(version) || version < 1 || version > 999_999_999) fallar("La versión tiene que ser un entero de 1 a 9 dígitos.");
if (!(maxLotes >= 1)) fallar("--lotes tiene que ser un número de 1 en adelante.");
const destino = path.resolve(salida);
if (destino === RAIZ_NUTRIGAJO || destino.startsWith(RAIZ_NUTRIGAJO + path.sep)) fallar("La carpeta de salida no puede estar dentro del repo de nutriGajo.");
const carpetaFichas = path.join(importador, "ficha");
const archivoEs = path.join(openGym, "catalogue", "i18n", "es.json");
if (!fs.existsSync(carpetaFichas)) fallar(`No encuentro ${carpetaFichas}: ¿es la carpeta que escribió importar-catalogo.mjs?`);
if (!fs.existsSync(archivoEs)) fallar(`No encuentro ${archivoEs}: ¿es el repo de openGym?`);

const leerJson = (ruta) => JSON.parse(fs.readFileSync(ruta, "utf8"));
const versionImportada = leerJson(path.join(importador, "version.json")).version;
if (version <= versionImportada) fallar(`La versión nueva (${version}) tiene que ser mayor que la del catálogo subido (${versionImportada}): usa --version.`);

const fichas = fs.readdirSync(carpetaFichas).filter((n) => /^\d{4,5}\.json$/.test(n)).sort().map((n) => leerJson(path.join(carpetaFichas, n)));
const es = leerJson(archivoEs);
const conPasosEnEspanol = new Set(Object.entries(es).filter(([, v]) => Array.isArray(v.instructions) && v.instructions.length).map(([id]) => id));
const items = itemsPorTraducir(fichas, conPasosEnEspanol);
const lotes = crearLotes(items);
// `--desde K` salta los primeros K lotes (para probar con lotes de otras partes del catálogo); `--lotes N` toma N a partir de ahí
const desde = valorDe("--desde") === undefined ? 0 : Number(valorDe("--desde"));
if (!Number.isInteger(desde) || desde < 0) fallar("--desde tiene que ser un entero de 0 en adelante.");
const seleccionados = lotes.slice(desde, desde + maxLotes);
const archivoEstado = path.join(destino, "traduccion.json");

try {
  if (enviar) await enviarLotes();
  else if (recoger) await recogerLotes();
  else if (exportar) exportarLotes();
  else if (importar) importarLotes();
  else simulacro();
} catch (error) {
  fallar(`Error: ${error.message}`);
}

function simulacro() {
  const enviados = seleccionados.flatMap((l) => l.items);
  const entrada = tokensAprox(enviados) + seleccionados.length * Math.ceil(INSTRUCCIONES.length / 4);
  const salidaTokens = Math.ceil(tokensAprox(enviados) * 1.1);
  console.log(`Fichas: ${fichas.length} · con pasos ya en español (no se envían): ${conPasosEnEspanol.size} · por traducir: ${items.length}`);
  console.log(`Lotes: ${lotes.length} de ${lotes[0]?.items.length} · a enviar: ${seleccionados.length}`);
  console.log(`Tokens aprox.: entrada ${entrada} · salida ${salidaTokens}`);
  for (const m of Object.keys(PRECIOS)) console.log(`  ${m}: ≈ US$ ${costoDeLote(entrada, salidaTokens, m).toFixed(2)} (en lote)`);
  console.log("Simulacro: no se llamó a la API ni se escribió nada (usa --enviar).");
}

async function enviarLotes() {
  if (fs.existsSync(archivoEstado)) fallar("Ya hay un lote en esa carpeta (traduccion.json): usa --recoger, o elige otra carpeta de salida.");
  const client = new Anthropic();
  const lote = await client.messages.batches.create({ requests: seleccionados.map((l) => pedidoDeLote(l, modelo)) });
  fs.mkdirSync(destino, { recursive: true });
  const estado = { batchId: lote.id, modelo, creado: new Date().toISOString(), lotes: seleccionados.map((l) => ({ id: l.id, ids: l.items.map((i) => i.id) })) };
  fs.writeFileSync(archivoEstado, JSON.stringify(estado, null, 2));
  console.log(`Lote enviado: ${lote.id} (${seleccionados.length} peticiones, modelo ${modelo}). Corre --recoger más tarde.`);
}

async function recogerLotes() {
  if (!fs.existsSync(archivoEstado)) fallar("No hay traduccion.json en esa carpeta: corre primero --enviar.");
  const estado = leerJson(archivoEstado);
  const client = new Anthropic();
  const lote = await client.messages.batches.retrieve(estado.batchId);
  if (lote.processing_status !== "ended") {
    const c = lote.request_counts;
    console.log(`Todavía no termina: ${c.processing} en proceso · ${c.succeeded} listas · ${c.errored} con error.`);
    return;
  }
  const porId = new Map(items.map((i) => [i.id, i]));
  const enviados = new Map(estado.lotes.map((l) => [l.id, { id: l.id, items: l.ids.map((id) => porId.get(id)) }]));
  const traducidas = new Map();
  const fallidos = new Set();
  const recibidos = new Set();
  for await (const resultado of await client.messages.batches.results(estado.batchId)) {
    const enviado = enviados.get(resultado.custom_id);
    if (!enviado) continue;
    recibidos.add(enviado.id);
    const leido = leerResultadoDeLote(resultado, enviado);
    leido.traducidas.forEach((t, id) => traducidas.set(id, t));
    leido.fallidos.forEach((id) => fallidos.add(id));
  }
  for (const [id, enviado] of enviados) if (!recibidos.has(id)) enviado.items.forEach((i) => fallidos.add(i.id));

  escribirSalida(traducidas, fallidos);
}

// Sin clave de API: los lotes se exportan como archivos, subagentes de Claude Code escriben su traducción y `--importar` la valida igual.
// `--reintentar` exporta solo los lotes que tienen ejercicios en fallidos.json (y borra su traducción vieja).
function exportarLotes() {
  const previos = reintentar && fs.existsSync(path.join(destino, "fallidos.json")) ? new Set(leerJson(path.join(destino, "fallidos.json"))) : null;
  const aExportar = previos ? seleccionados.filter((l) => l.items.some((i) => previos.has(i.id))) : seleccionados;
  fs.mkdirSync(path.join(destino, "entrada"), { recursive: true });
  fs.mkdirSync(path.join(destino, "traducido"), { recursive: true });
  fs.writeFileSync(path.join(destino, "INSTRUCCIONES.md"), `${instruccionesDeArchivos()}\n\n${INSTRUCCIONES}\n`);
  for (const l of aExportar) {
    fs.writeFileSync(path.join(destino, "entrada", `${l.id}.json`), JSON.stringify(l.items));
    fs.rmSync(path.join(destino, "traducido", `${l.id}.json`), { force: true });
  }
  console.log(`Exportados ${aExportar.length} lotes en ${path.join(destino, "entrada")}: ${aExportar[0]?.id ?? "-"} … ${aExportar.at(-1)?.id ?? "-"}.`);
}

function importarLotes() {
  const exportados = lotes.filter((l) => fs.existsSync(path.join(destino, "entrada", `${l.id}.json`)));
  if (exportados.length === 0) fallar("No hay lotes exportados en esa carpeta: corre primero --exportar.");
  const traducidas = new Map();
  const fallidos = new Set();
  for (const l of exportados) {
    const archivo = path.join(destino, "traducido", `${l.id}.json`);
    const leido = fs.existsSync(archivo)
      ? leerResultadoDeLote(resultadoDeTexto(l.id, fs.readFileSync(archivo, "utf8")), l)
      : { traducidas: new Map(), fallidos: l.items.map((i) => i.id) };
    leido.traducidas.forEach((t, id) => traducidas.set(id, t));
    leido.fallidos.forEach((id) => fallidos.add(id));
  }
  escribirSalida(traducidas, fallidos);
}

// Lo que se escribe en la carpeta de salida (ver la cabecera). Siempre parte de las fichas del importador: se puede repetir.
function escribirSalida(traducidas, fallidos) {
  const indice = fs.readdirSync(importador).find((n) => /^indice-[0-9]{1,9}\.json$/.test(n));
  if (!indice) fallar(`No encuentro indice-N.json en ${importador}.`);
  fs.mkdirSync(path.join(destino, "ficha"), { recursive: true });
  for (const f of fichas) fs.writeFileSync(path.join(destino, "ficha", `${f.id}.json`), JSON.stringify(aplicarTraduccion(f, traducidas.get(f.id))));
  fs.copyFileSync(path.join(importador, indice), path.join(destino, `indice-${version}.json`));
  fs.writeFileSync(path.join(destino, "fallidos.json"), JSON.stringify([...fallidos]));
  fs.writeFileSync(path.join(destino, "muestra.md"), muestra(fichas, traducidas));
  // lo último: con esto el catálogo «existe» para quien lo consulte
  fs.writeFileSync(path.join(destino, "version.json"), JSON.stringify({ version, total: fichas.length }));

  console.log(`Traducidas ${traducidas.size} de ${fichas.length} · fallidas ${fallidos.size} · versión ${version} · salida ${destino}`);
  if (traducidas.size < fichas.length) console.log("Incompleto: no subas esta carpeta.");
}

// (función y no constante: el bloque de arriba corre antes de que se evalúen las constantes de aquí abajo)
function instruccionesDeArchivos() {
  return `# Cómo traducir un lote

Cada archivo \`entrada/lote-NNNN.json\` es un arreglo JSON de ejercicios { id, titulo, descripcion, pasos }. Para cada lote que te toque, lee su archivo y escribe \`traducido/lote-NNNN.json\` con SOLO este JSON válido (sin texto alrededor, sin bloques de código): { "traducciones": [ { "id", "descripcion", "pasos" } ] }, un elemento por ejercicio recibido. Escapa bien las comillas dentro del texto. Las reglas de traducción están abajo.`;
}

// 20 ejercicios traducidos al azar, inglés contra español, para revisarlos a ojo.
function muestra(todas, traducidas) {
  const elegidas = todas.filter((f) => traducidas.has(f.id)).sort(() => Math.random() - 0.5).slice(0, 20);
  return elegidas
    .map((f) => {
      const t = aplicarTraduccion(f, traducidas.get(f.id));
      return `## ${f.id} · ${f.titulo}\n\n**EN** ${f.descripcion}\n- ${f.pasos[0] ?? ""}\n\n**ES** ${t.descripcion}\n- ${t.pasos[0] ?? ""}\n`;
    })
    .join("\n");
}
