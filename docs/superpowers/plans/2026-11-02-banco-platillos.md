# Banco global de platillos — plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Un nutriólogo o entrenador verificado crea platillos con foto y macros, el admin los publica, y el paciente los explora desde el inicio; el nutriólogo los elige al armar un plan.

**Architecture:** Se calca el módulo de ejercicios (`Ejercicio`, `EjerciciosRepository`, `EjerciciosViewModel`, reglas `ejercicios`, ruta de fotos del Worker, revisión del admin): un modelo `Platillo`, su colección `platillos/{id}` y las mismas piezas de pantalla. Lo que se repetiría palabra por palabra (subida y caché de fotos) se extrae a una clase compartida y la ruta del Worker se parametriza por carpeta.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Firebase Auth y Firestore, Cloudflare Worker + R2 (TypeScript), pruebas JUnit y pruebas de reglas contra los emuladores (`firebase/test/*.mjs`).

**Spec:** `docs/superpowers/specs/2026-11-02-banco-platillos-design.md` (aprobado por el usuario el 2 nov 2026).

## Global Constraints

- Todo el texto visible va en español y en `app/src/main/res/values/strings.xml`, con el prefijo `platillo_` (y `admin_`, `banco_` donde la pantalla ya lo usa). Ninguna cadena escrita en el código.
- No renombrar valores de enums: Firestore guarda el nombre como texto.
- Topes de un platillo (los mismos en `Platillo.kt` y en las reglas): nombre 3–80; resumen ≤ 300 (≥ 10 para revisión); ingredientes ≤ 20 de ≤ 80; pasos ≤ 12 de ≤ 300; proteína, grasas y carbos 0–500 g; kcal 0–3000; minutos 1–240; etiquetas ≤ 4; motivo de rechazo 1–200.
- Estados: `BORRADOR → EN_REVISION → PUBLICADO | RECHAZADO → RETIRADO`; solo el ADMIN publica, rechaza o retira lo ajeno; lo publicado es de solo lectura.
- kcal sugeridas = 4·proteína + 4·carbos + 9·grasas; se avisa si difieren más de 15 %.
- Foto: JPEG de hasta 1280 px, ≤ 3 MiB, en R2 bajo `platillos/{uid del autor}/{id}`.
- Momento: `MomentoComida` (desayuno, comida, cena). El rol del autor es `Rol.NUTRIOLOGO` o `Rol.ENTRENADOR`.
- Publicar reglas de Firestore y desplegar el Worker solo con el visto bueno explícito del usuario, cada vez. No crear cuentas ni datos en producción, no usar `pm clear` y no tocar la sesión real del teléfono (ver memoria `probar-en-telefono-sin-perder-sesion`).
- Commits locales, sin push, con el pie `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`. El proyecto solo confirma cuando el usuario lo pide: el paso «Commit» de cada tarea se hace solo si ya lo pidió; si no, queda en el árbol de trabajo.
- Gradle necesita `JAVA_HOME` = `C:\Program Files\Android\Android Studio\jbr`. Comandos: `.\gradlew.bat :app:testDebugUnitTest` y `.\gradlew.bat :app:assembleDebug`.
- Los comentarios del código se escriben en español y en el tono de los archivos vecinos.

## Review Focus

Lo que el spec implica y ninguna tarea de pantalla ejercita; cada línea tiene su prueba en la tarea que dice:

1. Una alergia escrita con otra mayúscula o sin acento («Lácteos» / «lacteos», «Cacahuate» / «CACAHUATE») debe esconder el platillo — Tarea 1.
2. Elegir un platillo largo para un plan no debe pasar de los 500 caracteres de un menú — Tarea 1.
3. Un autor no puede crear un platillo con el `autorRol` de otro rol — Tarea 2.
4. Reenviar a revisión un platillo rechazado no debe borrar quién lo rechazó ni cuándo — Tarea 2.
5. Con cero platillos publicados (cuenta nueva o sin red) el inicio del paciente muestra el estado vacío con su atajo, no una pantalla en blanco ni los platillos de muestra — Tarea 8.

---

### Tarea 1: Modelo `Platillo` y sus reglas de negocio

**Files:**
- Create: `app/src/main/java/com/example/nutrigajo/domain/model/Platillo.kt`
- Test: `app/src/test/java/com/example/nutrigajo/domain/model/PlatilloTest.kt`

