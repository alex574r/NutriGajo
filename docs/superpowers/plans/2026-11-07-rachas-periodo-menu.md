# Rachas, periodo del plan y menú del expediente — Plan de implementación

> **Para quien ejecute:** usar superpowers:executing-plans (inline, TDD). Los pasos son casillas `- [ ]`. Cada tarea deja un renglón en el ledger `.superpowers/sdd/2026-11-07-rachas-periodo-menu/progress.md`.

**Meta:** (1) que la racha no se pierda ni cambie sola; (2) que un plan dure N días (1–90) con ejercicios y menú por día; (3) que la pantalla del paciente del nutriólogo sea un menú de tarjetas con imagen en vez de un expediente apretado bajo cinco botones.

**Arquitectura:** (1) los días de la racha se guardan por cuenta (uid) y no se borran al cerrar sesión; el inicio los reconstruye desde Firestore (entrenamientos y carreras) y anota para siempre los días en que se llegó a la meta de pasos. (2) `PlanContenido` gana `duracionDias` (null = semana de lunes a domingo, como hasta hoy) y `menuDias` (opción de menú de cada día); con duración, `EjercicioAsignado.dia` es el día del plan (1…N) y el plan empieza el día que se envió. (3) `ResumenScreen` muestra, para el nutriólogo, un menú de tarjetas con foto; el expediente queda en su propia pantalla.

**Decisiones del usuario (7 nov 2026):** duración «Días 1…N, N a elección»; la dieta también por día («cada día su menú»); menú de tarjetas con imagen.

## Restricciones globales
- Solo git local, nada de push ni commit sin que lo pidan; no desplegar reglas/Worker/APK sin OK (instalar en el teléfono ya está autorizado para probar).
- Los planes ya enviados no cambian de significado: sin `duracionDias` el `dia` sigue siendo lunes = 1 … domingo = 7.
- `contenido` del plan es un mapa libre en las reglas de Firestore: no hace falta tocar reglas.
- Diseño: naranja #F97316, Inter, Esqueleto en vez de spinner, textos en español.
- Pruebas unitarias para lógica pura; lo de pantalla se verifica en el teléfono (cuenta nutrióloga Kelly Fernanda y paciente Alejandro; capturar la pantalla antes de pulsar con adb).

## Parte 1 — Rachas

### Tarea 1: lógica pura de la racha
**Files:** `domain/model/Racha.kt`, `RachaTest.kt`.
**Produce:** `fun diasDePasos(pasosPorDia: Map<Long, Int>, metaPasos: Int): Set<Long>` (días con pasos ≥ meta; meta 0 → vacío); `fun unirDiasRacha(guardados: Set<Long>, nuevos: Collection<Long>, tope: Int): Set<Long>` (unión, se quedan los `tope` más recientes).
- [ ] Pruebas primero (RED): `diasDePasos` marca solo los que llegan a la meta y con meta 0 da vacío; `unirDiasRacha` une, no repite y recorta a los más recientes; un día ya guardado no se pierde al subir la meta (`racha` con ese día en `diasCarrera` y meta 20000).
- [ ] Implementar y pasar GREEN.

### Tarea 2: almacén por cuenta
**Files:** `widgets/DatosWidget.kt` (`AlmacenWidget`).
- [ ] Los días se guardan bajo `diasCarrera.{uid}`; `diasCarrera(contexto)`/`agregarDiasCarrera(contexto, dias)` toman el uid de `FirebaseAuth` (o del perfil guardado). Migra la clave vieja `diasCarrera` al primer uid que la use.
- [ ] `borrar` limpia todo menos los días por cuenta (solo son fechas); otra cuenta en el mismo teléfono no ve los de la anterior.
- [ ] `agregarDiasCarrera` usa `unirDiasRacha`.

### Tarea 3: reconstruir al abrir el inicio
**Files:** `data/repository/SincronizarRacha.kt` (nuevo), `ui/paciente/inicio/InicioPacienteScreen.kt`.
- [ ] `suspend fun sincronizarDiasRacha(contexto)`: lista entrenamientos y carreras (cada uno en `runCatching`, sin red no pasa nada), anota sus días y actualiza widgets.
- [ ] El inicio la llama al abrir y vuelve a leer los días; además anota para siempre los días de pasos que llegaron a la meta, **solo con el vínculo cargado** (así la meta por defecto no marca días de más).

## Parte 2 — Menú de tarjetas del expediente (nutriólogo)

### Tarea 4: el menú
**Files:** `ui/shared/expediente/ResumenScreen.kt`, nuevo `MenuPaciente.kt`, `strings.xml`.
- [ ] Para el nutriólogo (hay `onPlan`/`onChat`/`onMetas`/`onCita`), `ResumenScreen` abre un menú: encabezado con el nombre, y tarjetas grandes con foto (Expediente, Plan, Chat con punto de «nuevo», Metas, Citas), cada una con un subtítulo (p. ej. «13 de 14 datos», «Plan enviado el …»). «Expediente» abre la pantalla actual sin la pila de botones; atrás vuelve al menú.
- [ ] El paciente sigue viendo lo de siempre.

## Parte 3 — Periodo del plan en N días

### Tarea 5: modelo y lógica pura
**Files:** `domain/model/Plan.kt`, nuevo `PeriodoPlan.kt`, `PlanMapa.kt`, tests.
**Produce:** `PlanContenido.duracionDias: Int? = null` (1..90), `PlanContenido.menuDias: List<Int> = emptyList()` (índice = día − 1, valor = índice en `opciones` o −1); `MAX_DIAS = 90`; `fun diaDelPlan(hoy: LocalDate, inicio: LocalDate, duracionDias: Int): Int?` (1…N o null si aún no empieza o ya terminó); `fun PlanContenido.diaDeHoy(hoy, enviadoEn zona): Int?` que da el día de la semana (1–7) si no hay duración; `EjercicioAsignado.valido(maxDia)`; `limpio()` descarta lo que cae fuera de la duración y recorta `menuDias`.
- [ ] Pruebas primero: `diaDelPlan` (primer día, último, antes, después); `limpio()` con duración 3 descarta un asignado del día 5 y recorta el menú; sin duración todo igual que hoy; mapeo ida y vuelta en `PlanMapa`.

### Tarea 6: editor
**Files:** `ui/nutriologo/plan/PlanScreen.kt`, `SeccionesPlan.kt`, `AsignarEjercicioSheet.kt`, `strings.xml`.
- [ ] Control de duración (7, 14, 21, 30 o un número) en el plan; con duración, las sesiones y la hoja de asignar eligen «Día 1…Día N» en lugar de lunes–domingo; sección «Menú por día» donde cada día elige una de sus opciones de menú.

### Tarea 7: paciente y PDF
**Files:** `domain/model/RutinaDelDia.kt`, `ui/paciente/inicio/InicioPacienteScreen.kt`, `ui/paciente/ejercicios/EjerciciosAsignadosScreen.kt`, `data/pdf/GeneradorPlanPdf.kt`.
- [ ] «Rutina de hoy» y «Mis ejercicios asignados» usan el día del plan; el inicio muestra «Hoy te toca: opción N»; el PDF lleva la duración, el menú por día y «Día k» en vez de lunes–domingo.

### Tarea 8: verificación
- [ ] Suite completa, compilar, instalar y ver en el teléfono (nutrióloga: menú, duración, menú por día; paciente: racha y «hoy»).
