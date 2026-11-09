# Citas que crea el nutriólogo — plan de implementación

> **Para quien ejecute:** usar superpowers:executing-plans (o subagent-driven-development). Los pasos llevan casillas `- [ ]`.

**Meta:** que un nutriólogo verificado agende a un paciente suyo y que el paciente se entere (notificación y fila en su inicio).

**Arquitectura:** se reutiliza `citas/{id}` y la reserva `directorio/{n}/ocupados/{inicio}`. La cita del nutriólogo nace `CONFIRMADA` con tres
campos nuevos (`origen`, `pacienteId`, `nota`) y no usa el candado `citasActivas`. El aviso sigue siendo `POST /citas/notificar` del Worker,
que ahora distingue «agendada», «confirmada» y «cancelada» por el estado y el origen de la cita.

**Tecnología:** Kotlin y Compose (Material 3), Firestore, Cloudflare Worker en TypeScript, pruebas de reglas con los emuladores (`firebase/test/*.mjs`).

**Diseño:** `docs/superpowers/specs/2026-11-05-citas-del-nutriologo-design.md` (aprobado el 5 nov 2026). Léelo antes de empezar.

## Restricciones globales

- Horario fijo de `Agenda.kt`: lunes a sábado, citas de una hora, de 9:00 a 17:00. Ventana: 14 días para el paciente, **30** para el nutriólogo.
- `inicio` en milisegundos, múltiplo de 900000, futuro y a menos de 5184000000 ms (60 días): lo de las reglas de hoy.
- `nota`: opcional, de **120** caracteres como máximo, recortada; si queda vacía no se guarda.
- `origen`: `PACIENTE` (ausente en las citas de antes) o `NUTRIOLOGO`. No renombrar valores: Firestore guarda el nombre del enum.
- Aviso: canal `citas`; `tipo` `CITA_AGENDADA` y `CITA_CANCELADA`. Textos exactos en la tarea 3.
- Reglas de Firestore y Worker **no se despliegan** sin que el usuario lo diga. Nada se confirma en git hasta que lo pida; nunca `push`.
- Idioma de la app y de los comentarios: español. No tocar `liquidglass/`, `sample/` ni los archivos de Gradle que ya están sin confirmar.

## Qué mirar además de lo que dice el diseño

1. **Dos personas piden la misma hora** (el nutriólogo agenda mientras un paciente pide lo mismo): las reglas rechazan la segunda; la hoja dice «Esa hora ya no está libre» y vuelve a leer las horas. → tareas 2 y 5.
2. **Citas de antes, sin `origen`:** se leen como del paciente y el aviso sigue diciendo «confirmó tu cita». → tareas 1 y 3.
3. **Paciente sin dispositivo registrado, o FCM falla:** la cita queda y al nutriólogo se le avisa en pantalla. → tareas 3 y 5.
4. **Nota vacía, de solo espacios o de más de 120:** se recorta y no se manda vacía. → tareas 1 y 2.
5. **Una cita cancelada o que ya pasó no es «Próxima cita»;** y una cita que se mueve y se vuelve a confirmar avisa otra vez. → tareas 1 y 3.

---

## Tarea 1: Modelo y mapeo de datos

**Archivos:**
- Modificar: `app/src/main/java/com/example/nutrigajo/domain/model/Cita.kt`
- Modificar: `app/src/main/java/com/example/nutrigajo/data/repository/CitasRepository.kt` (solo el mapeo; `agendarComoNutriologo` es de la tarea 4)
- Crear: `app/src/test/java/com/example/nutrigajo/domain/model/CitaTest.kt`, `app/src/test/java/com/example/nutrigajo/data/repository/CitasMapaTest.kt`

