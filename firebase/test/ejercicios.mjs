// Prueba de las reglas del banco global de ejercicios (ejercicios/{id}) y de la verificación del entrenador (verificaciones/{uid})
// contra los emuladores de Auth y Firestore (con firebase/firestore.rules cargado):
//   firebase emulators:start --only auth,firestore --project nutrigajo      (en otra terminal)
//   node firebase/test/ejercicios.mjs
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

async function cuenta(correo) {
  const r = await fetch(AUTH, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email: correo, password: "prueba-" + Math.random(), returnSecureToken: true }) });
  const d = await r.json();
  return { uid: d.localId, token: d.idToken, correo };
}

const sufijo = Math.random().toString(16).slice(2, 8);
const entrenador = await cuenta(`entre${sufijo}@prueba.test`);
const sinVerificar = await cuenta(`nuevo${sufijo}@prueba.test`);
const otroEntrenador = await cuenta(`entre2${sufijo}@prueba.test`);
const paciente = await cuenta(`pac${sufijo}@prueba.test`);
const admin = await cuenta(`admin${sufijo}@prueba.test`);
const aspirante = await cuenta(`aspira${sufijo}@prueba.test`);

const ahora = Date.now();
const perfil = (c, rol, verificado) => ({ nombre: "Persona " + rol, correo: c.correo, rol, verificado, activo: true });
for (const [c, rol, verificado] of [
  [entrenador, "ENTRENADOR", true],
  [sinVerificar, "ENTRENADOR", false],
  [otroEntrenador, "ENTRENADOR", true],
  [paciente, "PACIENTE", false],
  [admin, "ADMIN", true],
]) await commit("owner", { update: doc(`usuarios/${c.uid}`, perfil(c, rol, verificado)) });

// ---- registro: ENTRENADOR se elige al crear la cuenta, siempre sin verificar; ADMIN no
const nuevoPerfil = (c, rol, extra = {}) => commit(c.token, { update: doc(`usuarios/${c.uid}`, { ...perfil(c, rol, false), ...extra }), currentDocument: { exists: false } });
assert.equal(await nuevoPerfil(aspirante, "ENTRENADOR", { verificado: true }), 403, "no se registra ya verificado");
assert.equal(await nuevoPerfil(aspirante, "ADMIN"), 403, "nadie se da de alta como ADMIN");
assert.equal(await nuevoPerfil(aspirante, "ENTRENADOR"), 200, "se registra como entrenador");

// ---- solicitud de verificación del entrenador
const solicitud = (c, extra = {}) => ({ entrenadorId: c.uid, nombreCuenta: "Persona ENTRENADOR", correo: c.correo, credencial: "Certificación NSCA CPT, folio 123456", estado: "PENDIENTE", enviadoEn: ahora, ...extra });
const enviar = (token, uid, d) => commit(token, { update: doc(`verificaciones/${uid}`, d), currentDocument: { exists: false } });
assert.equal(await enviar(sinVerificar.token, sinVerificar.uid, solicitud(sinVerificar)), 200, "el entrenador envía su credencial");
assert.equal(await enviar(otroEntrenador.token, sinVerificar.uid, solicitud(sinVerificar)), 403, "otro no envía a nombre de él");
assert.equal(await enviar(paciente.token, paciente.uid, solicitud(paciente)), 403, "un paciente no pide ser entrenador");
assert.equal(await enviar(aspirante.token, aspirante.uid, solicitud(aspirante, { credencial: "corta" })), 403, "credencial muy corta");
assert.equal(await enviar(aspirante.token, aspirante.uid, solicitud(aspirante, { credencial: "x".repeat(201) })), 403, "credencial muy larga");
assert.equal(await enviar(aspirante.token, aspirante.uid, solicitud(aspirante, { estado: "APROBADA" })), 403, "no se aprueba solo");
assert.equal(await enviar(aspirante.token, aspirante.uid, solicitud(aspirante, { campoDeMas: 1 })), 403, "campos de más");
assert.equal(await enviar(aspirante.token, aspirante.uid, solicitud(aspirante, { correo: "otro@prueba.test" })), 403, "el correo es el de la cuenta");
assert.equal(await leer(sinVerificar.token, `verificaciones/${sinVerificar.uid}`), 200, "la lee su dueño");
assert.equal(await leer(otroEntrenador.token, `verificaciones/${sinVerificar.uid}`), 403, "otro no la lee");
assert.equal(await leer(admin.token, `verificaciones/${sinVerificar.uid}`), 200, "la lee el ADMIN");

