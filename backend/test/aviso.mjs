// Prueba del aviso de cita confirmada sin red ni cuenta de Google: `npm run test:aviso`.
// Empaqueta la ruta con esbuild (ya viene con wrangler), simula Google, Firestore y FCM con un `fetch` falso y comprueba
// lo que protege la ruta: de quién es la cita, su estado, que no se avise dos veces y el texto del mensaje.
import assert from "node:assert/strict";
import { generateKeyPairSync } from "node:crypto";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { pathToFileURL } from "node:url";
import { build } from "esbuild";

const salida = join(mkdtempSync(join(tmpdir(), "aviso-")), "cita.mjs");
await build({ entryPoints: ["src/routes/cita.ts"], bundle: true, format: "esm", platform: "node", outfile: salida, logLevel: "silent" });
const { avisarConfirmacion } = await import(pathToFileURL(salida).href);
const { avisoDeConfirmacion, cuando, zonaValida } = await import(pathToFileURL((await bundle("src/lib/aviso.ts"))).href);

async function bundle(entrada) {
  const destino = join(mkdtempSync(join(tmpdir(), "aviso-")), "lib.mjs");
  await build({ entryPoints: [entrada], bundle: true, format: "esm", platform: "node", outfile: destino, logLevel: "silent" });
  return destino;
}

// ---- el texto del aviso
const inicio = Date.UTC(2026, 9, 26, 16, 0); // lunes 26 de octubre de 2026, 16:00 UTC
assert.equal(cuando(inicio, "America/Mexico_City"), "Lunes 26 de octubre, 10:00");
assert.equal(cuando(inicio, "UTC"), "Lunes 26 de octubre, 16:00");
assert.equal(zonaValida("Mars/Olympus"), "America/Mexico_City");
assert.equal(zonaValida(undefined), "America/Mexico_City");
assert.equal(zonaValida("America/Bogota"), "America/Bogota");
assert.deepEqual(avisoDeConfirmacion("Lucía Prueba", inicio, "America/Mexico_City", "Calle 1"), {
  titulo: "Cita confirmada",
  cuerpo: "Lucía Prueba confirmó tu cita: Lunes 26 de octubre, 10:00. Consultorio: Calle 1.",
});
assert.equal(avisoDeConfirmacion("Lucía", inicio, "UTC").cuerpo, "Lucía confirmó tu cita: Lunes 26 de octubre, 16:00.");

// ---- la ruta, con Google, Firestore y FCM simulados
const { privateKey } = generateKeyPairSync("rsa", { modulusLength: 2048, privateKeyEncoding: { type: "pkcs8", format: "pem" }, publicKeyEncoding: { type: "spki", format: "pem" } });
const env = { FIREBASE_PROJECT_ID: "nutrigajo", FIREBASE_SERVICE_ACCOUNT: JSON.stringify({ client_email: "cuenta@nutrigajo.iam.gserviceaccount.com", private_key: privateKey }) };

const campo = (valor) => (typeof valor === "number" ? { integerValue: String(valor) } : { stringValue: valor });
const doc = (campos) => ({ fields: Object.fromEntries(Object.entries(campos).map(([k, v]) => [k, campo(v)])) });

let datos, llamadas, fcm;
function preparar({ cita = {}, paciente = {}, respuestaFcm = 200 } = {}) {
  datos = {
    "citas/c1": doc({ nutriologoId: "nutri1", nutriologoNombre: "Lucía Prueba", usuarioId: "pac1", estado: "CONFIRMADA", inicio, ...cita }),
    "usuarios/pac1": doc({ fcmToken: "token-del-telefono", fcmZona: "America/Mexico_City", ...paciente }),
    "directorio/nutri1": doc({ direccion: "Calle de prueba 123" }),
  };
  llamadas = [];
  fcm = respuestaFcm;
}
globalThis.fetch = async (url, init = {}) => {
  url = String(url);
  llamadas.push({ url, metodo: init.method ?? "GET", cuerpo: init.body });
  if (url.startsWith("https://oauth2.googleapis.com/token")) return Response.json({ access_token: "acceso", expires_in: 3600 });
  if (url.includes("/messages:send")) return new Response("{}", { status: fcm });
  const ruta = decodeURIComponent(url.split("/documents/")[1].split("?")[0]);
  if (init.method === "PATCH") return Response.json({});
  return datos[ruta] ? Response.json(datos[ruta]) : new Response("{}", { status: 404 });
};
const pedir = (cuerpo, uid = "nutri1", entorno = env) =>
  avisarConfirmacion(new Request("https://w/citas/notificar", { method: "POST", body: JSON.stringify(cuerpo) }), entorno, uid);
const envios = () => llamadas.filter((l) => l.url.includes("/messages:send"));

preparar();
assert.equal((await pedir({})).status, 400, "sin citaId");
assert.equal((await pedir({ citaId: "../usuarios" })).status, 400, "citaId con ruta");
assert.equal((await pedir({ citaId: "c1" }, "nutri1", { FIREBASE_PROJECT_ID: "nutrigajo" })).status, 503, "sin clave configurada");
assert.equal((await pedir({ citaId: "otra" })).status, 404, "cita inexistente");
assert.equal((await pedir({ citaId: "c1" }, "nutri2")).status, 403, "cita de otro nutriólogo");
assert.equal(envios().length, 0);

preparar({ cita: { estado: "PENDIENTE" } });
assert.equal((await pedir({ citaId: "c1" })).status, 409, "cita sin confirmar");
assert.equal(envios().length, 0);

preparar({ cita: { notificadaEn: 1 } });
assert.deepEqual(await (await pedir({ citaId: "c1" })).json(), { enviado: false, motivo: "ya avisado" });
assert.equal(envios().length, 0);

preparar({ paciente: { fcmToken: undefined } });
delete datos["usuarios/pac1"].fields.fcmToken;
assert.deepEqual(await (await pedir({ citaId: "c1" })).json(), { enviado: false, motivo: "sin dispositivo" });

preparar();
assert.deepEqual(await (await pedir({ citaId: "c1" })).json(), { enviado: true });
assert.equal(envios().length, 1);
const mensaje = JSON.parse(envios()[0].cuerpo).message;
assert.equal(mensaje.token, "token-del-telefono");
assert.equal(mensaje.notification.title, "Cita confirmada");
assert.equal(mensaje.notification.body, "Lucía Prueba confirmó tu cita: Lunes 26 de octubre, 10:00. Consultorio: Calle de prueba 123.");
assert.deepEqual(mensaje.data, { tipo: "CITA_CONFIRMADA", citaId: "c1" });
assert.equal(mensaje.android.notification.channel_id, "citas");
const marca = llamadas.find((l) => l.metodo === "PATCH" && l.url.includes("citas/c1"));
assert.ok(marca && marca.url.includes("updateMask.fieldPaths=notificadaEn"), "se marca la cita como avisada");

preparar({ respuestaFcm: 404 });
assert.deepEqual(await (await pedir({ citaId: "c1" })).json(), { enviado: false, motivo: "dispositivo caducado" });
assert.ok(llamadas.some((l) => l.metodo === "PATCH" && l.url.includes("usuarios/pac1") && l.url.includes("fcmToken")), "se quita el token caducado");
assert.ok(!llamadas.some((l) => l.metodo === "PATCH" && l.url.includes("citas/c1")), "no se marca como avisada");

preparar({ respuestaFcm: 500 });
assert.equal((await pedir({ citaId: "c1" })).status, 502, "FCM falla");

console.log("aviso.mjs: ok");
