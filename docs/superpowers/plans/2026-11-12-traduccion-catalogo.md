# Traducción del catálogo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Traducir al español la descripción (5,632) y los pasos (4,308) del catálogo de ejercicios de R2 con Claude, una sola vez, y publicar las fichas nuevas con una versión nueva del catálogo.

**Architecture:** Reglas puras en `backend/scripts/traducir.lib.mjs` (lotes, petición, lectura y validación de la respuesta, aplicar la traducción), probadas sin red. Un CLI delgado, `traducir-catalogo.mjs`, con tres modos (simulacro, `--enviar`, `--recoger`) sobre la Message Batches API. La salida es una carpeta nueva que `subir-catalogo.mjs` sube tal cual. La app no cambia.

**Tech Stack:** Node 22 (`.mjs`, `node:assert`, sin framework), `@anthropic-ai/sdk` (devDependency del `backend/`), Wrangler para la subida (ya existente).

**Spec:** `docs/superpowers/specs/2026-11-12-traduccion-catalogo-design.md`

## Global Constraints

- **Solo texto a la IA:** nunca fotos ni clips (licencia de Gym visual). No se envía `musculos`.
- **Los 1,324 conjuntos de pasos que ya están en español no se envían ni se modifican** (`i18n/es.json` → `instructions` no vacío).
- **La carpeta del importador no se modifica:** la salida va a una carpeta nueva fuera del repo (`subir-catalogo.mjs` solo sube `ficha/`, `indice-N.json`, `foto/`, `clip/`, `version.json`; ignora `traduccion.json`, `fallidos.json`, `muestra.md`).
- **Versión nueva** del catálogo: entero `YYYYMMDD` mayor que `20261107` y de a lo más 9 dígitos.
- **Modelo por defecto `claude-haiku-5-5`** (lo eligió el usuario el 12 nov 2026: «si es pura traducción, Haiku»), elegible con `--modelo`; la corrida real usa `output_config: {effort: "low"}` (traducción mecánica). Si la muestra de la prueba sale floja, se prueba Sonnet 5.5 u Opus 5.5 antes de la corrida completa.
- **Nada se gasta ni se sube sin el OK del usuario:** la corrida de la API, la prueba y la subida a R2 se piden por separado. La clave de API la pone el usuario (`ANTHROPIC_API_KEY` o `ant auth login`); no se escribe ni se imprime.
- **Sin commit** salvo que el usuario lo pida; mensajes de commit sin `Co-Authored-By` (regla del usuario del 11 nov 2026).
- Español neutro (de México), imperativo de «tú», igual que los pasos que ya están en español («Túmbate», «Agarra»).

## Review Focus

Entradas y fallas que el spec implica y las pruebas de cada tarea fijan:

1. La respuesta de un lote trae un número de pasos distinto, ids de más o de menos, o texto vacío: ese ejercicio queda en inglés y se lista como fallido; los demás del lote se aceptan.
2. El lote se cortó (`stop_reason` distinto de `end_turn`, p. ej. `max_tokens` o `refusal`), el resultado es `errored` o `expired`, o el JSON no se lee: todos los ejercicios del lote son fallidos.
3. Texto con comillas, saltos de línea, `ñ`, acentos y signos `¿¡`: sobrevive intacto a la ida y vuelta por JSON.
4. Ejercicio con pasos ya en español: sus pasos salen idénticos y no viajan en el pedido.
5. Los resultados de la API llegan en cualquier orden y `--recoger` se puede correr dos veces: siempre se parte de las fichas originales en inglés, nunca de la salida anterior.

---

### Task 1: Reglas puras de la traducción

**Files:**
- Create: `backend/scripts/traducir.lib.mjs`
- Create: `backend/test/catalogoTraducir.mjs`
- Modify: `backend/package.json` (script `"test:traducir": "node test/catalogoTraducir.mjs"`)

