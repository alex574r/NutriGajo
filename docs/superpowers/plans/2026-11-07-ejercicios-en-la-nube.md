# Ejercicios en la nube (fases 1 y 2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Que un entrenador suba un clip animado por ejercicio (guardado en R2) y que el nutriólogo asigne ejercicios del banco global a su paciente, quien los ve y los entrena.

**Architecture:** El clip es un MP4 corto en R2 detrás del Worker, referenciado por `clipVersion` en `ejercicios/{id}` como la foto. Los ejercicios asignados viajan dentro de `contenido` del plan (`PlanContenido.ejerciciosAsignados`), sin cambio de reglas. La «Rutina de hoy» usa lo asignado ese día y, si no hay nada resoluble, el baraje de siempre.

**Tech Stack:** Kotlin + Compose (app), Media3 ExoPlayer (nuevo), Firebase Firestore, Cloudflare Worker (TypeScript) con R2, reglas de Firestore con pruebas contra emuladores.

**Spec:** `docs/superpowers/specs/2026-11-07-ejercicios-en-la-nube-design.md` (fases 1 y 2). La fase 3 (catálogo de openGym en R2) espera la licencia de Gym visual por escrito y tendrá su propio plan.

## Global Constraints

- Clip: MP4 (`video/mp4`), **≤ 5 s y ≤ 2 MB** (2 × 1024 × 1024 bytes); 5.000 ms y 2 MiB exactos son válidos.
- Asignados: `dia` 1 = lunes … 7 = domingo (`DayOfWeek.value`); **tope 35 por plan y 10 por día**; lo asignado vigente es el del plan más reciente.
- Las sesiones de texto libre (`SesionEjercicio`) se conservan intactas.
- Sin commit, push ni despliegue (Worker, reglas, R2) sin el OK explícito del usuario cada vez; los pasos de commit quedan fuera de este plan salvo que lo pida (mensaje terminado en `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`). El árbol tiene cambios sin commit de otras tareas: editar solo lo propio. Nunca `pm clear` ni desinstalar en el teléfono (adb `78cb49`; instalar con `installRapido`).
- UI: naranja #F97316, Inter, carga con `Esqueleto` (no spinner), textos en español en `strings.xml`.
- Comandos Gradle en Git Bash: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` y `./gradlew.bat …`. Lógica pura con prueba unitaria al lado.

## Review Focus

- Un ejercicio asignado que luego se retira o no está publicado: el paciente ve el título guardado con «Ya no está disponible» y «Rutina de hoy» lo salta; si no queda ninguno, cae al baraje (Tasks 9 y 12).
- Clip sin duración legible (`MediaMetadataRetriever` da null), de 0 s o con otro tipo: se rechaza con mensaje, no se sube (Tasks 1 y 6).
- Domingo = 7 y el cambio de día: `rutinaAsignada` usa `fecha.dayOfWeek.value` y no mezcla días (Task 9).
- Plan con más de 10 asignados en un día, más de 35 en total o el mismo ejercicio dos veces el mismo día: `limpio()` recorta y deduplica sin perder el orden (Task 8).
- Planes anteriores a esta función (sin `ejerciciosAsignados`) se leen como lista vacía y su PDF no cambia (Tasks 8 y 11).
- Sin red al abrir un ejercicio con clip: se ve la foto, no hay cierre ni pantalla en blanco (Task 5).

---

## File Structure

Fase 1 (clips)
- Modify `app/src/main/java/com/example/nutrigajo/domain/model/Ejercicio.kt`: `clipVersion`, límites y `validarClip`.
- Create `backend/src/routes/clipBanco.ts`; modify `backend/src/index.ts`, `backend/package.json`; test `backend/test/clipBanco.mjs`.
- Modify `firebase/firestore.rules` (`ejercicioValido`) y `firebase/test/ejercicios.mjs`.
- Modify `app/.../data/repository/FotosBanco.kt` y `EjerciciosRepository.kt`.
- Create `app/.../ui/shared/ejercicios/ClipEjercicio.kt` (`ReproductorBucle`, `MediaEjercicio`); modify `gradle/libs.versions.toml`, `app/build.gradle.kts`.
- Modify `EjercicioVistas.kt`, `GuiaEjercicioScreen.kt`, `EntrenarScreen.kt` (usar `MediaEjercicio`), `EditorEjercicio.kt`, `EjerciciosViewModel.kt`, `InicioEntrenadorScreen.kt`.

Fase 2 (asignados)
- Modify `domain/model/Plan.kt`, `data/repository/PlanMapa.kt`, `domain/model/RutinaDelDia.kt`, `domain/model/Ejercicio.kt` (`comoItemDelBanco`).
- Modify `ui/nutriologo/plan/BancoScreen.kt`, `PlanScreen.kt`, `SeccionesPlan.kt`; create `ui/nutriologo/plan/AsignarEjercicioSheet.kt`.
- Modify `data/pdf/GeneradorPlanPdf.kt`.
- Create `ui/paciente/ejercicios/EjerciciosAsignadosScreen.kt`; modify `ui/paciente/inicio/InicioPacienteScreen.kt`, `RutinaYDieta.kt`.

---

## Fase 1: clips

### Task 1: Modelo y límites del clip

**Files:**
- Modify: `app/src/main/java/com/example/nutrigajo/domain/model/Ejercicio.kt`, `app/src/main/java/com/example/nutrigajo/data/repository/EjerciciosRepository.kt` (`aMapa` / `ejercicioDeMapa`)
- Test: `app/src/test/java/com/example/nutrigajo/domain/model/EjercicioTest.kt`, `app/src/test/java/com/example/nutrigajo/data/repository/EjerciciosMapaTest.kt`

**Interfaces:**
- Produces: `Ejercicio.clipVersion: Long? = null`; `Ejercicio.MAX_CLIP_MS = 5_000L`, `Ejercicio.MAX_CLIP_BYTES = 2L * 1024 * 1024`; `enum class ProblemaClip { TIPO, SIN_DURACION, MUY_LARGO, MUY_PESADO }`; `fun validarClip(duracionMs: Long?, bytes: Long, mime: String?): ProblemaClip?` (primer problema en ese orden; null si es válido).

- [ ] **Step 1: Write the failing tests**
  - `EjercicioTest`: `validarClip(5_000, MAX_CLIP_BYTES, "video/mp4") == null`; `validarClip(5_001, 1, "video/mp4") == MUY_LARGO`; `validarClip(3_000, MAX_CLIP_BYTES + 1, "video/mp4") == MUY_PESADO`; `validarClip(null, 1, "video/mp4") == SIN_DURACION`; `validarClip(0, 1, "video/mp4") == SIN_DURACION`; `validarClip(3_000, 1, "video/quicktime") == TIPO`; y `Ejercicio(clipVersion = 7).duplicado().clipVersion == null`.
  - `EjerciciosMapaTest`: `Ejercicio(clipVersion = 9L).aMapa()["clipVersion"] == 9L`; sin clip no hay clave; `ejercicioDeMapa` lee `clipVersion` numérico y lo deja null si falta.
- [ ] **Step 2: Run to verify they fail** — `./gradlew.bat :app:testDebugUnitTest --tests "*EjercicioTest" --tests "*EjerciciosMapaTest"`; Expected: no compila (`clipVersion`, `validarClip` no existen).
- [ ] **Step 3: Implement** the field, constants, enum and `validarClip` in `Ejercicio.kt`; `duplicado()` pone `clipVersion = null`; `aMapa` escribe `clipVersion` solo si no es null; `ejercicioDeMapa` lo lee con `(d["clipVersion"] as? Number)?.toLong()`.
- [ ] **Step 4: Run the same command**; Expected: PASS.

### Task 2: Worker: subir y leer el clip

**Files:**
- Create: `backend/src/routes/clipBanco.ts`
- Modify: `backend/src/index.ts` (junto a `bancoSubida` / `bancoLectura`), `backend/package.json` (script `"test:clip": "node test/clipBanco.mjs"`)
- Test: `backend/test/clipBanco.mjs` (mismo R2 de mentira que `test/fotoBanco.mjs`, ampliado para honrar `range`)

**Interfaces:**
- Produces: `subirClipBanco(req: Request, env: Env, uid: string, id: string): Promise<Response>` → 200 `{ version }`; `leerClipBanco(req: Request, env: Env, autor: string, id: string): Promise<Response>`. Clave R2 `ejercicios/{uid}/{id}/clip-{version}`. Rutas: `PUT /ejercicio/{id}/clip` (con sesión, regex `^/ejercicio/([A-Za-z0-9_-]{1,128})/clip$`) y `GET /ejercicio/{autor}/{id}/clip?v=` (pública, como la foto).

- [ ] **Step 1: Write the failing test** `clipBanco.mjs` con estas aserciones: subir `video/mp4` de 100 bytes → `{ version }` igual a `Date.now()` y queda en `ejercicios/{uid}/{id}/clip-{version}`; tipo `image/jpeg` → 415; `Content-Length` o cuerpo de `2 * 1024 * 1024 + 1` → 413; dos subidas con distinta hora no se pisan; `GET ?v=` → 200 con `Content-Type: video/mp4`, `Cache-Control` que contiene `immutable` y `X-Content-Type-Options: nosniff`; `GET` sin `v` o con `v=abc` → 404 (no hay clip sin versión); `GET` con `Range: bytes=0-9` → 206 con `Content-Range` y 10 bytes.
- [ ] **Step 2: Run** `cd backend && npm run test:clip`; Expected: FAIL (módulo inexistente).
- [ ] **Step 3: Implement** `clipBanco.ts` siguiendo `fotoBanco.ts` (tipo permitido solo `video/mp4`, tope 2 MiB, versión = `Date.now()`); la lectura usa `env.FOTOS.get(clave, { onlyIf, range: req.headers })` y responde 206 cuando el objeto trae `range`. Enrutarlo en `index.ts` (subida exige `verificarSesion`).
- [ ] **Step 4: Run** `npm run test:clip && npm run check`; Expected: PASS y `tsc` sin errores.

### Task 3: Reglas: `clipVersion`

**Files:**
- Modify: `firebase/firestore.rules` (función `ejercicioValido`), `firebase/test/ejercicios.mjs`

**Interfaces:**
- Produces: `ejercicioValido` acepta `clipVersion` opcional entero (se añade a `hasOnly` y `(!('clipVersion' in d.keys()) || d.clipVersion is int)`); publicar sigue pidiendo solo la foto.

- [ ] **Step 1: Write the failing test** en `ejercicios.mjs`: un entrenador verificado crea un ejercicio EN_REVISION con `fotoVersion` y `clipVersion` entero → permitido; con `clipVersion: "x"` → denegado; EN_REVISION con foto y sin clip → permitido.
- [ ] **Step 2: Run** (con `firebase emulators:start --only auth,firestore --project nutrigajo` en otra terminal) `node firebase/test/ejercicios.mjs`; Expected: FAIL en el caso con `clipVersion` entero (campo no permitido).
- [ ] **Step 3: Implement** el cambio en la regla.
- [ ] **Step 4: Run** `node firebase/test/ejercicios.mjs` y `node firebase/test/platillos.mjs`; Expected: todo PASS.

### Task 4: Datos: subir y bajar el clip

**Files:**
- Modify: `app/src/main/java/com/example/nutrigajo/data/repository/FotosBanco.kt`, `EjerciciosRepository.kt`
- Test: `app/src/test/java/com/example/nutrigajo/data/repository/FotosBancoClipTest.kt`

**Interfaces:**
- Consumes: `Ejercicio.clipVersion` (Task 1), rutas del Worker (Task 2).
- Produces: `FotosBanco.subirClip(id: String, mp4: ByteArray, cache: File): Long`; `FotosBanco.bajarClip(autorId: String, id: String, version: Long, cache: File): File?` (null si el Worker no responde 200); `internal fun nombreArchivoClip(autor: String, id: String, version: Long): String` = `"ejercicio-clip-$autor-$id-$version.mp4"`; `EjerciciosRepository.guardar(ejercicio, autorNombre, enviarARevision, foto: ByteArray?, cache: File, clip: ByteArray? = null): Ejercicio` (el parámetro nuevo va al final con valor por defecto, así el llamador actual sigue compilando; sube el clip antes y guarda `clipVersion`); `EjerciciosRepository.clip(ejercicio: Ejercicio, cache: File): File?`.

- [ ] **Step 1: Write the failing test** `FotosBancoClipTest`: `nombreArchivoClip("a","b",3) == "ejercicio-clip-a-b-3.mp4"` y que ese nombre no empieza por el prefijo `ejercicio-a-b-` de las fotos (para que limpiar versiones de la foto no borre el clip).
- [ ] **Step 2: Run** `./gradlew.bat :app:testDebugUnitTest --tests "*FotosBancoClipTest"`; Expected: no compila.
- [ ] **Step 3: Implement** `subirClip` (PUT `video/mp4` con el token, mismo esquema que `subir`), `bajarClip` (GET con `?v=`, guarda con `.part` y renombra, borra versiones anteriores del mismo clip) y los dos métodos del repositorio; `EjerciciosViewModel` empieza a pasar el clip en la Task 6.
- [ ] **Step 4: Run** `FotosBancoClipTest` y luego `./gradlew.bat :app:compileDebugKotlin`; Expected: PASS y BUILD SUCCESSFUL.

### Task 5: Reproductor y `MediaEjercicio`

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts` (dependencias `androidx.media3:media3-exoplayer` y `androidx.media3:media3-ui`, última versión estable que compile con Kotlin 2.2 y AGP 9.3.3)
- Create: `app/src/main/java/com/example/nutrigajo/ui/shared/ejercicios/ClipEjercicio.kt`
- Modify: `app/src/main/res/values/strings.xml` (`ejercicio_toca_pausar` «toca para pausar», `ejercicio_toca_reproducir` «toca para reproducir»)

