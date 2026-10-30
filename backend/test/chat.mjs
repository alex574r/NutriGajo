// Prueba de las rutas del chat sin red ni cuenta de Google: `npm run test:chat`.
// Empaqueta la ruta con esbuild, simula Google, Firestore, FCM y R2 y comprueba quién puede subir y leer la foto de un mensaje y a quién se avisa.
import assert from "node:assert/strict";
import { generateKeyPairSync } from "node:crypto";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { pathToFileURL } from "node:url";
import { build } from "esbuild";

const salida = join(mkdtempSync(join(tmpdir(), "chat-")), "chat.mjs");
await build({ entryPoints: ["src/routes/chat.ts"], bundle: true, format: "esm", platform: "node", outfile: salida, logLevel: "silent" });
const { subirFotoChat, leerFotoChat, avisarMensaje } = await import(pathToFileURL(salida).href);

const { privateKey } = generateKeyPairSync("rsa", { modulusLength: 2048, privateKeyEncoding: { type: "pkcs8", format: "pem" }, publicKeyEncoding: { type: "spki", format: "pem" } });
const cuenta = JSON.stringify({ client_email: "cuenta@nutrigajo.iam.gserviceaccount.com", private_key: privateKey });

// R2 falso: guarda los objetos en un mapa
let objetos;
const r2 = {
  head: async (clave) => (objetos.has(clave) ? {} : null),
  put: async (clave, cuerpo) => void objetos.set(clave, cuerpo),
  get: async (clave) => (objetos.has(clave) ? { body: new Response(objetos.get(clave)).body } : null),
};
const env = { FIREBASE_PROJECT_ID: "nutrigajo", FIREBASE_SERVICE_ACCOUNT: cuenta, FOTOS: r2 };

const campo = (v) => (typeof v === "boolean" ? { booleanValue: v } : typeof v === "number" ? { integerValue: String(v) } : { stringValue: v });
const doc = (campos) => ({ fields: Object.fromEntries(Object.entries(campos).map(([k, v]) => [k, campo(v)])) });

