# Actualizaciones internas de la app — plan de implementación

> **Para quien ejecute:** usar superpowers:executing-plans (o subagent-driven-development). Los pasos llevan casillas `- [ ]`.

**Meta:** que la app avise de una versión nueva, la descargue, compruebe y la instale sola (con la confirmación de Android), y que publicar una versión sea un comando.

**Arquitectura:** el APK firmado y un `ultima.json` viven en el R2 `appsmobile`; el Worker los sirve en dos rutas públicas de solo lectura; un script de la computadora los sube tras comprobar firma y versión. En la app, un dominio puro decide el estado, un repositorio consulta y descarga con comprobación de SHA-256 y un instalador usa `PackageInstaller`; un ViewModel por actividad y tres pantallas (hoja, obligatoria, fila de «Acerca de») lo enseñan.

**Tecnología:** Kotlin y Compose (Material 3), `PackageInstaller`, Cloudflare Worker (TypeScript) con R2, Node 22 para el script y las pruebas, `aapt2`/`apksigner` del SDK.

**Diseño:** `docs/superpowers/specs/2026-11-06-actualizaciones-internas-design.md`. Léelo antes de empezar; manda sobre este plan.

## Restricciones globales

- `minSdk` 29: se puede usar `PackageInfo.longVersionCode` y `PackageInstaller`; `setRequireUserAction` solo desde la API 31.
- Todo por HTTPS desde `BuildConfig.WORKER_URL`; la app **nunca** usa una dirección del manifiesto: arma `{base}/app/apk/{versionCode}`.
- Manifiesto: `versionCode` > 0; `versionName` de 1 a 40 caracteres; `sha256` de 64 hexadecimales en minúsculas; `bytes` entre 1 y `MAX_APK_BYTES = 200 * 1024 * 1024`; `notas` hasta 10 de hasta 200 caracteres; `minVersionCode` ≥ 0 y ≤ `versionCode`; `publicadaEn` entero.
- Se consulta como mucho cada 12 h (`CADA_MS = 12 * 3_600_000`); la consulta automática no muestra nada si falla o si está al día.
- Certificado del release (SHA-256 completo): `87807ca1881078dc124af1a8d6da07cfe585bb5de68e27823bc097423793df3f`; paquete `com.example.nutrigajo`.
- Rutas del Worker: `GET /app/ultima` y `GET /app/apk/{versionCode}` (solo dígitos, 1 a 9). Sin rutas de subida.
- Claves de R2: `apk/ultima.json` y `apk/{versionCode}.apk`. Caché: `ultima` → `public, max-age=300`; APK → `public, max-age=31536000, immutable`.
- Preferencias (`nutrigajo`): `actualizacion_ultima_consulta` (Long) y `actualizacion_aplazada` (Long, el `versionCode` pospuesto).
- Español en textos y comentarios. **El Worker no se despliega ni se publica ningún APK sin que el usuario lo diga.** Nada se confirma en git hasta que lo pida; nunca `push`. No tocar `liquidglass/` ni `sample/`.

## Qué mirar además de lo que dice el diseño

1. **Descarga cortada o archivo que no coincide:** se borra el archivo, se dice «no coincide con lo publicado» y no llega al instalador. → tareas 1 y 4.
2. **Sin red o Worker caído al abrir la app:** la consulta automática es silenciosa (sin ventana ni error); la manual sí dice que no pudo. → tarea 5.
3. **Manifiesto con la misma versión o menor, o mal formado:** «al día» o ignorado; nunca ofrece retroceder. → tarea 1.
4. **Permiso «Instalar apps desconocidas» sin dar o revocado:** se lleva a la pantalla del sistema y se retoma al volver; no se pierde lo descargado. → tarea 4.
5. **Firma distinta (teléfono con la llave de debug) y obligatoria sin salida:** mensaje claro de que no se puede actualizar desde la app; la obligatoria ofrece reintentar y cerrar, no deja atrapado. → tareas 4 y 5.

---

## Tarea 1: Dominio de la actualización