**Interfaces:**
- Consumes: la ficha del importador `{id, titulo, zona, tipo, equipo, musculos, descripcion, pasos, duracionMin, kcal}` (`catalogo.lib.mjs → fichaDe`).
- Produces (todo `export`):
  - `TAMANO_LOTE = 15`
  - `PRECIOS: Record<string, {entrada: number, salida: number}>` — USD por millón de tokens, precio normal, para `claude-opus-5-5` (4 / 20), `claude-sonnet-5-5` (2 / 10) y `claude-haiku-5-5` (0.1 / 0.5).
  - `costoDeLote(tokensEntrada: number, tokensSalida: number, modelo: string): number | null` — USD con el 50 % de descuento de Batches; `null` si el modelo no está en `PRECIOS`.
  - `itemsPorTraducir(fichas: Ficha[], conPasosEnEspanol: Set<string>): Item[]` con `Item = {id: string, titulo: string, descripcion: string, pasos: string[]}`; `pasos` es `[]` si el id está en el conjunto.
  - `crearLotes(items: Item[], tamano = TAMANO_LOTE): Array<{id: string, items: Item[]}>` — ids `lote-0001`, `lote-0002`…, en orden.
  - `tokensAprox(items: Item[]): number` — `Math.ceil(JSON.stringify(items).length / 4)`.
  - `ESQUEMA_SALIDA` y `INSTRUCCIONES` (texto del sistema con el glosario).
  - `pedidoDeLote(lote, modelo: string): {custom_id: string, params: object}` — `params`: `{model, max_tokens: 16000, system: INSTRUCCIONES, messages: [{role: "user", content: JSON.stringify(lote.items)}], output_config: {effort: "low", format: {type: "json_schema", schema: ESQUEMA_SALIDA}}}`.
  - `leerResultadoDeLote(resultado, lote): {traducidas: Map<string, {descripcion: string, pasos: string[]}>, fallidos: string[]}` — `resultado` es un renglón de `batches.results`: `{custom_id, result: {type: "succeeded", message: {stop_reason, content}} | {type: "errored" | "expired" | "canceled"}}`.
  - `aplicarTraduccion(ficha, traduccion | undefined): Ficha` — copia de la ficha con solo `descripcion` y `pasos` cambiados; sin traducción devuelve la ficha igual.

- [ ] **Step 1: Escribir la prueba que falla** — `backend/test/catalogoTraducir.mjs` (estilo de `catalogoImportar.mjs`: `import assert from "node:assert/strict"` y aserciones sueltas al nivel del módulo, termina con `console.log("catalogoTraducir: ok")`). Casos, con estos valores exactos:
  - `itemsPorTraducir`: dos fichas `{id:"0001", titulo:"Press", descripcion:"A press.", pasos:["Lie down."]}` y `{id:"0002", titulo:"Remo", descripcion:"A row.", pasos:["Siéntate."]}` con `new Set(["0002"])` → el item de `0002` lleva `pasos: []` y el de `0001` lleva `["Lie down."]`; ninguno lleva `musculos`.
  - `crearLotes`: 35 items → 3 lotes de 15, 15 y 5, ids `lote-0001`, `lote-0002`, `lote-0003`; con `tamano = 2` y 3 items → 2 lotes.
  - `pedidoDeLote`: `custom_id === lote.id`; `params.model` es el recibido; `params.output_config.effort === "low"`; `params.output_config.format.type === "json_schema"`; `JSON.parse(params.messages[0].content)` deep-equal a `lote.items`.
  - `leerResultadoDeLote` (con un lote de dos items, `0001` con 2 pasos y `0002` con 0 pasos): (a) respuesta válida (bloques `[{type:"thinking",thinking:""},{type:"text",text:JSON.stringify({traducciones:[{id:"0002",descripcion:"Un remo.",pasos:[]},{id:"0001",descripcion:"Un press.",pasos:["Túmbate.","Empuja."]}]})}]`, `stop_reason:"end_turn"`) → las dos en `traducidas`, aunque vengan en otro orden, y `fallidos` vacío; (b) `0001` con 1 paso en vez de 2 → solo `0002` traducida, `fallidos` = `["0001"]`; (c) falta `0002` → `fallidos` = `["0002"]`; (d) id desconocido `"9999"` extra → se ignora y no rompe; (e) `descripcion: "  "` → ese id fallido; (f) un paso vacío `""` → ese id fallido; (g) `stop_reason: "max_tokens"` o `"refusal"` → ambos fallidos; (h) `result.type` `"errored"`, `"expired"` y `"canceled"` → ambos fallidos; (i) texto que no es JSON → ambos fallidos; (j) sin bloque de texto → ambos fallidos.
  - Ida y vuelta de texto difícil: una traducción con `"Di «hola», ¿sí?\nLínea 2 con \"comillas\" y ñ."` pasa por `JSON.stringify` en la respuesta y sale idéntica en `traducidas`.
  - `aplicarTraduccion`: con traducción → `descripcion` y `pasos` nuevos y **todos los demás campos iguales** (deep-equal contra la original salvo esos dos); sin traducción (`undefined`) → deep-equal a la original; una traducción con `pasos: []` sobre una ficha con pasos en español → los pasos originales se conservan.
  - `costoDeLote(1_000_000, 1_000_000, "claude-opus-5-5") === 12` (mitad de 4 + 20); `costoDeLote(1, 1, "modelo-raro") === null`.
  - `tokensAprox([{id:"1"}]) === Math.ceil('[{"id":"1"}]'.length / 4)`.