**Interfaces:**
- Consumes: `EjerciciosRepository.clip` (Task 4), `FotoEjercicio` (existente).
- Produces: `@Composable fun ReproductorBucle(fuente: Uri, modifier: Modifier = Modifier, forma: Shape = RoundedCornerShape(24.dp), pausa: @Composable () -> Unit)` (ExoPlayer sin sonido, `REPEAT_MODE_ONE`, juega solo con la pantalla en `RESUMED`, un toque alterna reproducir/pausar y al pausar muestra `pausa()`, con la píldora de texto abajo a la derecha); `@Composable fun MediaEjercicio(ejercicio: Ejercicio, modifier: Modifier = Modifier, forma: Shape = RoundedCornerShape(24.dp))` (si `clipVersion == null` es `FotoEjercicio`; si no, muestra la foto mientras baja el clip y luego `ReproductorBucle`; si la descarga falla se queda con la foto).

- [ ] **Step 1: Add the dependencies** y comprobar `./gradlew.bat :app:compileDebugKotlin`.
- [ ] **Step 2: Implement** las dos piezas; el `ExoPlayer` se libera en `DisposableEffect`; la descarga del clip usa `produceState` con `runCatching` (sin red: foto, sin excepción).
- [ ] **Step 3: Verify** `./gradlew.bat :app:compileDebugKotlin`; Expected: BUILD SUCCESSFUL. La verificación visual va en la Task 6.