**Interfaces — produce:**
- `enum class OrigenCita { PACIENTE, NUTRIOLOGO }`
- `Cita` suma `val origen: OrigenCita = OrigenCita.PACIENTE`, `val pacienteId: String? = null`, `val nota: String = ""` (al final, con valor por defecto: nada de lo que ya crea una `Cita` cambia).
- `const val NOTA_MAX = 120` (en `Cita.kt`) y `fun List<Cita>.proxima(ahora: Long): Cita?` (la vigente de `inicio` más cercano).
- En `CitasRepository.kt`: `internal fun citaDeMapa(id: String, d: Map<String, Any?>): Cita?` (null si falta `nutriologoId`, `usuarioId`, `inicio` o el `estado` no es un `EstadoCita`; `origen` desconocido o ausente = `PACIENTE`) y `internal fun citaDelNutriologoAMapa(nutriologoId: String, nutriologoNombre: String, paciente: PacienteResumen, inicio: Long, nota: String, ahora: Long): Map<String, Any>`. `DocumentSnapshot.aCita()` pasa a usar `citaDeMapa(id, data.orEmpty())`.

- [ ] **Paso 1: escribir las pruebas que fallan.** `CitaTest`: `proxima_es_la_vigente_mas_cercana_y_ignora_canceladas_rechazadas_y_pasadas` (con `ahora` fijo: una confirmada ya pasada, una cancelada y una rechazada futuras, una pendiente a +2 h y una confirmada a +1 h → devuelve la de +1 h; lista vacía o todas cerradas → `null`). `CitasMapaTest`: `una_cita_de_antes_sin_origen_se_lee_como_del_paciente` (los 8 campos de hoy → `origen == PACIENTE`, `pacienteId == null`, `nota == ""`); `citaDeMapa_devuelve_null_con_un_campo_obligatorio_ausente_o_un_estado_desconocido`; `la_cita_del_nutriologo_nace_confirmada_con_origen_y_paciente` (las claves son exactamente `nutriologoId, nutriologoNombre, usuarioId, pacienteNombre, pacienteId, inicio, estado, origen, creadaEn, actualizadaEn` más `nota` si hay; `estado == "CONFIRMADA"`, `origen == "NUTRIOLOGO"`, `usuarioId == paciente.usuarioId`, `pacienteId == paciente.id`, `creadaEn == actualizadaEn == ahora`); `la_nota_se_recorta_y_si_queda_vacia_no_se_manda` (`"  Trae tus análisis  "` → `"Trae tus análisis"`; `"   "` → sin la clave `nota`); `la_nota_se_corta_a_120_caracteres`.
- [ ] **Paso 2: verlas fallar.** `./gradlew :app:testDebugUnitTest --tests "*CitaTest" --tests "*CitasMapaTest"` (con `JAVA_HOME` del JBR de Android Studio). Esperado: no compila (símbolos sin definir).
- [ ] **Paso 3: implementar** lo de «Interfaces». `pacienteNombre` sale de `paciente.nombre`.
- [ ] **Paso 4: verlas pasar** con el mismo comando, y la suite entera (`:app:testDebugUnitTest`) para ver que no se rompió nada.

---

## Tarea 2: Reglas de Firestore y su prueba

**Archivos:**
- Modificar: `firebase/firestore.rules` (`match /citas/{citaId}` y `match /directorio/{nutriologoId}/ocupados/{inicio}`)
- Crear: `firebase/test/citas.mjs` (mismo arnés que `firebase/test/platillos.mjs`: cuentas con el emulador de Auth, perfiles y `directorio`/`pacientes` sembrados como `owner`, lotes por REST)

**Interfaces — consume:** `rolDe(uid)`, `citaRef`, `directorioRef`, `textoOpcional(datos, campo, limite)` y `candadoLibre`, que ya están en el archivo.

- [ ] **Paso 1: escribir `citas.mjs`** con estos casos (cada uno es un `assert.equal(status, 200|403)` sobre un lote de dos escrituras: la cita y su `ocupados`, que es como los crea la app):
  1. el nutriólogo (con `directorio`) agenda a su paciente (`pacientes/{id}` con su `nutriologoId` y el `usuarioId` del paciente) → 200;
  2. a un paciente de otro nutriólogo → 403; con un `pacienteNombre` que no es el de `pacientes/{id}` → 403;
  3. la misma hora otra vez → 403 (el `ocupados` ya existe);
  4. un paciente crea una cita ya `CONFIRMADA`, o con `origen = "NUTRIOLOGO"` → 403; y su cita normal `PENDIENTE` con candado sigue → 200 (sin regresión);
  5. un nutriólogo sin `directorio` → 403; sin el `ocupados` en el lote → 403;
  6. `nota` de 120 caracteres → 200 y de 121 → 403; `inicio` pasado, a más de 60 días o no múltiplo de 15 min → 403; `estado` distinto de `CONFIRMADA` → 403;
  7. el nutriólogo cancela la cita confirmada y libera la hora en el mismo lote → 200; el paciente cancela la suya (también libera) → 200; el paciente la reagenda → 200 y queda `PENDIENTE` (comportamiento de hoy).