- [ ] **Step 2: Correr y ver que falla**

  Run: `cd backend && node test/catalogoTraducir.mjs`
  Expected: error `Cannot find module '../scripts/traducir.lib.mjs'` (o `ERR_MODULE_NOT_FOUND`).

- [ ] **Step 3: Implementar `backend/scripts/traducir.lib.mjs`** con las firmas de Interfaces. Notas que la firma no decide:
  - `ESQUEMA_SALIDA`: objeto `{type:"object", properties:{traducciones:{type:"array", items:{type:"object", properties:{id:{type:"string"}, descripcion:{type:"string"}, pasos:{type:"array", items:{type:"string"}}}, required:["id","descripcion","pasos"], additionalProperties:false}}}, required:["traducciones"], additionalProperties:false}`.
  - `INSTRUCCIONES` (texto fijo, en español): traducir `descripcion` y `pasos` del inglés al español neutro de México, imperativo de «tú», sin agregar ni quitar pasos, **mismo número de pasos y mismo orden**; devolver `pasos: []` cuando el pedido trae `pasos: []`; conservar números, unidades y signos; usar `titulo` solo como contexto de los términos y no traducirlo; glosario: spotter → ayudante, rack → soporte, bench → banco, barbell → barra, dumbbell → mancuerna, kettlebell → pesa rusa, resistance band → banda elástica, cable → polea, lat pulldown → jalón al pecho, row → remo, lunge → zancada, squat → sentadilla, deadlift → peso muerto, plank → plancha, crunch → abdominal, grip → agarre; tono directo y sin adornos, una frase por paso como el original.
  - `leerResultadoDeLote`: toma el primer bloque `type === "text"`; todo lo que no sea `succeeded` + `end_turn` + JSON legible con `traducciones` arreglo marca **todos** los ids del lote como fallidos; por id: válido solo si existe, `descripcion.trim()` no vacía, `pasos.length` igual al del item, ningún paso `trim()` vacío, y el id estaba en el lote. Ids repetidos: gana el primero.
  - `aplicarTraduccion`: `{...ficha, descripcion: t.descripcion.trim(), pasos: t.pasos.length ? t.pasos.map(trim) : ficha.pasos}`.

- [ ] **Step 4: Correr y ver que pasa**

  Run: `cd backend && node test/catalogoTraducir.mjs`
  Expected: sale `catalogoTraducir: ok` sin errores.

- [ ] **Step 5: Agregar el script a `package.json` y correr las pruebas existentes del catálogo**

  Run: `cd backend && npm run test:traducir && npm run test:catalogo && npm run check`
  Expected: las tres terminan sin error (`catalogoImportar` sigue pasando; `tsc` limpio).

---

### Task 2: CLI `traducir-catalogo.mjs`

**Files:**
- Create: `backend/scripts/traducir-catalogo.mjs`
- Modify: `backend/package.json` (devDependency `@anthropic-ai/sdk`; script `"traducir-catalogo": "node scripts/traducir-catalogo.mjs"`) y `backend/package-lock.json`.