### Task 6: Cableado: detalle, guía, entrenar y editor

**Files:**
- Modify: `ui/shared/ejercicios/EjercicioVistas.kt` (cabecera de `DetalleEjercicio`, hoy `FotoEjercicio` en la línea ~434), `GuiaEjercicioScreen.kt` (~53), `ui/paciente/rutina/EntrenarScreen.kt` (cabeceras ~139 y ~191), `ui/entrenador/ejercicios/EditorEjercicio.kt`, `ui/shared/ejercicios/EjerciciosViewModel.kt`, `ui/entrenador/inicio/InicioEntrenadorScreen.kt`, `strings.xml`
- Test: `app/src/test/java/com/example/nutrigajo/ui/shared/ejercicios/ProblemaClipMensajeTest.kt`

**Interfaces:**
- Consumes: `validarClip`, `ProblemaClip` (Task 1), `MediaEjercicio` y `ReproductorBucle` (Task 5), `guardar(..., clip, ...)` (Task 4).
- Produces: `fun ProblemaClip.mensaje(): Int` (@StringRes, en `EjercicioVistas.kt` junto a `ProblemaEjercicio.mensaje()`); `EditorEjercicio.onGuardar: (ejercicio: Ejercicio, enviarARevision: Boolean, foto: Uri?, clip: Uri?) -> Unit`; `EjerciciosViewModel.guardar(..., foto: Uri?, clip: Uri?, resolver, cache, alTerminar)` (lee los bytes del clip con el `ContentResolver`, ya validados).

