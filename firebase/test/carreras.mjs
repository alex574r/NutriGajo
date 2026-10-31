// Prueba de las reglas del modo carrera (usuarios/{uid}/carreras y usuarios/{uid}/modoCarrera) contra los emuladores de Auth y
// Firestore (con firebase/firestore.rules cargado):
//   firebase emulators:start --only auth,firestore --project nutrigajo      (en otra terminal)
//   node firebase/test/carreras.mjs
// Los puertos son 9099 (Auth) y 8080 (Firestore); no hace falta cuenta de Firebase.
import assert from "node:assert/strict";

const AUTH = "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=x";
const BASE = "projects/nutrigajo/databases/(default)/documents";
const FS = `http://127.0.0.1:8080/v1/${BASE}`;

const valor = (x) => {
  if (typeof x === "boolean") return { booleanValue: x };
  if (Number.isInteger(x)) return { integerValue: String(x) };
  if (typeof x === "number") return { doubleValue: x };
  if (Array.isArray(x)) return { arrayValue: { values: x.map(valor) } };
  if (x && typeof x === "object") return { mapValue: { fields: campos(x) } };
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
const yo = await cuenta(`corre${sufijo}@prueba.test`);
const otro = await cuenta(`otro${sufijo}@prueba.test`);

const ahora = Date.now();
const ruta = [19.432601, -99.133201, 19.432701, -99.133301, 19.432801, -99.133401];
const carrera = (extra = {}) => ({ tipo: "CAMINAR", inicio: ahora, duracionSeg: 1800, distanciaM: 2500, kcal: 140, ruta, ...extra });
const crear = (token, uid, d, id = "c" + Math.random().toString(16).slice(2, 8)) =>
  commit(token, { update: doc(`usuarios/${uid}/carreras/${id}`, d), currentDocument: { exists: false } });

// ---- quién guarda
assert.equal(await crear(yo.token, yo.uid, carrera()), 200, "guarda su caminata");
assert.equal(await crear(yo.token, yo.uid, carrera({ tipo: "CORRER" })), 200, "guarda su carrera");
assert.equal(await crear(otro.token, yo.uid, carrera()), 403, "otro no guarda en mi cuenta");
assert.equal(await commit(null, { update: doc(`usuarios/${yo.uid}/carreras/x`, carrera()), currentDocument: { exists: false } }), 403, "sin sesión no guarda");

// ---- lo que vale
assert.equal(await crear(yo.token, yo.uid, carrera({ tipo: "VOLAR" })), 403, "tipo desconocido");
assert.equal(await crear(yo.token, yo.uid, carrera({ duracionSeg: 5 })), 403, "muy corta");
assert.equal(await crear(yo.token, yo.uid, carrera({ duracionSeg: 90000 })), 403, "más de un día");
assert.equal(await crear(yo.token, yo.uid, carrera({ distanciaM: -1 })), 403, "distancia negativa");
assert.equal(await crear(yo.token, yo.uid, carrera({ distanciaM: 300000 })), 403, "300 km a pie");
assert.equal(await crear(yo.token, yo.uid, carrera({ kcal: 99999 })), 403, "demasiadas calorías");
assert.equal(await crear(yo.token, yo.uid, carrera({ inicio: "hoy" })), 403, "la fecha es un número");
assert.equal(await crear(yo.token, yo.uid, carrera({ ruta: [19.4, -99.1, 19.5] })), 403, "la ruta va de dos en dos");
assert.equal(await crear(yo.token, yo.uid, carrera({ ruta: "19.4,-99.1" })), 403, "la ruta es una lista");
assert.equal(await crear(yo.token, yo.uid, carrera({ ruta: Array.from({ length: 4004 }, (_, i) => 19 + i / 1e5) })), 403, "ruta demasiado larga");
assert.equal(await crear(yo.token, yo.uid, carrera({ ruta: Array.from({ length: 4002 }, (_, i) => 19 + i / 1e5) })), 200, "ruta del tope");
assert.equal(await crear(yo.token, yo.uid, carrera({ campoDeMas: 1 })), 403, "campos de más");
const { kcal, ...incompleta } = carrera();
assert.equal(await crear(yo.token, yo.uid, incompleta), 403, "faltan campos");
assert.equal(await crear(yo.token, yo.uid, carrera({ duracionSeg: 10, distanciaM: 0, kcal: 0, ruta: [] })), 200, "los mínimos");

// ---- una carrera guardada no se edita; se lee y se borra solo por su dueño
await commit("owner", { update: doc(`usuarios/${yo.uid}/carreras/fija`, carrera()) });
assert.equal(await commit(yo.token, { update: doc(`usuarios/${yo.uid}/carreras/fija`, carrera({ kcal: 1 })), currentDocument: { exists: true } }), 403, "no se edita");
assert.equal(await leer(yo.token, `usuarios/${yo.uid}/carreras/fija`), 200, "la lee su dueño");
assert.equal(await leer(otro.token, `usuarios/${yo.uid}/carreras/fija`), 403, "otro no la lee");
assert.equal(await commit(otro.token, { delete: `${BASE}/usuarios/${yo.uid}/carreras/fija` }), 403, "otro no la borra");
assert.equal(await commit(yo.token, { delete: `${BASE}/usuarios/${yo.uid}/carreras/fija` }), 200, "su dueño la borra");

// ---- el plan de caminata
const ruta_plan = (uid) => `usuarios/${uid}/modoCarrera/plan`;
const plan = (extra = {}) => ({ plan: "PRIMEROS_PASOS", inicio: ahora, ...extra });
assert.equal(await commit(yo.token, { update: doc(ruta_plan(yo.uid), plan()) }), 200, "elige un plan");
assert.equal(await commit(yo.token, { update: doc(ruta_plan(yo.uid), plan({ plan: "RESISTENCIA" })) }), 200, "cambia de plan");
assert.equal(await commit(yo.token, { update: doc(ruta_plan(yo.uid), plan({ plan: "MARATON" })) }), 403, "plan que no existe");
assert.equal(await commit(yo.token, { update: doc(ruta_plan(yo.uid), plan({ inicio: "hoy" })) }), 403, "el día es un número");
assert.equal(await commit(yo.token, { update: doc(ruta_plan(yo.uid), plan({ extra: 1 })) }), 403, "campos de más");
assert.equal(await commit(yo.token, { update: doc(`usuarios/${yo.uid}/modoCarrera/otro`, plan()) }), 403, "solo el documento plan");
assert.equal(await commit(otro.token, { update: doc(ruta_plan(yo.uid), plan()) }), 403, "otro no elige mi plan");
assert.equal(await leer(yo.token, ruta_plan(yo.uid)), 200, "lee su plan");
assert.equal(await leer(otro.token, ruta_plan(yo.uid)), 403, "otro no lo lee");
assert.equal(await commit(otro.token, { delete: `${BASE}/${ruta_plan(yo.uid)}` }), 403, "otro no lo quita");
assert.equal(await commit(yo.token, { delete: `${BASE}/${ruta_plan(yo.uid)}` }), 200, "lo quita su dueño");

console.log("carreras: todas las pruebas pasaron");