**Interfaces:**
- Consumes: `Rol`, `MomentoComida`, `Dieta`, `PlanContenido.MAX_MENU` (500) de `domain.model`.
- Produces (todo en `com.example.nutrigajo.domain.model`):
  - `enum class EtiquetaPlatillo { SIN_GLUTEN, SIN_LACTOSA, VEGETARIANO, VEGANO, ALTO_PROTEINA, BAJO_CARBOHIDRATOS }`
  - `enum class EstadoPlatillo { BORRADOR, EN_REVISION, PUBLICADO, RECHAZADO, RETIRADO }`
  - `data class Platillo(id: String = "", autorId: String = "", autorNombre: String = "", autorRol: Rol = Rol.NUTRIOLOGO, nombre: String = "", resumen: String = "", momento: MomentoComida = MomentoComida.COMIDA, proteinaG: Int = 0, grasasG: Int = 0, carbosG: Int = 0, kcal: Int = 0, minutos: Int = 15, ingredientes: List<String> = emptyList(), pasos: List<String> = emptyList(), etiquetas: List<EtiquetaPlatillo> = emptyList(), fotoVersion: Long? = null, estado: EstadoPlatillo = EstadoPlatillo.BORRADOR, motivoRechazo: String? = null, creadoEn: Long = 0, actualizadoEn: Long = 0, publicadoEn: Long? = null)` con un `companion` con los topes de «Global Constraints» (`MIN_NOMBRE`, `MAX_NOMBRE`, `MIN_RESUMEN`, `MAX_RESUMEN`, `MAX_INGREDIENTES`, `MAX_INGREDIENTE`, `MAX_PASOS`, `MAX_PASO`, `MAX_MACRO_G`, `MAX_KCAL`, `MIN_MINUTOS`, `MAX_MINUTOS`, `MAX_ETIQUETAS`, `MAX_MOTIVO`).
  - `enum class ProblemaPlatillo { NOMBRE, RESUMEN, INGREDIENTES, PASOS, MACROS, KCAL, MINUTOS, ETIQUETAS, FOTO }`
  - `fun Platillo.problemaParaRevision(): ProblemaPlatillo?` (el primero que impide mandarlo a revisión, en el orden del enum; null si está listo)
  - `fun Platillo.puedeGuardarseComoBorrador(): Boolean` (solo pide el nombre)
  - `fun Platillo.limpio(): Platillo` (recorta textos y quita ingredientes, pasos y etiquetas vacíos o repetidos)
  - `fun Platillo.duplicado(): Platillo` (borrador sin id, foto, motivo ni fechas)
  - `fun kcalSugeridas(proteinaG: Int, grasasG: Int, carbosG: Int): Int`
  - `fun Platillo.kcalNoCuadran(): Boolean` (false si los tres macros son 0; true si `kcal` difiere de las sugeridas en más del 15 % de las sugeridas)
  - `fun Platillo.coincide(busqueda: String, momento: MomentoComida?, etiqueta: EtiquetaPlatillo?): Boolean` (texto en nombre, resumen o ingredientes, sin distinguir mayúsculas)
  - `fun Platillo.textoParaPlan(): String` («Nombre: ingredientes separados por coma · N kcal», cortado con «…» a `PlanContenido.MAX_MENU`)
  - `fun List<Platillo>.paraPaciente(alergias: List<String>, dieta: Dieta?): List<Platillo>`

- [ ] **Step 1: Escribir las pruebas que fallan** en `PlatilloTest.kt`, con estas aserciones:

```kotlin
@Test fun alergia_con_acentos_y_mayusculas_esconde_el_platillo() {
    val p = Platillo(nombre = "Ensalada", ingredientes = listOf("Queso de Lácteos de cabra", "lechuga"))
    assertTrue(listOf(p).paraPaciente(listOf("lacteos"), null).isEmpty())
    assertTrue(listOf(Platillo(nombre = "Mole", ingredientes = listOf("CACAHUATE"))).paraPaciente(listOf("Cacahuate"), null).isEmpty())
    assertEquals(1, listOf(p).paraPaciente(listOf("", "  "), null).size) // alergias en blanco no esconden nada
}
@Test fun la_dieta_de_plantas_pone_primero_lo_vegetariano_y_no_quita_nada() { /* 3 platillos; con Dieta.PLANTAS el VEGANO o VEGETARIANO va primero y salen los 3 */ }
@Test fun el_texto_para_el_plan_cabe_en_el_tope_del_menu() {
    val largo = Platillo(nombre = "Plato", kcal = 400, ingredientes = List(20) { "ingrediente número $it con nombre largo" })
    assertTrue(largo.textoParaPlan().length <= PlanContenido.MAX_MENU)
    assertTrue(largo.textoParaPlan().endsWith("…"))
    assertTrue(Platillo(nombre = "Té", kcal = 5, ingredientes = listOf("hierbabuena")).textoParaPlan().startsWith("Té: hierbabuena"))
}
@Test fun las_kcal_se_comparan_con_4_4_9_y_un_15_por_ciento() {
    assertEquals(4 * 30 + 4 * 40 + 9 * 10, kcalSugeridas(30, 10, 40))
    assertFalse(Platillo(proteinaG = 30, grasasG = 10, carbosG = 40, kcal = 380).kcalNoCuadran())
    assertTrue(Platillo(proteinaG = 30, grasasG = 10, carbosG = 40, kcal = 600).kcalNoCuadran())
    assertFalse("sin macros no se avisa", Platillo(kcal = 5).kcalNoCuadran())
}
@Test fun para_revision_piden_resumen_ingredientes_pasos_y_foto_en_ese_orden() { /* un Platillo válido devuelve null; quitar la foto → FOTO; macros > 500 → MACROS; kcal > 3000 → KCAL */ }
@Test fun limpio_y_duplicado() { /* limpio recorta y quita vacíos y etiquetas repetidas; duplicado deja BORRADOR, id "", fotoVersion null */ }
@Test fun coincide_busca_en_nombre_resumen_e_ingredientes_y_filtra_por_momento_y_etiqueta() { /* … */ }
```

