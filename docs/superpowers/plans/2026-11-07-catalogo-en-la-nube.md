# Catálogo de ejercicios en la nube (fase 3) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (el usuario ya eligió ejecución nativa en las fases 1 y 2). Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cargar el catálogo de ejercicios de openGym (fichas, fotos y clips animados) al R2 de nutriGajo y que el nutriólogo lo use desde «Globales» para asignar, y el paciente lo vea y lo entrene, sin que nada viva en el APK.

**Architecture:** Un importador local convierte el repo de openGym en una carpeta (`version.json`, `indice-{n}.json`, `ficha/{id}.json`, `foto/{id}.webp`, `clip/{id}.mp4`) y otro script la sube a R2 bajo `catalogo/`. El Worker sirve esas claves solo con sesión de Firebase. La app baja el índice una vez (búsqueda local) y la ficha, la foto y el clip al abrir un ejercicio; el catálogo entra como `Ejercicio` con `fuente = CATALOGO`.

**Tech Stack:** Node 22 (scripts y pruebas `.mjs`), Cloudflare Worker + R2 (TypeScript), Kotlin + Compose (app), Media3 (ya instalado).

**Spec:** `docs/superpowers/specs/2026-11-07-ejercicios-en-la-nube-design.md` (sección 2, «Catálogo en R2», y fase 3). Fases 1 y 2: `docs/superpowers/plans/2026-11-07-ejercicios-en-la-nube.md` (hechas).

## Global Constraints