// ---- el ADMIN aprueba (verificaciones + usuarios en el mismo lote) y el entrenador queda verificado
const aprobar = (token, c, uidsVerificado = true) =>
  commit(
    token,
    { update: doc(`verificaciones/${c.uid}`, { estado: "APROBADA", revisadoPor: admin.uid, revisadoEn: ahora, metodo: "MANUAL" }), updateMask: { fieldPaths: ["estado", "revisadoPor", "revisadoEn", "metodo"] }, currentDocument: { exists: true } },
    { update: doc(`usuarios/${c.uid}`, { verificado: uidsVerificado, ultimaModificacion: ahora }), updateMask: { fieldPaths: ["verificado", "ultimaModificacion"] }, currentDocument: { exists: true } },
  );
assert.equal(await aprobar(sinVerificar.token, sinVerificar), 403, "no se aprueba a sí mismo");
assert.equal(await aprobar(admin.token, sinVerificar), 200, "el ADMIN aprueba");

// ---- ejercicios: crear
const ejercicio = (c, extra = {}) => ({
  autorId: c.uid, autorNombre: "Persona ENTRENADOR", titulo: "Sentadilla con peso corporal", descripcion: "Trabaja piernas y glúteos sin equipo.",
  zona: "PIERNAS", tipo: "FUERZA", nivel: "PRINCIPIANTE", equipo: "SIN_EQUIPO", duracionMin: 10,
  pasos: ["Pies al ancho de los hombros", "Baja hasta 90 grados", "Sube empujando el piso"], consejos: "", contraindicaciones: "", kcal: 60,
  fotoVersion: ahora, estado: "EN_REVISION", creadoEn: ahora, actualizadoEn: ahora, ...extra,
});
let n = 0;
const nuevoId = () => "e" + sufijo + n++;
const crear = (c, d, id = nuevoId()) => commit(c.token, { update: doc(`ejercicios/${id}`, d), currentDocument: { exists: false } });
const sembrar = async (c, extra = {}) => {
  const id = nuevoId();
  await commit("owner", { update: doc(`ejercicios/${id}`, ejercicio(c, extra)) });
  return id;
};
assert.equal(await crear(entrenador, ejercicio(entrenador)), 200, "el entrenador verificado manda uno a revisión");
assert.equal(await crear(entrenador, ejercicio(entrenador, { estado: "BORRADOR", descripcion: "", pasos: [], fotoVersion: undefined })), 200, "y guarda un borrador a medias");
assert.equal(await crear(entrenador, ejercicio(entrenador, { estado: "PUBLICADO" })), 403, "no se publica solo");
assert.equal(await crear(entrenador, ejercicio(entrenador, { estado: "RECHAZADO" })), 403, "no nace rechazado");
assert.equal(await crear(entrenador, ejercicio(entrenador, { publicadoEn: ahora })), 403, "no trae fecha de publicación");
assert.equal(await crear(entrenador, ejercicio(entrenador, { revisadoPor: admin.uid })), 403, "no trae revisor");
assert.equal(await crear(sinVerificar, ejercicio(sinVerificar)), 200, "ya verificado por el ADMIN (aprobado arriba), crea");
assert.equal(await crear(aspirante, ejercicio(aspirante)), 403, "el entrenador sin verificar no crea");
assert.equal(await crear(paciente, ejercicio(paciente)), 403, "un paciente no crea");
assert.equal(await crear(admin, ejercicio(admin)), 403, "el ADMIN no crea (revisa)");
assert.equal(await crear(entrenador, ejercicio(otroEntrenador)), 403, "no se firma como otro autor");

// ---- ejercicios: lo que vale
assert.equal(await crear(entrenador, ejercicio(entrenador, { titulo: "ab" })), 403, "título muy corto");
assert.equal(await crear(entrenador, ejercicio(entrenador, { titulo: "x".repeat(81) })), 403, "título muy largo");
assert.equal(await crear(entrenador, ejercicio(entrenador, { descripcion: "x".repeat(601) })), 403, "descripción muy larga");
assert.equal(await crear(entrenador, ejercicio(entrenador, { descripcion: "corta" })), 403, "a revisión pide descripción de 10 letras");
assert.equal(await crear(entrenador, ejercicio(entrenador, { pasos: [] })), 403, "a revisión pide al menos un paso");
assert.equal(await crear(entrenador, ejercicio(entrenador, { pasos: Array.from({ length: 11 }, () => "paso") })), 403, "más de 10 pasos");
assert.equal(await crear(entrenador, ejercicio(entrenador, { pasos: "uno" })), 403, "los pasos son una lista");
assert.equal(await crear(entrenador, ejercicio(entrenador, { fotoVersion: undefined })), 403, "a revisión pide foto");
assert.equal(await crear(entrenador, ejercicio(entrenador, { fotoVersion: "v1" })), 403, "la versión de la foto es un número");
assert.equal(await crear(entrenador, ejercicio(entrenador, { zona: "CEREBRO" })), 403, "zona desconocida");
assert.equal(await crear(entrenador, ejercicio(entrenador, { tipo: "BAILE" })), 403, "tipo desconocido");
assert.equal(await crear(entrenador, ejercicio(entrenador, { nivel: "DIOS" })), 403, "nivel desconocido");
assert.equal(await crear(entrenador, ejercicio(entrenador, { equipo: "COHETE" })), 403, "equipo desconocido");
assert.equal(await crear(entrenador, ejercicio(entrenador, { duracionMin: 0 })), 403, "duración mínima");
assert.equal(await crear(entrenador, ejercicio(entrenador, { duracionMin: 181 })), 403, "duración máxima");
assert.equal(await crear(entrenador, ejercicio(entrenador, { kcal: 2001 })), 403, "demasiadas calorías");
assert.equal(await crear(entrenador, ejercicio(entrenador, { kcal: -1 })), 403, "calorías negativas");
assert.equal(await crear(entrenador, ejercicio(entrenador, { consejos: "x".repeat(401) })), 403, "consejos muy largos");
assert.equal(await crear(entrenador, ejercicio(entrenador, { campoDeMas: 1 })), 403, "campos de más");
const { kcal, ...sinKcal } = ejercicio(entrenador);
assert.equal(await crear(entrenador, sinKcal), 403, "faltan campos");

