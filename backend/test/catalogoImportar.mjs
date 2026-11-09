// Prueba del mapeo del importador del catálogo (openGym → nutriGajo): `npm run test:catalogo` (Node 22). Solo lógica pura, sin archivos.
import assert from "node:assert/strict";
import { crearIndice, equipoDe, fichaDe, tipoDe, zonaDe } from "../scripts/catalogo.lib.mjs";

// zona: las once partes del cuerpo de openGym caen en las siete de la app; lo desconocido, en cuerpo completo
assert.equal(zonaDe("chest"), "PECHO");
assert.equal(zonaDe("back"), "ESPALDA");
assert.equal(zonaDe("upper legs"), "PIERNAS");
assert.equal(zonaDe("lower legs"), "PIERNAS");
assert.equal(zonaDe("upper arms"), "BRAZOS");
assert.equal(zonaDe("waist"), "ABDOMEN");
assert.equal(zonaDe("neck"), "CUERPO_COMPLETO");
assert.equal(zonaDe("cardio"), "CUERPO_COMPLETO");
assert.equal(zonaDe("rarisimo"), "CUERPO_COMPLETO", "una parte nueva no rompe la importación");

// equipo: los cinco de la app; lo que no se conoce cae en máquina
assert.equal(equipoDe("body weight"), "SIN_EQUIPO");
assert.equal(equipoDe("assisted"), "SIN_EQUIPO");
assert.equal(equipoDe("dumbbell"), "MANCUERNAS");
assert.equal(equipoDe("kettlebell"), "MANCUERNAS");
assert.equal(equipoDe("band"), "BANDAS");
assert.equal(equipoDe("resistance band"), "BANDAS");
for (const b of ["barbell", "ez barbell", "olympic barbell", "trap bar", "landmine", "weight plate"]) assert.equal(equipoDe(b), "BARRA", b);
assert.equal(equipoDe("cable"), "MAQUINA");
assert.equal(equipoDe("smith machine"), "MAQUINA");
assert.equal(equipoDe("nuevo equipo"), "MAQUINA", "un equipo nuevo no rompe la importación");

// tipo: de la categoría
assert.equal(tipoDe("cardio"), "CARDIO");
assert.equal(tipoDe("stretching"), "FLEXIBILIDAD");
assert.equal(tipoDe("mobility"), "FLEXIBILIDAD");
assert.equal(tipoDe("yoga"), "YOGA");
assert.equal(tipoDe("strength"), "FUERZA");
assert.equal(tipoDe("pilates"), "FUERZA");
assert.equal(tipoDe(undefined), "FUERZA");

// ficha: título en español con mayúscula, pasos sin vacíos, músculos juntos, duración y kcal por defecto
const entrada = {
  id: "0025", name: "barbell bench press", bodyPart: "chest", equipment: "barbell", target: "pectorals", secondaryMuscles: ["triceps", "shoulders"],
  category: "strength", description: " Clásica. ", instructions: ["a", " ", "b"],
};
const es = { "0025": { name: "press de banca con barra" } };
const ficha = fichaDe(entrada, es);
assert.deepEqual(ficha, {
  id: "0025", titulo: "Press de banca con barra", zona: "PECHO", tipo: "FUERZA", equipo: "BARRA", musculos: ["pectorals", "triceps", "shoulders"],
  descripcion: "Clásica.", pasos: ["a", "b"], duracionMin: 5, kcal: 25,
});
assert.equal(fichaDe(entrada).titulo, "Barbell bench press", "sin traducción sale el nombre en inglés, capitalizado");
assert.deepEqual(fichaDe(entrada, { "0025": { name: "x", instructions: ["uno", "dos"] } }).pasos, ["uno", "dos"], "los pasos en español mandan si existen");
assert.equal(fichaDe({ ...entrada, category: "cardio" }, es).kcal, 45, "cardio gasta 9 por minuto");
assert.equal(fichaDe({ ...entrada, variantOf: "0099" }, es), null, "una figura alterna no es un ejercicio aparte");
assert.deepEqual(fichaDe({ id: "1", name: "x", bodyPart: "chest", equipment: "cable", target: "pectorals" }).pasos, [], "sin pasos no falla");

// índice: por título (con acentos como en español) y desempate por id
const f = (id, titulo) => ({ id, titulo, zona: "PECHO", tipo: "FUERZA", equipo: "BARRA" });
const indice = crearIndice([f("0003", "Zancadas"), f("0002", "Abdominales"), f("0001", "Abdominales"), f("0004", "Élevación")]);
assert.deepEqual(indice.map((fila) => fila[0]), ["0001", "0002", "0004", "0003"]);
assert.deepEqual(indice[0], ["0001", "Abdominales", "PECHO", "FUERZA", "BARRA"]);

console.log("catalogoImportar: todas las pruebas pasaron");