**Archivos:**
- Crear: `app/src/main/java/com/example/nutrigajo/domain/Actualizacion.kt`
- Crear: `app/src/test/java/com/example/nutrigajo/domain/ActualizacionTest.kt`
- Modificar (solo si hace falta): `app/build.gradle.kts` → `testImplementation("org.json:json:20240303")` (en la JVM de las pruebas `org.json` de Android no funciona)

**Interfaces — produce:**
- `data class Actualizacion(val versionCode: Long, val versionName: String, val sha256: String, val bytes: Long, val notas: List<String>, val minVersionCode: Long, val publicadaEn: Long)`
- `enum class EstadoActualizacion { AL_DIA, DISPONIBLE, OBLIGATORIA }`
- `const val MAX_APK_BYTES = 200L * 1024 * 1024` y `const val CADA_MS = 12L * 3_600_000`
- `fun actualizacionDeJson(texto: String): Actualizacion?` (null si no cumple las restricciones globales)
- `fun estadoDe(versionActual: Long, ultima: Actualizacion): EstadoActualizacion` (`AL_DIA` si `ultima.versionCode <= versionActual`; `OBLIGATORIA` si además `versionActual < ultima.minVersionCode`; si no, `DISPONIBLE`)
- `fun tocaConsultar(ahora: Long, ultimaConsulta: Long): Boolean` (verdadero si pasaron `CADA_MS` o si `ultimaConsulta` es 0 o está en el futuro)
- `fun debeMostrar(estado: EstadoActualizacion, versionCode: Long, aplazada: Long): Boolean` (`OBLIGATORIA` siempre; `DISPONIBLE` salvo que `versionCode == aplazada`; `AL_DIA` nunca)

- [ ] **Paso 1: pruebas que fallan** (`ActualizacionTest`): `un_manifiesto_valido_se_lee_completo`; `un_manifiesto_sin_minVersionCode_lo_deja_en_cero`; `cada_campo_malo_lo_descarta` (una prueba por cada: `versionCode` 0 o negativo, `versionName` vacío o de 41 caracteres, `sha256` de 63 caracteres, con mayúsculas o con una letra no hexadecimal, `bytes` 0 y `MAX_APK_BYTES + 1`, 11 notas, una nota de 201 caracteres, `minVersionCode` mayor que `versionCode`, JSON que no es objeto o con un tipo equivocado); `estado_al_dia_si_la_publicada_no_es_mayor` (iguales y menor); `estado_disponible_y_obligatoria`; `toca_consultar_a_las_12_horas_y_siempre_si_nunca_se_consulto_o_el_reloj_va_atras`; `una_version_pospuesta_no_se_vuelve_a_mostrar_pero_la_obligatoria_si`.
- [ ] **Paso 2: verlas fallar:** `./gradlew :app:testDebugUnitTest --tests "*ActualizacionTest"` (con `JAVA_HOME` del JBR de Android Studio). Esperado: no compila (símbolos sin definir).
- [ ] **Paso 3: implementar** lo de «Interfaces» con `org.json`. **Paso 4: verlas pasar** con el mismo comando y la suite entera.

---

## Tarea 2: Rutas del Worker

**Archivos:**
- Crear: `backend/src/routes/app.ts`
- Modificar: `backend/src/index.ts` (dos rutas `GET`, antes del 404 final), `backend/package.json` (script `test:app`)
- Crear: `backend/test/app.mjs` (R2 falso como el de `backend/test/fotoBanco.mjs`)

**Interfaces — produce:**
- `export async function leerUltima(env: Env): Promise<Response>` y `export async function leerApk(req: Request, env: Env, versionCode: string): Promise<Response>`.
- `leerUltima`: `apk/ultima.json` tal cual, `Content-Type: application/json`, caché de las restricciones; 404 `Sin versión publicada` si no existe.
- `leerApk`: `versionCode` que no cumpla `^[0-9]{1,9}$` → 404 (sin tocar R2); objeto de `apk/{versionCode}.apk` con `Content-Type: application/vnd.android.package-archive`, `Content-Length`, `ETag`, `X-Content-Type-Options: nosniff`, caché de las restricciones y soporte de `If-None-Match` (304) como `leerFotoBanco`; 404 si no existe.
- `index.ts`: `GET /app/ultima` y `GET /app/apk/{código}` públicas (sin sesión); cualquier otro método o ruta bajo `/app/` → 404.

