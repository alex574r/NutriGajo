// Prueba de las reglas de las citas (citas/{id}, directorio/{nutriólogo}/ocupados/{inicio}, citasActivas/{uid}) contra los emuladores de
// Auth y Firestore (con firebase/firestore.rules cargado):
//   firebase emulators:start --only auth,firestore --project nutrigajo      (en otra terminal)
//   node firebase/test/citas.mjs
// Los puertos son 9099 (Auth) y 8080 (Firestore); no hace falta cuenta de Firebase.
// Cubre sobre todo la cita que agenda el nutriólogo a un paciente suyo (nace confirmada, con `origen`, `pacienteId` y `nota`) y que lo de
// siempre —pedir, cancelar, reagendar— no cambió.
import assert from "node:assert/strict";

const AUTH = "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=x";
const BASE = "projects/nutrigajo/databases/(default)/documents";
const FS = `http://127.0.0.1:8080/v1/${BASE}`;

const valor = (x) => {
  if (typeof x === "boolean") return { booleanValue: x };
  if (Number.isInteger(x)) return { integerValue: String(x) };
  if (typeof x === "number") return { doubleValue: x };
  if (x && typeof x === "object") return { mapValue: { fields: campos(x) } };
  return { stringValue: x };
};
const campos = (d) => Object.fromEntries(Object.entries(d).filter(([, x]) => x !== undefined).map(([k, x]) => [k, valor(x)]));
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

async function cuenta(correo) {
  const r = await fetch(AUTH, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email: correo, password: "prueba-" + Math.random(), returnSecureToken: true }) });
  const d = await r.json();
  return { uid: d.localId, token: d.idToken, correo };
}

const sufijo = Math.random().toString(16).slice(2, 8);
const nutriologo = await cuenta(`nutri${sufijo}@prueba.test`);
const otroNutriologo = await cuenta(`nutri2${sufijo}@prueba.test`);
const sinDirectorio = await cuenta(`nutri3${sufijo}@prueba.test`);
const pacienteUno = await cuenta(`pac1${sufijo}@prueba.test`);
const pacienteDos = await cuenta(`pac2${sufijo}@prueba.test`);
const pacienteAjeno = await cuenta(`pac3${sufijo}@prueba.test`);

const ahora = Date.now();
const nombres = new Map();
for (const [c, rol, nombre, verificado] of [
  [nutriologo, "NUTRIOLOGO", "Kelly Fernanda", true],
  [otroNutriologo, "NUTRIOLOGO", "Otro Nutriólogo", true],
  [sinDirectorio, "NUTRIOLOGO", "Sin Mapa", false],
  [pacienteUno, "PACIENTE", "Paciente Uno", false],
  [pacienteDos, "PACIENTE", "Paciente Dos", false],
  [pacienteAjeno, "PACIENTE", "Paciente Ajeno", false],
]) {
  nombres.set(c.uid, nombre);
  await commit("owner", { update: doc(`usuarios/${c.uid}`, { nombre, correo: c.correo, rol, verificado, activo: true }) });
}
for (const n of [nutriologo, otroNutriologo]) {
  await commit("owner", { update: doc(`directorio/${n.uid}`, { nutriologoId: n.uid, nombre: nombres.get(n.uid), lat: 19.3, lng: -99.6, cedula: "12345678", actualizadoEn: ahora }) });
}
// el registro de cada paciente ante su nutriólogo: de quién es y qué cuenta es
const registro = (id, n, p) => commit("owner", { update: doc(`pacientes/${id}`, { nutriologoId: n.uid, nutriologoNombre: nombres.get(n.uid), usuarioId: p.uid, nombre: nombres.get(p.uid), correo: p.correo, fechaAlta: ahora }) });
const idUno = "r1" + sufijo, idDos = "r2" + sufijo, idAjeno = "r3" + sufijo;
await registro(idUno, nutriologo, pacienteUno);
await registro(idDos, nutriologo, pacienteDos);
await registro(idAjeno, otroNutriologo, pacienteAjeno);

// ---- horas: cuartos de hora en el futuro, cada caso la suya para que las reservas no se pisen
const UNA_HORA = 3_600_000;
const base = Math.ceil((ahora + 2 * 86_400_000) / 900_000) * 900_000;
let k = 0;
const hora = () => base + UNA_HORA * k++;
let n = 0;
const nuevoId = () => "c" + sufijo + n++;

// la cita que arma la app del nutriólogo, con los campos que se le quieran cambiar
const citaDelNutriologo = (p, registroId, inicio, extra = {}) => ({
  nutriologoId: nutriologo.uid, nutriologoNombre: nombres.get(nutriologo.uid), usuarioId: p.uid, pacienteNombre: nombres.get(p.uid), pacienteId: registroId,
  inicio, estado: "CONFIRMADA", origen: "NUTRIOLOGO", creadaEn: ahora, actualizadaEn: ahora, ...extra,
});
// sin precondición, como el `set` de la app: si la hora ya estaba reservada, es una actualización y las reglas la rechazan
const reserva = (n_, citaId, inicio) => ({ update: doc(`directorio/${n_.uid}/ocupados/${inicio}`, { citaId, inicio }) });
const agendar = (c, datos, { citaId = nuevoId(), conReserva = true } = {}) =>
  commit(c.token, { update: doc(`citas/${citaId}`, datos), currentDocument: { exists: false } }, ...(conReserva ? [reserva(nutriologo, citaId, datos.inicio)] : []));