- [ ] **Step 2: Ejecutar** `.\gradlew.bat :app:testDebugUnitTest --tests "*PlatilloTest"` — Esperado: falla por símbolos sin definir.
- [ ] **Step 3: Implementar** las firmas de «Interfaces» en `Platillo.kt`. Para `paraPaciente` y `coincide`, normalizar con `java.text.Normalizer` (NFD, quitar marcas, minúsculas) tanto el término como el texto; `Dieta.PLANTAS` prefiere `VEGETARIANO`/`VEGANO` y `Dieta.BAJA_CARBOHIDRATOS` prefiere `BAJO_CARBOHIDRATOS`, con orden estable.
- [ ] **Step 4: Ejecutar** el mismo comando — Esperado: todas pasan.
- [ ] **Step 5: Commit** — `feat(platillos): modelo Platillo con sus reglas de negocio`.

---

### Tarea 2: Reglas de Firestore y su prueba

**Files:**
- Modify: `firebase/firestore.rules` (funciones junto a `entrenadorVerificado` y `ejercicioValido`; el `match` junto al de `ejercicios`)
- Create: `firebase/test/platillos.mjs` (mismos ayudantes `cuenta`, `commit`, `leer`, `doc` que `firebase/test/ejercicios.mjs`)

**Interfaces:**
- Consumes: `rolDe(uid)`, `esAdmin()` de las reglas; los topes de «Global Constraints».
- Produces: colección `platillos/{platilloId}` con los campos `autorId, autorNombre, autorRol, nombre, resumen, momento, proteinaG, grasasG, carbosG, kcal, minutos, ingredientes, pasos, etiquetas, fotoVersion, estado, motivoRechazo, creadoEn, actualizadoEn, publicadoEn, revisadoPor, revisadoEn`; `function autorVerificado(uid)` y `function platilloValido(d)`.

- [ ] **Step 1: Escribir `platillos.mjs`** con estos casos (cada uno es un `assert.equal(await …, código, "texto")`):
  - crea en revisión: nutriólogo verificado → 200; entrenador verificado → 200; entrenador sin verificar → 403; paciente → 403; admin → 403.
  - `autorRol` distinto del rol real del autor (entrenador con `autorRol: "NUTRIOLOGO"`) → 403.
  - borrador a medias (sin resumen, ingredientes, pasos ni foto) → 200; el mismo en `EN_REVISION` → 403; en `EN_REVISION` sin `fotoVersion` → 403.
  - fuera de rango: `proteinaG: 501`, `kcal: 3001`, `minutos: 0`, `momento: "COLACION"`, `etiquetas` con 5, `ingredientes` con 21, `pasos` con 13 → 403 cada uno.
  - no nace `PUBLICADO` ni `RECHAZADO`, ni con `publicadoEn` o `revisadoPor`.
  - el admin publica uno en revisión (200) y no puede publicar un borrador (403); el autor no se publica solo (403).
  - el admin rechaza con motivo (200); sin motivo o con más de 200 caracteres (403).
  - **reenvío tras rechazo**: el autor edita el rechazado y lo manda a `EN_REVISION` con `update` (200) y `revisadoPor` y `revisadoEn` conservan el valor que puso el admin (leer el documento y comprobar); si el reenvío los cambia → 403.
  - el autor retira uno publicado (200); otro autor no (403); lo publicado no se borra (403) y un borrador sí (200).
  - lectura: cualquier sesión lee uno `PUBLICADO`; un borrador lo leen su autor y el admin, no otro entrenador ni un paciente (403).
- [ ] **Step 2: Ejecutar** con los emuladores (`firebase emulators:start --only auth,firestore --project nutrigajo` desde una carpeta cuyo `firebase.json` apunte a `firebase/firestore.rules`) y `node firebase/test/platillos.mjs` — Esperado: falla (no hay reglas de `platillos`; todo devuelve 403 y los casos de 200 rompen).
- [ ] **Step 3: Implementar** `autorVerificado(uid)` (rol `NUTRIOLOGO` o `ENTRENADOR` y `verificado == true`), `platilloValido(d)` y el `match /platillos/{platilloId}` con las mismas ramas de lectura, creación, edición del autor, retiro y saca-de-revisión, y las tres del admin que `ejercicios`. Añadir en la creación `request.resource.data.autorRol == rolDe(request.auth.uid)`. En la edición del autor exigir `autorRol` sin cambio. Para `EN_REVISION`: resumen ≥ 10, `ingredientes.size() ≥ 1`, `pasos.size() ≥ 1` y `fotoVersion` presente.
- [ ] **Step 4: Ejecutar** `platillos.mjs`, `ejercicios.mjs`, `carreras.mjs` y `entrenamientos.mjs` — Esperado: «todas las pruebas pasaron» en las cuatro.
- [ ] **Step 5: Commit** — `feat(platillos): reglas de Firestore del banco global de platillos`. **No publicar las reglas** (eso es la Tarea 10, con permiso).