- [ ] **Step 1: Write the failing test** `ProblemaClipMensajeTest`: cada valor de `ProblemaClip` devuelve un `@StringRes` distinto de 0 y distinto entre sí (cuatro mensajes: tipo, sin duración, muy largo, muy pesado).
- [ ] **Step 2: Run** `./gradlew.bat :app:testDebugUnitTest --tests "*ProblemaClipMensajeTest"`; Expected: FAIL.
- [ ] **Step 3: Implement** `mensaje()` y sus cuatro textos en español («El clip debe ser MP4», «No se pudo leer la duración del clip», «El clip dura más de 5 s», «El clip pesa más de 2 MB»); en `DetalleEjercicio`, `GuiaEjercicioScreen` y las dos cabeceras de `EntrenarScreen` sustituir `FotoEjercicio` por `MediaEjercicio` (las listas y tarjetas siguen con la foto).
- [ ] **Step 4: Implement the editor**: sección «Animación (opcional)» bajo la foto con un botón que lanza `PickVisualMedia.VideoOnly`; al elegir, en `Dispatchers.IO` leer duración (`MediaMetadataRetriever`), tamaño (`OpenableColumns.SIZE`) y mime (`contentResolver.getType`), correr `validarClip` y, si falla, mostrar `mensaje()` y no aceptar el archivo; si pasa, vista previa con `ReproductorBucle` y botón «Quitar». Pasar `clip` por `onGuardar` y por `EjerciciosViewModel.guardar`.
- [ ] **Step 5: Run** `./gradlew.bat :app:testDebugUnitTest` y `:app:compileDebugKotlin`; Expected: todo PASS.
- [ ] **Step 6: Verify on the phone** (sin desplegar Worker no se puede subir de verdad): `./gradlew.bat installRapido`; ver que el detalle de un ejercicio con foto sigue igual y que el editor muestra «Animación» y rechaza un video largo. Un clip real solo se prueba tras desplegar el Worker (Task 14).

