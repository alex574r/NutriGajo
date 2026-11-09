# Eliminar cuenta, desvincularse y cambiar de nutriólogo — Plan de implementación

> **Para quien ejecute:** usar superpowers:executing-plans (inline, TDD). Los pasos son casillas `- [ ]`. Cada tarea deja un renglón en el ledger `.superpowers/sdd/2026-11-08-cuenta-y-vinculo/progress.md`. **No hay pasos de commit**: el usuario no los ha pedido (si los pide, un commit por tarea con `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`).

**Meta:** que un paciente pueda cerrar su vínculo y cambiar de nutriólogo, que un nutriólogo pueda dar de baja a un paciente, y que cualquier usuario (menos ADMIN) pueda borrar su cuenta confirmándolo con el enlace del correo de Firebase, sin perder nunca el registro clínico.

**Arquitectura:** un vínculo cerrado es `pacientes/{id}` con `activo = false` (no se borra; las lecturas siguen abiertas y las escrituras exigen `activo`). Cerrar, cambiar y dar de baja son lotes de Firestore desde la app, con las reglas como autoridad. Borrar la cuenta lo hace el Worker (`POST /cuenta/eliminar`) con la cuenta de servicio, después de que la app confirme la identidad con el enlace de Firebase (`auth_time` reciente). Lo de adentro se borra primero; `usuarios/{uid}` y la cuenta de Auth, al final.

**Tecnología:** Kotlin/Compose, Firebase Auth y Firestore (plan Spark), Worker de Cloudflare (TypeScript, `jose`, R2), pruebas de JVM, pruebas de reglas contra emuladores (`node firebase/test/*.mjs`) y pruebas del Worker con `fetch` falso (`npm run test:*` en `backend/`).

**Spec:** `docs/superpowers/specs/2026-11-08-cuenta-y-vinculo-design.md` (aprobado por el usuario el 8 nov 2026). Lo que el spec marca como «mis llamadas» es decisión del ejecutor aprobada al aprobar el spec.

