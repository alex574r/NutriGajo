// Prueba de los avisos de cita (confirmada, agendada por el nutriólogo y cancelada) sin red ni cuenta de Google: `npm run test:aviso`.
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
const { avisoDeAgendada, avisoDeCancelacion, avisoDeConfirmacion, cuando, zonaValida } = await import(pathToFileURL((await bundle("src/lib/aviso.ts"))).href);

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
assert.deepEqual(avisoDeAgendada("Lucía", inicio, "America/Mexico_City", "Calle 1"), {
  titulo: "Cita agendada",
  cuerpo: "Lucía te agendó una cita: Lunes 26 de octubre, 10:00. Consultorio: Calle 1.",
});
assert.equal(avisoDeAgendada("Lucía", inicio, "UTC").cuerpo, "Lucía te agendó una cita: Lunes 26 de octubre, 16:00.");
assert.deepEqual(avisoDeCancelacion("Lucía", inicio, "America/Mexico_City"), {
  titulo: "Cita cancelada",
  cuerpo: "Lucía canceló tu cita del Lunes 26 de octubre, 10:00.",
});

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
preparar({ cita: { estado: "RECHAZADA" } });
assert.equal((await pedir({ citaId: "c1" })).status, 409, "una cita rechazada no se avisa");
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

// ---- la cita que agenda el nutriólogo: ya nace confirmada y el aviso lo dice
preparar({ cita: { origen: "NUTRIOLOGO" } });
assert.deepEqual(await (await pedir({ citaId: "c1" })).json(), { enviado: true });
{
  const m = JSON.parse(envios()[0].cuerpo).message;
  assert.equal(m.notification.title, "Cita agendada");
  assert.equal(m.notification.body, "Lucía Prueba te agendó una cita: Lunes 26 de octubre, 10:00. Consultorio: Calle de prueba 123.");
  assert.deepEqual(m.data, { tipo: "CITA_AGENDADA", citaId: "c1" });
  assert.equal(m.android.notification.channel_id, "citas");
}
preparar({ cita: { origen: "PACIENTE" } });
assert.equal(JSON.parse((await pedir({ citaId: "c1" }), envios()[0].cuerpo)).message.notification.title, "Cita confirmada", "una cita pedida por el paciente sigue diciendo «confirmada»");

// ---- la cancelación: solo la pide el nutriólogo de la cita
preparar({ cita: { estado: "CANCELADA" } });
assert.deepEqual(await (await pedir({ citaId: "c1" })).json(), { enviado: true });
{
  const m = JSON.parse(envios()[0].cuerpo).message;
  assert.equal(m.notification.title, "Cita cancelada");
  assert.equal(m.notification.body, "Lucía Prueba canceló tu cita del Lunes 26 de octubre, 10:00.");
  assert.deepEqual(m.data, { tipo: "CITA_CANCELADA", citaId: "c1" });
}
assert.ok(llamadas.some((l) => l.metodo === "PATCH" && l.url.includes("citas/c1") && l.url.includes("updateMask.fieldPaths=notificadaEn")), "se marca la cancelación como avisada");
preparar({ cita: { estado: "CANCELADA" } });
assert.equal((await pedir({ citaId: "c1" }, "nutri2")).status, 403, "otro nutriólogo no pide el aviso de una cancelación");
assert.equal(envios().length, 0);

// ---- cada cambio de la cita se avisa una vez: «ya avisado» es que `notificadaPara` (el `actualizadaEn` que se avisó) sea el de ahora;
// no se comparan relojes (el de la cita es del teléfono y el del aviso, del servidor)
preparar({ cita: { notificadaEn: 1, notificadaPara: 2, actualizadaEn: 3 } });
assert.deepEqual(await (await pedir({ citaId: "c1" })).json(), { enviado: true }, "la cita cambió después del aviso: se avisa otra vez");
preparar({ cita: { notificadaEn: 2, notificadaPara: 2, actualizadaEn: 2 } });
assert.deepEqual(await (await pedir({ citaId: "c1" })).json(), { enviado: false, motivo: "ya avisado" });
preparar({ cita: { notificadaEn: 100, notificadaPara: 98, actualizadaEn: 99 } });
assert.deepEqual(await (await pedir({ citaId: "c1" })).json(), { enviado: true }, "con el reloj del teléfono atrasado, un cambio posterior se avisa");
preparar({ cita: { estado: "CANCELADA", notificadaEn: 5, notificadaPara: 4, actualizadaEn: 4 } });
assert.deepEqual(await (await pedir({ citaId: "c1" })).json(), { enviado: false, motivo: "ya avisado" }, "pedir dos veces la cancelación no la manda dos");
assert.equal(envios().length, 0);
preparar({ cita: { notificadaEn: 5, notificadaPara: 9000000000000000, actualizadaEn: 9000000000000000 } });
assert.deepEqual(await (await pedir({ citaId: "c1" })).json(), { enviado: false, motivo: "ya avisado" }, "una fecha disparatada no da avisos sin fin");
preparar({ cita: { actualizadaEn: 7 } });
assert.deepEqual(await (await pedir({ citaId: "c1" })).json(), { enviado: true });
{
  const marca = llamadas.find((l) => l.metodo === "PATCH" && l.url.includes("citas/c1"));
  assert.ok(marca.url.includes("updateMask.fieldPaths=notificadaEn") && marca.url.includes("updateMask.fieldPaths=notificadaPara"), "el aviso deja constancia de la hora y del cambio avisado");
  assert.equal(JSON.parse(marca.cuerpo).fields.notificadaPara.integerValue, "7");
}

console.log("aviso.mjs: ok");
