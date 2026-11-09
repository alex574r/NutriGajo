# Citas que crea el nutriólogo — diseño

Estado: **aprobado por el usuario el 5 nov 2026**, con la cita confirmada desde el inicio y el aviso de cancelación incluido; las
demás decisiones marcadas *(mi decisión)* quedan como están. Lo marcado *(mi decisión)* lo propuse yo.

## Para qué

Hoy solo el paciente pide cita (desde el mapa) y el nutriólogo la confirma o la rechaza. El nutriólogo **no puede agendar** a sus propios
pacientes: tiene que pedírselo por el chat y esperar a que el paciente la pida. El pedido es que el nutriólogo cree citas con sus pacientes
y que **el paciente se entere**.

## Lo que ya existe y se reutiliza

- `citas/{id}` con estados `PENDIENTE → CONFIRMADA | RECHAZADA`, y `CANCELADA` (`Cita.kt`, `CitasRepository`).
- `directorio/{nutriólogo}/ocupados/{inicio}`: la reserva de una hora, sin datos del paciente. Se crea en el mismo lote que la cita, así
  que dos personas no pueden tomar la misma hora.
- Candado `citasActivas/{uid}`: un paciente solo puede tener una cita pedida por él a la vez.
- Agenda del nutriólogo (mes, semana y lista, con detalle para confirmar, rechazar y cancelar) y «Mis citas» del paciente (cancelar y
  reagendar) — `AgendaScreen`, `CitasScreen`.
- Horario fijo (`Agenda.kt`): lunes a sábado, citas de una hora, de 9:00 a 17:00.
- Aviso al paciente por FCM cuando se confirma una cita (`POST /citas/notificar` del Worker, canal «citas», `usuarios/{uid}.fcmToken`).

Lo que **no** existe: crear una cita desde el lado del nutriólogo, avisar al paciente de una cita que no pidió, avisarle si se la cancelan,
y una señal de «tienes una cita» en el inicio del paciente (hoy solo está en el perfil, en «Mis citas»).

## El flujo

```
NUTRIÓLOGO                                   PACIENTE
Expediente del paciente → «Agendar cita»
(o «+» en su Agenda, y elige paciente)
   │
Hoja «Nueva cita»: paciente · día · hora · nota
   │  «Agendar»
   ▼
cita CONFIRMADA + hora reservada (un solo lote) ──►  push «Kelly te agendó una cita: lunes 9 de nov, 10:00»
   │                                                  inicio: fila «Próxima cita» con chip «Nueva»
   │                                                  Mis citas: «Agendada por tu nutriólogo» + nota
   │                                                  puede cancelarla o pedir otra hora
   ▼
si la cancela el nutriólogo ──────────────────────►  push «Kelly canceló tu cita del lunes 9 de nov»
```

**Nutriólogo**
1. Entra por «Agendar cita» dentro del expediente de su paciente (el paciente ya viene elegido) o por «+» en su Agenda (elige uno de sus
   pacientes). Solo aparece con la cuenta verificada, igual que hoy las citas *(mi decisión)*.
2. La hoja pide día (chips), hora (chips; las ocupadas, en gris) y una nota opcional de hasta 120 caracteres («Seguimiento», «Trae tus
   análisis»). Si el paciente ya tiene una cita vigente, la hoja lo dice arriba («Ya tiene una el lunes 9, 10:00») pero deja agendar otra.
3. «Agendar» crea la cita **ya confirmada** y reserva la hora en una sola operación. Si otro tomó esa hora mientras elegía, avisa («Esa
   hora ya no está libre») y vuelve a leer las horas.
4. Después manda el aviso al paciente, sin hacer esperar al nutriólogo. Si el aviso falla, la cita queda igual y se le dice «No pudimos
   avisarle por notificación; la verá en su app».

**Paciente**
- Le llega la notificación (canal «citas»); al tocarla se abre «Mis citas».
- En su inicio, debajo del plan, aparece la fila **«Próxima cita»** con día, hora y nutriólogo, y el chip «Nueva» hasta que la abre. Sale con
  cualquier cita vigente, también con las que pidió él *(mi decisión)*.
- En «Mis citas» la cita lleva «Agendada por tu nutriólogo» y la nota. Puede cancelarla o pedir otra hora; pedir otra hora la deja
  pendiente hasta que su nutriólogo la confirme (así funciona hoy).