---

## Fase 2: asignados

### Task 7: Fuente `Globales` para ejercicios (`comoItemDelBanco`)

**Files:**
- Modify: `domain/model/Ejercicio.kt`, `ui/nutriologo/plan/BancoScreen.kt`, `strings.xml`
- Test: `app/src/test/java/com/example/nutrigajo/domain/model/EjercicioTest.kt`

**Interfaces:**
- Consumes: `Ejercicio.coincide(busqueda, zona, tipo, nivel)` (existente), `ItemBanco`, `PlanContenido.MAX_MENU = 500`.
- Produces: `fun Ejercicio.textoParaPlan(): String` («Título: paso1; paso2 · N min», recortado con «…» para caber en `MAX_MENU`); `fun Ejercicio.comoItemDelBanco(): ItemBanco` (`tipo = EJERCICIO`, `nombre = titulo.take(ItemBanco.MAX_NOMBRE)`, `texto = textoParaPlan()`); `DestinoBanco` gana `onElegirEjercicio: ((Ejercicio) -> Unit)? = null`.

- [ ] **Step 1: Write the failing test**: `textoParaPlan()` de un ejercicio con 3 pasos largos mide `<= PlanContenido.MAX_MENU` y conserva el sufijo « · N min»; con pasos cortos contiene el título al inicio; `comoItemDelBanco().tipo == TipoBanco.EJERCICIO`.
- [ ] **Step 2: Run** `--tests "*EjercicioTest"`; Expected: FAIL.
- [ ] **Step 3: Implement** las dos funciones (mismo criterio de recorte que `Platillo.textoParaPlan`).
- [ ] **Step 4: Implement the tab**: cuando `destino.tipo == EJERCICIO` y hay `onElegir` o `onElegirEjercicio`, `BancoScreen` muestra `SelectorPestanas` «Mis ejercicios» | «Globales»; «Globales» usa un `EjerciciosViewModel(key = "banco-globales-ejercicios")` con `cargarPublicados()`, un campo de búsqueda y chips de zona, tipo y nivel (los mismos filtros de Explorar) y `FilaEjercicio` por resultado; con `onElegirEjercicio` solo existe «Globales» y tocar una fila lo entrega; con solo `onElegir` entrega `comoItemDelBanco()`. Estados: `Esqueleto` mientras carga y texto «Aún no hay ejercicios publicados» si está vacío.
- [ ] **Step 5: Run** `:app:testDebugUnitTest` y `:app:compileDebugKotlin`; Expected: PASS.

### Task 8: Modelo y almacenamiento de los asignados

**Files:**
- Modify: `domain/model/Plan.kt`, `data/repository/PlanMapa.kt`
- Test: `app/src/test/java/com/example/nutrigajo/domain/model/EjercicioAsignadoTest.kt`, `app/src/test/java/com/example/nutrigajo/data/repository/PlanMapaTest.kt`

