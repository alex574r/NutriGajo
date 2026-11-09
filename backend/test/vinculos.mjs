// Prueba del aviso de baja de un vínculo y de las consultas y borrados de Firestore del Worker, sin red ni cuenta de Google: `npm run test:vinculos`.
// Empaqueta las rutas con esbuild (ya viene con wrangler), simula Google, Firestore y FCM con un `fetch` falso y comprueba lo que protege la
// ruta: quién puede pedir el aviso, que el vínculo esté cerrado, que no se avise dos veces y el texto del mensaje.
import assert from "node:assert/strict";
import { generateKeyPairSync } from "node:crypto";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { pathToFileURL } from "node:url";
import { build } from "esbuild";

async function cargar(entrada) {
  const destino = join(mkdtempSync(join(tmpdir(), "vinculos-")), "modulo.mjs");
  await build({ entryPoints: [entrada], bundle: true, format: "esm", platform: "node", outfile: destino, logLevel: "silent" });
  return import(pathToFileURL(destino).href);
}
const { avisarBaja } = await cargar("src/routes/vinculo.ts");
const { avisoDeBaja } = await cargar("src/lib/aviso.ts");
const { consultar, borrar, borrarColeccion } = await cargar("src/lib/firestore.ts");
const { tokenDeServicio } = await cargar("src/auth/google.ts");

// ---- el texto del aviso
assert.deepEqual(avisoDeBaja("Ana", "PACIENTE"), { titulo: "Vínculo cerrado", cuerpo: "Ana dejó de ser tu paciente." });
assert.deepEqual(avisoDeBaja("Kelly", "NUTRIOLOGO"), { titulo: "Vínculo cerrado", cuerpo: "Kelly cerró el vínculo contigo." });
assert.equal(avisoDeBaja("Ana", "CUENTA_ELIMINADA", "PACIENTE").cuerpo, "Ana eliminó su cuenta.");
assert.equal(avisoDeBaja("Kelly", "CUENTA_ELIMINADA", "NUTRIOLOGO").cuerpo, "Kelly cerró el vínculo contigo.");

// ---- Firestore y FCM simulados
const { privateKey } = generateKeyPairSync("rsa", { modulusLength: 2048, privateKeyEncoding: { type: "pkcs8", format: "pem" }, publicKeyEncoding: { type: "spki", format: "pem" } });
const env = { FIREBASE_PROJECT_ID: "nutrigajo", FIREBASE_SERVICE_ACCOUNT: JSON.stringify({ client_email: "cuenta@nutrigajo.iam.gserviceaccount.com", private_key: privateKey }) };
const RAIZ = "projects/nutrigajo/databases/(default)/documents";

const campo = (v) => (typeof v === "boolean" ? { booleanValue: v } : typeof v === "number" ? { integerValue: String(v) } : { stringValue: v });
const doc = (campos) => ({ fields: Object.fromEntries(Object.entries(campos).map(([k, v]) => [k, campo(v)])) });