- Mientras tenga una cita vigente agendada por su nutriólogo, si intenta pedir otra desde el mapa verá esa cita (para cancelarla o
  reagendarla), como hoy con cualquier cita vigente: la app ya trata como «su cita vigente» a la primera que no pasó ni se cerró
  *(mi decisión)*. Las reglas no se lo impiden porque esta cita no usa el candado; es solo la app.
- Si su nutriólogo la cancela, le llega otro aviso.
- Sin notificaciones activadas o sin dispositivo registrado, se entera por la fila del inicio y por «Mis citas» (como hoy con la confirmación).

## Metas

1. El nutriólogo agenda a uno de sus pacientes en una hora libre en menos de cuatro toques desde el expediente.
2. El paciente se entera por notificación y por su inicio, sin tener que abrir el perfil.
3. Nunca hay dos citas en la misma hora de un nutriólogo, vengan de quien vengan.
4. Solo se agenda a pacientes vinculados al nutriólogo; las reglas de Firestore lo exigen, no solo la app.

## Fuera de alcance (esta versión)

- **Recordatorio** un día antes (necesita un trabajo programado en el Worker).
- Que el nutriólogo **mueva** una cita (para cambiarla, la cancela y crea otra: el paciente recibe dos avisos).
- Citas repetidas («cada semana»), disponibilidad configurable y zona horaria del consultorio.
- Aviso al nutriólogo cuando un paciente pide, cancela o reagenda (hoy tampoco existe).
- Que la campana del inicio del paciente liste las citas (hoy solo lista planes).
- Videollamada o modalidad en línea, y exportar al calendario del teléfono.

## Lo que haría fracasar esto aunque «funcione»

- Dos citas empalmadas porque el nutriólogo agendó a una hora que un paciente ya había pedido.
- Que el paciente descubra una cita por casualidad, o que le llegue el aviso de una cita que ya no existe.
- Que un nutriólogo pueda agendar a alguien que no es su paciente, o que `ocupados` deje ver datos del paciente.
- Que la cita agendada por el nutriólogo estorbe el flujo del paciente (que su «una cita a la vez» se confunda).

## Datos

`citas/{id}` gana tres campos opcionales; las citas de antes no los tienen y se leen como pedidas por el paciente:

| Campo | Qué es |
|---|---|
| `origen` | `PACIENTE` (ausente = esto) o `NUTRIOLOGO` |
| `pacienteId` | el id de `pacientes/{id}`; solo las citas del nutriólogo lo llevan |
| `nota` | texto de hasta 120 caracteres, opcional |

`Cita` suma `origen`, `pacienteId` y `nota`. Un nutriólogo crea con `estado = CONFIRMADA`; el resto del ciclo (cancelar, el paciente
reagenda) no cambia.

## Reglas de Firestore *(se despliegan solo con tu OK)*

- **`citas` create, rama nueva del nutriólogo:** `rolDe == NUTRIOLOGO`; `nutriologoId == auth.uid` y su nombre igual al de `directorio`;
  el paciente sale de `pacientes/{pacienteId}`, que debe ser suyo (`nutriologoId == auth.uid`), con el mismo `usuarioId` y `nombre`;
  `estado == CONFIRMADA`, `origen == NUTRIOLOGO`; la hora en el futuro, a menos de 60 días y en cuartos de hora (lo de hoy); la
  reserva en `ocupados` creada en el mismo lote. **No usa el candado** `citasActivas` *(mi decisión)*: ese candado frena a los pacientes que
  piden horas sin parar; un nutriólogo puede tener varias citas con el mismo paciente.
- **`ocupados` create:** además de lo que ya permite al paciente, deja al nutriólogo cuando la cita del mismo lote es suya, de origen
  `NUTRIOLOGO` y confirmada.
- **Lo demás no cambia:** el paciente sigue sin poder crear citas confirmadas ni con `origen = NUTRIOLOGO`; el nutriólogo cancela lo
  confirmado y eso libera la hora; el paciente cancela y reagenda lo suyo.

## Worker (`backend/`) *(se despliega solo con tu OK)*

