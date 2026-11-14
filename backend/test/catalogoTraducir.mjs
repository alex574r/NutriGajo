// Prueba de las reglas de la traducción del catálogo (openGym → español): `npm run test:traducir` (Node 22). Solo lógica pura, sin red ni archivos.
import assert from "node:assert/strict";
import {
  ESQUEMA_SALIDA,
  INSTRUCCIONES,
  TAMANO_LOTE,
  aplicarTraduccion,
  costoDeLote,
  crearLotes,
  itemsPorTraducir,
  leerResultadoDeLote,
  pedidoDeLote,
  resultadoDeTexto,
  tokensAprox,
} from "../scripts/traducir.lib.mjs";

const press = { id: "0001", titulo: "Press", zona: "PECHO", tipo: "FUERZA", equipo: "BARRA", musculos: ["pectorals"], descripcion: "A press.", pasos: ["Lie down."], duracionMin: 5, kcal: 25 };
const remo = { id: "0002", titulo: "Remo", zona: "ESPALDA", tipo: "FUERZA", equipo: "MANCUERNAS", musculos: ["lats"], descripcion: "A row.", pasos: ["Siéntate."], duracionMin: 5, kcal: 25 };

// itemsPorTraducir: solo lo que se envía; los pasos que ya están en español no viajan
assert.equal(TAMANO_LOTE, 15);
const items = itemsPorTraducir([press, remo], new Set(["0002"]));
assert.deepEqual(items[0], { id: "0001", titulo: "Press", descripcion: "A press.", pasos: ["Lie down."] });
assert.deepEqual(items[1], { id: "0002", titulo: "Remo", descripcion: "A row.", pasos: [] }, "pasos ya en español: no se envían");

// crearLotes: en orden, con ids lote-0001…
const muchos = Array.from({ length: 35 }, (_, i) => ({ id: String(i).padStart(4, "0"), titulo: "t", descripcion: "d", pasos: [] }));
const lotes = crearLotes(muchos);
assert.deepEqual(lotes.map((l) => l.items.length), [15, 15, 5]);
assert.deepEqual(lotes.map((l) => l.id), ["lote-0001", "lote-0002", "lote-0003"]);
assert.equal(lotes[1].items[0].id, "0015");
assert.equal(crearLotes(muchos.slice(0, 3), 2).length, 2);

// pedidoDeLote: la petición para la Batches API
const pedido = pedidoDeLote(lotes[0], "claude-opus-5-5");
assert.equal(pedido.custom_id, "lote-0001");
assert.equal(pedido.params.model, "claude-opus-5-5");
assert.equal(pedido.params.system, INSTRUCCIONES);
assert.equal(pedido.params.output_config.effort, "low");
assert.equal(pedido.params.output_config.format.type, "json_schema");
assert.equal(pedido.params.output_config.format.schema, ESQUEMA_SALIDA);
assert.deepEqual(JSON.parse(pedido.params.messages[0].content), lotes[0].items);
assert.equal(ESQUEMA_SALIDA.additionalProperties, false);
assert.ok(INSTRUCCIONES.includes("spotter"), "el glosario va en las instrucciones");

// leerResultadoDeLote: cada ejercicio se valida por separado; un lote roto los pierde todos
const lote = {
  id: "lote-0001",
  items: [
    { id: "0001", titulo: "Press", descripcion: "A press.", pasos: ["Lie down.", "Push."] },
    { id: "0002", titulo: "Remo", descripcion: "A row.", pasos: [] },
  ],
};
const ok1 = { id: "0001", descripcion: "Un press.", pasos: ["Túmbate.", "Empuja."] };
const ok2 = { id: "0002", descripcion: "Un remo.", pasos: [] };
const resp = (filas) => JSON.stringify({ traducciones: filas });
const exito = (texto, stop = "end_turn") => ({
  custom_id: "lote-0001",
  result: { type: "succeeded", message: { stop_reason: stop, content: [{ type: "thinking", thinking: "" }, { type: "text", text: texto }] } },
});
const ids = (m) => [...m.keys()].sort();

