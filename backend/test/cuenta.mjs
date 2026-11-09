// Prueba de `POST /cuenta/eliminar` (borrar la cuenta de un paciente, un nutriólogo o un entrenador) sin red ni cuenta de Google:
// `npm run test:cuenta`. Empaqueta las rutas con esbuild (ya viene con wrangler), simula Google, Firestore, R2, FCM y la API de cuentas con un
// `fetch` falso y comprueba lo que protege la ruta: que la confirmación de identidad sea reciente, qué se borra y qué se conserva por rol, el
// orden (lo de adentro primero; el perfil y la cuenta de Auth al final) y que se pueda reintentar tras un fallo a la mitad.
import assert from "node:assert/strict";
import { generateKeyPairSync } from "node:crypto";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { pathToFileURL } from "node:url";
import { build } from "esbuild";

async function cargar(entrada) {
  const destino = join(mkdtempSync(join(tmpdir(), "cuenta-")), "modulo.mjs");
  await build({ entryPoints: [entrada], bundle: true, format: "esm", platform: "node", outfile: destino, logLevel: "silent" });
  return import(pathToFileURL(destino).href);
}
const { eliminarCuenta } = await cargar("src/routes/cuenta.ts");
const { esReciente, pasosDeBorrado, VENTANA_SEGUNDOS } = await cargar("src/lib/borrado.ts");

// ---- la confirmación de identidad tiene que ser reciente
assert.equal(VENTANA_SEGUNDOS, 300);
assert.equal(esReciente(1000 - 299, 1000), true, "hace 299 s sí");
assert.equal(esReciente(1000 - 300, 1000), true, "hace 300 s todavía");
assert.equal(esReciente(1000 - 301, 1000), false, "hace 301 s no");
assert.equal(esReciente(1000 + 30, 1000), true, "un reloj adelantado unos segundos se tolera");
assert.equal(esReciente(1000 + 3600, 1000), false, "pero no uno muy en el futuro");

// ---- los pasos por rol
const paciente = pasosDeBorrado("PACIENTE");
assert.equal(paciente[0], "cerrar-vinculos", "el paciente empieza cerrando sus vínculos");
assert.deepEqual(paciente.slice(-2), ["usuario", "auth"], "y termina con el perfil y la cuenta");
const nutriologo = pasosDeBorrado("NUTRIOLOGO");
for (const paso of ["cerrar-vinculos", "citas", "consultorio", "codigos", "cedula", "verificacion", "autor"]) assert.ok(nutriologo.includes(paso), `el nutriólogo incluye ${paso}`);
assert.ok(nutriologo.indexOf("cedula") < nutriologo.indexOf("verificacion"), "la cédula se libera antes de borrar la solicitud que la nombra");
assert.deepEqual(nutriologo.slice(-2), ["usuario", "auth"]);
const entrenador = pasosDeBorrado("ENTRENADOR");
assert.ok(entrenador.includes("autor") && entrenador.includes("verificacion"), "el entrenador borra su solicitud y lo que publicó");
assert.ok(!entrenador.includes("cerrar-vinculos") && !entrenador.includes("consultorio"));
assert.deepEqual(entrenador.slice(-2), ["usuario", "auth"]);
assert.throws(() => pasosDeBorrado("ADMIN"), "la cuenta de ADMIN no se borra desde la app");

// ---- Google, Firestore, R2, FCM y la API de cuentas simulados
const { privateKey } = generateKeyPairSync("rsa", { modulusLength: 2048, privateKeyEncoding: { type: "pkcs8", format: "pem" }, publicKeyEncoding: { type: "spki", format: "pem" } });
const RAIZ = "projects/nutrigajo/databases/(default)/documents";
const campo = (v) => (typeof v === "boolean" ? { booleanValue: v } : typeof v === "number" ? { integerValue: String(v) } : { stringValue: v });
const doc = (campos) => ({ fields: Object.fromEntries(Object.entries(campos).map(([k, v]) => [k, campo(v)])) });
const valor = (ruta, nombre) => {
  const v = datos[ruta]?.fields?.[nombre];
  return v ? (v.stringValue ?? v.booleanValue ?? v.integerValue) : undefined;
};