const agendada = async (p, registroId, extra = {}) => {
  const citaId = nuevoId();
  const inicio = hora();
  assert.equal(await agendar(nutriologo, citaDelNutriologo(p, registroId, inicio, extra), { citaId }), 200, "se agenda la cita de partida");
  return { citaId, inicio };
};

// ---- el nutriólogo agenda: quién
let inicio = hora();
assert.equal(await agendar(nutriologo, citaDelNutriologo(pacienteUno, idUno, inicio)), 200, "el nutriólogo agenda a su paciente");
assert.equal(await agendar(nutriologo, citaDelNutriologo(pacienteAjeno, idAjeno, hora())), 403, "no agenda al paciente de otro nutriólogo");
assert.equal(await agendar(nutriologo, citaDelNutriologo(pacienteUno, idUno, hora(), { pacienteNombre: "Otro Nombre" })), 403, "el nombre es el del registro del paciente");
assert.equal(await agendar(nutriologo, citaDelNutriologo(pacienteDos, idUno, hora())), 403, "el registro tiene que ser de esa cuenta");
assert.equal(await agendar(nutriologo, citaDelNutriologo(pacienteUno, idUno, hora(), { nutriologoNombre: "Otro" })), 403, "el nombre del consultorio es el registrado");
assert.equal(await agendar(nutriologo, citaDelNutriologo(pacienteUno, "no-existe", hora())), 403, "el registro del paciente existe");
assert.equal(await agendar(otroNutriologo, citaDelNutriologo(pacienteUno, idUno, hora())), 403, "no se agenda a nombre de otro nutriólogo");
// un nutriólogo sin consultorio en el mapa
await commit("owner", { update: doc(`pacientes/${"r4" + sufijo}`, { nutriologoId: sinDirectorio.uid, nutriologoNombre: "Sin Mapa", usuarioId: pacienteUno.uid, nombre: nombres.get(pacienteUno.uid), correo: pacienteUno.correo, fechaAlta: ahora }) });
{
  const citaId = nuevoId();
  const i = hora();
  const datos = { ...citaDelNutriologo(pacienteUno, "r4" + sufijo, i), nutriologoId: sinDirectorio.uid, nutriologoNombre: "Sin Mapa" };
  assert.equal(await commit(sinDirectorio.token, { update: doc(`citas/${citaId}`, datos), currentDocument: { exists: false } }, reserva(sinDirectorio, citaId, i)), 403, "sin consultorio en el mapa no agenda");
}

// ---- la hora se reserva en el mismo lote y no se comparte
assert.equal(await agendar(nutriologo, citaDelNutriologo(pacienteDos, idDos, inicio)), 403, "la misma hora no se agenda dos veces");
assert.equal(await agendar(nutriologo, citaDelNutriologo(pacienteDos, idDos, hora()), { conReserva: false }), 403, "sin reservar la hora no se agenda");
{
  const citaId = nuevoId();
  const i = hora();
  assert.equal(await commit(nutriologo.token, { update: doc(`citas/${citaId}`, citaDelNutriologo(pacienteDos, idDos, i)), currentDocument: { exists: false } }, reserva(nutriologo, "otra-cita", i)), 403, "la reserva es de esa cita");
}

// ---- el paciente no puede hacerse pasar por el nutriólogo, y lo de siempre sigue igual
const delPaciente = (p, inicio_, extra = {}) => ({
  nutriologoId: nutriologo.uid, nutriologoNombre: nombres.get(nutriologo.uid), usuarioId: p.uid, pacienteNombre: nombres.get(p.uid),
  inicio: inicio_, estado: "PENDIENTE", creadaEn: ahora, actualizadaEn: ahora, ...extra,
});
const pedirCita = (p, datos, citaId = nuevoId()) =>
  commit(p.token, { update: doc(`citas/${citaId}`, datos), currentDocument: { exists: false } }, reserva(nutriologo, citaId, datos.inicio), { update: doc(`citasActivas/${p.uid}`, { citaId }) });
assert.equal(await pedirCita(pacienteAjeno, delPaciente(pacienteAjeno, hora(), { estado: "CONFIRMADA" })), 403, "un paciente no crea citas confirmadas");
assert.equal(await pedirCita(pacienteAjeno, delPaciente(pacienteAjeno, hora(), { origen: "NUTRIOLOGO" })), 403, "ni con origen de nutriólogo");
assert.equal(await pedirCita(pacienteAjeno, delPaciente(pacienteAjeno, hora(), { nota: "hola" })), 403, "ni con nota");
assert.equal(await pedirCita(pacienteAjeno, delPaciente(pacienteAjeno, hora())), 200, "la cita que pide el paciente sigue funcionando");