- [ ] **Paso 2: verlo fallar.** Levantar los emuladores como dice la cabecera de `platillos.mjs` (`firebase emulators:start --only auth,firestore --project nutrigajo`, con `firestore.rules` cargado) y correr `node firebase/test/citas.mjs`. Esperado: fallan los casos 1, 6 (nota de 120) y 7 (cancelar la del nutriólogo).
- [ ] **Paso 3: reglas.** Añadir un **segundo** `allow create` en `citas` para el nutriólogo: `rolDe(auth.uid) == 'NUTRIOLOGO'`; claves exactamente `hasOnly` las diez de la tarea 1 (`nota` opcional) y `hasAll` las nueve obligatorias; `nutriologoId == auth.uid` y `nutriologoNombre == get(directorioRef(auth.uid)).data.nombre`; el paciente sale de `get(/pacientes/$(request.resource.data.pacienteId))` con `nutriologoId == auth.uid`, `usuarioId == request.resource.data.usuarioId` y `nombre == pacienteNombre`; `estado == 'CONFIRMADA'`, `origen == 'NUTRIOLOGO'`; `inicio` con las mismas condiciones de hoy; `creadaEn` y `actualizadaEn` enteros; `existsAfter` del `ocupados` de esa hora y su `citaId == citaId` (con `getAfter`); `textoOpcional(request.resource.data, 'nota', 120)`. Sin candado. En `ocupados` `create`, volver la condición de quién crea una alternativa: la del paciente (la de hoy, intacta) **o** la del nutriólogo (`rolDe == 'NUTRIOLOGO'`, `nutriologoId == request.auth.uid`, la cita del lote no existe todavía, es de ese nutriólogo, de `origen` `NUTRIOLOGO`, `CONFIRMADA` y con ese `inicio`). La rama del paciente no cambia: nunca puede mandar `origen`.
- [ ] **Paso 4: verlo pasar**, y correr también `ejercicios.mjs`, `platillos.mjs`, `metas.mjs`, `chat.mjs`, `entrenamientos.mjs`, `carreras.mjs` y `comentarios.mjs` contra las reglas nuevas (ninguna debe cambiar).

---

## Tarea 3: Worker — texto y avisos

**Archivos:**
- Modificar: `backend/src/lib/aviso.ts`, `backend/src/routes/cita.ts`
- Modificar: `backend/test/aviso.mjs`

**Interfaces — produce (`aviso.ts`):**
- `avisoDeAgendada(nutriologo: string, inicio: number, zona: string, direccion?: string): { titulo: string; cuerpo: string }` → título `Cita agendada`, cuerpo `«{nutriologo} te agendó una cita: {cuando}.»` más ` Consultorio: {direccion}.` si hay dirección.
- `avisoDeCancelacion(nutriologo: string, inicio: number, zona: string): { titulo: string; cuerpo: string }` → título `Cita cancelada`, cuerpo `«{nutriologo} canceló tu cita del {cuando}.»`

**Comportamiento de `avisarConfirmacion` (`cita.ts`):** sigue exigiendo que la cita sea de quien pide. Según la cita: `CONFIRMADA` con `origen = NUTRIOLOGO` → `avisoDeAgendada` y `tipo: "CITA_AGENDADA"`; `CONFIRMADA` sin ese origen → lo de hoy (`avisoDeConfirmacion`, `CITA_CONFIRMADA`); `CANCELADA` → `avisoDeCancelacion` y `CITA_CANCELADA`; cualquier otro estado → 409 (`"La cita no está confirmada ni cancelada"`). «Ya avisado» pasa a ser `notificadaEn >= actualizadaEn` (si falta `actualizadaEn`, vale 0): así una cita que se mueve y se vuelve a confirmar, o que se cancela después de avisar su alta, avisa otra vez, y pedir dos veces el mismo aviso no manda dos. Actualizar el comentario de cabecera de `cita.ts`.

