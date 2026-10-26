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
const json = { "Content-Type": "application/json" };
assert.equal(await pedir("/citas/notificar", { method: "POST", body: "{}", headers: json }), 401, "avisar sin sesión");
assert.equal(await pedir("/citas/notificar", { method: "POST", body: "{}", headers: { ...json, Authorization: "Bearer falso" } }), 401, "avisar con token falso");
assert.equal(await pedir("/citas/notificar"), 404, "avisar con GET");
assert.equal(await pedir("/portada/../x"), 404, "ruta inexistente");
assert.equal(await pedir("/otra"), 404, "tipo desconocido");
console.log("humo.mjs: ok");