## Restricciones globales
- Solo git local; nada de push ni commit sin que lo pidan. Reglas de Firestore, Worker y APK **no se publican ni se despliegan** sin el OK explícito del usuario cada vez; instalar en el teléfono para probar sí está autorizado.
- Plan Spark: sin Cloud Functions ni extensiones. **Sin correo propio** (nada de Resend, Gmail ni SMTP): solo el correo de Firebase, que manda enlaces.
- Pruebas de borrado **solo con cuentas de prueba** (emuladores o cuentas desechables); nunca con Kelly Fernanda, Alejandro ni otra cuenta real. Nunca `pm clear`; no escribir contraseñas ni el PIN del teléfono.
- Gradle: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`; instalar con `.\gradlew.bat installRapido` (`ANDROID_SERIAL=78cb49`); capturar la pantalla del teléfono antes de mandar toques con adb.
- Un documento legado `pacientes/{id}` puede no traer `activo`: en reglas, `activo` ausente cuenta como verdadero (`resource.data.get('activo', true)`).
- Diseño: naranja #F97316, Inter, Esqueleto en vez de spinner, textos en español de México; la acción de borrar va en rojo con confirmación explícita.
- Sin subagentes (el usuario no los ha pedido).

## Review Focus
1. **Fallo a la mitad del borrado:** reintentar `POST /cuenta/eliminar` termina el trabajo y no falla por lo que ya se borró. Prueba en Tarea 9.
2. **Cambiar de nutriólogo es todo o nada:** si el código es inválido o el lote falla, el vínculo anterior sigue activo y el código sin usar. Pruebas en Tareas 1 y 2.
3. **Un vínculo cerrado no se puede escribir** (chat, metas, expediente, planes, mensajes) por nadie, ni con la app vieja. Prueba en Tarea 1.
4. **Token de sesión viejo:** `auth_time` de más de 5 minutos se rechaza con 401 y no borra nada. Prueba en Tarea 9.
5. **Citas al cerrar o borrar:** no quedan horas ocupadas ni candados `citasActivas` de citas ya canceladas. Pruebas en Tareas 3 y 9.

---

## Parte 1 — Vínculo (sin depender del enlace de Firebase)

### Tarea 1: reglas de Firestore para cerrar vínculos
**Files:** modify `firebase/firestore.rules` (regla de `pacientes`, `pacientes/*/mensajes`, `pacientes/*/planes`); create `firebase/test/vinculos.mjs`.
**Interfaces — Produces:** campos nuevos de `pacientes/{id}`: `cerradoEn` (int, ms), `cerradoPor` (`'PACIENTE' | 'NUTRIOLOGO' | 'CUENTA_ELIMINADA'`); alta con `expediente` opcional.

- [ ] **Paso 1: escribir la prueba (RED).** `firebase/test/vinculos.mjs`, con los mismos ayudantes (`cuenta`, `commit`, `pedir`, `doc`) que `firebase/test/citas.mjs` y fixtures de un nutriólogo, un paciente, un ajeno y dos registros (`pacientes/r1` activo, `pacientes/r2` cerrado con `activo:false`). Casos y resultado esperado (200 = permitido, 403 = denegado):
  - el paciente cierra su registro (`activo:false, cerradoEn, cerradoPor:'PACIENTE', ultimaModificacion`) → 200;
  - el nutriólogo cierra el suyo con `cerradoPor:'NUTRIOLOGO'` → 200;
  - cada uno firmando con el `cerradoPor` del otro → 403; un ajeno → 403;
  - cerrar tocando otro campo (`nombre`) → 403; **reabrir** (`activo:true` sobre uno cerrado) → 403;
  - en `r2` (cerrado): escribir `expediente`, `metas`, `ultimoMensaje`, crear un `mensaje` y crear un `plan` → todos 403; **leer** el registro, un mensaje y un plan, los dos dueños → 200;
  - un registro sin campo `activo` sigue aceptando `expediente` (legado) → 200;
  - alta del paciente con `expediente` válido (`actualizadoPor:'PACIENTE'`) en el lote del canje → 200; con `actualizadoPor:'NUTRIOLOGO'` → 403;
  - **cambio de nutriólogo como un solo lote** (cerrar `r1` + marcar el código usado + crear el registro nuevo): con un código bueno pasa entero; con un código ya usado el lote entero se rechaza y `r1` sigue `activo` (leerlo después).
- [ ] **Paso 2: correr y ver que falla.** Emuladores: `firebase emulators:start --only auth,firestore --project nutrigajo` (con un `firebase.json` en una carpeta de trabajo que apunte a `firebase/firestore.rules`; JBR en el PATH) y `node firebase/test/vinculos.mjs`. Esperado: fallan los casos de cierre y de escritura sobre cerrado.
- [ ] **Paso 3: implementar las reglas.** (a) `allow update` nuevo de `pacientes`: `resource.data.get('activo', true) == true && request.resource.data.activo == false`, `affectedKeys().hasOnly(['activo','cerradoEn','cerradoPor','ultimaModificacion'])`, `cerradoEn is int`, y `usuarioId == auth.uid` con `cerradoPor == 'PACIENTE'` o `nutriologoId == auth.uid` con `'NUTRIOLOGO'`. (b) Agregar `resource.data.get('activo', true) == true` a las tres reglas de `update` existentes (expediente, último mensaje, metas). (c) Agregar `get(pacientes/$(pacienteId)).data.get('activo', true) == true` al `create` de `mensajes` y de `planes`. (d) `create` de `pacientes`: sumar `'expediente'` a `hasOnly` y `(!('expediente' in request.resource.data) || (expedienteValido(request.resource.data.expediente) && request.resource.data.expediente.actualizadoPor == 'PACIENTE'))`.
- [ ] **Paso 4: pasar y no romper lo anterior.** `node firebase/test/vinculos.mjs`, y re-correr `chat.mjs`, `metas.mjs`, `citas.mjs`, `comentarios.mjs`, `platillos.mjs`, `ejercicios.mjs` contra los mismos emuladores. Esperado: todas pasan.

### Tarea 2: modelo y repositorio del vínculo en la app
**Files:** modify `domain/model/Vinculacion.kt`, `data/repository/VinculacionRepository.kt`; create `app/src/test/.../domain/model/VinculacionCierreTest.kt`.
**Interfaces — Produces:**
```kotlin
enum class CerradoPor { PACIENTE, NUTRIOLOGO, CUENTA_ELIMINADA }   // Vinculacion.kt; no renombrar: Firestore guarda el nombre
internal fun cierreAMapa(por: CerradoPor, ahora: Long): Map<String, Any>    // activo=false, cerradoEn, cerradoPor (name), ultimaModificacion
internal fun nuevoRegistroAMapa(...)   // el mapa de `canjear`, extraído para reutilizarlo; mismas claves que hoy
class VinculacionRepository {
    suspend fun cerrarVinculo(pacienteId: String, por: CerradoPor, citasACerrar: List<Cita>)    // un lote (ver Tarea 3); lo usan el paciente y el nutriólogo
    suspend fun cambiarNutriologo(codigoEscrito: String, paciente: Perfil, anterior: Vinculo, compartirExpediente: Boolean, citasACerrar: List<Cita>): Vinculo
}
```
- [ ] **Paso 1: pruebas (RED).** `VinculacionCierreTest`: `cierreAMapa(PACIENTE, 5L)` es exactamente `{activo=false, cerradoEn=5, cerradoPor="PACIENTE", ultimaModificacion=5}`; `nuevoRegistroAMapa` conserva las claves del alta actual (`nutriologoId, nutriologoNombre, usuarioId, nombre, correoVinculacion (minúsculas), codigoVinculacion, activo=true, fechaAlta, ultimaModificacion`) y suma `expediente` solo si se pasa uno.
- [ ] **Paso 2: correr y ver que falla.** `.\gradlew.bat testDebugUnitTest --tests "*VinculacionCierreTest*"`. Esperado: no compila / falla (funciones no existen).
- [ ] **Paso 3: implementar.** (Auditoría hecha al planear: los únicos que consultan `pacientes` por usuario o por nutriólogo son `VinculacionRepository.pacientes()` y `vinculoActual()`; `ChatRepository`, `ExpedienteRepository`, `PlanesRepository` y `MetasRepository` trabajan por el id del vínculo activo y las reglas bloquean sus escrituras sobre uno cerrado.) Extraer `nuevoRegistroAMapa` de `canjear`; `vinculoActual()` y `pacientes()` agregan `.whereEqualTo("activo", true)` (los documentos legados sin `activo` no existen: el alta siempre lo escribió); `cerrarVinculo` = un `runBatch` con `lote.update(pacientes/{id}, cierreAMapa(...))` más las cancelaciones de la Tarea 3; `cambiarNutriologo` valida el código **antes** (mismas excepciones `VinculoException` que `canjear`) y luego un solo `runBatch`: cierra el anterior (`PACIENTE`), marca el código usado (`usado, canjeadoPor, canjeadoEn`), crea el registro nuevo y, si `compartirExpediente`, lee antes el `expediente` del anterior (`ExpedienteRepository.leer`) y lo escribe en el nuevo con `actualizadoPor = PACIENTE`; más las cancelaciones de las citas con el anterior.
- [ ] **Paso 4: GREEN.** Misma orden; luego `.\gradlew.bat testDebugUnitTest` completa. Esperado: todo pasa (hoy 235).

### Tarea 3: qué pasa con las citas al cerrar
**Files:** modify `data/repository/CitasRepository.kt`, `domain/model/Cita.kt`; create `CitasAlCerrarTest.kt`.
**Interfaces — Produces:**
```kotlin
// Lo que le pasa a esa cita cuando `por` cierra el vínculo: null si ya no cuenta (pasada o ya cerrada).
fun estadoAlCerrar(cita: Cita, por: Rol, ahora: Long): EstadoCita?
// Las citas vigentes entre el nutriólogo y el paciente, para cerrarlas con el vínculo.
fun citasVigentesEntre(citas: List<Cita>, nutriologoId: String, usuarioId: String, ahora: Long): List<Cita>
// Escribe en `lote` el cambio de estado de la cita y libera su hora y su candado; es lo que hace hoy `cambiarEstado`, extraído.
internal fun escribirCambioDeEstado(db: FirebaseFirestore, lote: WriteBatch, cita: Cita, estado: EstadoCita)
```
- [ ] **Paso 1: pruebas (RED).** `estadoAlCerrar`: paciente + PENDIENTE → `CANCELADA`; paciente + CONFIRMADA → `CANCELADA`; nutriólogo + PENDIENTE → `RECHAZADA`; nutriólogo + CONFIRMADA → `CANCELADA`; cita con `inicio <= ahora` → `null`; ya `RECHAZADA` o `CANCELADA` → `null`. `citasVigentesEntre` descarta citas de otro par y las no vigentes. Un caso por cada fila.
- [ ] **Paso 2: correr y ver que falla.** `--tests "*CitasAlCerrarTest*"`.
- [ ] **Paso 3: implementar** y hacer que `cambiarEstado` use `escribirCambioDeEstado` (mismo comportamiento: ya existe `liberaCandado`). `cerrarVinculo` y `cambiarNutriologo` (Tarea 2) llaman a `escribirCambioDeEstado` por cada cita de `citasACerrar`, **dentro del mismo lote**.
- [ ] **Paso 4: GREEN** con la suite completa.

### Tarea 4: Worker — consultas/borrados en Firestore y aviso de baja
**Files:** modify `backend/src/lib/firestore.ts`, `backend/src/lib/aviso.ts`, `backend/src/index.ts`, `backend/package.json`; create `backend/src/routes/vinculo.ts`, `backend/test/vinculos.mjs`.
**Interfaces — Produces:**
```ts
// lib/firestore.ts
export async function consultar(env: Env, token: string, coleccion: string, campo: string, valor: string): Promise<{ ruta: string; doc: Documento }[]>  // runQuery, filtro de igualdad; sin resultados → []
export async function borrar(env: Env, token: string, ruta: string): Promise<void>                 // DELETE; 404 cuenta como hecho
export async function borrarColeccion(env: Env, token: string, ruta: string): Promise<void>        // lista los documentos de una subcolección y los borra
// lib/aviso.ts
export function avisoDeBaja(quien: string, por: "PACIENTE" | "NUTRIOLOGO" | "CUENTA_ELIMINADA"): { titulo: string; cuerpo: string }
// routes/vinculo.ts
export async function avisarBaja(req: Request, env: Env, uid: string): Promise<Response>   // POST /vinculos/notificar { pacienteId }
```
- [ ] **Paso 1: pruebas (RED).** `backend/test/vinculos.mjs` con el patrón de `test/aviso.mjs` (esbuild + `fetch` falso de Google, Firestore y FCM): `avisoDeBaja("Kelly","NUTRIOLOGO")` y `("Alejandro","PACIENTE")` y `("Ana","CUENTA_ELIMINADA")` dan los textos de abajo; la ruta responde 400 sin `pacienteId` o con ruta en el id, 404 si no existe, 403 si quien pide no es una de las dos partes, 409 si el registro sigue `activo`, `{enviado:false, motivo:"ya avisado"}` si trae `notificadaEn`, y con todo en regla manda **un** push a la **otra** parte (el `fcmToken` sale de `usuarios/{otro}`, nunca de la petición), marca `notificadaEn` y, con FCM 404, borra `fcmToken`. Textos: paciente cierra → «{nombre} dejó de ser tu paciente.»; nutriólogo cierra o cuenta eliminada del nutriólogo → «{nombre} cerró el vínculo contigo.»; cuenta eliminada del paciente → «{nombre} eliminó su cuenta.».
- [ ] **Paso 2: correr y ver que falla.** `cd backend; npm run test:vinculos` (agregar el script `node test/vinculos.mjs`). Esperado: falla por módulos inexistentes.
- [ ] **Paso 3: implementar.** `consultar` usa `POST .../documents:runQuery` con `structuredQuery` `fieldFilter EQUAL`; `borrarColeccion` usa `GET` de la colección con paginación y `borrar` por documento. `avisarBaja` sigue el patrón de `routes/cita.ts` (verifica en Firestore, no en la app) y se registra en `index.ts` como `POST /vinculos/notificar` con `verificarSesion`. Usar el canal `citas` ya existente salvo que `Avisos.crearCanales` ofrezca uno mejor (decisión del ejecutor).
- [ ] **Paso 4: GREEN.** `npm run test:vinculos`, `npm run check` (tsc) y `npm run test:aviso`. Esperado: pasan.

### Tarea 5: pantallas del vínculo en la app
**Files:** modify `ui/shared/ajustes/AjustesFlujo.kt` (nuevo `Ajuste.MI_NUTRIOLOGO`), `ui/shared/perfil/PerfilScreen.kt` (fila solo para PACIENTE con vínculo), `ui/paciente/inicio/VinculoViewModel.kt`, `ui/nutriologo/...` (expediente del paciente: «Dar de baja»), `res/values/strings.xml`; create `ui/shared/ajustes/MiNutriologoScreen.kt`, `data/repository/AvisoVinculoRepository.kt` (igual que `AvisoCitaRepository`, `POST /vinculos/notificar`).
**Interfaces — Produces:** `VinculoViewModel.desvincular()`, `VinculoViewModel.cambiar(codigo: String, compartirExpediente: Boolean)`; `fun MiNutriologoScreen(perfil: Perfil, vinculo: Vinculo, onVolver: () -> Unit, espacioInferior: Dp, ...)`.
- [ ] **Paso 1: lógica del ViewModel con prueba.** `VinculoViewModelCierreTest` (JVM, con un repositorio falso si el ViewModel ya admite inyección; si no, mover la decisión a una función pura): tras `desvincular()` correcto, `vinculo` pasa a `null`; si el repositorio falla, `vinculo` sigue igual y hay `error`; `cambiar` con código inválido deja el vínculo como estaba y muestra el error de `VinculoException`.
- [ ] **Paso 2: pantalla «Mi nutriólogo».** Dos acciones, «Cambiar de nutriólogo» y «Desvincularme». Cambiar: casilla del código de 4 dígitos (`CampoCodigo`), resumen «Dejarás a {actual} y pasarás con {nuevo}», casilla «Compartir mi expediente con el nuevo nutriólogo» (**apagada**) y botón de confirmar; todo con Esqueleto al cargar y diálogo de confirmación en rojo para «Desvincularme». Tras cerrar, avisar con `AvisoVinculoRepository` (si falla, no se nota) y refrescar `VinculoViewModel` para que aparezca la pantalla de «Vincular».
- [ ] **Paso 3: «Dar de baja» del nutriólogo** en el expediente del paciente, con diálogo de confirmación; cierra con `CerradoPor.NUTRIOLOGO`, la lista de pacientes se refresca (filtra `activo`).
- [ ] **Paso 4: verificación.** Suite completa + `installRapido`; en el teléfono, **con cuentas de prueba** (no Kelly/Alejandro): desvincular y volver a vincular; cambiar con y sin la casilla del expediente (comprobar que sin ella el expediente nuevo está vacío). Capturar antes de cada toque.

### Tarea 6: lo cerrado se puede ver (solo lectura)
**Files:** create `ui/paciente/planes/PlanesAnterioresScreen.kt`, `ui/nutriologo/pacientes/PacientesAnterioresScreen.kt`; modify `VinculacionRepository` (`vinculosCerrados(): List<Vinculo>` por `usuarioId`/`nutriologoId` con `activo == false`), `PlanesRepository` (listar planes de un vínculo cerrado), strings.
- [ ] **Paso 1: prueba (RED)** de la lógica pura que arma las filas (`filasAnteriores(registros)`: más reciente primero, con nombre y fecha de cierre; los cerrados por cuenta eliminada se rotulan «Cuenta eliminada»).
- [ ] **Paso 2: implementar** las dos listas, sin botones de escritura (ni chat, ni metas, ni nuevo plan); el paciente abre un plan anterior con el visor de PDF existente.
- [ ] **Paso 3: verificar** con la suite y en el teléfono con cuentas de prueba.

---

## Parte 2 — Eliminar la cuenta

### Tarea 7: prueba corta del enlace de Firebase («spike», puerta)
**No es código del producto.** Nada de esto se conserva si falla; no bloquea la Parte 1.
- [ ] **Paso 1: pedir al usuario** que active «Enlace de correo (inicio de sesión sin contraseña)» en la consola de Authentication y que cree una cuenta desechable con un correo suyo (por ejemplo un alias `+prueba`); yo no cambio la consola ni creo cuentas.
- [ ] **Paso 2: en una rama de trabajo del árbol (sin conservar),** una pantalla de depuración con tres botones: pedir enlace (`sendSignInLinkToEmail` con `ActionCodeSettings` sobre el dominio de Hosting; ver la guía de migración de Dynamic Links), pegar enlace, y reautenticar (`EmailAuthProvider.getCredentialWithLink(correo, enlace)` + `reauthenticateWithCredential`), y mostrar el `auth_time` del token antes y después de reautenticar.
- [ ] **Paso 3: criterios.** (a) el correo llega; (b) el enlace abre la app o se puede pegar; (c) `auth_time` sube tras reautenticar; (d) con una cuenta de correo+contraseña, completar el enlace **no** crea una cuenta nueva ni pide contraseña. Anotar el resultado y la configuración final (`ActionCodeSettings`, filtros de enlaces de `AndroidManifest.xml`, ajustes de Hosting) en el ledger.
- [ ] **Paso 4: decisión.** Si falla alguna de (a)–(c), **parar la Parte 2 y volver con el usuario**; no proponer correo por cuenta propia. Quitar la pantalla de depuración.

### Tarea 8: confirmación por enlace en la app
**Files:** modify `data/repository/AuthRepository.kt`, `AndroidManifest.xml`, `MainActivity.kt` (recibir el enlace), `ui/shared/ajustes/AjustesFlujo.kt` (nuevo `Ajuste.ELIMINAR_CUENTA`), `PerfilScreen.kt` (fila para todos menos ADMIN), strings; create `ui/shared/ajustes/EliminarCuentaScreen.kt`, `domain/EnlaceDeConfirmacion.kt`, su prueba.
**Interfaces — Produces:**
```kotlin
// AuthRepository
suspend fun enviarEnlaceDeConfirmacion()          // al correo de la cuenta actual
suspend fun confirmarConEnlace(enlace: String)    // reautentica; lanza si el enlace no es de esta cuenta o venció
// domain/EnlaceDeConfirmacion.kt
fun extraerEnlace(textoPegado: String): String?   // el primer https://... del texto, sin espacios; null si no hay
```
- [ ] **Paso 1: pruebas (RED).** `extraerEnlace`: devuelve el enlace de un texto con saltos de línea y espacios alrededor; `null` para texto sin `https://`; no corta un enlace con `&` ni con `%`.
- [ ] **Paso 2: correr y ver que falla**, luego **implementar** `extraerEnlace` y las dos funciones de `AuthRepository` (la configuración sale de la Tarea 7).
- [ ] **Paso 3: pantalla «Eliminar mi cuenta»** con los pasos 1–4 del spec: aviso por rol (al nutriólogo, cuántos pacientes tiene y que cada vínculo se cierra), «Enviarme el enlace de confirmación» (con espera de reenvío), casilla «Pega aquí el enlace del correo» como plan B, y «Eliminar definitivamente» en rojo solo tras reautenticar. `MainActivity` pasa el enlace entrante a la pantalla.
- [ ] **Paso 4: verificar** con la suite y en el teléfono **hasta antes de borrar** (el último botón se deja sin pulsar en esta tarea).

