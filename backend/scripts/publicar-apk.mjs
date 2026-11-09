// Publica un APK para las actualizaciones de la app: `npm run publicar-apk -- <ruta del APK> [--notas "una;otra"] [--minima N] [--solo-mostrar]`.
//  · lee del APK el paquete y la versión (aapt2) y la firma (apksigner), y rechaza lo que no sea el release de esta app o no suba de versión;
//  · calcula el SHA-256 y el tamaño y arma `apk/ultima.json`;
//  · sin --solo-mostrar, sube a R2 (bucket `appsmobile`, con `wrangler r2 object put --remote`) primero el APK y después el manifiesto:
//    el manifiesto es lo último que cambia, así nadie ve una versión cuyo APK todavía no está.
// --minima N hace obligatoria la actualización para quien tenga una versión menor que N (por defecto nadie queda forzado).
// Usa las herramientas del SDK de Android (ANDROID_HOME, o la carpeta de siempre del usuario) y Java para apksigner (JAVA_HOME).
import { execFileSync } from "node:child_process";
import { createHash } from "node:crypto";
import { existsSync, readdirSync, readFileSync, statSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { armarManifiesto, comprobarPublicable, leerBadging, leerCertificado } from "./publicar.lib.mjs";

const BUCKET = "appsmobile";
const BASE = process.env.WORKER_URL ?? "https://nutrigajo-api.alex574r.workers.dev";
const backend = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const windows = process.platform === "win32";

const argumentos = process.argv.slice(2);
const opcion = (nombre) => {
  const i = argumentos.indexOf(nombre);
  return i === -1 ? undefined : argumentos[i + 1];
};
const soloMostrar = argumentos.includes("--solo-mostrar");
const apk = argumentos.find((a, i) => !a.startsWith("--") && !(i > 0 && ["--notas", "--minima"].includes(argumentos[i - 1])));
if (!apk || !existsSync(apk)) {
  console.error("Uso: npm run publicar-apk -- <ruta del APK> [--notas \"una;otra\"] [--minima N] [--solo-mostrar]");
  process.exit(2);
}
const notas = (opcion("--notas") ?? "").split(";").map((n) => n.trim()).filter(Boolean);
const minima = opcion("--minima") === undefined ? 0 : Number(opcion("--minima"));

function herramientas() {
  const sdk = process.env.ANDROID_HOME ?? process.env.ANDROID_SDK_ROOT ?? join(process.env.LOCALAPPDATA ?? "", "Android", "Sdk");
  const carpeta = join(sdk, "build-tools");
  if (!existsSync(carpeta)) throw new Error(`No encuentro las build-tools del SDK en ${carpeta} (define ANDROID_HOME)`);
  const version = readdirSync(carpeta).sort().at(-1);
  return { aapt2: join(carpeta, version, windows ? "aapt2.exe" : "aapt2"), apksigner: join(carpeta, version, windows ? "apksigner.bat" : "apksigner") };
}

// Lo que ya está publicado (el versionCode), o null si aún no hay nada.
async function publicada() {
  const r = await fetch(`${BASE}/app/ultima`);
  if (r.status === 404) return null;
  if (!r.ok) throw new Error(`El Worker respondió ${r.status} al pedir la versión publicada`);
  const m = await r.json();
  return typeof m.versionCode === "number" ? m.versionCode : null;
}

const { aapt2, apksigner } = herramientas();
const entorno = { ...process.env, JAVA_HOME: process.env.JAVA_HOME ?? "C:\\Program Files\\Android\\Android Studio\\jbr" };
const badging = leerBadging(execFileSync(aapt2, ["dump", "badging", apk], { encoding: "utf8", maxBuffer: 64 * 1024 * 1024 }));
// si la firma no verifica, apksigner sale con error y se aborta aquí
const certificado = leerCertificado(execFileSync(`"${apksigner}"`, ["verify", "--print-certs", apk], { encoding: "utf8", shell: true, env: entorno }));
const yaPublicada = await publicada();
comprobarPublicable({ paquete: badging.paquete, certificadoSha256: certificado, versionCode: badging.versionCode, publicada: yaPublicada });

const manifiesto = armarManifiesto({
  versionCode: badging.versionCode,
  versionName: badging.versionName,
  sha256: createHash("sha256").update(readFileSync(apk)).digest("hex"),
  bytes: statSync(apk).size,
  notas,
  minVersionCode: minima,
  ahora: Date.now(),
});
console.log(`APK ${apk}\nya publicada: ${yaPublicada ?? "ninguna"}  ->  ${manifiesto.versionCode} (${manifiesto.versionName})\n${JSON.stringify(manifiesto, null, 2)}`);
if (soloMostrar) {
  console.log("\n--solo-mostrar: no se subió nada.");
  process.exit(0);
}

const subir = (clave, archivo, tipo) =>
  execFileSync("npx", ["wrangler", "r2", "object", "put", `${BUCKET}/${clave}`, "--file", archivo, "--content-type", tipo, "--remote"], { stdio: "inherit", shell: true, cwd: backend });
subir(`apk/${manifiesto.versionCode}.apk`, apk, "application/vnd.android.package-archive");
const archivoManifiesto = join(tmpdir(), "ultima.json");
writeFileSync(archivoManifiesto, JSON.stringify(manifiesto));
subir("apk/ultima.json", archivoManifiesto, "application/json");
console.log(`\nPublicada la versión ${manifiesto.versionCode}. Comprobando: ${(await fetch(`${BASE}/app/ultima`)).status} en ${BASE}/app/ultima`);
