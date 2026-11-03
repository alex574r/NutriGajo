// Prueba de las reglas del banco global de platillos (platillos/{id}) contra los emuladores de Auth y Firestore (con
// firebase/firestore.rules cargado):
//   firebase emulators:start --only auth,firestore --project nutrigajo      (en otra terminal)
//   node firebase/test/platillos.mjs
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
const leer = (token, ruta) => pedir(`${FS}/${ruta}`, token);
// los campos de un documento, leídos como «owner» (se salta las reglas)
const camposDe = async (ruta) => (await (await fetch(`${FS}/${ruta}`, { headers: { Authorization: "Bearer owner" } })).json()).fields;

async function cuenta(correo) {
  const r = await fetch(AUTH, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email: correo, password: "prueba-" + Math.random(), returnSecureToken: true }) });
  const d = await r.json();
  return { uid: d.localId, token: d.idToken, correo };
}

const sufijo = Math.random().toString(16).slice(2, 8);
const nutriologo = await cuenta(`nutri${sufijo}@prueba.test`);
const entrenador = await cuenta(`entre${sufijo}@prueba.test`);
const otroEntrenador = await cuenta(`entre2${sufijo}@prueba.test`);
const entrenadorSinVerificar = await cuenta(`entre3${sufijo}@prueba.test`);
const nutriologoSinVerificar = await cuenta(`nutri2${sufijo}@prueba.test`);
const paciente = await cuenta(`pac${sufijo}@prueba.test`);
const admin = await cuenta(`admin${sufijo}@prueba.test`);

const ahora = Date.now();
const perfil = (c, rol, verificado) => ({ nombre: "Persona " + rol, correo: c.correo, rol, verificado, activo: true });
for (const [c, rol, verificado] of [
  [nutriologo, "NUTRIOLOGO", true],
  [entrenador, "ENTRENADOR", true],
  [otroEntrenador, "ENTRENADOR", true],
  [entrenadorSinVerificar, "ENTRENADOR", false],
  [nutriologoSinVerificar, "NUTRIOLOGO", false],
  [paciente, "PACIENTE", false],
  [admin, "ADMIN", true],
]) await commit("owner", { update: doc(`usuarios/${c.uid}`, perfil(c, rol, verificado)) });

const platillo = (c, rol, extra = {}) => ({
  autorId: c.uid, autorNombre: "Persona " + rol, autorRol: rol, nombre: "Ensalada con pechuga", resumen: "Hojas verdes con pechuga a la plancha.",
  momento: "COMIDA", proteinaG: 25, grasasG: 17, carbosG: 12, kcal: 300, minutos: 20,
  ingredientes: ["150 g de pechuga", "2 tazas de lechuga"], pasos: ["Dora la pechuga", "Mezcla todo"], etiquetas: ["ALTO_PROTEINA"],
  fotoVersion: ahora, estado: "EN_REVISION", creadoEn: ahora, actualizadoEn: ahora, ...extra,
});
const deEntrenador = (extra) => platillo(entrenador, "ENTRENADOR", extra);
const deNutriologo = (extra) => platillo(nutriologo, "NUTRIOLOGO", extra);
let n = 0;
const nuevoId = () => "p" + sufijo + n++;
const crear = (c, d, id = nuevoId()) => commit(c.token, { update: doc(`platillos/${id}`, d), currentDocument: { exists: false } });
const sembrar = async (d) => {
  const id = nuevoId();
  await commit("owner", { update: doc(`platillos/${id}`, d) });
  return id;
};

// ---- crear: quién
assert.equal(await crear(nutriologo, deNutriologo()), 200, "el nutriólogo verificado manda uno a revisión");
assert.equal(await crear(entrenador, deEntrenador()), 200, "el entrenador verificado manda uno a revisión");
assert.equal(await crear(entrenador, deEntrenador({ estado: "BORRADOR", resumen: "", ingredientes: [], pasos: [], etiquetas: [], fotoVersion: undefined })), 200, "y guarda un borrador a medias");
assert.equal(await crear(entrenadorSinVerificar, platillo(entrenadorSinVerificar, "ENTRENADOR")), 403, "el entrenador sin verificar no crea");
assert.equal(await crear(nutriologoSinVerificar, platillo(nutriologoSinVerificar, "NUTRIOLOGO")), 403, "el nutriólogo sin verificar no crea");
assert.equal(await crear(paciente, platillo(paciente, "NUTRIOLOGO")), 403, "un paciente no crea");
assert.equal(await crear(admin, platillo(admin, "NUTRIOLOGO")), 403, "el ADMIN no crea (revisa)");
assert.equal(await crear(entrenador, platillo(otroEntrenador, "ENTRENADOR")), 403, "no se firma como otro autor");
assert.equal(await crear(entrenador, deEntrenador({ autorRol: "NUTRIOLOGO" })), 403, "un entrenador no se hace pasar por nutriólogo");
assert.equal(await crear(nutriologo, deNutriologo({ autorRol: "ENTRENADOR" })), 403, "ni un nutriólogo por entrenador");

