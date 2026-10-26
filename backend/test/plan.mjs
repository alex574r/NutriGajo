// Prueba de las rutas de planes (PDF) sin red ni cuenta de Google: `npm run test:plan`.
// Empaqueta la ruta con esbuild, simula Google, Firestore, FCM y R2 y comprueba quién puede subir, leer y avisar de un plan.
import assert from "node:assert/strict";
import { generateKeyPairSync } from "node:crypto";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { pathToFileURL } from "node:url";
import { build } from "esbuild";

const salida = join(mkdtempSync(join(tmpdir(), "plan-")), "plan.mjs");
await build({ entryPoints: ["src/routes/plan.ts"], bundle: true, format: "esm", platform: "node", outfile: salida, logLevel: "silent" });
const { subirPlan, leerPlan, avisarPlan } = await import(pathToFileURL(salida).href);

const { privateKey } = generateKeyPairSync("rsa", { modulusLength: 2048, privateKeyEncoding: { type: "pkcs8", format: "pem" }, publicKeyEncoding: { type: "spki", format: "pem" } });
const cuenta = JSON.stringify({ client_email: "cuenta@nutrigajo.iam.gserviceaccount.com", private_key: privateKey });

// R2 falso: guarda los objetos en un mapa
let objetos;
const r2 = {
  put: async (clave, cuerpo) => void objetos.set(clave, cuerpo),
  get: async (clave) => (objetos.has(clave) ? { body: new Response(objetos.get(clave)).body } : null),
};
const env = { FIREBASE_PROJECT_ID: "nutrigajo", FIREBASE_SERVICE_ACCOUNT: cuenta, FOTOS: r2 };

const campo = (v) => (typeof v === "number" ? { integerValue: String(v) } : { stringValue: v });
const doc = (campos) => ({ fields: Object.fromEntries(Object.entries(campos).map(([k, v]) => [k, campo(v)])) });