let datos, llamadas, fcm, authRespuesta, r2;
const env = () => ({
  FIREBASE_PROJECT_ID: "nutrigajo",
  FIREBASE_SERVICE_ACCOUNT: JSON.stringify({ client_email: "cuenta@nutrigajo.iam.gserviceaccount.com", private_key: privateKey }),
  FOTOS: {
    async delete(claves) {
      for (const c of [].concat(claves)) {
        r2.borrados.push(c);
        r2.objetos.delete(c);
      }
    },
    async list({ prefix }) {
      return { objects: [...r2.objetos].filter((k) => k.startsWith(prefix)).map((key) => ({ key })), truncated: false };
    },
  },
});
const cola = () => llamadas.filter((l) => l.metodo !== "GET" && !l.url.includes("oauth2") && !l.url.endsWith(":runQuery"));

globalThis.fetch = async (url, init = {}) => {
  url = String(url);
  const metodo = init.method ?? "GET";
  llamadas.push({ url, metodo, cuerpo: init.body });
  if (url.startsWith("https://oauth2.googleapis.com/token")) return Response.json({ access_token: "acceso", expires_in: 3600 });
  if (url.includes("/messages:send")) return new Response("{}", { status: fcm });
  if (url.includes("identitytoolkit.googleapis.com") && url.endsWith("accounts:delete")) {
    return new Response(authRespuesta.cuerpo ?? "{}", { status: authRespuesta.estado });
  }
  if (url.endsWith(":runQuery")) {
    const q = JSON.parse(init.body).structuredQuery;
    const coleccion = q.from[0].collectionId;
    const { field, value } = q.where.fieldFilter;
    const aciertos = Object.entries(datos).filter(
      ([ruta, d]) => ruta.startsWith(`${coleccion}/`) && !ruta.slice(coleccion.length + 1).includes("/") && d.fields?.[field.fieldPath]?.stringValue === value.stringValue,
    );
    return Response.json(aciertos.length ? aciertos.map(([ruta, d]) => ({ document: { name: `${RAIZ}/${ruta}`, ...d } })) : [{ readTime: "ahora" }]);
  }
  if (url.endsWith(":commit")) {
    for (const w of JSON.parse(init.body).writes) delete datos[w.delete.split("/documents/")[1]];
    return Response.json({});
  }
  const ruta = decodeURIComponent(url.split("/documents/")[1].split("?")[0]);
  if (metodo === "DELETE") {
    if (!(ruta in datos)) return new Response("{}", { status: 404 });
    delete datos[ruta];
    return Response.json({});
  }
  if (metodo === "PATCH") {
    const mascara = new URL(url).searchParams.getAll("updateMask.fieldPaths");
    const nuevos = JSON.parse(init.body).fields ?? {};
    const actual = datos[ruta] ?? { fields: {} };
    for (const m of mascara) {
      if (m in nuevos) actual.fields[m] = nuevos[m];
      else delete actual.fields[m];
    }
    datos[ruta] = actual;
    return Response.json({});
  }
  if (ruta in datos) return Response.json(datos[ruta]);
  const hijos = Object.keys(datos).filter((r) => r.startsWith(`${ruta}/`) && !r.slice(ruta.length + 1).includes("/"));
  if (hijos.length) return Response.json({ documents: hijos.map((r) => ({ name: `${RAIZ}/${r}`, ...datos[r] })) });
  return new Response("{}", { status: 404 });
};

const AHORA = Math.floor(Date.now() / 1000);
// las citas que aún no llegan (un nutriólogo que se va solo cierra esas)
const I1 = (AHORA + 86_400) * 1000;
const I2 = I1 + 900_000;
const I3 = I1 + 1_800_000;
const I4 = I1 + 2_700_000;
const pedir = (uid, { authTime = AHORA, entorno = env() } = {}) =>
  eliminarCuenta(new Request("https://w/cuenta/eliminar", { method: "POST" }), entorno, { uid, authTime });
const envios = () => llamadas.filter((l) => l.url.includes("/messages:send")).map((l) => JSON.parse(l.cuerpo).message);
const ultimoPaso = () => cola().at(-1);
const hayAuth = () => llamadas.some((l) => l.url.endsWith("accounts:delete"));