// ---- crear: el estado con el que nace
assert.equal(await crear(entrenador, deEntrenador({ estado: "PUBLICADO" })), 403, "no se publica solo");
assert.equal(await crear(entrenador, deEntrenador({ estado: "RECHAZADO" })), 403, "no nace rechazado");
assert.equal(await crear(entrenador, deEntrenador({ publicadoEn: ahora })), 403, "no trae fecha de publicación");
assert.equal(await crear(entrenador, deEntrenador({ revisadoPor: admin.uid })), 403, "no trae revisor");
assert.equal(await crear(entrenador, deEntrenador({ motivoRechazo: "x" })), 403, "no trae motivo de rechazo");

// ---- crear: lo que vale
assert.equal(await crear(entrenador, deEntrenador({ nombre: "ab" })), 403, "nombre muy corto");
assert.equal(await crear(entrenador, deEntrenador({ nombre: "x".repeat(81) })), 403, "nombre muy largo");
assert.equal(await crear(entrenador, deEntrenador({ resumen: "x".repeat(301) })), 403, "resumen muy largo");
assert.equal(await crear(entrenador, deEntrenador({ resumen: "corto" })), 403, "a revisión pide resumen de 10 letras");
assert.equal(await crear(entrenador, deEntrenador({ ingredientes: [] })), 403, "a revisión pide al menos un ingrediente");
assert.equal(await crear(entrenador, deEntrenador({ ingredientes: Array.from({ length: 21 }, () => "sal") })), 403, "más de 20 ingredientes");
assert.equal(await crear(entrenador, deEntrenador({ ingredientes: "sal" })), 403, "los ingredientes son una lista");
assert.equal(await crear(entrenador, deEntrenador({ pasos: [] })), 403, "a revisión pide al menos un paso");
assert.equal(await crear(entrenador, deEntrenador({ pasos: Array.from({ length: 13 }, () => "paso") })), 403, "más de 12 pasos");
assert.equal(await crear(entrenador, deEntrenador({ fotoVersion: undefined })), 403, "a revisión pide foto");
assert.equal(await crear(entrenador, deEntrenador({ fotoVersion: "v1" })), 403, "la versión de la foto es un número");
assert.equal(await crear(entrenador, deEntrenador({ momento: "COLACION" })), 403, "momento desconocido");
assert.equal(await crear(entrenador, deEntrenador({ proteinaG: 501 })), 403, "demasiada proteína");
assert.equal(await crear(entrenador, deEntrenador({ grasasG: -1 })), 403, "grasas negativas");
assert.equal(await crear(entrenador, deEntrenador({ carbosG: 2.5 })), 403, "los macros son enteros");
assert.equal(await crear(entrenador, deEntrenador({ kcal: 3001 })), 403, "demasiadas calorías");
assert.equal(await crear(entrenador, deEntrenador({ minutos: 0 })), 403, "minutos mínimos");
assert.equal(await crear(entrenador, deEntrenador({ minutos: 241 })), 403, "minutos máximos");
assert.equal(await crear(entrenador, deEntrenador({ etiquetas: ["SIN_GLUTEN", "SIN_LACTOSA", "VEGETARIANO", "VEGANO", "ALTO_PROTEINA"] })), 403, "más de 4 etiquetas");
assert.equal(await crear(entrenador, deEntrenador({ etiquetas: ["KETO"] })), 403, "etiqueta desconocida");
assert.equal(await crear(entrenador, deEntrenador({ campoDeMas: 1 })), 403, "campos de más");
const { kcal, ...sinKcal } = deEntrenador();
assert.equal(await crear(entrenador, sinKcal), 403, "faltan campos");

