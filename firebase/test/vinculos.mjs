// Prueba de las reglas de cerrar vínculos (pacientes/{id} con activo, cerradoEn y cerradoPor), de lo que no se puede escribir en un vínculo
// cerrado y del cambio de nutriólogo como un solo lote, contra los emuladores de Auth y Firestore (con firebase/firestore.rules cargado):
//   firebase emulators:start --only auth,firestore --project nutrigajo      (en otra terminal)
//   node firebase/test/vinculos.mjs
// Los puertos son 9099 (Auth) y 8080 (Firestore); no hace falta cuenta de Firebase. Si el 8080 lo usa otra cosa, se cambia con
// FS_PUERTO (y AUTH_PUERTO), por ejemplo `FS_PUERTO=8181 node firebase/test/vinculos.mjs`, con el mismo puerto en firebase.json.
import assert from "node:assert/strict";

const AUTH = `http://127.0.0.1:${process.env.AUTH_PUERTO ?? 9099}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=x`;
const BASE = "projects/nutrigajo/databases/(default)/documents";
const FS = `http://127.0.0.1:${process.env.FS_PUERTO ?? 8080}/v1/${BASE}`;

const valor = (x) => {
  if (typeof x === "boolean") return { booleanValue: x };
  if (Number.isInteger(x)) return { integerValue: String(x) };
  if (typeof x === "number") return { doubleValue: x };
  if (x && typeof x === "object") return { mapValue: { fields: campos(x) } };
  return { stringValue: x };
};
const campos = (d) => Object.fromEntries(Object.entries(d).filter(([, x]) => x !== undefined).map(([k, x]) => [k, valor(x)]));
const doc = (ruta, d) => ({ name: `${BASE}/${ruta}`, fields: campos(d) });
// una escritura que cambia solo esos campos, como el `update` de la app
const parcial = (ruta, d) => ({ update: doc(ruta, d), updateMask: { fieldPaths: Object.keys(d) } });

async function pedir(url, token, cuerpo) {
  const r = await fetch(url, {
    method: cuerpo ? "POST" : "GET",
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: cuerpo ? JSON.stringify(cuerpo) : undefined,
  });
  return r.status;
}
const commit = (token, ...writes) => pedir(`${FS}:commit`, token, { writes });
const leer = (ruta, token) => pedir(`${FS}/${ruta}`, token);
// el valor de un campo, leído con la llave de administración del emulador
async function campo(ruta, nombre) {
  const r = await fetch(`${FS}/${ruta}`, { headers: { Authorization: "Bearer owner" } });
  const d = await r.json();
  const v = d.fields?.[nombre];
  return v ? (v.booleanValue ?? v.stringValue ?? v.integerValue) : undefined;
}

async function cuenta(correo) {
  const r = await fetch(AUTH, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email: correo, password: "prueba-" + Math.random(), returnSecureToken: true }) });
  const d = await r.json();
  return { uid: d.localId, token: d.idToken, correo };
}

const sufijo = Math.random().toString(16).slice(2, 8);
const nutriologo = await cuenta(`nutri${sufijo}@prueba.test`);
const otroNutriologo = await cuenta(`nutri2${sufijo}@prueba.test`);
const paciente = await cuenta(`pac${sufijo}@prueba.test`);
const ajeno = await cuenta(`ajeno${sufijo}@prueba.test`);

const ahora = Date.now();
for (const [c, rol, nombre] of [
  [nutriologo, "NUTRIOLOGO", "Kelly Fernanda"],
  [otroNutriologo, "NUTRIOLOGO", "Otro Nutriólogo"],
  [paciente, "PACIENTE", "Paciente Uno"],
  [ajeno, "PACIENTE", "Paciente Ajeno"],
]) {
  await commit("owner", { update: doc(`usuarios/${c.uid}`, { nombre, correo: c.correo, rol, verificado: rol === "NUTRIOLOGO", activo: true }) });
}

// el registro del paciente ante Kelly; `activo: undefined` lo deja sin el campo (los de antes)
let k = 0;
const nuevoRegistro = async (extra = {}) => {
  const id = "p" + sufijo + k++;
  await commit("owner", {
    update: doc(`pacientes/${id}`, {
      nutriologoId: nutriologo.uid, nutriologoNombre: "Kelly Fernanda", usuarioId: paciente.uid, nombre: "Paciente Uno",
      correoVinculacion: paciente.correo, activo: true, fechaAlta: ahora, ultimaModificacion: ahora, ...extra,
    }),
  });
  return id;
};
const cierre = (por, extra = {}) => ({ activo: false, cerradoEn: ahora, cerradoPor: por, ultimaModificacion: ahora, ...extra });
const cerrado = () => nuevoRegistro({ activo: false, cerradoEn: ahora, cerradoPor: "PACIENTE" });