**Interfaces:**
- Consumes: Task 1 completo; `fichaDe` no se usa (se leen las fichas ya hechas); `@anthropic-ai/sdk` → `new Anthropic()`, `client.messages.batches.create({requests})`, `.retrieve(id)`, `.results(id)`.
- Produces (uso):
  `node scripts/traducir-catalogo.mjs <carpeta del importador> <repo de openGym> <carpeta de salida> [--modelo ID] [--lotes N] [--version N] [--enviar | --recoger]`
  - **Sin `--enviar` ni `--recoger` (simulacro):** no llama a la API ni escribe nada. Imprime: fichas leídas, ejercicios con pasos ya en español, ejercicios por traducir, lotes, tokens de entrada aproximados (`tokensAprox` de todos los items), tokens de salida aproximados (1.1 × la entrada) y el costo por modelo de `PRECIOS` con `costoDeLote`.
  - **`--enviar`:** crea el lote en la Batches API con un `pedidoDeLote` por lote (solo los primeros `N` con `--lotes N`, para la prueba) y escribe `<salida>/traduccion.json` = `{batchId, modelo, creado, lotes: [{id, ids: string[]}]}`. Si ese archivo ya existe, se niega y manda a `--recoger`.
  - **`--recoger`:** consulta el lote; si `processing_status !== "ended"`, imprime cuántas peticiones faltan y termina con código 0. Si terminó: lee `batches.results`, aplica `leerResultadoDeLote` por `custom_id` y escribe en `<salida>`: `ficha/{id}.json` para **todas** las fichas del importador (traducida si hay, original si no), `indice-{versión}.json` (copia del `indice-*.json` del importador), `version.json` = `{"version": V, "total": N}` (`V` = `--version` o la fecha de hoy `YYYYMMDD`), `fallidos.json` (ids), `muestra.md` (20 ejercicios al azar: id, título, descripción y primer paso en inglés y en español). Imprime `Traducidas X de N · fallidas Y`; si `X < N` imprime **«Incompleto: no subas esta carpeta»**. Idempotente: siempre parte de `<importador>/ficha/`, nunca de `<salida>`.

- [ ] **Step 1: Instalar el SDK en el backend**

  Run: `cd backend && npm install --save-dev @anthropic-ai/sdk`
  Expected: `package.json` gana la devDependency; `npm run check` sigue limpio (el Worker no la importa).

- [ ] **Step 2: Implementar el CLI** con la interfaz de arriba. Notas: argumentos como en `subir-catalogo.mjs` (posicionales + banderas; salir con mensaje de uso y código 1 si faltan o si las carpetas no existen; `--modelo` por defecto `claude-opus-5-5`; `--version` entero de 1 a 9 dígitos y mayor que la versión de `<importador>/version.json`); las ids con pasos en español salen de `<repo de openGym>/catalogue/i18n/es.json` (`instructions` arreglo no vacío); la salida no puede estar dentro del repo de nutriGajo (misma guarda que `importar-catalogo.mjs`); los `fallidos` reintentables se vuelven a pedir corriendo el modo `--enviar` con una `<salida>` nueva solo con ellos (no se automatiza: se reporta).

- [ ] **Step 3: Verificar el simulacro contra la carpeta real, sin gasto**

  Run (rutas del importador y de openGym; salida en el scratchpad):
  `cd backend && node scripts/traducir-catalogo.mjs "C:/Users/alex5/AppData/Local/Temp/claude/6ae0e14d20615ed1/4a8f49e6-e95c-4c50-a474-4bc6e8a8aaa8/scratchpad/catalogo-salida" "C:/Users/alex5/Desktop/openGym" "<scratchpad>/catalogo-es"`
  Expected: `Fichas: 5632`, con pasos en español `1324`, por traducir `5632`, lotes `376`, tokens de entrada cercanos a 400 mil y un costo por modelo con Opus por debajo de US$ 10; no se creó la carpeta de salida.

- [ ] **Step 4: Verificar que sin clave no hace nada raro**

  Run: la misma orden con `--enviar` y `ANTHROPIC_API_KEY` sin definir.
  Expected: error claro de credenciales del SDK, sin crear `traduccion.json`.