// ---- quién lo ve
const publicado = await sembrar(deEntrenador({ estado: "PUBLICADO", publicadoEn: ahora, revisadoPor: admin.uid, revisadoEn: ahora }));
const enRevision = await sembrar(deEntrenador());
const borrador = await sembrar(deEntrenador({ estado: "BORRADOR" }));
assert.equal(await leer(paciente.token, `platillos/${publicado}`), 200, "cualquier sesión ve lo publicado");
assert.equal(await leer(null, `platillos/${publicado}`), 403, "sin sesión no ve nada");
assert.equal(await leer(paciente.token, `platillos/${enRevision}`), 403, "lo que está en revisión no lo ve un paciente");
assert.equal(await leer(otroEntrenador.token, `platillos/${borrador}`), 403, "ni otro entrenador ve el borrador");
assert.equal(await leer(entrenador.token, `platillos/${borrador}`), 200, "su autor ve el borrador");
assert.equal(await leer(admin.token, `platillos/${enRevision}`), 200, "el ADMIN ve lo que revisa");

// ---- el autor edita lo suyo mientras sea borrador o rechazado
const editar = (c, id, d) => commit(c.token, { update: doc(`platillos/${id}`, d), currentDocument: { exists: true } });
assert.equal(await editar(entrenador, borrador, deEntrenador({ estado: "BORRADOR", nombre: "Ensalada con salmón" })), 200, "edita su borrador");
assert.equal(await editar(entrenador, borrador, deEntrenador({ estado: "EN_REVISION", nombre: "Ensalada con salmón" })), 200, "lo manda a revisión");
assert.equal(await editar(entrenador, borrador, deEntrenador({ estado: "BORRADOR" })), 403, "en revisión no se edita (se saca primero)");
assert.equal(await editar(entrenador, publicado, deEntrenador({ estado: "BORRADOR", publicadoEn: ahora, revisadoPor: admin.uid, revisadoEn: ahora })), 403, "lo publicado es de solo lectura");
assert.equal(await editar(otroEntrenador, enRevision, platillo(otroEntrenador, "ENTRENADOR", { estado: "BORRADOR" })), 403, "otro no edita lo ajeno");
const rechazado = await sembrar(deEntrenador({ estado: "RECHAZADO", motivoRechazo: "La foto no muestra el platillo", revisadoPor: admin.uid, revisadoEn: ahora }));
assert.equal(await editar(entrenador, rechazado, deEntrenador({ estado: "EN_REVISION", revisadoPor: admin.uid, revisadoEn: ahora })), 200, "corrige uno rechazado y lo reenvía");
const despues = await camposDe(`platillos/${rechazado}`);
assert.equal(despues.revisadoPor.stringValue, admin.uid, "el reenvío conserva quién lo rechazó");
assert.equal(despues.revisadoEn.integerValue, String(ahora), "y cuándo");
const rechazado2 = await sembrar(deEntrenador({ estado: "RECHAZADO", motivoRechazo: "Falta el resumen", revisadoPor: admin.uid, revisadoEn: ahora }));
assert.equal(await editar(entrenador, rechazado2, deEntrenador({ estado: "EN_REVISION", revisadoPor: otroEntrenador.uid, revisadoEn: ahora })), 403, "no cambia al revisor");
assert.equal(await editar(entrenador, rechazado2, deEntrenador({ estado: "EN_REVISION", revisadoEn: ahora })), 403, "ni lo borra");
assert.equal(await editar(entrenador, rechazado2, deEntrenador({ estado: "EN_REVISION", creadoEn: ahora + 1, revisadoPor: admin.uid, revisadoEn: ahora })), 403, "la fecha de creación no cambia");
assert.equal(await editar(entrenador, rechazado2, deEntrenador({ estado: "EN_REVISION", autorRol: "NUTRIOLOGO", revisadoPor: admin.uid, revisadoEn: ahora })), 403, "el rol del autor no cambia");

// ---- el autor retira lo publicado o saca de revisión lo suyo
const soloEstado = (c, id, estado) => commit(c.token, { update: doc(`platillos/${id}`, { estado, actualizadoEn: ahora + 5 }), updateMask: { fieldPaths: ["estado", "actualizadoEn"] }, currentDocument: { exists: true } });
const aRetirar = await sembrar(deEntrenador({ estado: "PUBLICADO", publicadoEn: ahora, revisadoPor: admin.uid, revisadoEn: ahora }));
assert.equal(await soloEstado(otroEntrenador, aRetirar, "RETIRADO"), 403, "otro no retira lo ajeno");
assert.equal(await soloEstado(entrenador, aRetirar, "PUBLICADO"), 403, "no vuelve a publicarse solo");
assert.equal(await soloEstado(entrenador, aRetirar, "RETIRADO"), 200, "su autor retira lo publicado");
const aSacar = await sembrar(deEntrenador());
assert.equal(await soloEstado(entrenador, aSacar, "BORRADOR"), 200, "saca de revisión lo suyo");
assert.equal(await soloEstado(entrenador, aSacar, "PUBLICADO"), 403, "de borrador no pasa a publicado");
const delNutriologo = await sembrar(deNutriologo({ estado: "PUBLICADO", publicadoEn: ahora, revisadoPor: admin.uid, revisadoEn: ahora }));
assert.equal(await soloEstado(nutriologo, delNutriologo, "RETIRADO"), 200, "el nutriólogo retira lo suyo");