- [ ] **Paso 1: pruebas que fallan** (`app.mjs`): sin `apk/ultima.json` → 404; con él → 200, JSON idéntico y `max-age=300`; `/app/apk/2` ausente → 404; presente → 200 con los bytes, tipo y longitud exactos, `immutable` y `ETag`; con `If-None-Match` igual → 304; `/app/apk/abc`, `/app/apk/1234567890`, `/app/apk/..%2Fultima` y `/app/apk/` → 404 y el R2 falso no recibe ninguna lectura; `POST /app/ultima` y `PUT /app/apk/2` → 404.
- [ ] **Paso 2: verlas fallar:** en `backend/`, `npm run test:app`. Esperado: falla (rutas inexistentes).
- [ ] **Paso 3: implementar.** **Paso 4: verlas pasar:** `npm run test:app`, `npx tsc --noEmit` y las demás pruebas de `backend/test`.

---

## Tarea 3: Script de publicación y versionado de Gradle

**Archivos:**
- Crear: `backend/scripts/publicar-apk.mjs`, `backend/scripts/publicar.lib.mjs` (la parte pura), `backend/test/publicar.mjs`
- Modificar: `backend/package.json` (scripts `publicar-apk` y `test:publicar`), `app/build.gradle.kts`

**Interfaces — produce (`publicar.lib.mjs`):**
- `leerBadging(texto: string): { paquete: string; versionCode: number; versionName: string }` (de la salida de `aapt2 dump badging`: línea `package: name='…' versionCode='…' versionName='…'`; lanza si falta).
- `armarManifiesto({ versionCode, versionName, sha256, bytes, notas, minVersionCode, ahora }): object` con las claves del diseño; lanza si algo incumple las restricciones globales (mismos límites que la tarea 1).
- `comprobarPublicable({ paquete, certificadoSha256, versionCode, publicada }): void`: lanza si el paquete no es `com.example.nutrigajo`, si el certificado no es el del release o si `versionCode` no es mayor que `publicada` (null si no hay nada publicado).
- `publicar-apk.mjs <apk> [--notas "a;b"] [--minima N] [--solo-mostrar]`: usa `aapt2` y `apksigner verify --print-certs` del SDK (ruta por `ANDROID_HOME` o la de siempre del usuario), calcula SHA-256 y tamaño, lee la versión ya publicada con `GET /app/ultima` del Worker, muestra el manifiesto y, salvo `--solo-mostrar`, sube con `wrangler r2 object put appsmobile/apk/{N}.apk --remote --content-type application/vnd.android.package-archive` y después `apk/ultima.json` (en ese orden: el manifiesto es lo último que cambia).

**Gradle:** `versionCode` y `versionName` salen de `-PversionCode=N` y `-PversionName=X.Y` (por defecto 1 y «1.0»); `buildConfigField("boolean", "ACTUALIZACION_INTERNA", …)` es `true` salvo con `-PsinActualizacionInterna=true`.

- [ ] **Paso 1: pruebas que fallan** (`publicar.mjs`): `leerBadging` con la salida real de `aapt2` pegada en la prueba (y que lanza si falta la línea); `armarManifiesto` válido; cada límite malo lanza (mismos casos de la tarea 1); `comprobarPublicable` lanza con otro paquete, con el certificado de debug, con versión igual o menor que la publicada y no lanza con la siguiente.
- [ ] **Paso 2: verlas fallar:** `npm run test:publicar` en `backend/`. **Paso 3: implementar** la librería, el script y los cambios de Gradle. **Paso 4: verlas pasar,** y ejecutar `npm run publicar-apk -- ../app/build/outputs/apk/release/app-release.apk --solo-mostrar` (no sube nada; esperado: el manifiesto con SHA-256 y tamaño reales). Comprobar con `./gradlew :app:assembleRelease -PversionCode=2 -PversionName=1.1` que `aapt2` lee 2 y «1.1».

---

## Tarea 4: Datos e instalador en la app