---

### Tarea 3: Ruta de fotos del Worker, parametrizada por carpeta

**Files:**
- Modify: `backend/src/routes/ejercicio.ts` (pasa a servir a las dos carpetas)
- Modify: `backend/src/index.ts:33-42` (rutas `/platillo/…` junto a las de `/ejercicio/…`)
- Modify: `backend/test/humo.mjs:17-21`

**Interfaces:**
- Produces: `subirFotoBanco(req: Request, env: Env, uid: string, id: string, carpeta: "ejercicios" | "platillos"): Promise<Response>` y `leerFotoBanco(req: Request, env: Env, autor: string, id: string, carpeta: "ejercicios" | "platillos"): Promise<Response>` (el mismo comportamiento de hoy; la llave de R2 es `${carpeta}/${uid}/${id}`). Rutas HTTP: `PUT /platillo/{id}` (con sesión) y `GET /platillo/{autor}/{id}` (pública), con las mismas expresiones regulares de `/ejercicio/…`.

- [ ] **Step 1: Escribir las pruebas de humo** que fallan, copiando las cinco líneas de `ejercicio` de `humo.mjs` con `/platillo/` y el prefijo «platillo:» en el texto: subir sin sesión → 401; con token falso → 401; leer una foto que no existe → 404; leer sin el autor → 404; subir con la ruta de lectura → 404.
- [ ] **Step 2: Ejecutar** `node backend/test/humo.mjs` contra `wrangler dev` (`npx wrangler dev` en `backend/`) — Esperado: las de `/platillo` devuelven 404 donde se espera 401.
- [ ] **Step 3: Implementar** renombrando las dos funciones de `ejercicio.ts` a `subirFotoBanco`/`leerFotoBanco` con el parámetro `carpeta` (y el archivo a `fotoBanco.ts`), y en `index.ts` registrar las cuatro rutas (las de ejercicio con `"ejercicios"`, las de platillo con `"platillos"`).
- [ ] **Step 4: Ejecutar** `humo.mjs` — Esperado: pasan las de `ejercicio` y las de `platillo`.
- [ ] **Step 5: Commit** — `feat(worker): fotos de platillos en R2`. **No desplegar** (Tarea 10).

---

### Tarea 4: Capa de datos — fotos compartidas, repositorio y ViewModel

**Files:**
- Create: `app/src/main/java/com/example/nutrigajo/data/repository/FotosBanco.kt`
- Create: `app/src/main/java/com/example/nutrigajo/data/repository/PlatillosRepository.kt`
- Create: `app/src/main/java/com/example/nutrigajo/ui/shared/platillos/PlatillosViewModel.kt`
- Modify: `app/src/main/java/com/example/nutrigajo/data/repository/EjerciciosRepository.kt` (usa `FotosBanco`; quita su `subirFoto`, `foto`, `archivoDe` y `guardarEnCache`)
- Modify: `app/src/main/java/com/example/nutrigajo/data/remote/firebase/firestore/FirestorePaths.kt` (`const val PLATILLOS = "platillos"` y la línea del árbol)
- Test: `app/src/test/java/com/example/nutrigajo/data/repository/PlatillosMapaTest.kt` (al estilo de `EjerciciosMapaTest.kt`)

**Interfaces:**
- Consumes: `Platillo`, `EstadoPlatillo`, `limpio()` (Tarea 1); `mensajeDeFirestore`, `FotoPerfilRepository.preparar` (existentes).
- Produces:
  - `class FotosBanco(auth: FirebaseAuth = FirebaseAuth.getInstance(), base: String = BuildConfig.WORKER_URL)` con `suspend fun subir(ruta: String, id: String, jpeg: ByteArray, cache: File): Long` y `suspend fun bajar(ruta: String, autorId: String, id: String, version: Long, cache: File): Bitmap?`. `ruta` es `"ejercicio"` o `"platillo"`: es el segmento de la URL del Worker y el prefijo de los archivos de caché (`ejercicio-{autor}-{id}-{version}.jpg`, como hoy).
  - `class PlatillosRepository(auth, db, fotos: FotosBanco)` con `mios()`, `publicados()`, `enRevision()`, `guardar(platillo: Platillo, autorNombre: String, autorRol: Rol, enviarARevision: Boolean, foto: ByteArray?, cache: File): Platillo`, `retirar(id)`, `sacarDeRevision(id)`, `eliminar(id)`, `publicar(id)`, `rechazar(id: String, motivo: String)`, `foto(platillo: Platillo, cache: File): Bitmap?`; y `internal fun Platillo.aMapa(): Map<String, Any>` y `internal fun platilloDeMapa(id: String, d: Map<String, Any?>): Platillo?`.
  - `data class PlatillosEstado(cargando = true, guardando = false, trabajando: String? = null, items: List<Platillo> = emptyList(), @StringRes error: Int? = null)` y `class PlatillosViewModel(repo = PlatillosRepository(), fotos = FotoPerfilRepository())` con `estado`, `cargarMios()`, `cargarPublicados()`, `cargarEnRevision()`, `guardar(platillo, autorNombre, autorRol, enviarARevision, foto: Uri?, resolver, cache, alTerminar: (Boolean) -> Unit)`, `retirar`, `sacarDeRevision`, `eliminar`, `publicar`, `rechazar(platillo, motivo, alTerminar)` y `limpiarError()`, con el mismo comportamiento que `EjerciciosViewModel`.

