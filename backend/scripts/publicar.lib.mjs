// La parte pura del script que publica un APK (scripts/publicar-apk.mjs): qué APK se acepta y qué manifiesto se arma. Los límites son los de
// `actualizacionDeJson` de la app (app/.../domain/Actualizacion.kt): un manifiesto que la app descartaría no se publica.
export const PAQUETE = "com.example.nutrigajo";
// El certificado (SHA-256) con el que está firmado el release que ya reparte la app: una actualización firmada con otra llave (la de debug,
// por ejemplo) no la aceptaría ningún teléfono.
export const CERTIFICADO_RELEASE = "87807ca1881078dc124af1a8d6da07cfe585bb5de68e27823bc097423793df3f";
export const MAX_APK_BYTES = 200 * 1024 * 1024;

const SHA256 = /^[0-9a-f]{64}$/;
const entero = (v) => typeof v === "number" && Number.isInteger(v);

// El paquete y la versión del APK, de la salida de `aapt2 dump badging`.
export function leerBadging(texto) {
  const m = /package: name='([^']+)' versionCode='(\d+)' versionName='([^']*)'/.exec(texto);
  if (!m) throw new Error("No se pudo leer el package de la salida de aapt2 dump badging");
  return { paquete: m[1], versionCode: Number(m[2]), versionName: m[3] };
}

// El certificado de firma, de la salida de `apksigner verify --print-certs`.
export function leerCertificado(texto) {
  const huellas = [...texto.matchAll(/certificate SHA-256 digest: ([0-9a-f]{64})/g)].map((m) => m[1]);
  if (huellas.length === 0) throw new Error("No se encontró el certificado en la salida de apksigner");
  if (new Set(huellas).size > 1) throw new Error("El APK tiene más de un firmante distinto");
  return huellas[0];
}

// El manifiesto `apk/ultima.json`. Lanza si algo no cumple los límites (la app lo descartaría).
export function armarManifiesto({ versionCode, versionName, sha256, bytes, notas = [], minVersionCode = 0, ahora }) {
  if (!entero(versionCode) || versionCode <= 0) throw new Error("versionCode debe ser un entero mayor que 0");
  if (typeof versionName !== "string" || versionName.trim() === "" || versionName.length > 40) throw new Error("versionName debe tener de 1 a 40 caracteres");
  if (typeof sha256 !== "string" || !SHA256.test(sha256)) throw new Error("sha256 debe ser de 64 hexadecimales en minúsculas");
  if (!entero(bytes) || bytes < 1 || bytes > MAX_APK_BYTES) throw new Error(`bytes debe estar entre 1 y ${MAX_APK_BYTES}`);
  if (!Array.isArray(notas) || notas.length > 10 || notas.some((n) => typeof n !== "string" || n.length > 200)) {
    throw new Error("notas: hasta 10 textos de hasta 200 caracteres");
  }
  if (!entero(minVersionCode) || minVersionCode < 0 || minVersionCode > versionCode) throw new Error("minVersionCode debe estar entre 0 y versionCode");
  if (!entero(ahora) || ahora < 0) throw new Error("ahora debe ser un entero");
  return { versionCode, versionName, sha256, bytes, notas, minVersionCode, publicadaEn: ahora };
}

// Lo que impide publicar por error: otro paquete, otra llave de firma o una versión que no sube. `publicada` es el versionCode ya publicado (null si no hay).
export function comprobarPublicable({ paquete, certificadoSha256, versionCode, publicada }) {
  if (paquete !== PAQUETE) throw new Error(`El paquete es ${paquete}, no ${PAQUETE}`);
  if (certificadoSha256 !== CERTIFICADO_RELEASE) {
    throw new Error(`El certificado del APK (${certificadoSha256}) no es el del release (${CERTIFICADO_RELEASE}): ningún teléfono con la app instalada lo aceptaría`);
  }
  if (publicada !== null && versionCode <= publicada) throw new Error(`La versión ${versionCode} no es mayor que la ya publicada (${publicada})`);
}