- [ ] **Paso 1: pruebas que fallan** en `aviso.mjs`: los dos textos con `inicio` = lunes 26 de octubre 2026 16:00 UTC y zona `America/Mexico_City` (`«Lucía te agendó una cita: Lunes 26 de octubre, 10:00. Consultorio: Calle 1.»`, `«Lucía canceló tu cita del Lunes 26 de octubre, 10:00.»`); la ruta con `origen: "NUTRIOLOGO"` manda `tipo: "CITA_AGENDADA"` y el título `Cita agendada`; una cita `CANCELADA` manda `CITA_CANCELADA`; la cita de antes (sin `origen`) sigue mandando `Cita confirmada` / `CITA_CONFIRMADA`; `CANCELADA` pedida por otro nutriólogo → 403; `notificadaEn: 1` con `actualizadaEn: 2` vuelve a avisar y con `actualizadaEn: 1` dice `ya avisado`; `PENDIENTE` → 409. El caso de hoy `notificadaEn: 1` sin `actualizadaEn` sigue diciendo `ya avisado`.
- [ ] **Paso 2: verlas fallar:** en `backend/`, `npm run test:aviso`. Esperado: falla por los textos y tipos nuevos.
- [ ] **Paso 3: implementar** lo de arriba (la hora y la zona siguen saliendo del perfil del paciente, nunca del cuerpo).
- [ ] **Paso 4: verlas pasar:** `npm run test:aviso` y `npx tsc --noEmit`; correr también las demás pruebas de `backend/test` (`humo.mjs` solo si ya se corría sin red).

---

## Tarea 4: Datos del nutriólogo (agenda, repositorio, avisos)

**Archivos:**
- Modificar: `app/src/main/java/com/example/nutrigajo/domain/Agenda.kt`, `app/src/test/java/com/example/nutrigajo/domain/AgendaTest.kt`
- Modificar: `.../data/repository/CitasRepository.kt`, `.../data/repository/AvisoCitaRepository.kt`, `.../ui/shared/citas/CitasViewModel.kt`

**Interfaces — produce:**
- `Agenda.DIAS_PACIENTE = 14L` y `Agenda.DIAS_NUTRIOLOGO = 30L`; `Agenda.dias(ocupados: Set<Long>, ahora: Instant, zona: ZoneId, ventana: Long = DIAS_PACIENTE): List<LocalDate>`.
- `suspend fun CitasRepository.agendarComoNutriologo(nutriologo: Perfil, paciente: PacienteResumen, inicio: Long, nota: String): String` — un lote con `citaDelNutriologoAMapa(...)` y `ocupados/{inicio}` (`citaId`, `inicio`); devuelve el id de la cita.
- `AvisoCitaRepository.confirmada(citaId)` se renombra `avisar(citaId)` (hace la misma llamada). `CitasViewModel.cambiarEstado` avisa cuando el nuevo estado es `CONFIRMADA`, o `CANCELADA` siendo el rol `NUTRIOLOGO` (un paciente que cancela no avisa a nadie).

- [ ] **Paso 1: prueba que falla** en `AgendaTest`: `el_nutriologo_ve_una_ventana_de_30_dias` (`hoy` = 4 nov 2026, miércoles, `ahora` = 10:00: `Agenda.dias(emptySet(), ahora, zona, Agenda.DIAS_NUTRIOLOGO)` tiene 26 días —30 menos los cuatro domingos— y el último es el 3 dic 2026); la prueba de hoy (12 días) no cambia.
- [ ] **Paso 2: verla fallar** (`--tests "*AgendaTest"`); **Paso 3: implementar** los cuatro puntos de «Interfaces» (el lote por `db.runBatch`, como `agendar`); **Paso 4: verla pasar** y compilar la app entera (`:app:compileDebugKotlin`).

---

## Tarea 5: Pantallas del nutriólogo

