// Prueba de humo contra un Worker en marcha (`npm run dev` en otra terminal): `npm test`.
// Comprueba lo que protege el servidor sin necesitar una sesión: rutas, sesión obligatoria y tipos permitidos.
import assert from "node:assert/strict";

const base = process.env.WORKER_URL ?? "http://localhost:8787";
const uidAjeno = "A".repeat(28);
const pedir = (ruta, init) => fetch(base + ruta, init).then((r) => r.status);

for (const tipo of ["portada", "avatar"]) {
  const jpeg = { "Content-Type": "image/jpeg" };
  assert.equal(await pedir(`/${tipo}`, { method: "PUT", body: "x", headers: jpeg }), 401, `${tipo}: subir sin sesión`);
  assert.equal(await pedir(`/${tipo}`, { method: "PUT", body: "x", headers: { ...jpeg, Authorization: "Bearer falso" } }), 401, `${tipo}: subir con token falso`);
  assert.equal(await pedir(`/${tipo}/${uidAjeno}`), 404, `${tipo}: leer una foto que no existe`);
}
// fotos de los ejercicios: subir exige sesión, leer una que no existe es 404 y las rutas mal formadas no existen
const jpegEj = { "Content-Type": "image/jpeg" };
assert.equal(await pedir("/ejercicio/e1", { method: "PUT", body: "x", headers: jpegEj }), 401, "ejercicio: subir sin sesión");
assert.equal(await pedir("/ejercicio/e1", { method: "PUT", body: "x", headers: { ...jpegEj, Authorization: "Bearer falso" } }), 401, "ejercicio: subir con token falso");
assert.equal(await pedir(`/ejercicio/${uidAjeno}/e1`), 404, "ejercicio: leer una foto que no existe");
assert.equal(await pedir("/ejercicio/e1"), 404, "ejercicio: leer sin el autor");
assert.equal(await pedir(`/ejercicio/${uidAjeno}/e1`, { method: "PUT", body: "x", headers: jpegEj }), 404, "ejercicio: subir con la ruta de lectura");
const json = { "Content-Type": "application/json" };
assert.equal(await pedir("/citas/notificar", { method: "POST", body: "{}", headers: json }), 401, "avisar sin sesión");
assert.equal(await pedir("/citas/notificar", { method: "POST", body: "{}", headers: { ...json, Authorization: "Bearer falso" } }), 401, "avisar con token falso");
assert.equal(await pedir("/citas/notificar"), 404, "avisar con GET");
const pdf = { "Content-Type": "application/pdf" };
assert.equal(await pedir("/plan/p1/plan1"), 401, "leer un plan sin sesión");
assert.equal(await pedir("/plan/p1/plan1", { headers: { Authorization: "Bearer falso" } }), 401, "leer un plan con token falso");
assert.equal(await pedir("/plan/p1/plan1", { method: "PUT", body: "%PDF-", headers: pdf }), 401, "subir un plan sin sesión");
assert.equal(await pedir("/planes/notificar", { method: "POST", body: "{}", headers: json }), 401, "avisar de un plan sin sesión");
assert.equal(await pedir("/plan/p1/plan1", { method: "DELETE" }), 404, "borrar un plan");
assert.equal(await pedir("/portada/../x"), 404, "ruta inexistente");
assert.equal(await pedir("/otra"), 404, "tipo desconocido");
console.log("humo.mjs: ok");
