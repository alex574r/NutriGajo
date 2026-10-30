// Prueba de las reglas de las metas diarias contra los emuladores de Auth y Firestore (con firebase/firestore.rules cargado):
//   firebase emulators:start --only auth,firestore --project nutrigajo      (en otra terminal)
//   node firebase/test/metas.mjs
// Los puertos son 9099 (Auth) y 8080 (Firestore); no hace falta cuenta de Firebase.
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
const metas = (por, extra = {}) => ({ aguaMl: 2500, pasos: 9000, kcal: 1800, minutos: 40, por, actualizadoEn: ahora, ...extra });
// solo la clave `metas`, como lo escribe la app
const guardar = (token, m) => commit(token, { update: doc("pacientes/pac1", { metas: m }), updateMask: { fieldPaths: ["metas"] }, currentDocument: { exists: true } });

// ---- quién escribe
assert.equal(await guardar(nutri.token, metas("NUTRIOLOGO")), 200, "el nutriólogo fija las metas");
assert.equal(await guardar(paciente.token, metas("PACIENTE", { aguaMl: 3000 })), 200, "el paciente las ajusta");
assert.equal(await guardar(paciente.token, metas("NUTRIOLOGO")), 403, "el paciente no firma como su nutriólogo");
assert.equal(await guardar(nutri.token, metas("PACIENTE")), 403, "el nutriólogo no firma como el paciente");
assert.equal(await guardar(otraNutri.token, metas("NUTRIOLOGO")), 403, "otro nutriólogo no las toca");
assert.equal(await guardar(otro.token, metas("PACIENTE")), 403, "otro paciente no las toca");

// ---- lo que vale
assert.equal(await guardar(nutri.token, metas("NUTRIOLOGO", { aguaMl: 100 })), 403, "poca agua");
assert.equal(await guardar(nutri.token, metas("NUTRIOLOGO", { aguaMl: 9000 })), 403, "demasiada agua");
assert.equal(await guardar(nutri.token, metas("NUTRIOLOGO", { pasos: 999_999 })), 403, "demasiados pasos");
assert.equal(await guardar(nutri.token, metas("NUTRIOLOGO", { kcal: 50 })), 403, "pocas calorías");
assert.equal(await guardar(nutri.token, metas("NUTRIOLOGO", { minutos: 0 })), 403, "sin minutos");
assert.equal(await guardar(nutri.token, metas("NUTRIOLOGO", { aguaMl: "2500" })), 403, "el agua es un número");
assert.equal(await guardar(nutri.token, metas("NUTRIOLOGO", { actualizadoEn: "hoy" })), 403, "la hora es un número");
assert.equal(await guardar(nutri.token, metas("NUTRIOLOGO", { campoDeMas: 1 })), 403, "campos de más");
assert.equal(await guardar(nutri.token, metas("ADMIN")), 403, "solo NUTRIOLOGO o PACIENTE");
const { minutos, ...incompletas } = metas("NUTRIOLOGO");
assert.equal(await guardar(nutri.token, incompletas), 403, "faltan campos");
// los topes de cada lado valen
assert.equal(await guardar(nutri.token, metas("NUTRIOLOGO", { aguaMl: 250, pasos: 1000, kcal: 200, minutos: 5 })), 200, "los mínimos");
assert.equal(await guardar(nutri.token, metas("NUTRIOLOGO", { aguaMl: 6000, pasos: 50000, kcal: 5000, minutos: 300 })), 200, "los máximos");

// ---- no abre nada más del registro
assert.equal(
  await commit(nutri.token, { update: doc("pacientes/pac1", { metas: metas("NUTRIOLOGO"), nombre: "Otro" }), updateMask: { fieldPaths: ["metas", "nombre"] }, currentDocument: { exists: true } }),
  403,
  "las metas no sirven de puerta para tocar otros campos",
);
assert.equal(
  await commit(paciente.token, { update: doc("pacientes/pac1", { metas: metas("PACIENTE"), nutriologoId: paciente.uid }), updateMask: { fieldPaths: ["metas", "nutriologoId"] }, currentDocument: { exists: true } }),
  403,
  "el paciente no se cambia de nutriólogo con las metas",
);

// ---- leer
assert.equal(await leer(paciente.token, "pacientes/pac1"), 200, "el paciente las lee");
assert.equal(await leer(nutri.token, "pacientes/pac1"), 200, "el nutriólogo las lee");
assert.equal(await leer(otro.token, "pacientes/pac1"), 403, "otro no las lee");

console.log("metas: todas las pruebas pasaron");