**Archivos:**
- Crear: `ui/nutriologo/citas/NuevaCitaViewModel.kt`, `ui/nutriologo/citas/NuevaCitaSheet.kt`
- Modificar: `ui/paciente/citas/AgendarCitaSheet.kt` (`DiaChip` y `HoraChip` pasan de `private` a `internal` para reutilizarlos), `ui/shared/agenda/AgendaScreen.kt`, `ui/shared/expediente/ExpedienteFlujo.kt` y `ResumenScreen.kt`, `ui/nutriologo/inicio/InicioNutriologoScreen.kt`, `res/values/strings.xml`

**Interfaces — produce:**
- `data class NuevaCitaEstado(abierta: Boolean = false, paciente: PacienteResumen? = null, cargando: Boolean = false, ocupados: Set<Long> = emptySet(), dias: List<LocalDate> = emptyList(), dia: LocalDate? = null, hora: Long? = null, nota: String = "", yaTiene: Cita? = null, enviando: Boolean = false, agendada: Boolean = false, sinAviso: Boolean = false, @StringRes error: Int? = null)`
- `class NuevaCitaViewModel(repo: CitasRepository = CitasRepository(), avisos: AvisoCitaRepository = AvisoCitaRepository()) : ViewModel()` con `val estado: StateFlow<NuevaCitaEstado>` y `abrir(nutriologo: Perfil, paciente: PacienteResumen?)` (null: la hoja pide elegir paciente), `elegirPaciente(p)`, `elegirDia(d)`, `elegirHora(inicio)`, `escribirNota(texto)` (corta a `NOTA_MAX`), `agendar()` y `cerrar()`.
- `@Composable fun NuevaCitaSheet(estado, pacientes: List<PacienteResumen>, onPaciente, onDia, onHora, onNota, onAgendar, onCerrar)`.
- `AgendaScreen(..., onNuevaCita: (() -> Unit)? = null)`: con valor, un botón «+» junto a «Hoy». `ExpedienteFlujo(..., onCita: (() -> Unit)? = null)` (y `ResumenScreen`): con valor, la acción «Agendar cita» junto a las metas.

**Comportamiento:**
- Al abrir: horas = `repo.horasOcupadas(nutriologo.uid)`, días = `Agenda.dias(..., Agenda.DIAS_NUTRIOLOGO)`, y `yaTiene` = la primera cita vigente de `repo.citas(Rol.NUTRIOLOGO)` con el `usuarioId` de ese paciente (la hoja lo dice arriba y deja agendar otra).
- `agendar()`: `agendarComoNutriologo` y luego `avisos.avisar(id)` **sin esperarlo**; si el aviso falla o el Worker contesta `enviado: false`, `sinAviso = true` («No pudimos avisarle por notificación; la verá en su app»). Un `PERMISSION_DENIED` es casi siempre «otro tomó la hora»: `error = «Esa hora ya no está libre»` y se vuelven a leer las horas (como `AgendaViewModel`). `agendada = true` muestra la confirmación y, al cerrar, `InicioNutriologoScreen` recarga las citas.
- La hoja se pinta al final de `InicioNutriologoScreen` (como `AgendarCitaSheet` en el inicio del paciente), con `estado.pacientes` como lista; solo se ofrecen las entradas con la cuenta verificada (`aprobada`).
- Textos nuevos (español, `strings.xml`): «Nueva cita», «Agendar cita», «Paciente», «Nota (opcional)», «Agendar», «Esa hora ya no está libre», «Ya tiene una cita el %1$s», «Cita agendada», «No pudimos avisarle por notificación; la verá en su app».

- [ ] **Paso 1:** hacer `internal` los dos chips y compilar. **Paso 2:** escribir el ViewModel y la hoja. **Paso 3:** cablear las dos entradas y la hoja en `InicioNutriologoScreen`. **Paso 4:** `:app:compileDebugKotlin` sin errores ni avisos nuevos, y `:app:testDebugUnitTest` verde.

---

## Tarea 6: Pantallas del paciente

**Archivos:**
- Crear: `ui/paciente/inicio/ProximaCita.kt`, `ui/paciente/citas/CitaVista.kt`
- Modificar: `ui/paciente/inicio/InicioPacienteScreen.kt`, `MainActivity.kt`, `data/push/Avisos.kt`, `ui/shared/agenda/AgendaScreen.kt` (`DetalleCita`), `ui/shared/citas/CitasScreen.kt`, `res/values/strings.xml`