- [ ] **Step 1: Escribir `PlatillosMapaTest`** que falla: `aMapa()` de un platillo completo y `platilloDeMapa` devuelven el mismo platillo (ida y vuelta, con `autorRol`, `momento` y `etiquetas` como texto); `platilloDeMapa` devuelve null con un `estado` desconocido o sin `autorId`; con un `momento` o una etiqueta desconocidos usa el valor por defecto y descarta la etiqueta; `aMapa()` no incluye `fotoVersion` si es null ni los campos del ADMIN.
- [ ] **Step 2: Ejecutar** `.\gradlew.bat :app:testDebugUnitTest --tests "*PlatillosMapaTest"` — Esperado: falla.
- [ ] **Step 3: Implementar** `FotosBanco` moviendo (sin cambiar su lógica) `subirFoto`, `foto`, `archivoDe` y `guardarEnCache` de `EjerciciosRepository` y cambiando `"ejercicio"` por el parámetro `ruta`; hacer que `EjerciciosRepository` la use con `ruta = "ejercicio"`; escribir `PlatillosRepository` y el ViewModel calcando los de ejercicios (`guardar` hace `set` al crear y `update` con `FieldValue.delete()` del motivo al reeditar, para no tocar `revisadoPor` ni `revisadoEn`).
- [ ] **Step 4: Ejecutar** `.\gradlew.bat :app:testDebugUnitTest` — Esperado: pasan todas, incluidas `EjerciciosMapaTest` y `EjercicioTest` (el refactor de fotos no cambió nada).
- [ ] **Step 5: Commit** — `feat(platillos): repositorio y ViewModel del banco global de platillos`.

---

### Tarea 5: Piezas de pantalla compartidas de platillo

**Files:**
- Create: `app/src/main/java/com/example/nutrigajo/ui/shared/platillos/PlatilloVistas.kt`
- Modify: `app/src/main/res/values/strings.xml` (etiquetas de `EtiquetaPlatillo`, de `EstadoPlatillo`, de `ProblemaPlatillo`, textos de macros, y el aviso «no sustituye las indicaciones de tu nutriólogo», con prefijo `platillo_`)

**Interfaces:**
- Consumes: `Platillo`, `PlatillosRepository.foto(...)`, y de `ui/shared/ejercicios`: `CabeceraOscura`, `BotonVolverCristal`, `DatosEnColumnas`, `PasosPager`, `rememberPasosEstado`, `montadaSobreLaCabecera`, `FotoOMarcador`, `GrupoOpciones`, `Dato`.
- Produces (en `com.example.nutrigajo.ui.shared.platillos`):
  - `@StringRes fun EtiquetaPlatillo.titulo(): Int`, `fun EstadoPlatillo.titulo(): Int`, `fun ProblemaPlatillo.mensaje(): Int` (y reutiliza `MomentoComida.titulo()`, que ya es `internal` en `ui/nutriologo/plan/BancoScreen.kt` y se ve desde todo el módulo)
  - `@Composable fun rememberFotoPlatillo(platillo: Platillo): ImageBitmap?`
  - `@Composable fun TarjetaPlatillo(platillo: Platillo, onClick: () -> Unit, modifier: Modifier = Modifier)` — la tarjeta de la imagen de referencia: foto, dos chips de cristal (proteína y grasas), nombre, kcal y flecha naranja; toda la tarjeta es tocable
  - `@Composable fun FilaPlatillo(platillo: Platillo, onClick: () -> Unit, modifier: Modifier = Modifier)` (la fila compacta de las listas) y `@Composable fun EtiquetaEstadoPlatillo(estado: EstadoPlatillo, modifier: Modifier = Modifier)`
  - `@Composable fun DetallePlatillo(platillo: Platillo, onVolver: () -> Unit, aviso: String? = null, acciones: (@Composable ColumnScope.() -> Unit)? = null)` — cabecera oscura con foto, quién lo hizo («Receta de nutriólogo» o «Receta de entrenador»), macros en tres columnas, ingredientes, los pasos en `PasosPager`, y el aviso.