// ---- ejercicios: quién lo ve
const publicado = await sembrar(entrenador, { estado: "PUBLICADO", publicadoEn: ahora, revisadoPor: admin.uid, revisadoEn: ahora });
const enRevision = await sembrar(entrenador);
const borrador = await sembrar(entrenador, { estado: "BORRADOR" });
assert.equal(await leer(paciente.token, `ejercicios/${publicado}`), 200, "cualquier sesión ve lo publicado");
assert.equal(await leer(null, `ejercicios/${publicado}`), 403, "sin sesión no ve nada");
assert.equal(await leer(paciente.token, `ejercicios/${enRevision}`), 403, "lo que está en revisión no lo ve un paciente");
assert.equal(await leer(otroEntrenador.token, `ejercicios/${borrador}`), 403, "ni otro entrenador ve el borrador");
assert.equal(await leer(entrenador.token, `ejercicios/${borrador}`), 200, "su autor ve el borrador");
assert.equal(await leer(admin.token, `ejercicios/${enRevision}`), 200, "el ADMIN ve lo que revisa");

// ---- ejercicios: el autor edita lo suyo mientras sea borrador o rechazado
const editar = (c, id, d) => commit(c.token, { update: doc(`ejercicios/${id}`, d), currentDocument: { exists: true } });
assert.equal(await editar(entrenador, borrador, ejercicio(entrenador, { estado: "BORRADOR", titulo: "Sentadilla con salto" })), 200, "edita su borrador");
assert.equal(await editar(entrenador, borrador, ejercicio(entrenador, { estado: "EN_REVISION", titulo: "Sentadilla con salto" })), 200, "lo manda a revisión");
assert.equal(await editar(entrenador, borrador, ejercicio(entrenador, { estado: "BORRADOR" })), 403, "en revisión no se edita (se retira primero)");
assert.equal(await editar(entrenador, publicado, ejercicio(entrenador, { estado: "BORRADOR", publicadoEn: ahora, revisadoPor: admin.uid, revisadoEn: ahora })), 403, "lo publicado es de solo lectura");
assert.equal(await editar(otroEntrenador, enRevision, ejercicio(otroEntrenador, { estado: "BORRADOR" })), 403, "otro no edita lo ajeno");
const rechazado = await sembrar(entrenador, { estado: "RECHAZADO", motivoRechazo: "La foto no muestra el ejercicio", revisadoPor: admin.uid, revisadoEn: ahora });
const corregido = ejercicio(entrenador, { estado: "EN_REVISION", revisadoPor: admin.uid, revisadoEn: ahora });
assert.equal(await editar(entrenador, rechazado, corregido), 200, "corrige uno rechazado y lo reenvía");
const rechazado2 = await sembrar(entrenador, { estado: "RECHAZADO", motivoRechazo: "Falta descripción", revisadoPor: admin.uid, revisadoEn: ahora });
assert.equal(await editar(entrenador, rechazado2, ejercicio(entrenador, { estado: "EN_REVISION", revisadoPor: otroEntrenador.uid, revisadoEn: ahora })), 403, "no cambia al revisor");
assert.equal(await editar(entrenador, rechazado2, ejercicio(entrenador, { estado: "EN_REVISION", creadoEn: ahora + 1, revisadoPor: admin.uid, revisadoEn: ahora })), 403, "la fecha de creación no cambia");