function nuevoMundo(extra = {}) {
  datos = {
    "usuarios/pac1": doc({ rol: "PACIENTE", nombre: "Ana", correo: "ana@x.com", fcmToken: "tok-pac" }),
    "usuarios/pac1/entrenamientos/e1": doc({ minutos: 10 }),
    "usuarios/pac1/carreras/c1": doc({ tipo: "CORRER" }),
    "usuarios/pac1/modoCarrera/m1": doc({ a: "b" }),
    "usuarios/pac1/banco/b1": doc({ a: "b" }),
    "usuarios/nutri1": doc({ rol: "NUTRIOLOGO", nombre: "Kelly", fcmToken: "tok-nutri" }),
    "usuarios/pac2": doc({ rol: "PACIENTE", nombre: "Luis", fcmToken: "tok-pac2" }),
    "usuarios/entre1": doc({ rol: "ENTRENADOR", nombre: "Beto" }),
    "usuarios/admin1": doc({ rol: "ADMIN", nombre: "Admin" }),
    "pacientes/p1": doc({ nutriologoId: "nutri1", nutriologoNombre: "Kelly", usuarioId: "pac1", nombre: "Ana", correoVinculacion: "ana@x.com", codigoVinculacion: "1234", activo: true }),
    "pacientes/p0": doc({ nutriologoId: "nutri9", nutriologoNombre: "Otra", usuarioId: "pac1", nombre: "Ana", correoVinculacion: "ana@x.com", codigoVinculacion: "9999", activo: false, cerradoPor: "PACIENTE", cerradoEn: 5 }),
    "pacientes/p2": doc({ nutriologoId: "nutri1", nutriologoNombre: "Kelly", usuarioId: "pac2", nombre: "Luis", activo: true }),
    "pacientes/p3": doc({ nutriologoId: "nutri1", nutriologoNombre: "Kelly", usuarioId: "pac3", nombre: "Viejo", activo: false, cerradoPor: "NUTRIOLOGO", cerradoEn: 7 }),
    "citas/c1": doc({ usuarioId: "pac1", nutriologoId: "nutri1", inicio: I1, estado: "CONFIRMADA", origen: "PACIENTE" }),
    "citas/c2": doc({ usuarioId: "pac2", nutriologoId: "nutri1", inicio: I2, estado: "PENDIENTE", origen: "PACIENTE" }),
    "citas/c3": doc({ usuarioId: "pac3", nutriologoId: "nutri1", inicio: I3, estado: "CANCELADA", origen: "PACIENTE" }),
    "citas/c4": doc({ usuarioId: "pac2", nutriologoId: "nutri9", inicio: I4, estado: "CONFIRMADA", origen: "PACIENTE" }),
    "directorio/nutri1": doc({ nombre: "Consultorio Kelly" }),
    [`directorio/nutri1/ocupados/${I1}`]: doc({ citaId: "c1", inicio: I1 }),
    [`directorio/nutri1/ocupados/${I2}`]: doc({ citaId: "c2", inicio: I2 }),
    "citasActivas/pac1": doc({ citaId: "c1" }),
    "citasActivas/pac2": doc({ citaId: "c2" }),
    "comentarios/k1": doc({ usuarioId: "pac1", mensaje: "hola" }),
    "comentarios/k2": doc({ usuarioId: "otro", mensaje: "adiós" }),
    "codigosVinculacion/AAAA": doc({ nutriologoId: "nutri1", usado: false }),
    "codigosVinculacion/BBBB": doc({ nutriologoId: "nutri1", usado: true }),
    "verificaciones/nutri1": doc({ cedula: "1234567", estado: "APROBADA" }),
    "cedulas/1234567": doc({ nutriologoId: "nutri1" }),
    "verificaciones/entre1": doc({ credencial: "X", estado: "APROBADA" }),
    "platillos/pl1": doc({ autorId: "nutri1", autorNombre: "Kelly", estado: "PUBLICADO" }),
    "platillos/pl2": doc({ autorId: "nutri1", autorNombre: "Kelly", estado: "BORRADOR" }),
    "ejercicios/ej1": doc({ autorId: "entre1", autorNombre: "Beto", estado: "PUBLICADO" }),
    "ejercicios/ej2": doc({ autorId: "entre1", autorNombre: "Beto", estado: "EN_REVISION" }),
    ...extra,
  };
  llamadas = [];
  fcm = 200;
  authRespuesta = { estado: 200 };
  r2 = {
    borrados: [],
    objetos: new Set(["avatares/pac1", "portadas/pac1", "avatares/nutri1", "platillos/nutri1/pl1/456", "platillos/nutri1/pl2/123", "platillos/nutri1/pl2/clip-9", "ejercicios/entre1/ej1/777", "ejercicios/entre1/ej2/888", "ejercicios/entre1/ej2/clip-5"]),
  };
}