### Tarea 9: Worker — `POST /cuenta/eliminar`
**Files:** modify `backend/src/auth/firebase.ts`, `backend/src/lib/firestore.ts`, `backend/src/index.ts`, `backend/package.json`; create `backend/src/routes/cuenta.ts`, `backend/src/lib/borrado.ts`, `backend/test/cuenta.mjs`.
**Interfaces — Produces:**
```ts
// auth/firebase.ts
export async function verificarSesionConHora(req: Request, proyecto: string): Promise<{ uid: string; authTime: number } | null>
// lib/borrado.ts (puro, sin red)
export const VENTANA_SEGUNDOS = 300;
export function esReciente(authTime: number, ahoraSeg: number): boolean
export type Rol = "PACIENTE" | "NUTRIOLOGO" | "ENTRENADOR" | "ADMIN";
export function pasosDeBorrado(rol: Rol): string[]   // nombres de los pasos, en orden; ADMIN → lanza
// routes/cuenta.ts
export async function eliminarCuenta(req: Request, env: Env, sesion: { uid: string; authTime: number }): Promise<Response>
// lib/firestore.ts
export async function borrarCuentaDeAuth(env: Env, token: string, uid: string): Promise<void>   // POST identitytoolkit accounts:delete {localId}; usuario inexistente cuenta como hecho
```
- [ ] **Paso 1: pruebas (RED).** `backend/test/cuenta.mjs` (esbuild + `fetch` falso, como `test/aviso.mjs`):
  - `esReciente`: 299 s sí, 301 s no; `authTime` en el futuro lejano no.
  - `pasosDeBorrado("PACIENTE")` termina en `["usuario","auth"]` y empieza por cerrar vínculos; `("NUTRIOLOGO")` incluye consultorio, códigos, verificación, cédula y autor; `("ENTRENADOR")` incluye autor; `("ADMIN")` lanza.
  - Ruta: 401 si `authTime` es viejo y **no** hay ninguna llamada de borrado; 403 si el rol es ADMIN; 503 sin `FIREBASE_SERVICE_ACCOUNT`; **paciente** feliz: sus registros quedan `activo:false`, `cerradoPor:"CUENTA_ELIMINADA"`, sin `usuarioId`, `correoVinculacion` ni `codigoVinculacion`; se borran sus citas, su `citasActivas`, sus comentarios, `usuarios/{uid}` con subcolecciones y avatar y portada de R2, y **por último** la cuenta de Auth (comprobar el orden de las llamadas); **nutriólogo**: vínculos de sus pacientes cerrados, citas pendientes/confirmadas canceladas y sus `ocupados` liberados, `directorio/{uid}` y `ocupados`, códigos sin usar, `verificaciones/{uid}`, `cedulas/{numero}` liberada; **autor**: borradores borrados (con sus archivos de R2), publicados con `autorNombre:"Cuenta eliminada"` y `autorId` intacto.
  - **Idempotencia:** fallar la llamada de borrar la cuenta de Auth (500) responde 502 y deja `usuarios/{uid}` existiendo; repetir la petición con todo ya borrado responde 200.