**Archivos:**
- Crear: `data/repository/ActualizacionRepository.kt`, `data/actualizacion/InstaladorApk.kt`, `data/actualizacion/InstalacionReceiver.kt`
- Modificar: `app/src/main/AndroidManifest.xml` (permiso `REQUEST_INSTALL_PACKAGES` y el receptor sin exportar), `res/xml/file_paths.xml` solo si hace falta
- Crear: `app/src/test/java/com/example/nutrigajo/data/repository/ActualizacionArchivoTest.kt`, `.../data/actualizacion/ResultadoInstalacionTest.kt`

**Interfaces — consume:** `Actualizacion`, `actualizacionDeJson` (tarea 1).
**Interfaces — produce:**
- `class ActualizacionRepository(base: String = BuildConfig.WORKER_URL)` con `suspend fun consultar(): Actualizacion?` (GET `/app/ultima`; null si 404 o manifiesto inválido; lanza `IOException` si no hay red o el Worker responde otra cosa) y `suspend fun descargar(act: Actualizacion, destino: File, onAvance: (Long) -> Unit)` (GET `/app/apk/{versionCode}` a `destino.part`, y solo si `archivoCoincide` lo renombra a `destino`; si no coincide borra y lanza `IOException("no coincide")`).
- `internal fun archivoCoincide(archivo: File, bytes: Long, sha256: String): Boolean`.
- `enum class ResultadoInstalacion { EXITO, CANCELADA, FIRMA_DISTINTA, SIN_ESPACIO, OTRO }` y `internal fun resultadoDe(status: Int, mensaje: String?): ResultadoInstalacion` (`STATUS_SUCCESS` → `EXITO`; `STATUS_FAILURE_ABORTED` → `CANCELADA`; `STATUS_FAILURE_STORAGE` → `SIN_ESPACIO`; `STATUS_FAILURE_CONFLICT`, `STATUS_FAILURE_INCOMPATIBLE` o un mensaje que contenga «signature» → `FIRMA_DISTINTA`; el resto `OTRO`).
- `object InstaladorApk`: `fun puedeInstalar(contexto: Context): Boolean` (`packageManager.canRequestPackageInstalls()`), `fun intentDePermiso(contexto: Context): Intent` (`Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` con `package:`), `fun instalar(contexto: Context, apk: File)` (sesión `MODE_FULL_INSTALL`, `setRequireUserAction(USER_ACTION_NOT_REQUIRED)` desde la API 31, escribe y confirma con un `PendingIntent` **mutable** al receptor).
- `InstalacionReceiver`: ante `STATUS_PENDING_USER_ACTION` lanza el `EXTRA_INTENT` con `FLAG_ACTIVITY_NEW_TASK`; con cualquier otro estado publica el `ResultadoInstalacion` en `InstalacionReceiver.resultado: StateFlow<ResultadoInstalacion?>` (compartido por proceso).

- [ ] **Paso 1: pruebas que fallan:** `archivoCoincide` con archivos temporales (contenido «abc» → SHA-256 `ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad`, 3 bytes: coincide; un byte de más, un hash distinto y un archivo que no existe: no coincide); `resultadoDe` con cada estado de la lista y con el mensaje «INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package … signatures do not match» → `FIRMA_DISTINTA`.
- [ ] **Paso 2: verlas fallar** (`--tests "*ActualizacionArchivoTest" --tests "*ResultadoInstalacionTest"`). **Paso 3: implementar** lo de «Interfaces» y el manifiesto. **Paso 4: verlas pasar,** la suite entera y `:app:compileDebugKotlin` sin errores ni avisos nuevos.

---

## Tarea 5: ViewModel y pantallas

**Archivos:**
- Crear: `ui/actualizacion/ActualizacionViewModel.kt`, `ui/actualizacion/ActualizacionHoja.kt`, `ui/actualizacion/ActualizacionObligatoria.kt`
- Modificar: `MainActivity.kt` (el ViewModel de la actividad y el enganche en `Paso.INICIO`), `ui/shared/ajustes/AcercaDeScreen.kt`, `ui/shared/ajustes/AjustesFlujo.kt` y quien llama a `AjustesFlujo` (para pasar el estado y las acciones), `res/values/strings.xml`