let datos, llamadas, fcm;
globalThis.fetch = async (url, init = {}) => {
  url = String(url);
  const metodo = init.method ?? "GET";
  llamadas.push({ url, metodo, cuerpo: init.body });
  if (url.startsWith("https://oauth2.googleapis.com/token")) return Response.json({ access_token: "acceso", expires_in: 3600 });
  if (url.includes("/messages:send")) return new Response("{}", { status: fcm });
  if (url.endsWith(":runQuery")) {
    const q = JSON.parse(init.body).structuredQuery;
    const coleccion = q.from[0].collectionId;
    const { field, op, value } = q.where.fieldFilter;
    const aciertos = Object.entries(datos).filter(
      ([ruta, d]) => ruta.startsWith(`${coleccion}/`) && !ruta.slice(coleccion.length + 1).includes("/") && op === "EQUAL" && d.fields?.[field.fieldPath]?.stringValue === value.stringValue,
    );
    return Response.json(aciertos.length ? aciertos.map(([ruta, d]) => ({ document: { name: `${RAIZ}/${ruta}`, ...d } })) : [{ readTime: "ahora" }]);
  }
  if (url.endsWith(":commit")) {
    // un lote de borrados
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
    const mascara = [...new URL(url).searchParams.getAll("updateMask.fieldPaths")];
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
  // una colección: sus documentos directos
  const hijos = Object.keys(datos).filter((r) => r.startsWith(`${ruta}/`) && !r.slice(ruta.length + 1).includes("/"));
  if (hijos.length) return Response.json({ documents: hijos.map((r) => ({ name: `${RAIZ}/${r}`, ...datos[r] })) });
  return new Response("{}", { status: 404 });
};
const envios = () => llamadas.filter((l) => l.url.includes("/messages:send"));
const parches = (ruta) => llamadas.filter((l) => l.metodo === "PATCH" && decodeURIComponent(l.url).includes(`/documents/${ruta}`));

// ---- consultar, borrar y borrarColeccion
datos = {
  "pacientes/p1": doc({ usuarioId: "pac1", nombre: "Ana" }),
  "pacientes/p2": doc({ usuarioId: "pac1", nombre: "Ana (otro)" }),
  "pacientes/p3": doc({ usuarioId: "pac2", nombre: "Luis" }),
  "pacientes/p1/mensajes/m1": doc({ usuarioId: "pac1", texto: "hola" }),
  "usuarios/pac1/entrenamientos/e1": doc({ minutos: 10 }),
  "usuarios/pac1/entrenamientos/e2": doc({ minutos: 20 }),
  "usuarios/pac2/entrenamientos/e1": doc({ minutos: 30 }),
};
llamadas = [];
const token = await tokenDeServicio(env);
{
  const aciertos = await consultar(env, token, "pacientes", "usuarioId", "pac1");
  assert.deepEqual(aciertos.map((a) => a.ruta).sort(), ["pacientes/p1", "pacientes/p2"], "consultar devuelve los documentos que coinciden, con su ruta");
  assert.equal(aciertos[0].doc.fields.usuarioId.stringValue, "pac1", "y sus campos");
  const consulta = llamadas.find((l) => l.url.endsWith(":runQuery"));
  assert.equal(consulta.metodo, "POST");
  assert.match(consulta.cuerpo, /"collectionId":"pacientes"/);
  assert.match(consulta.cuerpo, /"op":"EQUAL"/);
  assert.deepEqual(await consultar(env, token, "pacientes", "usuarioId", "nadie"), [], "sin coincidencias devuelve una lista vacía");
}
await borrar(env, token, "pacientes/p3");
assert.ok(!("pacientes/p3" in datos), "borrar quita el documento");
await borrar(env, token, "pacientes/p3"); // ya no existe: cuenta como hecho
await borrarColeccion(env, token, "usuarios/pac1/entrenamientos");
assert.ok(!("usuarios/pac1/entrenamientos/e1" in datos) && !("usuarios/pac1/entrenamientos/e2" in datos), "borrarColeccion vacía la subcolección");
assert.ok("usuarios/pac2/entrenamientos/e1" in datos && "pacientes/p1" in datos, "y no toca lo demás");
await borrarColeccion(env, token, "usuarios/pac1/entrenamientos"); // ya vacía: no falla

// ---- la ruta de aviso
function preparar({ registro = {}, pac = {}, nutri = {}, respuestaFcm = 200 } = {}) {
  datos = {
    "pacientes/p1": doc({ nutriologoId: "nutri1", nutriologoNombre: "Kelly", usuarioId: "pac1", nombre: "Ana", activo: false, cerradoPor: "PACIENTE", cerradoEn: 5, ...registro }),
    "usuarios/pac1": doc({ fcmToken: "tok-pac", ...pac }),
    "usuarios/nutri1": doc({ fcmToken: "tok-nutri", ...nutri }),
  };
  llamadas = [];
  fcm = respuestaFcm;
}
const pedir = (cuerpo, uid = "pac1", entorno = env) =>
  avisarBaja(new Request("https://w/vinculos/notificar", { method: "POST", body: JSON.stringify(cuerpo) }), entorno, uid);

preparar();
assert.equal((await pedir({})).status, 400, "sin pacienteId");
assert.equal((await pedir({ pacienteId: "../usuarios" })).status, 400, "pacienteId con ruta");
assert.equal((await pedir({ pacienteId: "p1" }, "pac1", { FIREBASE_PROJECT_ID: "nutrigajo" })).status, 503, "sin clave configurada");
assert.equal((await pedir({ pacienteId: "otro" })).status, 404, "vínculo inexistente");
assert.equal((await pedir({ pacienteId: "p1" }, "intruso")).status, 403, "quien no es de ese vínculo");
assert.equal((await pedir({ pacienteId: "p1" }, "nutri1")).status, 403, "cerró el paciente: no lo pide el nutriólogo");
assert.equal(envios().length, 0, "nada de eso mandó un aviso");

preparar({ registro: { cerradoPor: "NUTRIOLOGO" } });
assert.equal((await pedir({ pacienteId: "p1" }, "pac1")).status, 403, "cerró el nutriólogo: no lo pide el paciente");

preparar({ registro: { activo: true } });
assert.equal((await pedir({ pacienteId: "p1" })).status, 409, "un vínculo activo no se avisa");
preparar({ registro: { cerradoPor: "CUENTA_ELIMINADA" } });
assert.equal((await pedir({ pacienteId: "p1" })).status, 409, "el aviso de una cuenta eliminada lo manda el borrado, no la app");

// cierra el paciente: se avisa al nutriólogo, con el dispositivo que sale de su perfil
preparar();
let r = await pedir({ pacienteId: "p1" });
assert.equal(r.status, 200);
assert.deepEqual(await r.json(), { enviado: true });
assert.equal(envios().length, 1, "un solo aviso");
{
  const mensaje = JSON.parse(envios()[0].cuerpo).message;
  assert.equal(mensaje.token, "tok-nutri", "al nutriólogo");
  assert.equal(mensaje.notification.title, "Vínculo cerrado");
  assert.equal(mensaje.notification.body, "Ana dejó de ser tu paciente.");
  assert.equal(mensaje.android.notification.channel_id, "citas");
  assert.equal(mensaje.data.tipo, "VINCULO_CERRADO");
  assert.equal(mensaje.data.pacienteId, "p1");
}
assert.equal(parches("pacientes/p1").length, 1, "se marca como avisado");
r = await pedir({ pacienteId: "p1" });
assert.deepEqual(await r.json(), { enviado: false, motivo: "ya avisado" }, "no se avisa dos veces");
assert.equal(envios().length, 1);

// cierra el nutriólogo: se avisa al paciente
preparar({ registro: { cerradoPor: "NUTRIOLOGO" } });
r = await pedir({ pacienteId: "p1" }, "nutri1");
assert.deepEqual(await r.json(), { enviado: true });
{
  const mensaje = JSON.parse(envios()[0].cuerpo).message;
  assert.equal(mensaje.token, "tok-pac", "al paciente");
  assert.equal(mensaje.notification.body, "Kelly cerró el vínculo contigo.");
}

// sin dispositivo: no hay a dónde mandarlo y no se marca como avisado
preparar({ nutri: { fcmToken: undefined } });
datos["usuarios/nutri1"] = doc({});
r = await pedir({ pacienteId: "p1" });
assert.deepEqual(await r.json(), { enviado: false, motivo: "sin dispositivo" });
assert.equal(parches("pacientes/p1").length, 0);

// el teléfono ya no existe para FCM: se quita su token
preparar({ respuestaFcm: 404 });
r = await pedir({ pacienteId: "p1" });
assert.deepEqual(await r.json(), { enviado: false, motivo: "dispositivo caducado" });
assert.ok(!datos["usuarios/nutri1"].fields.fcmToken, "se borra el fcmToken caducado");

// FCM falla: 502, sin marcar
preparar({ respuestaFcm: 500 });
assert.equal((await pedir({ pacienteId: "p1" })).status, 502);
assert.equal(parches("pacientes/p1").length, 0);

console.log("vinculos.mjs: ok");