- [ ] **Step 1: Escribir las cadenas** y las tres funciones de etiquetas (un `when` exhaustivo por enum).
- [ ] **Step 2: Implementar** las cuatro composables calcando `TarjetaEjercicio`, `FilaEjercicio` y `DetalleEjercicio` de `ui/shared/ejercicios/EjercicioVistas.kt`; sin foto se ve el marcador gris de `FotoOMarcador`.
- [ ] **Step 3: Ejecutar** `.\gradlew.bat :app:compileDebugKotlin` — Esperado: compila sin errores.
- [ ] **Step 4: Commit** — `feat(platillos): tarjeta, fila y detalle de platillo`.

---

### Tarea 6: «Mis platillos» del autor (nutriólogo y entrenador)

**Files:**
- Create: `app/src/main/java/com/example/nutrigajo/ui/shared/platillos/EditorPlatillo.kt`
- Create: `app/src/main/java/com/example/nutrigajo/ui/shared/platillos/MisPlatillosScreen.kt`
- Modify: `app/src/main/java/com/example/nutrigajo/ui/entrenador/inicio/InicioEntrenadorScreen.kt` (estado `enPlatillos`; entrada en `TabEjercicios`)
- Modify: `app/src/main/java/com/example/nutrigajo/ui/nutriologo/inicio/InicioNutriologoScreen.kt` y `TabInicio.kt` (estado `platillosAbiertos`; una `FilaAjuste` «Mis platillos» junto a las demás)

**Interfaces:**
- Consumes: `PlatillosViewModel`, `Platillo` y sus funciones (Tareas 1 y 4), `TarjetaPlatillo`/`DetallePlatillo`/`EtiquetaEstadoPlatillo` (Tarea 5); `EditorEjercicio` como modelo del formulario.
- Produces:
  - `@Composable fun EditorPlatillo(inicial: Platillo, guardando: Boolean, @StringRes error: Int?, onGuardar: (platillo: Platillo, enviarARevision: Boolean, foto: Uri?) -> Unit, onVolver: () -> Unit)` — campos: nombre, resumen, momento, tres macros y kcal (con el aviso `kcalNoCuadran()` y la sugerencia `kcalSugeridas`), minutos, ingredientes y pasos (listas que crecen), etiquetas (hasta 4) y foto; «Guardar borrador» solo pide el nombre y «Mandar a revisión» se activa cuando `problemaParaRevision()` es null (si no, muestra su mensaje).
  - `@Composable fun MisPlatillosScreen(perfil: Perfil, puedeCrear: Boolean, onVolver: () -> Unit, vm: PlatillosViewModel = viewModel(key = "platillos-${perfil.uid}"))` — lista con `TarjetaPlatillo` y la etiqueta de estado, detalle con las acciones editar, duplicar, retirar, sacar de revisión y eliminar (las de `AccionesEjercicio`), y el botón «Nuevo platillo» que solo actúa con `puedeCrear`. Guarda con `perfil.rol` como `autorRol`.

- [ ] **Step 1: Implementar `EditorPlatillo`** calcando `EditorEjercicio` (`ui/entrenador/ejercicios/EditorEjercicio.kt`) con los campos de «Interfaces».
- [ ] **Step 2: Implementar `MisPlatillosScreen`** calcando el flujo de `InicioEntrenadorScreen.kt:131-170` (editor → detalle → lista) y la columna de acciones `AccionesEjercicio`.
- [ ] **Step 3: Conectar** la entrada en el inicio del entrenador (`puedeCrear = aprobada`) y en el del nutriólogo (`puedeCrear` = el mismo indicador de cuenta aprobada que ya usa `Atajos`), con `BackHandler` para volver.
- [ ] **Step 4: Ejecutar** `.\gradlew.bat :app:compileDebugKotlin` y `.\gradlew.bat :app:testDebugUnitTest` — Esperado: compila y pasan las pruebas.
- [ ] **Step 5: Commit** — `feat(platillos): «Mis platillos» para nutriólogos y entrenadores`.

---

### Tarea 7: Revisión del admin

**Files:**
- Modify: `app/src/main/java/com/example/nutrigajo/ui/admin/inicio/TabContenido.kt`
- Modify: `app/src/main/java/com/example/nutrigajo/ui/admin/inicio/InicioAdminScreen.kt` (VM de platillos, detalle y diálogo de rechazo)
- Modify: `app/src/main/res/values/strings.xml` (`admin_contenido_ejercicios`, `admin_contenido_platillos`, `admin_rechazar_platillo`, `admin_contenido_vacio_platillos`)

**Interfaces:**
- Consumes: `PlatillosViewModel.cargarEnRevision/publicar/rechazar`, `TarjetaPlatillo`, `DetallePlatillo`.
- Produces: `TabContenido(perfil: Perfil, ejercicios: EjerciciosEstado, platillos: PlatillosEstado, onEjercicio: (Ejercicio) -> Unit, onPlatillo: (Platillo) -> Unit, onActualizar: () -> Unit, onPerfil: () -> Unit, modifier: Modifier = Modifier, transicion: TransicionPerfil? = null)` con un `SelectorPestanas` «Ejercicios | Platillos» (estado `rememberSaveable`) sobre la misma cola; en `InicioAdminScreen` un `PlatillosViewModel` con clave `"admin-platillos-${perfil.uid}"`, que se carga junto al de ejercicios y se actualiza con el mismo botón.