**Interfaces:**
- Produces: `enum class FuenteEjercicio { BANCO, CATALOGO }`; `data class EjercicioAsignado(val dia: Int, val fuente: FuenteEjercicio = FuenteEjercicio.BANCO, val id: String, val titulo: String, val series: Int? = null, val repeticiones: Int? = null, val minutos: Int? = null, val nota: String = "")` con `MAX_SERIES = 20`, `MAX_REPETICIONES = 100`, `MAX_MINUTOS = 180`, `MAX_NOTA = 200`; `fun EjercicioAsignado.valido(): Boolean`; `fun EjercicioAsignado.duracionMin(): Int` (= `minutos` o, sin él, `(series ?: 1) * 2`, mínimo 1); `PlanContenido.ejerciciosAsignados: List<EjercicioAsignado> = emptyList()`; `PlanContenido.MAX_ASIGNADOS = 35`, `MAX_ASIGNADOS_DIA = 10`; `fun List<EjercicioAsignado>.delDia(dia: Int): List<EjercicioAsignado>`.

- [ ] **Step 1: Write the failing tests**
  - `EjercicioAsignadoTest`: `valido()` falla con `dia = 0` y `dia = 8`, con `id` o `titulo` en blanco, con `series = 21`, con `minutos = 181`; `duracionMin()` da `minutos` si existe, `2 * series` si no, `2` si no hay ninguno y nunca menos de 1.
  - `limpio()` de un plan con 12 asignados el mismo `dia` deja 10; con 40 repartidos deja 35; el mismo `(dia, fuente, id)` dos veces deja uno, el primero; recorta `nota`; conserva el orden; un plan sin asignados sigue `vacio` si todo lo demás está vacío, y con uno ya no está `vacio`.
  - `PlanMapaTest`: ida y vuelta de `ejerciciosAsignados` por `aMapa()` / `aPlanContenido()` (con `series`/`minutos` nulos); un mapa sin la clave da lista vacía; un elemento con `fuente` desconocida (`"X"`) o sin `id` se descarta sin romper el resto.
- [ ] **Step 2: Run** `--tests "*EjercicioAsignadoTest" --tests "*PlanMapaTest"`; Expected: no compila.
- [ ] **Step 3: Implement** los tipos y reglas en `Plan.kt` (`limpio()` aplica `valido()`, deduplica, aplica los topes en ese orden y recorta la nota; `vacio` incluye la lista) y la (de)serialización en `PlanMapa.kt` (clave `ejerciciosAsignados`, lista de mapas con `dia`, `fuente` como nombre, `id`, `titulo`, `series`, `repeticiones`, `minutos`, `nota`; lo nulo no se escribe).
- [ ] **Step 4: Run** las mismas pruebas; Expected: PASS.

### Task 9: `rutinaAsignada`

**Files:**
- Modify: `domain/model/RutinaDelDia.kt`
- Test: `app/src/test/java/com/example/nutrigajo/domain/model/RutinaDelDiaTest.kt`

**Interfaces:**
- Consumes: `EjercicioAsignado`, `duracionMin()` (Task 8).
- Produces: `RutinaDelDia` gana `val asignada: Boolean = false`; `fun rutinaAsignada(fecha: LocalDate, asignados: List<EjercicioAsignado>, resolver: (EjercicioAsignado) -> Ejercicio?): RutinaDelDia?` — toma los de `fecha.dayOfWeek.value` en el orden del plan, resuelve cada uno (los null se saltan), cada ejercicio sale con `duracionMin = asignado.duracionMin()` y `kcal` escaladas (`kcal * nueva / original`, sin dividir entre 0), `enfoque = EnfoqueDia.de(fecha.dayOfWeek)`, `asignada = true`; `null` si no queda ninguno.

- [ ] **Step 1: Write the failing tests** (con los helpers `ej(...)` y `lunes` ya en el archivo): un asignado el lunes devuelve rutina con `asignada == true` en lunes y `null` el martes; el domingo (`LocalDate.of(2026, 11, 8)`) usa `dia = 7`; un asignado cuyo `resolver` da null se salta y si solo había ese devuelve `null`; el orden del plan se conserva; con `minutos = 20` sobre un ejercicio de 10 min y 50 kcal la rutina trae `duracionMin = 20` y `kcal = 100`; `rutinaDelDia(...)` sigue devolviendo `asignada == false`.
- [ ] **Step 2: Run** `--tests "*RutinaDelDiaTest"`; Expected: FAIL.
- [ ] **Step 3: Implement** en `RutinaDelDia.kt`.
- [ ] **Step 4: Run** la misma prueba; Expected: PASS.