- [ ] **Paso 2: correr y ver que falla.** `npm run test:cuenta` (agregar el script).
- [ ] **Paso 3: implementar.** `verificarSesionConHora` devuelve también `payload.auth_time`; `eliminarCuenta` lee `usuarios/{uid}` para el rol, ejecuta `pasosDeBorrado(rol)` con los ayudantes de la Tarea 4 y de `env.FOTOS.delete([...])`, y responde `{ borrado: true }`. Las claves de R2 de avatar y portada son `avatares/{uid}` y `portadas/{uid}` (`routes/foto.ts`); las de ejercicios y platillos salen de `routes/fotoBanco.ts` y `routes/clipBanco.ts`. Avisa con `avisoDeBaja` (Tarea 4) a cada parte afectada, sin que un fallo del aviso detenga el borrado.
- [ ] **Paso 4: GREEN.** `npm run test:cuenta`, `npm run test:vinculos`, `npm run test:aviso`, `npm run check`.

### Tarea 10: conectar la app con el Worker
**Files:** create `data/repository/CuentaRepository.kt`; modify `EliminarCuentaScreen.kt`, `ui/shared/auth/AuthViewModel.kt`.
**Interfaces — Produces:** `class CuentaRepository { suspend fun eliminar() }` (`POST /cuenta/eliminar` con el ID token **recién obtenido** `getIdToken(true)`, como `AvisoCitaRepository`; lanza `IOException` si el Worker no responde 200; con 401, un error que la pantalla traduce a «Vuelve a confirmar con el correo»).
- [ ] **Paso 1: prueba de la traducción de errores** (`EliminarCuentaErrorTest`): 401 → volver al paso del enlace; 5xx o sin red → «No se pudo eliminar, intenta de nuevo» (la cuenta sigue entrando).
- [ ] **Paso 2: implementar** `CuentaRepository.eliminar` y el botón «Eliminar definitivamente»: al éxito, `AuthViewModel` cierra sesión (`dispositivo.olvidar()` ya no aplica: el perfil no existe; saltarlo), limpia el estado local y vuelve al inicio de sesión.
- [ ] **Paso 3: prueba real en producción, solo con el OK del usuario y una cuenta desechable** (la que usó en la Tarea 7): crear datos mínimos con esa cuenta, borrarla, y comprobar a mano en la consola que Auth y `usuarios/{uid}` ya no existen y que un vínculo cerrado conserva su `nombre`. **No se despliega nada sin pedirlo.**

### Tarea 11: cierre
- [ ] **Paso 1: suite completa:** `.\gradlew.bat testDebugUnitTest`, `cd backend; npm run check; npm run test:vinculos; npm run test:cuenta; npm run test:aviso`, y las pruebas de reglas contra los emuladores (`vinculos.mjs`, `chat.mjs`, `metas.mjs`, `citas.mjs`). Anotar cuántas pasan.
- [ ] **Paso 2: memoria y ledger.** Crear la memoria `cuenta-y-vinculo.md` (qué quedó hecho, qué se probó y dónde, qué falta publicar) y su renglón en `MEMORY.md`.
- [ ] **Paso 3: lista de publicación para el usuario** (nada se publica sin su OK, uno por uno): (1) reglas de Firestore con el CLI (`firebase deploy --only firestore:rules`, `--dry-run` antes); (2) `wrangler deploy` del Worker; (3) la app nueva. **Orden:** reglas y Worker antes que la app nueva; con las reglas nuevas la app vieja sigue funcionando (el cierre es opcional), pero la app nueva sin reglas no puede cerrar ni cambiar.