// ---- cerrar: quién y cómo
let id = await nuevoRegistro();
assert.equal(await commit(paciente.token, parcial(`pacientes/${id}`, cierre("PACIENTE"))), 200, "el paciente cierra su vínculo");
assert.equal(await campo(`pacientes/${id}`, "activo"), false, "quedó cerrado");
assert.equal(await campo(`pacientes/${id}`, "cerradoPor"), "PACIENTE", "dice quién lo cerró");

id = await nuevoRegistro();
assert.equal(await commit(nutriologo.token, parcial(`pacientes/${id}`, cierre("NUTRIOLOGO"))), 200, "el nutriólogo da de baja a su paciente");

id = await nuevoRegistro();
assert.equal(await commit(paciente.token, parcial(`pacientes/${id}`, cierre("NUTRIOLOGO"))), 403, "el paciente no firma como nutriólogo");
assert.equal(await commit(nutriologo.token, parcial(`pacientes/${id}`, cierre("PACIENTE"))), 403, "el nutriólogo no firma como paciente");
assert.equal(await commit(ajeno.token, parcial(`pacientes/${id}`, cierre("PACIENTE"))), 403, "un ajeno no cierra");
assert.equal(await commit(otroNutriologo.token, parcial(`pacientes/${id}`, cierre("NUTRIOLOGO"))), 403, "otro nutriólogo no da de baja");
assert.equal(await commit(paciente.token, parcial(`pacientes/${id}`, cierre("PACIENTE", { nombre: "Otro Nombre" }))), 403, "cerrar no toca otros campos");
assert.equal(await commit(paciente.token, parcial(`pacientes/${id}`, cierre("CUENTA_ELIMINADA"))), 403, "CUENTA_ELIMINADA solo la escribe el Worker");
assert.equal(await campo(`pacientes/${id}`, "activo"), true, "los intentos rechazados no lo cerraron");

id = await cerrado();
assert.equal(await commit(paciente.token, parcial(`pacientes/${id}`, { activo: true, ultimaModificacion: ahora })), 403, "un vínculo cerrado no se reabre");
assert.equal(await commit(nutriologo.token, parcial(`pacientes/${id}`, { activo: true, ultimaModificacion: ahora })), 403, "ni el nutriólogo lo reabre");
assert.equal(await commit(paciente.token, parcial(`pacientes/${id}`, cierre("PACIENTE"))), 403, "tampoco se cierra dos veces");

// ---- lo que se escribe en un vínculo activo y no en uno cerrado
const expediente = (por) => ({ expediente: { paso: 1, completo: false, actualizadoPor: por, actualizadoEn: ahora }, ultimaModificacion: ahora });
const metas = (por) => ({ metas: { aguaMl: 2000, pasos: 8000, kcal: 2000, minutos: 30, por, actualizadoEn: ahora } });
const ultimo = (uid) => ({ ultimoMensaje: { autorId: uid, enviadoEn: ahora, texto: "hola", foto: false } });
const mensaje = (uid) => ({ autorId: uid, texto: "hola", enviadoEn: ahora });
const plan = () => ({ enviadoPor: nutriologo.uid, nutriologoNombre: "Kelly Fernanda", enviadoEn: ahora, titulo: "Plan", contenido: {} });
const nuevo = (ruta, d) => ({ update: doc(ruta, d), currentDocument: { exists: false } });

const activo = await nuevoRegistro();
assert.equal(await commit(paciente.token, parcial(`pacientes/${activo}`, expediente("PACIENTE"))), 200, "el paciente escribe su expediente (activo)");
assert.equal(await commit(nutriologo.token, parcial(`pacientes/${activo}`, metas("NUTRIOLOGO"))), 200, "el nutriólogo fija las metas (activo)");
assert.equal(await commit(paciente.token, parcial(`pacientes/${activo}`, ultimo(paciente.uid))), 200, "el último mensaje (activo)");
assert.equal(await commit(paciente.token, nuevo(`pacientes/${activo}/mensajes/m1`, mensaje(paciente.uid))), 200, "un mensaje nuevo (activo)");
assert.equal(await commit(nutriologo.token, nuevo(`pacientes/${activo}/planes/pl1`, plan())), 200, "un plan nuevo (activo)");

const viejo = await cerrado();
assert.equal(await commit(paciente.token, parcial(`pacientes/${viejo}`, expediente("PACIENTE"))), 403, "el paciente no escribe el expediente de un vínculo cerrado");
assert.equal(await commit(nutriologo.token, parcial(`pacientes/${viejo}`, expediente("NUTRIOLOGO"))), 403, "el nutriólogo tampoco");
assert.equal(await commit(nutriologo.token, parcial(`pacientes/${viejo}`, metas("NUTRIOLOGO"))), 403, "no hay metas nuevas");
assert.equal(await commit(paciente.token, parcial(`pacientes/${viejo}`, metas("PACIENTE"))), 403, "ni las del paciente");
assert.equal(await commit(paciente.token, parcial(`pacientes/${viejo}`, ultimo(paciente.uid))), 403, "no hay último mensaje nuevo");
assert.equal(await commit(paciente.token, nuevo(`pacientes/${viejo}/mensajes/m1`, mensaje(paciente.uid))), 403, "el paciente no escribe en el chat cerrado");
assert.equal(await commit(nutriologo.token, nuevo(`pacientes/${viejo}/mensajes/m2`, mensaje(nutriologo.uid))), 403, "el nutriólogo tampoco");
assert.equal(await commit(nutriologo.token, nuevo(`pacientes/${viejo}/planes/pl1`, plan())), 403, "no hay planes nuevos en un vínculo cerrado");