**Interfaces — produce:**
- `@Composable fun ProximaCita(cita: Cita, nueva: Boolean, onAbrir: () -> Unit, modifier: Modifier = Modifier)`: un `FilaAjuste` con `ic_calendario`, título `diaYHora(cita.inicio)` y etiqueta `«Próxima cita · {cita.nutriologoNombre}»`, con el prefijo `«Nueva · »` cuando `nueva`.
- `internal fun citaVistaId(contexto: Context, uid: String): String?` y `internal fun marcarCitaVista(contexto: Context, uid: String, citaId: String)` (SharedPreferences `nutrigajo`, clave `cita_vista_$uid`, como `vistoHasta`/`marcarVisto` de los planes).
- `Avisos.TIPO_CITA_AGENDADA = "CITA_AGENDADA"` y `Avisos.TIPO_CITA_CANCELADA = "CITA_CANCELADA"`; en `MainActivity`, un `abrirCitas` que se enciende con cualquier aviso cuyo `tipo` empiece por `CITA_` y que `InicioPacienteScreen` consume (`abrirCitas: Boolean`, `onCitasConsumido: () -> Unit`) poniendo `enCitas = true`, igual que con el aviso de un plan.

**Comportamiento:**
- `InicioPacienteScreen` pide las citas al abrir el inicio y al jalar para actualizar (`citasVm.cargar(Rol.PACIENTE)`), y le pasa a `InicioPaciente` solo `proxima: Cita?` (`citas.citas.proxima(ahora)`) y `citaNueva: Boolean` (`proxima.origen == NUTRIOLOGO && proxima.id != citaVistaId(...)`) para no recomponer el inicio por otros cambios de citas. La fila va **debajo del plan** y toca `enCitas = true`; al abrir «Mis citas» con una cita próxima, `marcarCitaVista`. Sin cita vigente la fila no aparece.
- `DetalleCita` y la tarjeta de la lista muestran «Agendada por tu nutriólogo» (al nutriólogo, «Agendada por ti») cuando `origen == NUTRIOLOGO`, y la `nota` si hay.
- Textos nuevos: «Próxima cita», «Nueva», «Agendada por tu nutriólogo», «Agendada por ti».

- [ ] **Paso 1:** helpers de lo visto y constantes de aviso. **Paso 2:** `ProximaCita` y su cableado en el inicio (sin leer el estado de las citas dentro del cuerpo de `InicioPaciente`: que un cambio de citas no recomponga el inicio entero, como ya se hizo con el expediente al jalar para actualizar). **Paso 3:** `MainActivity` y los textos de origen y nota. **Paso 4:** `:app:compileDebugKotlin` y `:app:testDebugUnitTest` verdes.

---

## Tarea 7: Verificación y entrega

- [ ] **Paso 1:** `:app:testDebugUnitTest` completo (esperado: todo verde; hoy son 104 pruebas), `:app:assembleRelease` compila con R8, `npm run test:aviso` y `npx tsc --noEmit` en `backend/`, y todas las pruebas de reglas contra los emuladores.
- [ ] **Paso 2: revisión de todo el cambio** con un revisor nuevo (`superpowers:requesting-code-review`), cuyos hallazgos importantes se corrigen con prueba antes de entregar.
- [ ] **Paso 3: en el teléfono — preguntar antes.** Para ver el flujo de punta a punta hacen falta dos cuentas (nutriólogo verificado y paciente vinculado) y los emuladores de Firebase, que sacan al usuario de su sesión real. Pedirle permiso; con él, correrlo como en las pruebas del banco de platillos (dos cuentas sembradas, contraseñas solo en el directorio temporal). Sin su permiso, dejarlo dicho como no verificado.
- [ ] **Paso 4: despliegue.** Preguntar por separado si publicar las reglas y el Worker; hasta entonces el flujo no funciona contra producción (las reglas viejas rechazan la cita del nutriólogo). Nada se despliega por su cuenta.
- [ ] **Paso 5:** actualizar la nota de memoria del proyecto con lo que quedó hecho, lo no verificado y lo desplegado.
