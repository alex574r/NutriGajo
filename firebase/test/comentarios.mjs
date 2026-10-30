// Prueba de las reglas de los comentarios (Configuración, Enviar comentarios) contra los emuladores de Auth y Firestore (con
// firebase/firestore.rules cargado):
//   firebase emulators:start --only auth,firestore --project nutrigajo      (en otra terminal)
//   node firebase/test/comentarios.mjs
// Los puertos son 9099 (Auth) y 8080 (Firestore); no hace falta cuenta de Firebase.
import assert from "node:assert/strict";

const AUTH = "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=x";
const BASE = "projects/nutrigajo/databases/(default)/documents";
const FS = `http://127.0.0.1:8080/v1/${BASE}`;

const valor = (x) => {
  if (typeof x === "boolean") return { booleanValue: x };
  if (Number.isInteger(x)) return { integerValue: String(x) };
  if (Array.isArray(x)) return { arrayValue: { values: x.map(valor) } };
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

async function cuenta(correo, rol) {
  const r = await fetch(AUTH, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email: correo, password: "prueba-" + Math.random(), returnSecureToken: true }) });
  const d = await r.json();
  await commit("owner", { update: doc(`usuarios/${d.localId}`, { rol }) });
  return { uid: d.localId, token: d.idToken };
}

const sufijo = Math.random().toString(16).slice(2, 8);
const paciente = await cuenta(`pac${sufijo}@prueba.test`, "PACIENTE");
const nutri = await cuenta(`nutri${sufijo}@prueba.test`, "NUTRIOLOGO");
const admin = await cuenta(`admin${sufijo}@prueba.test`, "ADMIN");

const ahora = Date.now();
const comentario = (quien, rol, extra = {}) => ({ usuarioId: quien.uid, rol, areas: ["chat", "errores"], mensaje: "Se traba al abrir el chat", versionApp: "1.0", creadoEn: ahora, ...extra });
// ids con el sufijo de la corrida: el emulador conserva lo de la anterior
const enviar = (token, id, d) => commit(token, { update: doc(`comentarios/${sufijo}-${id}`, d), currentDocument: { exists: false } });

// ---- quién escribe y qué
assert.equal(await enviar(paciente.token, "c1", comentario(paciente, "PACIENTE")), 200, "el paciente envía un comentario");
assert.equal(await enviar(nutri.token, "c2", comentario(nutri, "NUTRIOLOGO")), 200, "el nutriólogo también");
assert.equal(await enviar(paciente.token, "c3", comentario(nutri, "PACIENTE")), 403, "no se firma con otra cuenta");
assert.equal(await enviar(paciente.token, "c4", comentario(paciente, "NUTRIOLOGO")), 403, "no se firma con otro rol");
assert.equal(await enviar(paciente.token, "c5", comentario(paciente, "PACIENTE", { areas: [], mensaje: "Solo texto" })), 200, "solo texto");
assert.equal(await enviar(paciente.token, "c6", comentario(paciente, "PACIENTE", { areas: ["plan"], mensaje: "" })), 200, "solo áreas");
assert.equal(await enviar(paciente.token, "c7", comentario(paciente, "PACIENTE", { areas: [], mensaje: "" })), 403, "vacío");

// ---- lo que vale
assert.equal(await enviar(paciente.token, "c9", comentario(paciente, "PACIENTE", { mensaje: "x".repeat(1001) })), 403, "texto demasiado largo");
assert.equal(await enviar(paciente.token, "c10", comentario(paciente, "PACIENTE", { mensaje: "x".repeat(1000) })), 200, "texto de 1000");
assert.equal(await enviar(paciente.token, "c11", comentario(paciente, "PACIENTE", { areas: Array(9).fill("otro") })), 403, "demasiadas áreas");
assert.equal(await enviar(paciente.token, "c12", comentario(paciente, "PACIENTE", { areas: "chat" })), 403, "las áreas son una lista");
assert.equal(await enviar(paciente.token, "c13", comentario(paciente, "PACIENTE", { creadoEn: "hoy" })), 403, "la hora es un número");
assert.equal(await enviar(paciente.token, "c14", comentario(paciente, "PACIENTE", { campoDeMas: 1 })), 403, "campos de más");
const { versionApp, ...sinVersion } = comentario(paciente, "PACIENTE");
assert.equal(await enviar(paciente.token, "c15", sinVersion), 403, "faltan campos");

// ---- leer, editar y borrar
assert.equal(await leer(admin.token, `comentarios/${sufijo}-c1`), 200, "el ADMIN los lee");
assert.equal(await leer(paciente.token, `comentarios/${sufijo}-c1`), 403, "ni su autor los lee");
assert.equal(await leer(nutri.token, `comentarios/${sufijo}-c1`), 403, "otro no los lee");
assert.equal(
  await commit(paciente.token, { update: doc(`comentarios/${sufijo}-c1`, comentario(paciente, "PACIENTE", { mensaje: "cambiado" })), currentDocument: { exists: true } }),
  403,
  "no se editan",
);
assert.equal(await commit(paciente.token, { delete: `${BASE}/comentarios/${sufijo}-c1` }), 403, "no se borran");

console.log("comentarios: todas las pruebas pasaron");