// ---- lo que vale
const intento = (extra, p = pacienteUno, id = idUno) => agendar(nutriologo, citaDelNutriologo(p, id, hora(), extra));
assert.equal(await intento({ nota: "a".repeat(120) }), 200, "una nota de 120 caracteres");
assert.equal(await intento({ nota: "a".repeat(121) }), 403, "una nota de 121 caracteres");
assert.equal(await intento({ nota: 5 }), 403, "la nota es texto");
assert.equal(await intento({ inicio: ahora - 3_600_000 - ((ahora - 3_600_000) % 900_000) }), 403, "una hora pasada");
assert.equal(await intento({ inicio: base + 61 * 86_400_000 - (base % 900_000) }), 403, "a más de 60 días");
assert.equal(await agendar(nutriologo, citaDelNutriologo(pacienteUno, idUno, hora() + 60_000)), 403, "la hora cae en cuartos de hora");
assert.equal(await intento({ estado: "PENDIENTE" }), 403, "nace confirmada");
assert.equal(await intento({ origen: "PACIENTE" }), 403, "el origen es del nutriólogo");
assert.equal(await intento({ campoDeMas: 1 }), 403, "campos de más");
{
  const { pacienteId, ...sinRegistro } = citaDelNutriologo(pacienteUno, idUno, hora());
  assert.equal(await agendar(nutriologo, sinRegistro), 403, "falta el registro del paciente");
}

// ---- después de agendada
{
  const { citaId, inicio: i } = await agendada(pacienteUno, idUno);
  const cancelar = (c, id, ini) => commit(c.token,
    { update: doc(`citas/${id}`, { estado: "CANCELADA", actualizadaEn: Date.now() }), updateMask: { fieldPaths: ["estado", "actualizadaEn"] }, currentDocument: { exists: true } },
    { delete: `${BASE}/directorio/${nutriologo.uid}/ocupados/${ini}` });
  assert.equal(await cancelar(otroNutriologo, citaId, i), 403, "otro nutriólogo no la cancela");
  assert.equal(await cancelar(nutriologo, citaId, i), 200, "el nutriólogo la cancela y libera la hora");
  assert.equal(await agendar(nutriologo, citaDelNutriologo(pacienteDos, idDos, i)), 200, "la hora liberada se puede agendar otra vez");
}
{
  const { citaId, inicio: i } = await agendada(pacienteUno, idUno);
  const cancelaElPaciente = commit(pacienteUno.token,
    { update: doc(`citas/${citaId}`, { estado: "CANCELADA", actualizadaEn: Date.now() }), updateMask: { fieldPaths: ["estado", "actualizadaEn"] }, currentDocument: { exists: true } },
    { delete: `${BASE}/directorio/${nutriologo.uid}/ocupados/${i}` });
  assert.equal(await cancelaElPaciente, 200, "el paciente cancela la cita que le agendaron");
}
{
  const { citaId, inicio: i } = await agendada(pacienteUno, idUno);
  const nueva = hora();
  const reagendar = (p) => commit(p.token,
    { update: doc(`citas/${citaId}`, { inicio: nueva, estado: "PENDIENTE", actualizadaEn: Date.now() }), updateMask: { fieldPaths: ["inicio", "estado", "actualizadaEn"] }, currentDocument: { exists: true } },
    reserva(nutriologo, citaId, nueva),
    { delete: `${BASE}/directorio/${nutriologo.uid}/ocupados/${i}` });
  assert.equal(await reagendar(pacienteDos), 403, "otro paciente no la mueve");
  assert.equal(await reagendar(pacienteUno), 200, "el paciente la pasa a otra hora y espera la confirmación");
}

// ---- el candado es de la cita que pidió el paciente: la del nutriólogo no lo toca al cancelarse
{
  const x = nuevoId();
  assert.equal(await pedirCita(pacienteDos, delPaciente(pacienteDos, hora()), x), 200, "el paciente pide su cita (y queda con candado)");
  const { citaId, inicio: i } = await agendada(pacienteDos, idDos);
  const cancelacion = { update: doc(`citas/${citaId}`, { estado: "CANCELADA", actualizadaEn: Date.now() }), updateMask: { fieldPaths: ["estado", "actualizadaEn"] }, currentDocument: { exists: true } };
  const liberar = { delete: `${BASE}/directorio/${nutriologo.uid}/ocupados/${i}` };
  assert.equal(await commit(nutriologo.token, cancelacion, liberar, { delete: `${BASE}/citasActivas/${pacienteDos.uid}` }), 403, "borrar el candado de otra cita hace fallar el lote");
  assert.equal(await commit(nutriologo.token, cancelacion, liberar), 200, "sin tocar el candado, el nutriólogo cancela la suya");
}

console.log("citas.mjs: ok");