### Task 10: Editor del plan: sección «Ejercicios asignados»

**Files:**
- Create: `app/src/main/java/com/example/nutrigajo/ui/nutriologo/plan/AsignarEjercicioSheet.kt`
- Modify: `ui/nutriologo/plan/PlanScreen.kt` (rama `SeccionPlan.EJERCICIO`), `SeccionesPlan.kt` (`resumen`), `strings.xml` (+ plural `plan_resumen_asignados`)
- Test: `app/src/test/java/com/example/nutrigajo/ui/nutriologo/plan/AsignarEjercicioTest.kt`

**Interfaces:**
- Consumes: `DestinoBanco(onElegirEjercicio)` (Task 7), `EjercicioAsignado` y topes (Task 8), `PlanViewModel.editar`.
- Produces: `internal fun EjercicioAsignado.Companion.desde(ejercicio: Ejercicio, dia: Int, series: String, repeticiones: String, minutos: String, nota: String): EjercicioAsignado?` (parsea los campos de texto; `null` si no pasa `valido()`; `titulo = ejercicio.titulo`, `fuente = BANCO`); `internal fun PlanContenido.puedeAsignar(dia: Int): Boolean` (hay lugar en el día y en el total); `AsignarEjercicioSheet(ejercicio: Ejercicio, dias: List<Int>, onAsignar: (List<EjercicioAsignado>) -> Unit, onCerrar: () -> Unit)`.

- [ ] **Step 1: Write the failing tests**: `desde(ej, 1, "3", "10", "", "")` da series 3 y repeticiones 10; `desde(ej, 1, "", "", "15", "")` da `minutos = 15`; `desde(ej, 1, "0", "", "", "")` y `desde(ej, 9, "3", "", "", "")` dan null; `PlanContenido(...).puedeAsignar(1)` es false con 10 en el día 1 y true en el día 2; es false con 35 en total.
- [ ] **Step 2: Run** `--tests "*AsignarEjercicioTest"`; Expected: no compila.
- [ ] **Step 3: Implement** las funciones puras y la hoja: series y repeticiones o minutos (campos numéricos), nota opcional y siete chips de día (lunes … domingo; elegir uno o varios; deshabilitar los días sin lugar).
- [ ] **Step 4: Wire the section**: bajo las sesiones de texto, «Ejercicios asignados» agrupados por día (cada fila con título, «3 × 10» o «15 min», editar y quitar) y el botón «Asignar ejercicio» (deshabilitado con 35) que abre `BancoScreen` con `DestinoBanco(EJERCICIO, onElegirEjercicio = …)` y, al elegir, la hoja; el resultado se agrega con `editar { it.copy(ejerciciosAsignados = it.ejerciciosAsignados + nuevos) }`. `resumen(SeccionPlan.EJERCICIO, plan)` cuenta sesiones y asignados.
- [ ] **Step 5: Run** `:app:testDebugUnitTest` y `:app:compileDebugKotlin`; Expected: PASS.

### Task 11: PDF del plan

**Files:**
- Modify: `app/src/main/java/com/example/nutrigajo/data/pdf/GeneradorPlanPdf.kt` (condición ~81 y función `ejercicio(lienzo, plan)`)
- Test: `app/src/test/java/com/example/nutrigajo/data/pdf/FilasAsignadosTest.kt`

**Interfaces:**
- Consumes: `EjercicioAsignado` (Task 8).
- Produces: `internal fun filasAsignadas(asignados: List<EjercicioAsignado>, nombreDia: (Int) -> String): List<List<String>>` — filas `[día, ejercicio, series×repeticiones o minutos, nota]` ordenadas por día (lunes primero) y, dentro del día, en el orden del plan.

