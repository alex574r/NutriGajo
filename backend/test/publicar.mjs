// Prueba de la parte pura del script que publica un APK (scripts/publicar.lib.mjs): `npm run test:publicar`. Lo que sube y a dónde lo prueba
// `--solo-mostrar` a mano; aquí se comprueba lo que protege: qué APK se acepta y qué manifiesto se arma.
import assert from "node:assert/strict";
import { armarManifiesto, CERTIFICADO_RELEASE, comprobarPublicable, leerBadging, leerCertificado, MAX_APK_BYTES, PAQUETE } from "../scripts/publicar.lib.mjs";

// ---- aapt2 dump badging (salida real sobre el release)
const badging = `package: name='com.example.nutrigajo' versionCode='1' versionName='1.0' platformBuildVersionName='17' platformBuildVersionCode='37' compileSdkVersion='37' compileSdkVersionCodename='17'
minSdkVersion:'29'
targetSdkVersion:'36'`;
assert.deepEqual(leerBadging(badging), { paquete: "com.example.nutrigajo", versionCode: 1, versionName: "1.0" });
assert.deepEqual(leerBadging("package: name='otra.app' versionCode='12' versionName='2.5-beta'\n"), { paquete: "otra.app", versionCode: 12, versionName: "2.5-beta" });
assert.throws(() => leerBadging("nada que ver"), /badging/, "sin la línea package");
assert.throws(() => leerBadging("package: name='x' versionName='1.0'"), /badging/, "sin versionCode");

// ---- apksigner verify --print-certs
const firma = `Verifies
Number of signers: 1
Signer #1 certificate DN: CN=Nutri Gajo, O=Nutri Gajo, C=MX
Signer #1 certificate SHA-256 digest: ${CERTIFICADO_RELEASE}
Signer #1 certificate SHA-1 digest: 1111111111111111111111111111111111111111`;
assert.equal(leerCertificado(firma), CERTIFICADO_RELEASE);
assert.throws(() => leerCertificado("Verifies\nNumber of signers: 0"), /certificado/, "sin certificado");
assert.throws(
  () => leerCertificado(`Signer #1 certificate SHA-256 digest: ${"a".repeat(64)}\nSigner #2 certificate SHA-256 digest: ${"b".repeat(64)}`),
  /firmante/,
  "con dos firmantes distintos no se sabe de quién es",
);
assert.equal(CERTIFICADO_RELEASE, "87807ca1881078dc124af1a8d6da07cfe585bb5de68e27823bc097423793df3f", "el certificado del release que ya reparte la app");
assert.equal(PAQUETE, "com.example.nutrigajo");

// ---- el manifiesto
const sha = "a".repeat(64);
const base = { versionCode: 2, versionName: "1.1", sha256: sha, bytes: 13349873, notas: ["Citas agendadas", "Arreglos"], minVersionCode: 0, ahora: 1793900000000 };
assert.deepEqual(armarManifiesto(base), {
  versionCode: 2,
  versionName: "1.1",
  sha256: sha,
  bytes: 13349873,
  notas: ["Citas agendadas", "Arreglos"],
  minVersionCode: 0,
  publicadaEn: 1793900000000,
});
assert.equal(armarManifiesto({ ...base, minVersionCode: undefined, notas: undefined }).minVersionCode, 0, "sin mínima, nadie queda forzado");
assert.deepEqual(armarManifiesto({ ...base, notas: undefined }).notas, [], "sin notas, una lista vacía");

const malos = {
  "versionCode 0": { versionCode: 0 },
  "versionCode negativo": { versionCode: -1 },
  "versionCode con decimales": { versionCode: 2.5 },
  "versionCode como texto": { versionCode: "2" },
  "versionName vacío": { versionName: "" },
  "versionName de espacios": { versionName: "   " },
  "versionName de 41 caracteres": { versionName: "v".repeat(41) },
  "sha256 de 63 caracteres": { sha256: "a".repeat(63) },
  "sha256 con mayúsculas": { sha256: "A".repeat(64) },
  "sha256 con una letra no hexadecimal": { sha256: "a".repeat(63) + "g" },
  "bytes 0": { bytes: 0 },
  "bytes de más": { bytes: MAX_APK_BYTES + 1 },
  "11 notas": { notas: Array.from({ length: 11 }, (_, i) => `nota ${i}`) },
  "una nota de 201 caracteres": { notas: ["n".repeat(201)] },
  "una nota que no es texto": { notas: [1] },
  "minVersionCode mayor que versionCode": { minVersionCode: 3 },
  "minVersionCode negativo": { minVersionCode: -1 },
};
for (const [que, cambio] of Object.entries(malos)) assert.throws(() => armarManifiesto({ ...base, ...cambio }), undefined, `debía rechazarse: ${que}`);
// y en el límite sí pasa
armarManifiesto({ ...base, bytes: MAX_APK_BYTES, versionName: "v".repeat(40), notas: Array.from({ length: 10 }, () => "n".repeat(200)) });

// ---- qué APK se puede publicar
const bueno = { paquete: PAQUETE, certificadoSha256: CERTIFICADO_RELEASE, versionCode: 2, publicada: 1 };
comprobarPublicable(bueno);
comprobarPublicable({ ...bueno, versionCode: 1, publicada: null }); // nada publicado todavía
assert.throws(() => comprobarPublicable({ ...bueno, paquete: "otra.app" }), /paquete/, "otro paquete");
assert.throws(() => comprobarPublicable({ ...bueno, certificadoSha256: "0".repeat(64) }), /certificado/, "firmado con otra llave (por ejemplo la de debug)");
assert.throws(() => comprobarPublicable({ ...bueno, versionCode: 1 }), /versión/, "la misma versión que la publicada");
assert.throws(() => comprobarPublicable({ ...bueno, versionCode: 1, publicada: 5 }), /versión/, "una menor que la publicada");

console.log("publicar.mjs: ok");