// ---- lo que se rechaza sin tocar nada
nuevoMundo();
assert.equal((await pedir("pac1", { authTime: AHORA - 301 })).status, 401, "una confirmación de hace más de 5 minutos");
assert.equal(cola().length, 0, "y no se borró nada");
assert.equal((await pedir("pac1", { entorno: { FIREBASE_PROJECT_ID: "nutrigajo", FOTOS: env().FOTOS } })).status, 503, "sin clave configurada");
assert.equal((await pedir("admin1")).status, 403, "el ADMIN no se borra desde la app");
assert.equal(cola().length, 0);

// ---- paciente
nuevoMundo();
let r = await pedir("pac1");
assert.equal(r.status, 200);
assert.deepEqual(await r.json(), { borrado: true });
assert.equal(valor("pacientes/p1", "activo"), false, "su vínculo activo quedó cerrado");
assert.equal(valor("pacientes/p1", "cerradoPor"), "CUENTA_ELIMINADA");
for (const id of ["p1", "p0"]) {
  for (const c of ["usuarioId", "correoVinculacion", "codigoVinculacion"]) assert.equal(valor(`pacientes/${id}`, c), undefined, `pacientes/${id} ya no trae ${c}`);
  assert.equal(valor(`pacientes/${id}`, "nombre"), "Ana", "el registro se queda con el nombre");
}
assert.equal(valor("pacientes/p0", "cerradoPor"), "PACIENTE", "uno ya cerrado conserva su motivo");
assert.equal(valor("pacientes/p2", "usuarioId"), "pac2", "los registros de otros no se tocan");
assert.ok(!("citas/c1" in datos) && "citas/c2" in datos, "sus citas se borran y las de otros no");
assert.ok(!(`directorio/nutri1/ocupados/${I1}` in datos) && `directorio/nutri1/ocupados/${I2}` in datos, "se libera su hora");
assert.ok(!("citasActivas/pac1" in datos) && "citasActivas/pac2" in datos, "y su candado");
assert.ok(!("comentarios/k1" in datos) && "comentarios/k2" in datos, "sus comentarios");
for (const ruta of ["usuarios/pac1", "usuarios/pac1/entrenamientos/e1", "usuarios/pac1/carreras/c1", "usuarios/pac1/modoCarrera/m1", "usuarios/pac1/banco/b1"]) {
  assert.ok(!(ruta in datos), `${ruta} se borró`);
}
assert.deepEqual(r2.borrados.sort(), ["avatares/pac1", "portadas/pac1"], "su avatar y su portada");
assert.deepEqual(envios().map((m) => [m.token, m.notification.body]), [["tok-nutri", "Ana eliminó su cuenta."]], "solo avisa al nutriólogo del vínculo que seguía activo");
assert.ok(ultimoPaso().url.endsWith("accounts:delete"), "la cuenta de Auth se borra al final");
assert.ok(cola().findIndex((l) => l.url.endsWith("accounts:delete")) > cola().findIndex((l) => l.metodo === "DELETE" && l.url.includes("usuarios/pac1")), "después del perfil");
assert.match(ultimoPaso().cuerpo, /"localId":"pac1"/);