// ---- el ADMIN publica, rechaza con motivo y retira
const revisar = (id, extra, mascara) => commit(admin.token, { update: doc(`platillos/${id}`, extra), updateMask: { fieldPaths: mascara }, currentDocument: { exists: true } });
const publicar = (id, extra = {}) => revisar(id, { estado: "PUBLICADO", publicadoEn: ahora, revisadoPor: admin.uid, revisadoEn: ahora, actualizadoEn: ahora, ...extra }, ["estado", "publicadoEn", "revisadoPor", "revisadoEn", "actualizadoEn"]);
const rechazar = (id, extra = {}) => revisar(id, { estado: "RECHAZADO", motivoRechazo: "La foto es borrosa", revisadoPor: admin.uid, revisadoEn: ahora, actualizadoEn: ahora, ...extra }, ["estado", "motivoRechazo", "revisadoPor", "revisadoEn", "actualizadoEn"]);
const porPublicar = await sembrar(deEntrenador());
const porRechazar = await sembrar(deEntrenador());
assert.equal(await commit(entrenador.token, { update: doc(`platillos/${porPublicar}`, { estado: "PUBLICADO", publicadoEn: ahora, revisadoPor: entrenador.uid, revisadoEn: ahora, actualizadoEn: ahora }), updateMask: { fieldPaths: ["estado", "publicadoEn", "revisadoPor", "revisadoEn", "actualizadoEn"] }, currentDocument: { exists: true } }), 403, "el autor no se publica a sí mismo");
assert.equal(await publicar(porPublicar, { revisadoPor: entrenador.uid }), 403, "el revisor es quien publica");
assert.equal(await publicar(await sembrar(deEntrenador({ estado: "BORRADOR" }))), 403, "un borrador no se publica");
assert.equal(await publicar(porPublicar), 200, "el ADMIN publica");
assert.equal(await leer(paciente.token, `platillos/${porPublicar}`), 200, "y ya lo ve el paciente");
assert.equal(await rechazar(porRechazar, { motivoRechazo: "" }), 403, "rechazar pide motivo");
assert.equal(await rechazar(porRechazar, { motivoRechazo: "x".repeat(201) }), 403, "motivo muy largo");
assert.equal(await rechazar(porRechazar), 200, "el ADMIN rechaza con motivo");
assert.equal(await rechazar(porPublicar), 403, "lo ya publicado no se rechaza (se retira)");
assert.equal(await publicar(porRechazar), 403, "lo rechazado no se publica sin que el autor lo reenvíe");
assert.equal(await revisar(porPublicar, { estado: "RETIRADO", revisadoPor: admin.uid, revisadoEn: ahora, actualizadoEn: ahora }, ["estado", "revisadoPor", "revisadoEn", "actualizadoEn"]), 200, "el ADMIN retira lo publicado");
assert.equal(await revisar(enRevision, { nombre: "Cambiado por el ADMIN" }, ["nombre"]), 403, "el ADMIN no reescribe contenido ajeno");

// ---- borrar
const borrar = (c, id) => commit(c.token, { delete: `${BASE}/platillos/${id}` });
assert.equal(await borrar(otroEntrenador, rechazado2), 403, "otro no borra");
assert.equal(await borrar(entrenador, rechazado2), 200, "el autor borra un rechazado");
assert.equal(await borrar(entrenador, aSacar), 200, "el autor borra un borrador");
assert.equal(await borrar(entrenador, publicado), 403, "lo publicado no se borra (se retira)");
assert.equal(await borrar(entrenador, enRevision), 403, "lo que está en revisión tampoco");
assert.equal(await borrar(admin, aRetirar), 403, "el ADMIN no borra");

console.log("platillos: todas las pruebas pasaron");