- [ ] **Step 1: Write the failing test**: dos asignados del día 3 y uno del 1 salen con el del 1 primero; `series = 3, repeticiones = 10` da «3 × 10»; solo `minutos = 15` da «15 min»; nada de lo anterior da «—»; una lista vacía da lista vacía.
- [ ] **Step 2: Run** `--tests "*FilasAsignadosTest"`; Expected: FAIL.
- [ ] **Step 3: Implement** `filasAsignadas` y usarla en `ejercicio(lienzo, plan)` con una tabla «Ejercicios asignados» tras las sesiones; la página de ejercicio también se agrega cuando `plan.ejerciciosAsignados.isNotEmpty()`. Un plan sin asignados no cambia.
- [ ] **Step 4: Run** `:app:testDebugUnitTest`; Expected: PASS.

### Task 12: Paciente: «Rutina de hoy» y «Mis ejercicios asignados»

**Files:**
- Create: `app/src/main/java/com/example/nutrigajo/ui/paciente/ejercicios/EjerciciosAsignadosScreen.kt`
- Modify: `ui/paciente/inicio/InicioPacienteScreen.kt` (donde hoy se calcula `rutina`, ~línea 231), `ui/paciente/inicio/RutinaYDieta.kt` (etiqueta en `RutinaDeHoy`), `strings.xml`
- Test: `app/src/test/java/com/example/nutrigajo/domain/model/AsignadosDelPacienteTest.kt`

**Interfaces:**
- Consumes: `rutinaAsignada` (Task 9), `FilaEjercicio`, `DetalleEjercicio`, `GuiaEjercicioScreen`, `MediaEjercicio` (existentes/Task 5).
- Produces: `fun resolverDelBanco(banco: List<Ejercicio>): (EjercicioAsignado) -> Ejercicio?` (solo `FuenteEjercicio.BANCO` y solo `PUBLICADO`; el resto null); `EjerciciosAsignadosScreen(asignados: List<EjercicioAsignado>, banco: List<Ejercicio>, cargando: Boolean, onVolver: () -> Unit)` (por día, lunes a domingo; cada fila abre el detalle; si no se resuelve, la fila muestra el título y «Ya no está disponible» sin abrir nada).

- [ ] **Step 1: Write the failing test** `AsignadosDelPacienteTest`: `resolverDelBanco(banco)` devuelve el ejercicio publicado con ese `id`; devuelve null para uno retirado, para uno inexistente y para `fuente = CATALOGO`; con un banco donde solo está retirado el asignado, `rutinaAsignada(hoy, asignados, resolver) == null` (para que el inicio caiga al baraje).
- [ ] **Step 2: Run** `--tests "*AsignadosDelPacienteTest"`; Expected: FAIL.
- [ ] **Step 3: Implement** `resolverDelBanco` (en `RutinaDelDia.kt`) y la pantalla.
- [ ] **Step 4: Wire the home**: `asignados = plan.planes.firstOrNull()?.contenido?.ejerciciosAsignados.orEmpty()`; `rutina = rutinaAsignada(LocalDate.now(), asignados, resolverDelBanco(ejercicios.items)) ?: rutinaDelDia(LocalDate.now(), ejercicios.items)`; `RutinaDeHoy` muestra «Asignada por tu nutriólogo» cuando `rutina.asignada`; con asignados, una `FilaAjuste` «Mis ejercicios asignados» (estado `enAsignados` guardado con `rememberSaveable`) abre la pantalla.
- [ ] **Step 5: Run** `:app:testDebugUnitTest` y `:app:compileDebugKotlin`; Expected: PASS.

### Task 13: Verificación en el teléfono

- [ ] **Step 1:** `./gradlew.bat :app:testDebugUnitTest installRapido` (sin cerrar sesión).
- [ ] **Step 2:** Con el nutriólogo de prueba (emuladores o una cuenta de prueba): asignar dos ejercicios de hoy en el plan, enviar el plan y comprobar en el paciente «Rutina de hoy» con la etiqueta, «Mis ejercicios asignados» y el PDF con la tabla. Anotar lo que no se pudo ver.

### Task 14: Publicación (solo con el OK del usuario, cada paso por separado)

- [ ] **Step 1:** Correr `node firebase/test/ejercicios.mjs` y `npm run test:clip` y mostrar el resultado.
- [ ] **Step 2:** Preguntar al usuario y, si dice que sí, `cd backend && npx wrangler deploy` (rutas del clip).
- [ ] **Step 3:** Preguntar al usuario y, si dice que sí, `firebase deploy --only firestore:rules --project nutrigajo`.
- [ ] **Step 4:** Probar un clip real de un entrenador: crear un ejercicio con foto y clip, mandarlo a revisión, publicarlo con el admin y verlo en Explorar.
