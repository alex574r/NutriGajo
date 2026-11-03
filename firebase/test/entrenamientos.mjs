// Prueba de las reglas del historial de «Entrenar ahora» (usuarios/{uid}/entrenamientos) contra los emuladores de Auth y Firestore
// (con firebase/firestore.rules cargado):
//   firebase emulators:start --only auth,firestore --project nutrigajo      (en otra terminal)
//   node firebase/test/entrenamientos.mjs
// Los puertos son 9099 (Auth) y 8080 (Firestore); no hace falta cuenta de Firebase.
import assert from "node:assert/strict";

const AUTH = "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=x";
const BASE = "projects/nutrigajo/databases/(default)/documents";
const FS = `http://127.0.0.1:8080/v1/${BASE}`;

const valor = (x) => {
  if (typeof x === "boolean") return { booleanValue: x };
  if (Number.isInteger(x)) return { integerValue: String(x) };
  if (typeof x === "number") return { doubleValue: x };
  return { stringValue: x };
};
const campos = (d) => Object.fromEntries(Object.entries(d).map(([k, x]) => [k, valor(x)]));
const doc = (ruta, d) => ({ name: `${BASE}/${ruta}`, fields: campos(d) });

async function pedir(url, token, cuerpo) {
  const r = await fetch(url, {
    method: cuerpo ? "POST" : "GET",
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
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
const yo = await cuenta(`entreno${sufijo}@prueba.test`);
const otro = await cuenta(`otro${sufijo}@prueba.test`);

const ahora = Date.now();
const entreno = (extra = {}) => ({ fecha: ahora, enfoque: "PIERNAS", minutos: 42, kcal: 310, hechos: 5, total: 6, ...extra });
const crear = (token, uid, d, id = "t" + Math.random().toString(16).slice(2, 8)) =>
  commit(token, { update: doc(`usuarios/${uid}/entrenamientos/${id}`, d), currentDocument: { exists: false } });

// ---- quién guarda
assert.equal(await crear(yo.token, yo.uid, entreno()), 200, "guarda su entrenamiento");
assert.equal(await crear(otro.token, yo.uid, entreno()), 403, "otro no guarda en mi historial");
assert.equal(await commit(null, { update: doc(`usuarios/${yo.uid}/entrenamientos/x`, entreno()), currentDocument: { exists: false } }), 403, "sin sesión no guarda");

// ---- lo que vale
assert.equal(await crear(yo.token, yo.uid, entreno({ enfoque: "BAILE" })), 403, "enfoque desconocido");
assert.equal(await crear(yo.token, yo.uid, entreno({ enfoque: "MOVILIDAD" })), 200, "todos los enfoques valen");
assert.equal(await crear(yo.token, yo.uid, entreno({ minutos: 0 })), 403, "al menos un minuto");
assert.equal(await crear(yo.token, yo.uid, entreno({ minutos: 601 })), 403, "más de diez horas");
assert.equal(await crear(yo.token, yo.uid, entreno({ minutos: 600 })), 200, "el tope de minutos");
assert.equal(await crear(yo.token, yo.uid, entreno({ kcal: -1 })), 403, "calorías negativas");
assert.equal(await crear(yo.token, yo.uid, entreno({ kcal: 10001 })), 403, "demasiadas calorías");
assert.equal(await crear(yo.token, yo.uid, entreno({ hechos: 0 })), 403, "sin ningún ejercicio hecho no hay entrenamiento");
assert.equal(await crear(yo.token, yo.uid, entreno({ hechos: 7, total: 6 })), 403, "no se hacen más de los que había");
assert.equal(await crear(yo.token, yo.uid, entreno({ total: 21, hechos: 5 })), 403, "rutina de más de 20");
assert.equal(await crear(yo.token, yo.uid, entreno({ total: 0, hechos: 0 })), 403, "rutina vacía");
assert.equal(await crear(yo.token, yo.uid, entreno({ fecha: "hoy" })), 403, "la fecha es un número");
assert.equal(await crear(yo.token, yo.uid, entreno({ minutos: 4.5 })), 403, "los minutos son enteros");
assert.equal(await crear(yo.token, yo.uid, entreno({ campoDeMas: 1 })), 403, "campos de más");
const { total, ...incompleto } = entreno();
assert.equal(await crear(yo.token, yo.uid, incompleto), 403, "faltan campos");

// ---- un entrenamiento guardado no se edita; lo lee y lo borra solo su dueño
await commit("owner", { update: doc(`usuarios/${yo.uid}/entrenamientos/fijo`, entreno()) });
assert.equal(await commit(yo.token, { update: doc(`usuarios/${yo.uid}/entrenamientos/fijo`, entreno({ kcal: 1 })), currentDocument: { exists: true } }), 403, "no se edita");
assert.equal(await leer(yo.token, `usuarios/${yo.uid}/entrenamientos/fijo`), 200, "lo lee su dueño");
assert.equal(await leer(otro.token, `usuarios/${yo.uid}/entrenamientos/fijo`), 403, "otro no lo lee");
assert.equal(await commit(otro.token, { delete: `${BASE}/usuarios/${yo.uid}/entrenamientos/fijo` }), 403, "otro no lo borra");
assert.equal(await commit(yo.token, { delete: `${BASE}/usuarios/${yo.uid}/entrenamientos/fijo` }), 200, "su dueño lo borra");

console.log("entrenamientos: todas las pruebas pasaron");