let datos, llamadas, fcm;
function preparar({ mensaje = {}, sinMensaje = false, tokenPaciente = "token-del-paciente", tokenNutriologo = "token-del-nutriologo", respuestaFcm = 200 } = {}) {
  objetos = new Map();
  datos = {
    "pacientes/pac1": doc({ nutriologoId: "nutri1", usuarioId: "cuentaPac1" }),
    "usuarios/nutri1": doc({ nombre: "Lucía Nutri", ...(tokenNutriologo ? { fcmToken: tokenNutriologo } : {}) }),
    "usuarios/cuentaPac1": doc({ nombre: "Alejandro", ...(tokenPaciente ? { fcmToken: tokenPaciente } : {}) }),
  };
  if (!sinMensaje) datos["pacientes/pac1/mensajes/m1"] = doc({ autorId: "nutri1", texto: "Hola, ¿cómo te fue con el plan?", enviadoEn: 1, ...mensaje });
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

const JPEG = new Uint8Array([0xff, 0xd8, 0xff, 0xe0, 1, 2, 3]);
const subir = (cuerpo = JPEG, tipo = "image/jpeg", uid = "nutri1", mensajeId = "m2", entorno = env) =>
  subirFotoChat(new Request("https://w/chat/pac1/" + mensajeId, { method: "PUT", body: cuerpo, headers: { "Content-Type": tipo } }), entorno, uid, "pac1", mensajeId);
const avisar = (cuerpo, uid = "nutri1") => avisarMensaje(new Request("https://w/mensajes/notificar", { method: "POST", body: JSON.stringify(cuerpo) }), env, uid);
const pushes = () => llamadas.filter((l) => l.url.includes("/messages:send"));
const mensaje = (l) => JSON.parse(l.cuerpo).message;

// ---- subir la foto
preparar();
assert.equal((await subir(JPEG, "image/png")).status, 415, "tipo de archivo");
assert.equal((await subir(JPEG, "image/jpeg", "nutri1", "m2", { ...env, FIREBASE_SERVICE_ACCOUNT: undefined })).status, 503, "sin clave");
assert.equal((await subir(JPEG, "image/jpeg", "curioso")).status, 403, "un tercero no sube fotos a ese chat");
assert.equal((await subir(new TextEncoder().encode("<html>"))).status, 400, "no es un JPEG");
assert.equal((await subir(new Uint8Array(0))).status, 413, "vacío");
assert.equal(objetos.size, 0);
assert.equal((await subir()).status, 200, "el nutriólogo sube su foto");
assert.equal((await subir(JPEG, "image/jpeg", "cuentaPac1", "m3")).status, 200, "el paciente sube su foto");
assert.deepEqual([...objetos.keys()].sort(), ["chat/pac1/m2.jpg", "chat/pac1/m3.jpg"]);
assert.equal((await subir(JPEG, "image/jpeg", "nutri1", "m2")).status, 409, "una foto enviada no se sobrescribe");
const inexistente = await subirFotoChat(new Request("https://w/chat/nadie/m9", { method: "PUT", body: JPEG, headers: { "Content-Type": "image/jpeg" } }), env, "nutri1", "nadie", "m9");
assert.equal(inexistente.status, 404, "paciente inexistente");
const grande = new Request("https://w/chat/pac1/m4", { method: "PUT", body: JPEG, headers: { "Content-Type": "image/jpeg", "Content-Length": String(4 * 1024 * 1024) } });
assert.equal((await subirFotoChat(grande, env, "nutri1", "pac1", "m4")).status, 413, "demasiado grande");

// ---- leer la foto
preparar();
objetos.set("chat/pac1/m2.jpg", JPEG);
const leer = (uid, paciente = "pac1", id = "m2") => leerFotoChat(env, uid, paciente, id);
const propia = await leer("cuentaPac1");
assert.equal(propia.status, 200, "el paciente lee la foto");
assert.equal(propia.headers.get("Content-Type"), "image/jpeg");
assert.match(propia.headers.get("Cache-Control"), /private/, "la foto no se guarda en cachés compartidas");
assert.deepEqual(new Uint8Array(await propia.arrayBuffer()), JPEG);
assert.equal((await leer("nutri1")).status, 200, "el nutriólogo lee la foto");
assert.equal((await leer("curioso")).status, 403, "un tercero no la lee");
assert.equal((await leer("cuentaPac1", "pac1", "otra")).status, 404, "foto inexistente");
assert.equal((await leer("cuentaPac1", "nadie")).status, 404, "paciente inexistente");

// ---- avisar
preparar();
assert.equal((await avisar({})).status, 400, "sin ids");
assert.equal((await avisar({ pacienteId: "pac1", mensajeId: "../x" })).status, 400, "id con ruta");
assert.equal((await avisar({ pacienteId: "pac1", mensajeId: "m1" }, "curioso")).status, 403, "un tercero no avisa");
assert.equal((await avisar({ pacienteId: "pac1", mensajeId: "no-existe" })).status, 404, "mensaje sin documento");
assert.equal((await avisar({ pacienteId: "pac1", mensajeId: "m1" }, "cuentaPac1")).status, 403, "el mensaje es del nutriólogo: el paciente no pide avisar de él");
assert.equal(pushes().length, 0);

// el nutriólogo escribe: el aviso es para el paciente, firmado con el nombre del nutriólogo
preparar();
assert.deepEqual(await (await avisar({ pacienteId: "pac1", mensajeId: "m1" })).json(), { enviado: true });
assert.equal(pushes().length, 1);
let push = mensaje(pushes()[0]);
assert.equal(push.token, "token-del-paciente");
assert.equal(push.notification.title, "Lucía Nutri");
assert.equal(push.notification.body, "Hola, ¿cómo te fue con el plan?");
assert.deepEqual(push.data, { tipo: "MENSAJE_NUEVO", pacienteId: "pac1" });
assert.equal(push.android.notification.channel_id, "mensajes");
assert.ok(llamadas.some((l) => l.metodo === "PATCH" && l.url.includes("mensajes/m1") && l.url.includes("notificadaEn")), "se marca el mensaje como avisado");

// el paciente escribe: el aviso es para el nutriólogo
preparar({ mensaje: { autorId: "cuentaPac1", texto: "Todo bien" } });
assert.deepEqual(await (await avisar({ pacienteId: "pac1", mensajeId: "m1" }, "cuentaPac1")).json(), { enviado: true });
push = mensaje(pushes()[0]);
assert.equal(push.token, "token-del-nutriologo");
assert.equal(push.notification.title, "Alejandro");

// textos largos se recortan y una foto sin texto dice «foto»
preparar({ mensaje: { texto: "x".repeat(300) } });
await avisar({ pacienteId: "pac1", mensajeId: "m1" });
assert.equal(mensaje(pushes()[0]).notification.body, "x".repeat(100) + "…");
preparar({ mensaje: { texto: "", foto: true } });
await avisar({ pacienteId: "pac1", mensajeId: "m1" });
assert.equal(mensaje(pushes()[0]).notification.body, "Te envió una foto");

// no se avisa dos veces, ni sin dispositivo
preparar({ mensaje: { notificadaEn: 1 } });
assert.deepEqual(await (await avisar({ pacienteId: "pac1", mensajeId: "m1" })).json(), { enviado: false, motivo: "ya avisado" });
preparar({ tokenPaciente: "" });
assert.deepEqual(await (await avisar({ pacienteId: "pac1", mensajeId: "m1" })).json(), { enviado: false, motivo: "sin dispositivo" });
assert.equal(pushes().length, 0);

// FCM: teléfono caducado (se quita del perfil) y error (502)
preparar({ respuestaFcm: 404 });
assert.deepEqual(await (await avisar({ pacienteId: "pac1", mensajeId: "m1" })).json(), { enviado: false, motivo: "dispositivo caducado" });
assert.ok(llamadas.some((l) => l.metodo === "PATCH" && l.url.includes("usuarios/cuentaPac1") && l.url.includes("fcmToken")), "se borra el token caducado");
preparar({ respuestaFcm: 500 });
assert.equal((await avisar({ pacienteId: "pac1", mensajeId: "m1" })).status, 502, "FCM rechaza el aviso");

console.log("chat.mjs: ok");