- [ ] **Step 1: Cambiar `TabContenido`** a las dos secciones y el contador de cada una.
- [ ] **Step 2: En `InicioAdminScreen`** añadir el segundo ViewModel, el detalle del platillo con los botones publicar y rechazar (como `InicioAdminScreen.kt:95-115`) y `DialogoRechazo(titulo = R.string.admin_rechazar_platillo, …)`.
- [ ] **Step 3: Ejecutar** `.\gradlew.bat :app:compileDebugKotlin` — Esperado: compila.
- [ ] **Step 4: Commit** — `feat(platillos): revisión de platillos en el panel del admin`.

---

### Tarea 8: Inicio del paciente y Explorar platillos

**Files:**
- Create: `app/src/main/java/com/example/nutrigajo/ui/paciente/platillos/ExplorarPlatillosScreen.kt`
- Modify: `app/src/main/java/com/example/nutrigajo/ui/paciente/inicio/RutinaYDieta.kt` (la fila `DietaNutricion` pasa a usar platillos reales; la `TarjetaComida` privada se reemplaza por `TarjetaPlatillo`)
- Modify: `app/src/main/java/com/example/nutrigajo/ui/paciente/inicio/InicioPacienteScreen.kt` (estado `enPlatillos`, `platilloInicial`, `platillosVm`)
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `PlatillosViewModel.cargarPublicados`, `paraPaciente`, `coincide`, `TarjetaPlatillo`, `FilaPlatillo`, `DetallePlatillo`; el expediente (`Expediente.alergias`, `Expediente.dieta`) que `InicioPaciente` ya tiene en `expedienteVm`.
- Produces:
  - `@Composable fun ExplorarPlatillosScreen(onVolver: () -> Unit, abrirId: String? = null, alergias: List<String> = emptyList(), dieta: Dieta? = null, vm: PlatillosViewModel = viewModel(key = "explorar-platillos"))` — búsqueda, filtros de momento y etiqueta, lista de `FilaPlatillo` y detalle con el aviso de que no sustituye al nutriólogo; con `abrirId` abre ese platillo y atrás vuelve al inicio (igual que `ExplorarEjerciciosScreen`).
  - `@Composable fun DietaNutricion(estado: PlatillosEstado, platillos: List<Platillo>, onAbrir: (String?) -> Unit, onMisComidas: () -> Unit, modifier: Modifier = Modifier)` — encabezado «Dieta & nutrición» con «Ver todas» (`onAbrir(null)`), un carrusel de `TarjetaPlatillo` (`onAbrir(id)`), esqueletos mientras carga y, con cero platillos, una `FilaAjuste` de estado vacío como la de `BancoEjercicios` (`CoachYExtras.kt:123`); debajo, una `FilaAjuste` «Mis comidas» que llama a `onMisComidas`.

- [ ] **Step 1: Escribir `ExplorarPlatillosScreen`** calcando `ExplorarEjerciciosScreen.kt` (sin la cabecera de foto: barra superior, búsqueda y chips bastan).
- [ ] **Step 2: Reemplazar `DietaNutricion`** y cablear `InicioPacienteScreen`: `platillosVm = viewModel(key = "explorar-platillos")` con `cargarPublicados()` al abrir; en `InicioPaciente` calcular `val visibles = remember(estado.items, expediente.expediente) { estado.items.paraPaciente(expediente.expediente.alergias, expediente.expediente.dieta) }`; `onAbrir` abre Explorar (con o sin id); `onMisComidas` abre `DietaFlujo` como hoy con `onDieta(-1)` (decisión de este plan: «Mis comidas» de muestra se conserva con su propio atajo porque «Ver todas» ya no lo abre).
- [ ] **Step 3: Verificar el estado vacío** (Review Focus 5): ejecutar la app con la lista vacía (cuenta sin platillos publicados) y comprobar que la fila muestra el atajo; no puede quedar en blanco ni con los platos de `DatosMuestra`. Si no hay cómo verlo en pantalla, dejar anotado que quedó sin ver.
- [ ] **Step 4: Ejecutar** `.\gradlew.bat :app:compileDebugKotlin` y `.\gradlew.bat :app:testDebugUnitTest` — Esperado: compila y pasan las pruebas.
- [ ] **Step 5: Commit** — `feat(platillos): «Dieta & nutrición» y Explorar con platillos reales`.

---

### Tarea 9: Pestaña «Globales» al armar un plan