// ---- ejercicios: el autor retira lo publicado o saca de revisión lo suyo
const soloEstado = (c, id, estado) => commit(c.token, { update: doc(`ejercicios/${id}`, { estado, actualizadoEn: ahora + 5 }), updateMask: { fieldPaths: ["estado", "actualizadoEn"] }, currentDocument: { exists: true } });
const aRetirar = await sembrar(entrenador, { estado: "PUBLICADO", publicadoEn: ahora, revisadoPor: admin.uid, revisadoEn: ahora });
assert.equal(await soloEstado(otroEntrenador, aRetirar, "RETIRADO"), 403, "otro no retira lo ajeno");
assert.equal(await soloEstado(entrenador, aRetirar, "PUBLICADO"), 403, "no vuelve a publicarse solo");
assert.equal(await soloEstado(entrenador, aRetirar, "RETIRADO"), 200, "su autor retira lo publicado");
const aSacar = await sembrar(entrenador);
assert.equal(await soloEstado(entrenador, aSacar, "BORRADOR"), 200, "saca de revisión lo suyo");
assert.equal(await soloEstado(entrenador, aSacar, "PUBLICADO"), 403, "de borrador no pasa a publicado");

// ---- ejercicios: el ADMIN publica, rechaza con motivo y retira
const revisar = (id, extra, mascara) => commit(admin.token, { update: doc(`ejercicios/${id}`, extra), updateMask: { fieldPaths: mascara }, currentDocument: { exists: true } });
const publicar = (id, extra = {}) => revisar(id, { estado: "PUBLICADO", publicadoEn: ahora, revisadoPor: admin.uid, revisadoEn: ahora, actualizadoEn: ahora, ...extra }, ["estado", "publicadoEn", "revisadoPor", "revisadoEn", "actualizadoEn"]);
const rechazar = (id, extra = {}) => revisar(id, { estado: "RECHAZADO", motivoRechazo: "La foto es borrosa", revisadoPor: admin.uid, revisadoEn: ahora, actualizadoEn: ahora, ...extra }, ["estado", "motivoRechazo", "revisadoPor", "revisadoEn", "actualizadoEn"]);
const porPublicar = await sembrar(entrenador);
const porRechazar = await sembrar(entrenador);
assert.equal(await commit(entrenador.token, { update: doc(`ejercicios/${porPublicar}`, { estado: "PUBLICADO", publicadoEn: ahora, revisadoPor: entrenador.uid, revisadoEn: ahora, actualizadoEn: ahora }), updateMask: { fieldPaths: ["estado", "publicadoEn", "revisadoPor", "revisadoEn", "actualizadoEn"] }, currentDocument: { exists: true } }), 403, "el autor no se publica a sí mismo");
assert.equal(await publicar(porPublicar, { revisadoPor: entrenador.uid }), 403, "el revisor es quien publica");
assert.equal(await publicar(porPublicar), 200, "el ADMIN publica");
assert.equal(await leer(paciente.token, `ejercicios/${porPublicar}`), 200, "y ya lo ve el paciente");
assert.equal(await rechazar(porRechazar, { motivoRechazo: "" }), 403, "rechazar pide motivo");
assert.equal(await rechazar(porRechazar, { motivoRechazo: "x".repeat(201) }), 403, "motivo muy largo");
assert.equal(await rechazar(porRechazar), 200, "el ADMIN rechaza con motivo");
assert.equal(await rechazar(porPublicar), 403, "lo ya publicado no se rechaza (se retira)");
assert.equal(await publicar(porRechazar), 403, "lo rechazado no se publica sin que el autor lo reenvíe");
assert.equal(await revisar(porPublicar, { estado: "RETIRADO", revisadoPor: admin.uid, revisadoEn: ahora, actualizadoEn: ahora }, ["estado", "revisadoPor", "revisadoEn", "actualizadoEn"]), 200, "el ADMIN retira lo publicado");
assert.equal(await revisar(enRevision, { titulo: "Cambiado por el ADMIN" }, ["titulo"]), 403, "el ADMIN no reescribe contenido ajeno");

// ---- ejercicios: borrar
const borrar = (c, id) => commit(c.token, { delete: `${BASE}/ejercicios/${id}` });
assert.equal(await borrar(otroEntrenador, rechazado2), 403, "otro no borra");
assert.equal(await borrar(entrenador, rechazado2), 200, "el autor borra un rechazado");
assert.equal(await borrar(entrenador, aSacar), 200, "el autor borra un borrador");
assert.equal(await borrar(entrenador, publicado), 403, "lo publicado no se borra (se retira)");
assert.equal(await borrar(entrenador, enRevision), 403, "lo que está en revisión tampoco");
assert.equal(await borrar(admin, aRetirar), 403, "el ADMIN no borra");

console.log("ejercicios: todas las pruebas pasaron");
