// Prueba de las reglas del chat contra los emuladores de Auth y Firestore (con firebase/firestore.rules cargado):
//   firebase emulators:start --only auth,firestore --project nutrigajo      (en otra terminal; ver "Ejecución" abajo)
//   node firebase/test/chat.mjs
// Ejecución: la configuración de los emuladores apunta a firebase/firestore.rules con "firestore": { "rules": "…" } y los
// puertos 9099 (Auth) y 8080 (Firestore); no hace falta cuenta de Firebase.
import assert from "node:assert/strict";

const AUTH = "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=x";
const BASE = "projects/nutrigajo/databases/(default)/documents";
const FS = `http://127.0.0.1:8080/v1/${BASE}`;

const valor = (x) => {
  if (typeof x === "boolean") return { booleanValue: x };
  if (Number.isInteger(x)) return { integerValue: String(x) };
  if (x && typeof x === "object") return { mapValue: { fields: campos(x) } };
  return { stringValue: x };
};
const campos = (d) => Object.fromEntries(Object.entries(d).map(([k, x]) => [k, valor(x)]));
const doc = (ruta, d) => ({ name: `${BASE}/${ruta}`, fields: campos(d) });

async function pedir(url, token, cuerpo) {
  const r = await fetch(url, {
    method: cuerpo ? "POST" : "GET",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: cuerpo ? JSON.stringify(cuerpo) : undefined,
  });
  return r.status;
}
const commit = (token, ...writes) => pedir(`${FS}:commit`, token, { writes });
const leer = (token, ruta) => pedir(`${FS}/${ruta}`, token);
// crea el mensaje y actualiza el resumen del paciente en un solo lote, como lo hace la app
const enviar = (token, id, mensaje, resumen) =>
  commit(token, { update: doc(`pacientes/pac1/mensajes/${id}`, mensaje) }, ...(resumen ? [{ update: doc("pacientes/pac1", { ultimoMensaje: resumen }), updateMask: { fieldPaths: ["ultimoMensaje"] }, currentDocument: { exists: true } }] : []));

async function cuenta(correo) {
  const r = await fetch(AUTH, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email: correo, password: "prueba-" + Math.random(), returnSecureToken: true }) });
  const d = await r.json();
  return { uid: d.localId, token: d.idToken };
}

const sufijo = Math.random().toString(16).slice(2, 8);
const nutri = await cuenta(`nutri${sufijo}@prueba.test`);
const otraNutri = await cuenta(`nutri2${sufijo}@prueba.test`);
const paciente = await cuenta(`pac${sufijo}@prueba.test`);
const otro = await cuenta(`otro${sufijo}@prueba.test`);
await commit("owner", { update: doc("pacientes/pac1", { nutriologoId: nutri.uid, usuarioId: paciente.uid, nombre: "Pac" }) });

const ahora = Date.now();
const mensaje = (autorId, extra = {}) => ({ autorId, texto: "Hola", enviadoEn: ahora, ...extra });
const resumen = (autorId, extra = {}) => ({ autorId, enviadoEn: ahora, texto: "Hola", foto: false, ...extra });

// ---- escribir
assert.equal(await enviar(nutri.token, "m1", mensaje(nutri.uid), resumen(nutri.uid)), 200, "el nutriólogo escribe");
assert.equal(await enviar(paciente.token, "m2", mensaje(paciente.uid), resumen(paciente.uid)), 200, "el paciente escribe");
assert.equal(await enviar(paciente.token, "m3", mensaje(paciente.uid, { texto: "", foto: true }), resumen(paciente.uid, { texto: "", foto: true })), 200, "una foto sin texto");
assert.equal(await enviar(otraNutri.token, "m4", mensaje(otraNutri.uid)), 403, "otro nutriólogo no escribe en ese chat");
assert.equal(await enviar(otro.token, "m5", mensaje(otro.uid)), 403, "otro paciente no escribe en ese chat");
assert.equal(await enviar(paciente.token, "m6", mensaje(nutri.uid)), 403, "no se firma como la otra persona");
assert.equal(await enviar(nutri.token, "m7", mensaje(nutri.uid, { texto: "" })), 403, "ni texto ni foto");
assert.equal(await enviar(nutri.token, "m8", mensaje(nutri.uid, { texto: "x".repeat(2001) })), 403, "texto demasiado largo");
assert.equal(await enviar(nutri.token, "m9", mensaje(nutri.uid, { notificadaEn: ahora })), 403, "campos de más (notificadaEn es del Worker)");
assert.equal(await enviar(nutri.token, "m10", mensaje(nutri.uid, { enviadoEn: "ayer" })), 403, "la hora debe ser un número");
assert.equal(await enviar(nutri.token, "m11", mensaje(nutri.uid, { foto: "si" })), 403, "foto debe ser booleano");

// ---- leer
assert.equal(await leer(paciente.token, "pacientes/pac1/mensajes/m1"), 200, "el paciente lee");
assert.equal(await leer(nutri.token, "pacientes/pac1/mensajes/m2"), 200, "el nutriólogo lee");
assert.equal(await leer(otraNutri.token, "pacientes/pac1/mensajes/m1"), 403, "otro nutriólogo no lee");
assert.equal(await leer(otro.token, "pacientes/pac1/mensajes/m1"), 403, "otro paciente no lee");

// ---- lo enviado no cambia ni se borra
const cambio = { update: doc("pacientes/pac1/mensajes/m1", { texto: "Editado" }), updateMask: { fieldPaths: ["texto"] }, currentDocument: { exists: true } };
assert.equal(await commit(nutri.token, cambio), 403, "el mensaje no se edita");
assert.equal(await commit(paciente.token, cambio), 403, "el mensaje no se edita (el otro)");
assert.equal(await commit(nutri.token, { delete: `${BASE}/pacientes/pac1/mensajes/m1` }), 403, "el mensaje no se borra");

// ---- resumen en el documento del paciente
const soloResumen = (token, r) => commit(token, { update: doc("pacientes/pac1", { ultimoMensaje: r }), updateMask: { fieldPaths: ["ultimoMensaje"] }, currentDocument: { exists: true } });
assert.equal(await soloResumen(nutri.token, resumen(nutri.uid)), 200, "el nutriólogo actualiza el resumen");
assert.equal(await soloResumen(paciente.token, resumen(paciente.uid)), 200, "el paciente actualiza el resumen");
assert.equal(await soloResumen(otraNutri.token, resumen(otraNutri.uid)), 403, "un tercero no toca el resumen");
assert.equal(await soloResumen(paciente.token, resumen(nutri.uid)), 403, "el resumen se firma con la propia cuenta");
assert.equal(await soloResumen(nutri.token, resumen(nutri.uid, { texto: "x".repeat(121) })), 403, "resumen demasiado largo");
assert.equal(await soloResumen(nutri.token, resumen(nutri.uid, { extra: "no" })), 403, "campos de más en el resumen");
const otroCampo = { update: doc("pacientes/pac1", { nombre: "Otro nombre" }), updateMask: { fieldPaths: ["nombre"] }, currentDocument: { exists: true } };
assert.equal(await commit(paciente.token, otroCampo), 403, "el resumen no abre el resto del registro del paciente");

console.log("chat.mjs: ok");