**Files:**
- Modify: `app/src/main/java/com/example/nutrigajo/ui/nutriologo/plan/BancoScreen.kt` (con `onElegir != null` y tipo comida: selector «Mis comidas | Globales»)
- Modify: `app/src/main/java/com/example/nutrigajo/ui/nutriologo/plan/PlanScreen.kt:279` (pasar el ViewModel)
- Modify: `app/src/main/res/values/strings.xml` (`banco_mis_comidas`, `banco_globales`, `banco_vacio_globales`)

**Interfaces:**
- Consumes: `PlatillosViewModel.cargarPublicados`, `Platillo.textoParaPlan()`, `Platillo.coincide`, `FilaPlatillo`; `DestinoBanco.onElegir: ((ItemBanco) -> Unit)?` (existente, sin cambios).
- Produces: `internal fun BancoScreen(destino: DestinoBanco, vm: BancoViewModel, globales: PlatillosViewModel, onSalir: () -> Unit)`. En «Globales» la lista son los publicados del momento del campo (`destino.momento`), y tocar uno llama `elegir(ItemBanco(tipo = TipoBanco.COMIDA, nombre = platillo.nombre, texto = platillo.textoParaPlan(), momento = platillo.momento))`, así `PlanScreen` y el tope de 500 caracteres no cambian. Administrando el banco propio (sin `onElegir`) no aparece la pestaña.

- [ ] **Step 1: Escribir la prueba que falla** en `PlatilloTest.kt`: `un_platillo_elegido_se_convierte_en_item_del_banco_con_texto_que_cabe` — el `ItemBanco` armado como arriba cumple `nombre.length <= ItemBanco.MAX_NOMBRE`, `texto.length <= PlanContenido.MAX_MENU` y `textoParaPlan()` del item devuelve el mismo texto (no lo antepone otra vez).
- [ ] **Step 2: Ejecutar** `.\gradlew.bat :app:testDebugUnitTest --tests "*PlatilloTest"` — Esperado: falla si `textoParaPlan` repite el nombre o pasa del tope; pasa si la Tarea 1 lo hizo bien (en ese caso el paso solo confirma).
- [ ] **Step 3: Implementar** la pestaña y la lista de globales en `BancoScreen`, con esqueletos al cargar, estado vacío y el error de la carga.
- [ ] **Step 4: Ejecutar** `.\gradlew.bat :app:compileDebugKotlin` y `.\gradlew.bat :app:testDebugUnitTest` — Esperado: compila y pasan las pruebas.
- [ ] **Step 5: Commit** — `feat(platillos): el nutriólogo elige platillos globales al armar un plan`.

---

### Tarea 10: Siembra, publicación y revisión en el teléfono (con permisos del usuario)

**Files:**
- Create (fuera del repo, en el directorio temporal de la sesión): script de siembra que escribe `platillos/semilla-p01…p06` con la sesión de Firebase CLI, como se hizo con los 150 ejercicios (ver la memoria `plan-entrenador-banco-global`).

**Interfaces:**
- Consumes: los 6 `Comida` de `app/src/main/java/com/example/nutrigajo/ui/paciente/inicio/DatosMuestra.kt` (`platos`: nombre, macros, kcal, minutos, tipo, resumen, ingredientes, pasos).
- Produces: 6 platillos `PUBLICADO` con `autorId = "nutrigajo-semilla"`, `autorNombre = "Equipo Nutri Gajo"`, `autorRol = "NUTRIOLOGO"`, sin foto, y `currentDocument.exists = false` para no pisar nada.

- [ ] **Step 1: Pedir permiso** al usuario, por separado, para: publicar las reglas (`firebase deploy --only firestore:rules --project nutrigajo --config firebase.json` desde la carpeta `emu/` del scratchpad), desplegar el Worker (`npx wrangler deploy` en `backend/`) y sembrar los 6 platillos. No hacer ninguna de las tres sin el sí de cada una.
- [ ] **Step 2: Con el sí**, publicar reglas, desplegar el Worker y comprobar sin escribir (`GET /platillo/{uid}/inexistente` → 404, `PUT /platillo/x` sin sesión → 401).
- [ ] **Step 3: Con el sí**, sembrar y leer el resultado con la sesión de Firebase CLI (6 documentos `PUBLICADO`).
- [ ] **Step 4: Revisar en el teléfono sin cerrar la sesión real:** compilar `.\gradlew.bat :app:assembleDebug` (apunta a producción), instalar con `adb install -r` (nunca `uninstall` ni `pm clear`) y mirar el inicio del paciente: la fila con los 6 platillos, «Ver todas», el detalle con su receta y el atajo «Mis comidas». Esperado: se ve la tarjeta de la imagen de referencia con el marcador gris en lugar de foto.
- [ ] **Step 5: Revisar crear y publicar** (entrenador, nutriólogo y admin) solo con los emuladores y el Worker falso, y solo si el usuario acepta cerrar su sesión en el teléfono para esa prueba; si no, dejar dicho que esa parte quedó probada solo con `platillos.mjs`.
- [ ] **Step 6: Actualizar la memoria** (`plan-entrenador-banco-global.md` o una nota nueva) con lo hecho, lo publicado y lo que no se vio.