`POST /citas/notificar {citaId}` ya existe. Cambia así, sin ruta nueva:
- Si la cita es `CONFIRMADA` con `origen = NUTRIOLOGO`, el texto es «{nutriólogo} te agendó una cita: {día, hora}. Consultorio: {dirección}.»
  y los datos del aviso llevan `tipo: CITA_AGENDADA`. La confirmación de una cita pedida por el paciente queda como está.
- Si la cita está `CANCELADA` y quien pide el aviso es su nutriólogo, el texto es «{nutriólogo} canceló tu cita del {día, hora}.» y
  `tipo: CITA_CANCELADA`.
- Cada aviso se manda una sola vez: se compara `notificadaEn` con `actualizadaEn` de la cita (hay aviso pendiente si la cita cambió después
  del último). Así también avisa otra vez una cita que se mueve y se vuelve a confirmar *(cambié esto al planear: era un segundo campo
  `canceladaAvisoEn`)*. El dispositivo y la zona horaria del paciente salen de su perfil, nunca del cuerpo de la petición.

## Pantallas y archivos

- **Modelo y datos:** `Cita.kt`; `CitasRepository` suma `agendarComoNutriologo(paciente, inicio, nota)` (un lote: cita + `ocupados`) y
  `AvisoCitaRepository` pide el aviso de cita agendada y de cita cancelada.
- **Nutriólogo:** hoja `NuevaCitaSheet` (día y hora como en `AgendarCitaSheet`, que ya tiene los chips) y su ViewModel; el botón «+» en
  `AgendaScreen` y la acción «Agendar cita» en el expediente del paciente. `Agenda.dias` recibe cuántos días ofrecer: 14 para el paciente, 30
  para el nutriólogo *(mi decisión: seguimientos a tres semanas)*.
- **Paciente:** fila `ProximaCita` en `InicioPacienteScreen` con el chip «Nueva» (lo visto se guarda en el teléfono, como lo visto de los
  planes); el aviso toca → abre «Mis citas» (`MainActivity` ya hace esto con planes y chat); etiqueta «Agendada por tu nutriólogo» y la nota
  en `AgendaScreen` y `CitasScreen`.
- **Textos** en `strings.xml`.

## Pruebas

- Reglas (`firebase/test/citas.mjs`, nuevo: hoy las citas no tienen prueba de reglas): el nutriólogo agenda a su paciente; no a uno
  ajeno; no a una hora ya ocupada; el paciente no puede crear confirmadas ni con `origen = NUTRIOLOGO`; sin verificar no agenda;
  cancelar libera la hora.
- Worker (`backend/test/aviso.mjs`): los dos textos nuevos, el aviso único y que un tercero no pueda pedirlo.
- Unitarias: `Agenda.dias` con otra ventana, `Cita` con los campos nuevos, y la decisión de qué cita es «la próxima».
- En el teléfono: agendar como nutriólogo y ver el aviso y la fila en el paciente (hacen falta dos cuentas; sin cerrar la sesión real).

## Fases

1. Modelo, reglas y sus pruebas.
2. Hoja del nutriólogo y las dos entradas.
3. Worker y aviso.
4. Inicio y «Mis citas» del paciente.

Despliegue de reglas y del Worker después de la 3, cada uno con tu confirmación.

## Para validar

1. **¿La cita nace confirmada o el paciente la acepta?** Propongo **confirmada**: el paciente se entera y puede cancelarla o pedir otra hora, y
   el nutriólogo no queda esperando. La otra opción es un estado nuevo `PROPUESTA` en el que el paciente acepta o rechaza; es más
   respetuosa con el paciente pero añade un estado, más reglas y un paso más para ambos.
2. **¿Va incluido el aviso de cancelación?** Propongo que sí (sin él, el paciente llega a una cita cancelada); quitarlo ahorra una rama del Worker.

## Mis llamadas (puedes cambiarlas)

Solo con cuenta verificada · sin candado para las citas del nutriólogo · 30 días de ventana para el nutriólogo y el mismo horario fijo
(lunes a sábado, 9 a 17, de una hora) para evitar empalmes · nota opcional de 120 caracteres · la fila «Próxima cita» sale con cualquier cita
vigente · el chip «Nueva» se guarda en el teléfono · «+» en la Agenda y «Agendar cita» en el expediente · el nutriólogo cancela y crea para
mover una cita.