- **Autorización:** el usuario afirmó el 7 nov 2026 que tiene la autorización para usar el catálogo de openGym y sus ilustraciones (licencia de Gym visual). El plan lo toma como dicho; no se copia ningún medio al repo de nutriGajo ni a git (la carpeta de importación va fuera del repo) y los medios no se alimentan a ninguna IA.
- Solo se importan las **5.632 entradas principales** (sin `variantOf`); las figuras femeninas quedan fuera. Fotos y clips de 180 px tal como vienen.
- Claves en R2 (`appsmobile`): `catalogo/version.json`, `catalogo/indice-{n}.json`, `catalogo/ficha/{id}.json`, `catalogo/foto/{id}.webp`, `catalogo/clip/{id}.mp4`. Id = `^[0-9]{4,5}$`.
- Todo `GET /catalogo/...` **exige sesión válida de Firebase** (401 sin ella) y responde con `Cache-Control: private` (`no-cache` en `version.json`, `private, max-age=31536000, immutable` en lo demás); los clips aceptan `Range`.
- Zona, tipo y equipo salen ya en los enums de la app (`ZonaCuerpo`, `TipoEjercicio`, `EquipoEjercicio`); nivel = `PRINCIPIANTE`, duración = 5 min y kcal = duración × (FUERZA 5, CARDIO 9, FLEXIBILIDAD 3, YOGA 3) por defecto.
- Título en español (`i18n/es.json`, primera letra en mayúscula); descripción y pasos en inglés salvo los 1.324 con pasos en español.
- Subir a R2, desplegar el Worker o publicar reglas **solo con el OK explícito del usuario cada vez**. Sin commit, push ni `pm clear` ni desinstalar. Árbol compartido: editar solo lo propio. Comandos Gradle: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`. Lógica pura con prueba unitaria al lado.

## Review Focus

- Una ficha con el nombre en español ausente o un paso vacío: cae al inglés / se descarta el paso vacío, sin romper el índice (Task 1).
- Equipo o categoría de openGym que el mapeo no conoce: cae a `MAQUINA` / `FUERZA` en vez de fallar la importación entera (Task 1).
- Un id sin foto o sin clip en el repo de openGym: la importación lo reporta y lo salta, no sube fichas sin medios (Task 2).
- Sin sesión o con una id mal formada (`../`, letras) el Worker responde 401 / 404 y nunca lee otra clave (Task 3).
- Resubir el catálogo con otra versión no pisa lo que ya ven los teléfonos: el índice lleva la versión en su nombre y `version.json` es lo último que se sube (Tasks 2 y 4).
- Sin red al abrir un ejercicio del catálogo en la app: se ve el título guardado y no hay cierre; el índice descargado antes sigue sirviendo a la búsqueda (Tasks 6 y 7).
- Un asignado de `fuente = CATALOGO` cuyo ejercicio ya no está en el catálogo: «Ya no está disponible», no cierre (Task 7).

---

## File Structure

- Create `backend/scripts/catalogo.lib.mjs` (mapeo y reglas puras), `backend/scripts/importar-catalogo.mjs` (CLI), `backend/scripts/subir-catalogo.mjs` (CLI de subida); tests `backend/test/catalogoImportar.mjs`, `backend/test/catalogoRutas.mjs`, `backend/test/catalogoSubir.mjs`.
- Create `backend/src/routes/catalogo.ts`; modify `backend/src/index.ts`, `backend/package.json`.
- Create `app/.../domain/model/Catalogo.kt` (índice, ficha, adaptador); modify `Ejercicio.kt` (`fuente`), `Plan.kt`/`RutinaDelDia.kt` (resolver), `ui/nutriologo/plan/AsignarEjercicioSheet.kt` (`desde` usa la fuente).
- Create `app/.../data/repository/CatalogoRepository.kt`; create `ui/shared/ejercicios/CatalogoViewModel.kt`.
- Modify `ui/shared/ejercicios/EjercicioVistas.kt` y `ClipEjercicio.kt` (foto y clip por fuente), `ui/nutriologo/plan/BancoScreen.kt` (Globales mezcla el catálogo), `ui/paciente/inicio/InicioPacienteScreen.kt` y `ui/paciente/ejercicios/EjerciciosAsignadosScreen.kt` (resolver con el catálogo).

---

### Task 1: Mapeo y reglas del importador

**Files:**
- Create: `backend/scripts/catalogo.lib.mjs`
- Modify: `backend/package.json` (script `"test:catalogo": "node test/catalogoImportar.mjs"`)
- Test: `backend/test/catalogoImportar.mjs`

**Interfaces:**
- Produces (todas exportadas, puras): `zonaDe(bodyPart: string): string` (`chest→PECHO`, `back→ESPALDA`, `shoulders→HOMBROS`, `waist→ABDOMEN`, `upper arms|lower arms→BRAZOS`, `upper legs|lower legs→PIERNAS`, `full body|cardio|neck→CUERPO_COMPLETO`; desconocido → `CUERPO_COMPLETO`); `equipoDe(equipment: string): string` (`body weight|assisted→SIN_EQUIPO`; `dumbbell|kettlebell→MANCUERNAS`; `band|resistance band→BANDAS`; `barbell|ez barbell|olympic barbell|trap bar|landmine|weight plate→BARRA`; cualquier otro, incluido el desconocido → `MAQUINA`); `tipoDe(category: string | undefined): string` (`cardio→CARDIO`; `stretching|mobility→FLEXIBILIDAD`; `yoga→YOGA`; el resto y el desconocido → `FUERZA`); `kcalPorMinuto(tipo)`; `capitalizar(texto)`; `fichaDe(entrada, es?): Ficha | null` con `Ficha = { id, titulo, zona, tipo, equipo, musculos: string[], descripcion, pasos: string[], duracionMin: 5, kcal }` (null si es `variantOf`); `crearIndice(fichas): Array<[id, titulo, zona, tipo, equipo]>` ordenado por título (`localeCompare("es")`) y luego id.

- [ ] **Step 1: Write the failing test** `catalogoImportar.mjs` con `node:assert/strict`: `zonaDe("upper legs") === "PIERNAS"`, `zonaDe("neck") === "CUERPO_COMPLETO"`, `zonaDe("rarisimo") === "CUERPO_COMPLETO"`; `equipoDe("kettlebell") === "MANCUERNAS"`, `equipoDe("resistance band") === "BANDAS"`, `equipoDe("assisted") === "SIN_EQUIPO"`, `equipoDe("cable") === "MAQUINA"`, `equipoDe("nuevo equipo") === "MAQUINA"`; `tipoDe("stretching") === "FLEXIBILIDAD"`, `tipoDe(undefined) === "FUERZA"`, `tipoDe("pilates") === "FUERZA"`; `fichaDe({ variantOf: "0025", ... }) === null`; con `{ id: "0025", name: "barbell bench press", bodyPart: "chest", equipment: "barbell", target: "pectorals", secondaryMuscles: ["triceps"], category: "strength", description: "Clásica.", instructions: ["a", " ", "b"] }` y `es = { "0025": { name: "press de banca con barra" } }` da `titulo: "Press de banca con barra"`, `musculos: ["pectorals", "triceps"]`, `pasos: ["a", "b"]` (descarta el vacío), `duracionMin: 5`, `kcal: 25`; sin `es` el título sale del inglés capitalizado; con `es[id].instructions` los pasos salen de ahí; `crearIndice` pone «Abdominales» antes que «Zancadas» y desempata por id.
- [ ] **Step 2: Run** `cd backend && npm run test:catalogo`; Expected: FAIL (módulo inexistente).
- [ ] **Step 3: Implement** `catalogo.lib.mjs` con las firmas de arriba (`kcal = duracionMin * kcalPorMinuto(tipo)`).
- [ ] **Step 4: Run** `npm run test:catalogo`; Expected: PASS.

### Task 2: CLI del importador y corrida real

**Files:**
- Create: `backend/scripts/importar-catalogo.mjs`

**Interfaces:**
- Consumes: `fichaDe`, `crearIndice` (Task 1).
- Produces: `node scripts/importar-catalogo.mjs <repo openGym> <carpeta de salida> [--version N]` escribe en la salida `ficha/{id}.json`, `foto/{id}.webp`, `clip/{id}.mp4` (copiados), `indice-{N}.json` y, **al final**, `version.json` (`{ "version": N, "total": T }`); `N` por defecto = fecha `YYYYMMDD` como entero. Imprime cuántas fichas, fotos y clips escribió y la lista de ids saltados (sin foto o sin clip). Sale con código 1 si no escribió ninguna ficha o la carpeta de salida está dentro del repo de nutriGajo.

- [ ] **Step 1: Write the CLI** (lee `catalogue/exercises/*.json`, `catalogue/i18n/es.json`, `catalogue/media/still` y `clip`; una ficha solo se escribe si existen su foto y su clip).
- [ ] **Step 2: Run on the real repo**: `node backend/scripts/importar-catalogo.mjs "<ruta-a-openGym>" "<scratchpad>/catalogo-salida" --version 20261107`; Expected: 5.632 fichas, 5.632 fotos, 5.632 clips, 0 saltados, un `indice-20261107.json` de unos 600 KB y `version.json` con `total: 5632`.
- [ ] **Step 3: Verify** con un script de una línea que cuenta archivos y comprueba que `ficha/0025.json` tiene `zona: "PECHO"`, `equipo: "BARRA"` y `tipo: "FUERZA"`; Expected: coincide.

### Task 3: Rutas del Worker para el catálogo

**Files:**
- Create: `backend/src/routes/catalogo.ts`
- Modify: `backend/src/index.ts`, `backend/package.json` (`"test:catalogoRutas": "node test/catalogoRutas.mjs"`)
- Test: `backend/test/catalogoRutas.mjs` (mismo R2 de mentira con `range` que `clipBanco.mjs`)

**Interfaces:**
- Produces: `leerCatalogo(req: Request, env: Env, ruta: string): Promise<Response>` donde `ruta` es lo que sigue a `/catalogo/` y solo se acepta si coincide con `version.json`, `indice-{n}.json` (`n` de 1 a 9 dígitos), `ficha/{id}.json`, `foto/{id}.webp` o `clip/{id}.mp4` (id `^[0-9]{4,5}$`); lo demás → 404. En `index.ts`: `GET /catalogo/*` verifica la sesión con `verificarSesion` (401 sin ella) y llama a `leerCatalogo`. Tipos: json `application/json`, webp `image/webp`, mp4 `video/mp4`; `nosniff` siempre; `Accept-Ranges` en el clip; 206 con `Content-Range` ante `Range`.

- [ ] **Step 1: Write the failing test**: `version.json` → 200 con `Cache-Control: no-cache` y `private`; `ficha/0025.json` → 200 `application/json` con `private, max-age=31536000, immutable`; `foto/0025.webp` → `image/webp`; `clip/0025.mp4` con `Range: bytes=0-9` → 206 y `Content-Range: bytes 0-9/{tamaño}`; claves inexistentes → 404; rutas mal formadas (`ficha/../x.json`, `ficha/ab.json`, `clip/0025.mp4/extra`, `indice-abc.json`, `foto/0025.png`) → 404 sin llamar a `env.FOTOS.get` (el R2 de mentira cuenta las lecturas); un `Content-Type` ausente en el objeto cae al del tipo de la ruta.
- [ ] **Step 2: Run** `npm run test:catalogoRutas`; Expected: FAIL.
- [ ] **Step 3: Implement** `catalogo.ts` y el enrutado (clave R2 = `catalogo/${ruta}`, validada por las expresiones de arriba).
- [ ] **Step 4: Run** `npm run test:catalogoRutas && npm run check`; Expected: PASS y `tsc` sin errores.

### Task 4: Script de subida a R2

**Files:**
- Create: `backend/scripts/subir-catalogo.mjs`
- Test: `backend/test/catalogoSubir.mjs`

**Interfaces:**
- Produces: `planDeSubida(carpeta: string): Array<{ archivo: string, clave: string, tipo: string }>` (exportada; recorre la carpeta de la Task 2 y devuelve la lista con `version.json` **siempre al final** y los `indice-*.json` justo antes; `clave = "catalogo/" + ruta relativa con /`; tipos `application/json`, `image/webp`, `video/mp4`; ignora lo que no sea de esos cuatro tipos). CLI: `node scripts/subir-catalogo.mjs <carpeta> [--remote] [--concurrencia 8]`; sin `--remote` solo imprime el plan y los totales (simulacro); con `--remote` ejecuta `wrangler r2 object put appsmobile/{clave} --file {archivo} --content-type {tipo} --remote` en paralelo, anota cada clave subida en `<carpeta>/subidos.txt` y se salta las ya anotadas (se puede reanudar), y sube `version.json` solo si todo lo anterior salió bien.

- [ ] **Step 1: Write the failing test**: con una carpeta temporal de 3 fichas, 3 fotos, 3 clips, un índice y `version.json` más un archivo ajeno (`notas.txt`), `planDeSubida` devuelve 11 elementos, el último es `version.json`, el anterior es el índice, todas las claves empiezan por `catalogo/`, la de la foto es `catalogo/foto/0025.webp` con tipo `image/webp` y `notas.txt` no aparece.
- [ ] **Step 2: Run** `node test/catalogoSubir.mjs`; Expected: FAIL.
- [ ] **Step 3: Implement** `planDeSubida` y el CLI.
- [ ] **Step 4: Run** la prueba y un simulacro sobre la salida real de la Task 2 (`node scripts/subir-catalogo.mjs "<salida>"`); Expected: PASS y el simulacro imprime ≈ 16.900 elementos y `version.json` como último, sin subir nada.

### Task 5: Dominio del catálogo en la app

**Files:**
- Create: `app/src/main/java/com/example/nutrigajo/domain/model/Catalogo.kt`
- Modify: `domain/model/Ejercicio.kt` (`fuente`), `ui/nutriologo/plan/AsignarEjercicioSheet.kt` (`desde`)
- Test: `app/src/test/java/com/example/nutrigajo/domain/model/CatalogoTest.kt`, `EjercicioAsignadoTest.kt`/`AsignarEjercicioTest.kt`

**Interfaces:**
- Produces: `Ejercicio.fuente: FuenteEjercicio = FuenteEjercicio.BANCO` (nuevo campo con valor por defecto; no se guarda en Firestore); `data class EntradaCatalogo(val id: String, val titulo: String, val zona: ZonaCuerpo, val tipo: TipoEjercicio, val equipo: EquipoEjercicio)`; `fun parsearIndice(json: String): List<EntradaCatalogo>` (filas que no son de 5 elementos o con enums desconocidos se saltan); `fun EntradaCatalogo.aEjercicio(): Ejercicio` (`fuente = CATALOGO`, `estado = PUBLICADO`, `autorNombre = "Catálogo Nutri Gajo"`, `duracionMin = 5`, `nivel = PRINCIPIANTE`, kcal por tipo, sin `fotoVersion`/`clipVersion`); `fun fichaAEjercicio(json: String): Ejercicio?` (lee `ficha/{id}.json` completo: descripción, pasos, músculos como parte de la descripción no hace falta; null si falta id o título); `fun List<EntradaCatalogo>.buscar(busqueda: String, zona: ZonaCuerpo?, tipo: TipoEjercicio?, limite: Int = 60): List<EntradaCatalogo>` (sin acentos ni mayúsculas, por título; vacío = primeros `limite`); `fun debeActualizarIndice(versionLocal: Int?, versionRemota: Int): Boolean`; `EjercicioAsignado.desde` usa `ejercicio.fuente` en vez de `BANCO` fijo.

- [ ] **Step 1: Write the failing tests** (`CatalogoTest`): `parsearIndice("""[["0025","Press de banca","PECHO","FUERZA","BARRA"],["x"],["0026","Mal","CEREBRO","FUERZA","BARRA"]]""")` devuelve una sola entrada; `aEjercicio()` da `fuente == CATALOGO`, `kcal == 25` para FUERZA y `45` para CARDIO, `estado == PUBLICADO`; `fichaAEjercicio` de un JSON completo trae `pasos` y `descripcion`, y `null` sin `titulo`; `buscar("PRESS", ...)` encuentra «Press de banca» y `buscar("sentadilla")` ignora acentos («Sentadilla búlgara»); `buscar("", null, null, limite = 2).size == 2`; filtros de zona y tipo; `debeActualizarIndice(null, 1)`, `(3, 4)` son true y `(4, 4)`, `(5, 4)` son false. En `AsignarEjercicioTest`: `desde(Ejercicio(id = "0025", titulo = "Press", fuente = CATALOGO), 1, "3", "", "", "")!!.fuente == CATALOGO`.
- [ ] **Step 2: Run** `./gradlew.bat :app:testDebugUnitTest --tests "*CatalogoTest" --tests "*AsignarEjercicioTest"`; Expected: no compila.
- [ ] **Step 3: Implement** lo anterior (JSON con `org.json`, que ya usa `FotosBanco`; en los tests JVM puros `org.json` puede venir vacío: usar `kotlinx` no está disponible, así que el parseo recibe la lista ya decodificada si `org.json` falla en la prueba — decidir al ver el primer RED y anotarlo).
- [ ] **Step 4: Run** las mismas pruebas y la suite completa; Expected: PASS.

### Task 6: Repositorio del catálogo (datos)

**Files:**
- Create: `app/src/main/java/com/example/nutrigajo/data/repository/CatalogoRepository.kt`
- Test: `app/src/test/java/com/example/nutrigajo/data/repository/CatalogoRepositoryTest.kt`

**Interfaces:**
- Consumes: `debeActualizarIndice`, `parsearIndice`, `fichaAEjercicio` (Task 5); `BuildConfig.WORKER_URL`.
- Produces: `class CatalogoRepository(auth, base, directorio: File)` con `suspend fun indice(): List<EntradaCatalogo>` (lee `version.json`; si cambió, baja `indice-{n}.json` a `directorio/catalogo/` y guarda la versión; sin red devuelve lo guardado o lista vacía, sin lanzar), `suspend fun ficha(id: String): Ejercicio?`, `suspend fun foto(id: String): Bitmap?`, `suspend fun clip(id: String): File?` (caché en disco por id; solo se bajan al abrir); todo con `Authorization: Bearer {ID token}`; `internal fun nombreArchivoCatalogo(tipo: String, id: String): String` (`"catalogo-foto-0025.webp"`, `"catalogo-clip-0025.mp4"`, `"catalogo-ficha-0025.json"`), `internal fun esIdCatalogo(id: String): Boolean` (`^[0-9]{4,5}$`).

- [ ] **Step 1: Write the failing test**: nombres de archivo para los tres tipos; `esIdCatalogo("0025")` y `("10189")` true, `("../x")`, `("abc")`, `("123")`, `("")` false.
- [ ] **Step 2: Run** `--tests "*CatalogoRepositoryTest"`; Expected: no compila.
- [ ] **Step 3: Implement** el repositorio (mismo esquema de conexión que `FotosBanco`; una id que no pasa `esIdCatalogo` devuelve null sin pedir nada).
- [ ] **Step 4: Run** la prueba y `:app:compileDebugKotlin`; Expected: PASS y BUILD SUCCESSFUL.

### Task 7: Cableado en la app

**Files:**
- Create: `app/src/main/java/com/example/nutrigajo/ui/shared/ejercicios/CatalogoViewModel.kt`
- Modify: `ui/shared/ejercicios/EjercicioVistas.kt` (`FotoEjercicio`), `ClipEjercicio.kt` (`MediaEjercicio`), `ui/nutriologo/plan/BancoScreen.kt`, `domain/model/RutinaDelDia.kt` (`resolverDelBanco`), `ui/paciente/inicio/InicioPacienteScreen.kt`, `ui/paciente/ejercicios/EjerciciosAsignadosScreen.kt`, `strings.xml`
- Test: `app/src/test/java/com/example/nutrigajo/domain/model/AsignadosDelPacienteTest.kt`

**Interfaces:**
- Consumes: `CatalogoRepository` (Task 6), `EntradaCatalogo.aEjercicio`, `buscar` (Task 5).
- Produces: `resolverDelBanco(banco: List<Ejercicio>, catalogo: Map<String, Ejercicio> = emptyMap())` (para `fuente = CATALOGO` resuelve con `catalogo[id]`); `class CatalogoViewModel` con `estado: StateFlow<CatalogoEstado(cargando, indice, fichas: Map<String, Ejercicio>, error)>`, `cargarIndice()` y `cargarFichas(ids: Collection<String>)`; `FotoEjercicio` y `MediaEjercicio` descargan de `CatalogoRepository` cuando `ejercicio.fuente == CATALOGO` (si falla, marcador/foto por defecto, sin cierre); la pestaña «Globales» de ejercicios en `BancoScreen` muestra primero lo publicado por entrenadores y debajo (o al buscar) las entradas del catálogo (`buscar(...)`, máx. 60, con `Esqueleto` mientras baja el índice); el inicio del paciente pide las fichas de sus asignados del catálogo y las pasa al resolver.

- [ ] **Step 1: Write the failing test**: en `AsignadosDelPacienteTest`, `resolverDelBanco(banco, mapOf("0025" to ficha))(asignadoCatalogo("0025")) == ficha`; un asignado de `CATALOGO` sin esa ficha da `null`; uno de `BANCO` no se confunde con un catálogo del mismo id; con solo asignados de catálogo sin fichas, `rutinaAsignada(...) == null` (cae al baraje).
- [ ] **Step 2: Run** `--tests "*AsignadosDelPacienteTest"`; Expected: FAIL.
- [ ] **Step 3: Implement** `resolverDelBanco` con el segundo parámetro y el resto del cableado de arriba.
- [ ] **Step 4: Run** `./gradlew.bat :app:testDebugUnitTest :app:compileDebugKotlin`; Expected: todo PASS y BUILD SUCCESSFUL.

### Task 8: Puertas de producción (cada una con OK del usuario)

- [ ] **Step 1:** Pasar todas las pruebas (`backend`: `test:catalogo`, `test:catalogoRutas`, `catalogoSubir.mjs`, `test:clip`, `test:foto`, `check`; app: suite completa) y mostrar el resultado.
- [ ] **Step 2:** Preguntar y, si dice que sí, subir con `node backend/scripts/subir-catalogo.mjs "<salida>" --remote` (≈ 165 MB, ≈ 16.900 objetos; reanudable).
- [ ] **Step 3:** Preguntar y, si dice que sí, `cd backend && npx wrangler deploy` (rutas `/catalogo/*`).
- [ ] **Step 4:** Comprobar sin escribir: `GET /catalogo/version.json` sin sesión → 401; instalar `installRapido` y, con el nutriólogo de prueba, abrir «Asignar ejercicio» y ver el catálogo con fotos y clips.