// (a) válida y en otro orden
let r = leerResultadoDeLote(exito(resp([ok2, ok1])), lote);
assert.deepEqual(ids(r.traducidas), ["0001", "0002"]);
assert.deepEqual(r.fallidos, []);
assert.deepEqual(r.traducidas.get("0001"), { descripcion: "Un press.", pasos: ["Túmbate.", "Empuja."] });
// (b) distinto número de pasos: solo ese ejercicio falla
r = leerResultadoDeLote(exito(resp([{ ...ok1, pasos: ["Túmbate."] }, ok2])), lote);
assert.deepEqual(ids(r.traducidas), ["0002"]);
assert.deepEqual(r.fallidos, ["0001"]);
// (c) falta un ejercicio
r = leerResultadoDeLote(exito(resp([ok1])), lote);
assert.deepEqual(r.fallidos, ["0002"]);
// (d) un id que no estaba en el lote se ignora; un id repetido gana el primero
r = leerResultadoDeLote(exito(resp([ok1, { ...ok1, descripcion: "Otra." }, ok2, { id: "9999", descripcion: "x", pasos: [] }])), lote);
assert.deepEqual(ids(r.traducidas), ["0001", "0002"]);
assert.equal(r.traducidas.get("0001").descripcion, "Un press.");
assert.deepEqual(r.fallidos, []);
// (e) descripción en blanco y (f) un paso vacío
assert.deepEqual(leerResultadoDeLote(exito(resp([ok1, { ...ok2, descripcion: "  " }])), lote).fallidos, ["0002"]);
assert.deepEqual(leerResultadoDeLote(exito(resp([{ ...ok1, pasos: ["Túmbate.", " "] }, ok2])), lote).fallidos, ["0001"]);
// (g) el lote se cortó o se rechazó
for (const stop of ["max_tokens", "refusal"]) {
  r = leerResultadoDeLote(exito(resp([ok1, ok2]), stop), lote);
  assert.equal(r.traducidas.size, 0, stop);
  assert.deepEqual([...r.fallidos].sort(), ["0001", "0002"], stop);
}
// (h) la petición falló, expiró o se canceló
for (const tipo of ["errored", "expired", "canceled"]) {
  r = leerResultadoDeLote({ custom_id: "lote-0001", result: { type: tipo } }, lote);
  assert.equal(r.traducidas.size, 0, tipo);
  assert.deepEqual([...r.fallidos].sort(), ["0001", "0002"], tipo);
}
// (i) texto que no es JSON o sin `traducciones`, (j) sin bloque de texto
for (const texto of ["esto no es json", "{}", '{"traducciones":"x"}']) {
  assert.deepEqual([...leerResultadoDeLote(exito(texto), lote).fallidos].sort(), ["0001", "0002"], texto);
}
const sinTexto = { custom_id: "lote-0001", result: { type: "succeeded", message: { stop_reason: "end_turn", content: [{ type: "thinking", thinking: "" }] } } };
assert.deepEqual([...leerResultadoDeLote(sinTexto, lote).fallidos].sort(), ["0001", "0002"]);

// resultadoDeTexto: el archivo que escribe un subagente de Claude Code se valida igual que un renglón de la Batches API
r = leerResultadoDeLote(resultadoDeTexto("lote-0001", resp([ok2, ok1])), lote);
assert.deepEqual(ids(r.traducidas), ["0001", "0002"]);
assert.deepEqual(r.fallidos, []);
assert.deepEqual([...leerResultadoDeLote(resultadoDeTexto("lote-0001", "basura"), lote).fallidos].sort(), ["0001", "0002"]);
assert.deepEqual(leerResultadoDeLote(resultadoDeTexto("lote-0001", resp([{ ...ok1, pasos: [] }, ok2])), lote).fallidos, ["0001"], "un archivo con pasos de menos falla solo ese ejercicio");

// ida y vuelta de texto difícil: comillas, salto de línea, ñ, ¿¡ y «»
const dificil = 'Di «hola», ¿sí? ¡Ya!\nLínea 2 con "comillas" y ñ.';
const loteDificil = { id: "lote-0002", items: [{ id: "0003", titulo: "x", descripcion: "d", pasos: ["p"] }] };
r = leerResultadoDeLote(exito(resp([{ id: "0003", descripcion: dificil, pasos: [dificil] }])), loteDificil);
assert.equal(r.traducidas.get("0003").descripcion, dificil);
assert.equal(r.traducidas.get("0003").pasos[0], dificil);

// aplicarTraduccion: cambia solo descripción y pasos; no toca la original
const aplicada = aplicarTraduccion(press, { descripcion: " Un press. ", pasos: [" Túmbate. "] });
assert.deepEqual(aplicada, { ...press, descripcion: "Un press.", pasos: ["Túmbate."] });
assert.notEqual(aplicada, press);
assert.deepEqual(press.pasos, ["Lie down."]);
assert.deepEqual(aplicarTraduccion(press, undefined), press, "sin traducción, la ficha queda como estaba");
assert.deepEqual(aplicarTraduccion(remo, { descripcion: "Un remo.", pasos: [] }), { ...remo, descripcion: "Un remo." }, "pasos ya en español se conservan");

// costo (mitad del precio normal en lote) y tokens aproximados
assert.equal(costoDeLote(1_000_000, 1_000_000, "claude-opus-5-5"), 12);
assert.equal(costoDeLote(1, 1, "modelo-raro"), null);
assert.equal(tokensAprox([{ id: "1" }]), Math.ceil('[{"id":"1"}]'.length / 4));

console.log("catalogoTraducir: ok");