// ---- nutriólogo
nuevoMundo();
r = await pedir("nutri1");
assert.equal(r.status, 200);
assert.equal(valor("pacientes/p1", "activo"), false);
assert.equal(valor("pacientes/p1", "cerradoPor"), "CUENTA_ELIMINADA", "cierra el vínculo de cada paciente");
assert.equal(valor("pacientes/p1", "usuarioId"), "pac1", "los pacientes conservan su acceso al registro");
assert.equal(valor("pacientes/p3", "cerradoPor"), "NUTRIOLOGO", "uno ya cerrado no cambia");
assert.equal(valor("pacientes/p0", "usuarioId"), "pac1", "ni los de otros nutriólogos");
assert.equal(valor("citas/c1", "estado"), "CANCELADA", "su cita confirmada se cancela");
assert.equal(valor("citas/c2", "estado"), "RECHAZADA", "la pendiente se rechaza");
assert.equal(valor("citas/c3", "estado"), "CANCELADA", "la ya cancelada no cambia");
assert.equal(valor("citas/c4", "estado"), "CONFIRMADA", "las de otros nutriólogos no se tocan");
assert.ok(!("citasActivas/pac2" in datos), "libera el candado del paciente de la pendiente");
assert.ok(!("citasActivas/pac1" in datos), "el candado de la confirmada también se libera");
assert.ok(!("directorio/nutri1" in datos) && !(`directorio/nutri1/ocupados/${I1}` in datos) && !(`directorio/nutri1/ocupados/${I2}` in datos), "su consultorio y sus horas");
assert.ok(!("codigosVinculacion/AAAA" in datos) && "codigosVinculacion/BBBB" in datos, "sus códigos sin usar, no los usados");
assert.ok(!("verificaciones/nutri1" in datos) && !("cedulas/1234567" in datos), "su solicitud y su cédula reservada");
assert.equal(valor("platillos/pl1", "autorNombre"), "Cuenta eliminada", "lo publicado se queda con autor «Cuenta eliminada»");
assert.equal(valor("platillos/pl1", "autorId"), "nutri1", "y conserva el id del autor (las fotos viven bajo él)");
assert.ok(!("platillos/pl2" in datos), "el borrador se borra");
assert.ok(r2.borrados.includes("platillos/nutri1/pl2/123") && r2.borrados.includes("platillos/nutri1/pl2/clip-9"), "con sus archivos");
assert.ok(!r2.borrados.some((k) => k.startsWith("platillos/nutri1/pl1")), "pero no los de lo publicado");
assert.ok(r2.borrados.includes("avatares/nutri1"));
assert.ok(!("usuarios/nutri1" in datos));
assert.deepEqual(envios().map((m) => m.notification.body).sort(), ["Kelly cerró el vínculo contigo.", "Kelly cerró el vínculo contigo."], "avisa a los dos pacientes con vínculo activo");
assert.ok(ultimoPaso().url.endsWith("accounts:delete"));

// ---- entrenador
nuevoMundo();
r = await pedir("entre1");
assert.equal(r.status, 200);
assert.ok(!("verificaciones/entre1" in datos), "su solicitud");
assert.equal(valor("ejercicios/ej1", "autorNombre"), "Cuenta eliminada");
assert.equal(valor("ejercicios/ej1", "autorId"), "entre1");
assert.ok(!("ejercicios/ej2" in datos), "lo que estaba en revisión se borra");
assert.ok(r2.borrados.includes("ejercicios/entre1/ej2/888") && r2.borrados.includes("ejercicios/entre1/ej2/clip-5"));
assert.ok(!r2.borrados.some((k) => k.startsWith("ejercicios/entre1/ej1")));
assert.ok(!("usuarios/entre1" in datos));
assert.equal(envios().length, 0, "un entrenador no avisa a nadie");
assert.ok(ultimoPaso().url.endsWith("accounts:delete"));

// ---- un fallo a la mitad se puede reintentar
nuevoMundo();
authRespuesta = { estado: 500 };
r = await pedir("pac1");
assert.equal(r.status, 502, "si Auth falla, la respuesta es 502");
assert.ok(!("usuarios/pac1" in datos), "el perfil ya se había borrado (el orden es lo de adentro primero)");
const pushesAntes = envios().length;
authRespuesta = { estado: 200 };
r = await pedir("pac1");
assert.equal(r.status, 200, "reintentar con el perfil ya borrado termina el trabajo");
assert.ok(ultimoPaso().url.endsWith("accounts:delete"), "y borra la cuenta");
assert.equal(envios().length, pushesAntes, "sin avisar otra vez");

// con todo ya borrado y la cuenta inexistente, sigue contestando bien
authRespuesta = { estado: 400, cuerpo: JSON.stringify({ error: { message: "USER_NOT_FOUND" } }) };
r = await pedir("pac1");
assert.equal(r.status, 200, "una cuenta que ya no existe cuenta como borrada");

// un fallo de Firestore en un paso intermedio: 502 y no se toca ni el perfil ni la cuenta
nuevoMundo();
const fetchBueno = globalThis.fetch;
globalThis.fetch = async (url, init = {}) => (String(url).endsWith(":runQuery") && JSON.parse(init.body).structuredQuery.from[0].collectionId === "citas" ? new Response("{}", { status: 500 }) : fetchBueno(url, init));
r = await pedir("pac1");
assert.equal(r.status, 502);
assert.ok("usuarios/pac1" in datos && !hayAuth(), "si un paso falla, el perfil y la cuenta siguen: se reintenta");
globalThis.fetch = fetchBueno;

console.log("cuenta.mjs: ok");