// ---- pero lo cerrado se lee: los dos dueños, y nadie más
await commit("owner", nuevo(`pacientes/${viejo}/mensajes/m0`, mensaje(nutriologo.uid)), nuevo(`pacientes/${viejo}/planes/pl0`, plan()));
for (const [quien, c] of [["el paciente", paciente], ["el nutriólogo", nutriologo]]) {
  assert.equal(await leer(`pacientes/${viejo}`, c.token), 200, `${quien} lee el registro cerrado`);
  assert.equal(await leer(`pacientes/${viejo}/mensajes/m0`, c.token), 200, `${quien} lee el chat cerrado`);
  assert.equal(await leer(`pacientes/${viejo}/planes/pl0`, c.token), 200, `${quien} lee los planes cerrados`);
}
assert.equal(await leer(`pacientes/${viejo}`, ajeno.token), 403, "un ajeno no lo lee");

// ---- los registros de antes no traen `activo`: cuentan como activos
const legado = await nuevoRegistro({ activo: undefined });
assert.equal(await commit(paciente.token, parcial(`pacientes/${legado}`, expediente("PACIENTE"))), 200, "un registro sin `activo` sigue aceptando el expediente");
assert.equal(await commit(paciente.token, parcial(`pacientes/${legado}`, cierre("PACIENTE"))), 200, "y se puede cerrar");

// ---- alta y cambio de nutriólogo: un código nuevo de Otro Nutriólogo por intento
let c = 0;
const codigo = async (extra = {}) => {
  const id_ = "K" + sufijo + c++;
  await commit("owner", { update: doc(`codigosVinculacion/${id_}`, { nutriologoId: otroNutriologo.uid, nutriologoNombre: "Otro Nutriólogo", creadoEn: ahora, expiraEn: ahora + 86_400_000, usado: false, ...extra }) });
  return id_;
};
const alta = (cod, extra = {}) => nuevo(`pacientes/${"n" + sufijo + c++}`, {
  nutriologoId: otroNutriologo.uid, nutriologoNombre: "Otro Nutriólogo", usuarioId: paciente.uid, nombre: "Paciente Uno",
  correoVinculacion: paciente.correo.toLowerCase(), codigoVinculacion: cod, activo: true, fechaAlta: ahora, ultimaModificacion: ahora, ...extra,
});
const canje = (cod) => parcial(`codigosVinculacion/${cod}`, { usado: true, canjeadoPor: paciente.uid, canjeadoEn: ahora });

let cod = await codigo();
assert.equal(await commit(paciente.token, canje(cod), alta(cod, { expediente: { paso: 3, completo: true, actualizadoPor: "PACIENTE", actualizadoEn: ahora } })), 200, "el alta puede llevar el expediente copiado, firmado por el paciente");
cod = await codigo();
assert.equal(await commit(paciente.token, canje(cod), alta(cod, { expediente: { paso: 3, completo: true, actualizadoPor: "NUTRIOLOGO", actualizadoEn: ahora } })), 403, "pero no firmado por un nutriólogo");
cod = await codigo();
assert.equal(await commit(paciente.token, canje(cod), alta(cod, { expediente: { paso: 99, completo: true, actualizadoPor: "PACIENTE", actualizadoEn: ahora } })), 403, "ni con un expediente inválido");
cod = await codigo();
assert.equal(await commit(paciente.token, canje(cod), alta(cod)), 200, "el alta de siempre (sin expediente) sigue pasando");

// el cambio de nutriólogo es un solo lote: cerrar el anterior + canjear + crear el nuevo
let anterior = await nuevoRegistro();
cod = await codigo();
assert.equal(await commit(paciente.token, parcial(`pacientes/${anterior}`, cierre("PACIENTE")), canje(cod), alta(cod)), 200, "el cambio con un código bueno pasa entero");
assert.equal(await campo(`pacientes/${anterior}`, "activo"), false, "y el anterior quedó cerrado");

anterior = await nuevoRegistro();
cod = await codigo({ usado: true });
assert.equal(await commit(paciente.token, parcial(`pacientes/${anterior}`, cierre("PACIENTE")), canje(cod), alta(cod)), 403, "con un código ya usado el lote entero se rechaza");
assert.equal(await campo(`pacientes/${anterior}`, "activo"), true, "y el vínculo anterior sigue activo");

console.log("reglas de vínculos: bien");