**Interfaces — consume:** tareas 1 y 4; `FilaAjuste`, `BotonPrimario`, `Cargando`-style esqueletos del proyecto.
**Interfaces — produce:**
- `sealed interface FaseActualizacion { Ociosa; Consultando; AlDia; class Disponible(val act: Actualizacion, val obligatoria: Boolean); class Descargando(val act: Actualizacion, val bytes: Long); class PermisoPendiente(val act: Actualizacion); class Instalando(val act: Actualizacion); class Error(@StringRes val mensaje: Int, val act: Actualizacion?) }`
- `class ActualizacionViewModel(app: Application) : AndroidViewModel(app)` con `val fase: StateFlow<FaseActualizacion>` y `verificar(manual: Boolean)` (respeta `tocaConsultar` y `debeMostrar` si no es manual; actualiza `actualizacion_ultima_consulta`), `actualizar()` (descarga a `cache/actualizaciones/nutrigajo-{N}.apk`, pide el permiso si falta y retoma al volver con `retomar()`, luego instala), `masTarde()` (guarda `actualizacion_aplazada`; no hace nada si es obligatoria), `reintentar()`. Al arrancar borra lo que haya en `cache/actualizaciones/`. Con `BuildConfig.ACTUALIZACION_INTERNA` en falso no consulta nunca.
- `@Composable fun ActualizacionHost(vm: ActualizacionViewModel)`: según la fase, pinta `ActualizacionHoja` (notas, avance, «Actualizar» y «Más tarde») o `ActualizacionObligatoria` (pantalla completa, sin «Más tarde», con «Reintentar» y «Cerrar la app»). `AcercaDeScreen` gana la fila «Buscar actualizaciones» / «Estás al día» / «Versión X disponible».
- Textos nuevos en español para: hay versión nueva, notas, descargando, comprobando, instalando, estás al día, no se pudo buscar, no hay red, el archivo no coincide con lo publicado, falta espacio, activa «Instalar apps desconocidas» para NutriGajo, esta versión no se puede actualizar desde la app (firma distinta), instalación cancelada, actualización obligatoria.

- [ ] **Paso 1:** estados y textos. **Paso 2:** ViewModel, con las tres consultas posibles (automática, manual, obligatoria) y el resultado del instalador mapeado a mensajes. **Paso 3:** hoja, pantalla obligatoria y fila de «Acerca de»; enganche en `MainActivity` solo con sesión. **Paso 4:** `:app:compileDebugKotlin` sin errores ni avisos y `:app:testDebugUnitTest` verde (la lógica pura ya está probada en las tareas 1 y 4; los ViewModels del proyecto no se prueban con unidad porque dependen de Android).

---

## Tarea 6: Verificación y entrega

- [ ] **Paso 1:** suite unitaria completa, `npm run test:app`, `npm run test:publicar`, `npx tsc --noEmit`, y `:app:assembleRelease` (R8) sin errores; revisión de todo el cambio con un revisor nuevo (`superpowers:requesting-code-review`); los hallazgos importantes se corrigen con prueba.
- [ ] **Paso 2: en el teléfono — preguntar antes.** Con un Worker de mentira que sirva el manifiesto y el APK (`adb reverse tcp:8787`), instalar con `-PworkerUrl=http://localhost:8787 -PversionCode=1`, compilar una segunda con `-PversionCode=2 -PversionName=1.1` (misma llave de debug) y verificar: la hoja con las notas, la descarga con avance, el permiso «Instalar apps desconocidas», la ventana de confirmación y que la app queda en la versión 2 **con la sesión intacta**; además un archivo cortado (el Worker falso corta la respuesta) y una obligatoria. Después reinstalar la variante `rapido` apuntando a producción y quitar los `adb reverse`. Nunca `pm clear` ni desinstalar.
- [ ] **Paso 3: despliegue.** Preguntar por separado: desplegar el Worker (`npx wrangler deploy` en `backend/`; las rutas son de solo lectura y sin APK publicado contestan 404) y, después, **publicar la primera versión** con el script. Hasta entonces la app no encuentra nada y no avisa de nada.
- [ ] **Paso 4:** actualizar la nota de memoria del proyecto (qué quedó, qué se desplegó, cómo publicar una versión y la limitación de las firmas).