---

### Task 3: Prueba real de 2 lotes y revisión de calidad (con OK del usuario)

**Files:** ninguno del repo (la salida va al scratchpad); si la muestra pide ajustar el glosario, se edita `INSTRUCCIONES` en `traducir.lib.mjs` y se vuelve a correr `npm run test:traducir`.

**Interfaces:**
- Consumes: Task 2.
- Produces: la decisión del usuario sobre el modelo y la calidad.

- [ ] **Step 1: Pedir el OK del gasto.** Mostrar el simulacro y el costo por modelo; confirmar el modelo (Haiku 5.5, el elegido) y confirmar que la clave está en el entorno (o `ant auth status` con perfil activo). **No continuar sin el sí.**
- [ ] **Step 2: Enviar la prueba**

  Run: `cd backend && node scripts/traducir-catalogo.mjs <importador> <openGym> <salida-prueba> --lotes 2 --modelo <modelo> --enviar`
  Expected: imprime el id del lote (`msgbatch_…`) y crea `traduccion.json`.
- [ ] **Step 3: Recoger** (corre `--recoger` cada pocos minutos, no en bucle; los lotes suelen terminar en menos de 1 hora)

  Run: `… --recoger`
  Expected: al terminar, `Traducidas 30 de 5632 · fallidas 0` seguido de «Incompleto: no subas esta carpeta» (es la prueba). Correr `--recoger` una segunda vez y comprobar que `ficha/` queda idéntica (mismo hash de `ficha/0025.json` y de una ficha traducida): el modo es idempotente.
- [ ] **Step 4: Revisar `muestra.md` con el usuario** (mostrar 5 pares inglés/español en el chat). Criterios: términos del glosario, mismo número de pasos, tono de los pasos ya existentes. Si algo falla, ajustar `INSTRUCCIONES`, repetir Steps 2-3 con una salida nueva.

---

### Task 4: Corrida completa, verificación y subida a R2 (con OK del usuario)

**Files:** ninguno del repo; al final, nota de memoria `ejercicios-en-la-nube.md`.

**Interfaces:**
- Consumes: Tasks 2-3 (modelo y glosario aprobados).
- Produces: fichas en español en R2 y versión nueva del catálogo.

- [ ] **Step 1: Pedir el OK de la corrida completa** con el costo del modelo elegido.
- [ ] **Step 2: Enviar y recoger** con una `<salida>` nueva sin `--lotes`.

  Expected: `Traducidas 5632 de 5632 · fallidas 0` (o pocas fallidas; con fallidas, volver a enviar solo esas a una salida nueva y fusionar a mano sus fichas en la salida completa, o aceptar que queden en inglés con el OK del usuario).
- [ ] **Step 3: Verificar la carpeta antes de subir**

  Run: `node -e` que cuente `ficha/*.json` (5,632), lea `version.json` (versión nueva, `total` 5632), compruebe que `indice-{versión}.json` existe y que ninguna ficha tiene `descripcion` vacía ni `pasos` vacíos.
  Expected: todo cumple.
- [ ] **Step 4: Simulacro de subida**

  Run: `cd backend && node scripts/subir-catalogo.mjs <salida>`
  Expected: `Plan: 5634 archivos` (5,632 fichas, el índice y `version.json`); último `catalogo/version.json`.
- [ ] **Step 5: Pedir el OK de la subida (escribe en R2 de producción) y subir**

  Run: `cd backend && node scripts/subir-catalogo.mjs <salida> --remote`
  Expected: `Listo: 5633 archivos y version.json subidos a appsmobile/catalogo/.` (unos 40 minutos; reanudable con `subidos.txt`).
- [ ] **Step 6: Ver el resultado en el teléfono** (instalada la app que ya está; no hace falta reinstalar): abrir «Ver todas» de «Más ejercicios extra», entrar a un ejercicio del catálogo que antes salía en inglés (p. ej. «Atrape de press de banca con barra» o «Apertura alta con banda elástica»).
  Expected: descripción y pasos en español. Anotar el resultado en `ejercicios-en-la-nube.md` y avisar que falta decidir la atribución de las fuentes.