let datos, llamadas, fcm;
function preparar({ plan = true, planCampos = {}, token = "token-del-telefono", respuestaFcm = 200 } = {}) {
  objetos = new Map();
  datos = {
    "pacientes/pac1": doc({ nutriologoId: "nutri1", usuarioId: "cuentaPac1" }),
    "usuarios/cuentaPac1": doc({ ...(token ? { fcmToken: token } : {}) }),
  };
  if (plan) datos["pacientes/pac1/planes/plan1"] = doc({ enviadoPor: "nutri1", nutriologoNombre: "Lucía Prueba", ...planCampos });
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

const PDF = new TextEncoder().encode("%PDF-1.4 contenido");
const subir = (cuerpo = PDF, tipo = "application/pdf", uid = "nutri1", planId = "plan2", entorno = env) =>
  subirPlan(new Request("https://w/plan/pac1/" + planId, { method: "PUT", body: cuerpo, headers: { "Content-Type": tipo } }), entorno, uid, "pac1", planId);
const avisar = (cuerpo, uid = "nutri1", entorno = env) =>
  avisarPlan(new Request("https://w/planes/notificar", { method: "POST", body: JSON.stringify(cuerpo) }), entorno, uid);
const pushes = () => llamadas.filter((l) => l.url.includes("/messages:send"));

// ---- subir
preparar();
assert.equal((await subir(PDF, "image/png")).status, 415, "tipo de archivo");
assert.equal((await subir(PDF, "application/pdf", "nutri1", "plan2", { ...env, FIREBASE_SERVICE_ACCOUNT: undefined })).status, 503, "sin clave");
assert.equal((await subir(PDF, "application/pdf", "otro")).status, 403, "paciente de otro nutriólogo");
assert.equal((await subir(PDF, "application/pdf", "cuentaPac1")).status, 403, "el paciente no sube su propio plan");
assert.equal((await subir(PDF, "application/pdf", "nutri1", "plan1")).status, 409, "un plan enviado no se sobrescribe");
assert.equal((await subir(new TextEncoder().encode("<html>"))).status, 400, "no es un PDF");
assert.equal((await subir(new Uint8Array(0))).status, 413, "vacío");
assert.equal(objetos.size, 0);
assert.equal((await subir()).status, 200, "el nutriólogo sube su plan");
assert.deepEqual([...objetos.keys()], ["planes/pac1/plan2.pdf"]);
const inexistente = await subirPlan(new Request("https://w/plan/nadie/plan2", { method: "PUT", body: PDF, headers: { "Content-Type": "application/pdf" } }), env, "nutri1", "nadie", "plan2");
assert.equal(inexistente.status, 404, "paciente inexistente");

// ---- leer
preparar();
objetos.set("planes/pac1/plan1.pdf", PDF);
const leer = (uid, paciente = "pac1", plan = "plan1") => leerPlan(env, uid, paciente, plan);
const propio = await leer("cuentaPac1");
assert.equal(propio.status, 200, "el paciente lee su plan");
assert.equal(propio.headers.get("Content-Type"), "application/pdf");
assert.match(propio.headers.get("Cache-Control"), /private/, "el plan no se guarda en cachés compartidas");
assert.equal(new TextDecoder().decode(await propio.arrayBuffer()), "%PDF-1.4 contenido");
assert.equal((await leer("nutri1")).status, 200, "el nutriólogo lee el plan");
assert.equal((await leer("curioso")).status, 403, "un tercero no lee el plan");
assert.equal((await leer("cuentaPac1", "pac1", "otro")).status, 404, "plan inexistente");
assert.equal((await leer("cuentaPac1", "nadie")).status, 404, "paciente inexistente");

// ---- avisar
preparar();
assert.equal((await avisar({})).status, 400, "sin ids");
assert.equal((await avisar({ pacienteId: "pac1", planId: "../x" })).status, 400, "id con ruta");
assert.equal((await avisar({ pacienteId: "pac1", planId: "plan1" }, "otro")).status, 403, "paciente de otro nutriólogo");
assert.equal((await avisar({ pacienteId: "pac1", planId: "no-existe" })).status, 404, "plan sin documento");
assert.equal(pushes().length, 0);

preparar({ planCampos: { enviadoPor: "otro" } });
assert.equal((await avisar({ pacienteId: "pac1", planId: "plan1" })).status, 403, "plan de otro nutriólogo");

preparar({ planCampos: { notificadaEn: 1 } });
assert.deepEqual(await (await avisar({ pacienteId: "pac1", planId: "plan1" })).json(), { enviado: false, motivo: "ya avisado" });

preparar({ token: "" });
assert.deepEqual(await (await avisar({ pacienteId: "pac1", planId: "plan1" })).json(), { enviado: false, motivo: "sin dispositivo" });

preparar();
assert.deepEqual(await (await avisar({ pacienteId: "pac1", planId: "plan1" })).json(), { enviado: true });
assert.equal(pushes().length, 1);
const mensaje = JSON.parse(pushes()[0].cuerpo).message;
assert.equal(mensaje.token, "token-del-telefono");
assert.equal(mensaje.notification.title, "Tu plan ya está listo");
assert.match(mensaje.notification.body, /^Lucía Prueba te envió tu plan de alimentación y ejercicio/);
assert.deepEqual(mensaje.data, { tipo: "PLAN_NUEVO", pacienteId: "pac1", planId: "plan1" });
assert.equal(mensaje.android.notification.channel_id, "planes");
assert.ok(llamadas.some((l) => l.metodo === "PATCH" && l.url.includes("pacientes/pac1/planes/plan1") && l.url.includes("notificadaEn")), "se marca como avisado");

preparar({ respuestaFcm: 404 });
assert.deepEqual(await (await avisar({ pacienteId: "pac1", planId: "plan1" })).json(), { enviado: false, motivo: "dispositivo caducado" });
assert.ok(llamadas.some((l) => l.metodo === "PATCH" && l.url.includes("usuarios/cuentaPac1") && l.url.includes("fcmToken")), "se quita el token caducado");

preparar({ respuestaFcm: 500 });
assert.equal((await avisar({ pacienteId: "pac1", planId: "plan1" })).status, 502, "FCM falla");

console.log("plan.mjs: ok");
