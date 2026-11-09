# Ejercicios en la nube: catálogo, ilustraciones animadas y ejercicios asignados

Fecha: 2026-11-07. Estado: borrador para aprobar (nada construido).

## Por qué

Hoy el banco global de ejercicios (`ejercicios/{id}` en Firestore, foto en R2) tiene 150 ejercicios sembrados sin foto propia y los que crea
un entrenador, con una sola foto. El nutriólogo escribe los ejercicios de un plan como texto libre (`SesionEjercicio`: día, enfoque,
detalle), sin poder elegir del banco, y la «Rutina de hoy» del paciente se arma sola barajando el banco. El usuario quiere:

1. ilustraciones animadas como las de openGym (un clip corto en bucle con «toca para pausar»), **alojadas en la nube, no en el APK**;
2. un banco global grande, que además de lo de los entrenadores incluya un catálogo curado (el de openGym, con su licencia);
3. que el nutriólogo **asigne ejercicios concretos** a su paciente y que el paciente los vea y los entrene.

## Metas, no metas y antimetas

Metas
- Un clip MP4 corto por ejercicio (y su foto fija) servido desde R2; el APK no crece.
- El entrenador sube su propio clip al crear un ejercicio (flujo de revisión del admin sin cambios).
- Un catálogo de miles de ejercicios en la nube, con búsqueda y filtros rápidos y sin leer miles de documentos de Firestore.
- El nutriólogo elige ejercicios del banco global (pestaña «Globales») y los asigna a un paciente con series, repeticiones o minutos y días.
- El paciente ve lo asignado en su plan y en «Rutina de hoy» / «Entrenar ahora» (lo asignado manda ese día).

No metas (por ahora)
- Que el entrenador tenga pacientes y asigne él (hoy no existe vínculo entrenador-paciente).
- Que el paciente se asigne ejercicios a sí mismo.
- Compresión o recorte de video en el teléfono (un clip fuera de límite se rechaza con un mensaje).
- Detalle al tocar una fila de «Actividades»; traducciones de las descripciones del catálogo.

Antimetas (fallaría aunque «funcione»)
- Copiar o usar las animaciones de openGym/Gym visual sin licencia que cubra este uso. Ni se alimenta a una IA con ellas.
- Publicar nada en producción (R2, Firestore, Worker, reglas) sin el OK explícito del usuario cada vez.
- Que el APK, el inicio o la búsqueda se vuelvan lentos por cargar el catálogo entero.

## Restricciones

- Licencia de las animaciones: son de Gym visual (gymvisual.com), solo para openGym. Esta app es distinta: **antes de importar una sola
  animación hay que tener la licencia por escrito** y confirmar que cubre (a) uso en nutriGajo, (b) alojarlas en nuestro propio R2 y
  transmitirlas, (c) el número de usuarios previsto. El texto del catálogo: los 1,324 ejercicios de ExerciseDB son MIT (con atribución);
  lo demás lo escribió la comunidad de openGym bajo AGPL, y no está claro que se pueda usar en una app que no sea AGPL: preguntarlo al autor
  o usar solo el texto MIT y escribir el resto.
- Sin commit, push ni despliegue sin OK. Árbol de trabajo compartido: editar solo lo propio. Nunca `pm clear` ni desinstalar en el teléfono.
- UI según la memoria de diseño (naranja #F97316, Inter, carga con Esqueleto, no spinner).
- Los ids del catálogo son permanentes (como en openGym): los planes los guardan y nunca se reutilizan.

## Lo que el usuario decidió

- Las animaciones viven en la nube. Consigue la licencia de Gym visual.
- El banco global entra al plan como pestaña «Globales» al elegir un ejercicio, con búsqueda y filtros.
- «Asignados» = el nutriólogo asigna a su paciente. Con algo asignado ese día, «Rutinas de hoy» y «Entrenar ahora» usan solo eso; los
  días sin asignación sigue la rutina al azar de siempre.

## Diseño

### 1. Dos fuentes de ejercicios, una sola cara

- **Catálogo** (nuevo, solo lectura, curado): estático en R2.
- **Banco de entrenadores** (existe): `ejercicios/{id}` en Firestore con revisión del admin; gana un clip opcional.

Un ejercicio se nombra con una referencia `{fuente, id}` (`FuenteEjercicio { CATALOGO, BANCO }`). Para pantallas y rutina, el catálogo se
adapta a `Ejercicio` (el modelo actual) con valores por defecto donde openGym no trae el dato (ver «Mapeo»). El adaptador marca
`fuente = CATALOGO` y `FotoEjercicio` / `ClipEjercicio` eligen de dónde bajar según la fuente (R2 del catálogo o `ejercicios/{autor}/{id}`);
así el detalle, la guía y `EntrenarScreen` no distinguen de dónde vino.

### 2. Catálogo en R2 (no en Firestore)

Claves en el bucket `appsmobile`:

```
catalogo/version.json        { "version": 3, "total": 5632 }
catalogo/indice-v3.json      [[id, nombre, zona, tipo, equipo], …]   (~600 KB, ~150 KB comprimido)
catalogo/ficha/{id}.json     { id, titulo, zona, tipo, equipo, musculos[], descripcion, pasos[], clip: true }
catalogo/foto/{id}.webp      180 px
catalogo/clip/{id}.mp4       H.264, ~3 s, ~25 KB
```

El Worker las sirve con rutas de solo lectura (`GET /catalogo/...`) que **exigen una sesión válida de Firebase** (a diferencia de las fotos de
perfil y de los bancos de entrenadores, que son públicas por dirección): las animaciones son material con licencia y no deben poder
enlazarse ni bajarse sin iniciar sesión. Respuestas con `Cache-Control: private, max-age=31536000, immutable` en lo versionado;
`GET /catalogo/version` es corta (`no-cache`). Los clips aceptan `Range`. Por qué R2 y no Firestore: 5,632 documentos en cada apertura
serían lentos y costosos; el índice se baja una vez y se busca en el teléfono.

En la app: `CatalogoRepository` baja `version.json` al abrir (una vez al día), compara con la guardada y, si cambió, baja el índice nuevo a un
archivo en `filesDir`; búsqueda y filtros corren sobre el índice en memoria (mismo cuidado con acentos que `Platillo.coincide`). La ficha, la
foto y el clip se bajan al abrir el detalle (caché en disco como `FotosBanco`).

Importación (una sola vez, con el script en `backend/scripts/`, sin tocar producción hasta el OK): lee el repo de openGym, valida, mapea y
escribe los archivos de arriba a una carpeta local; otra orden los sube a R2 con wrangler (`--remote`).

#### Mapeo openGym → nutriGajo (pierde detalle, a propósito)

| openGym | nutriGajo |
|---|---|
| bodyPart: chest, back, shoulders, waist, full body, cardio, neck | PECHO, ESPALDA, HOMBROS, ABDOMEN, CUERPO_COMPLETO, CUERPO_COMPLETO, CUERPO_COMPLETO |
| upper arms, lower arms | BRAZOS |
| upper legs, lower legs | PIERNAS |
| equipment: body weight, assisted… | SIN_EQUIPO |
| dumbbell, kettlebell | MANCUERNAS |
| band, resistance band | BANDAS |
| barbell, ez barbell, olympic barbell, trap bar, landmine, weight plate | BARRA |
| el resto (cable, máquinas, smith…) | MAQUINA |
| category: strength, calisthenics, olympic, plyometrics, combat, isometric, rehab, pilates | FUERZA |
| cardio | CARDIO |
| stretching, mobility | FLEXIBILIDAD |
| yoga | YOGA |

Faltan en openGym: nivel (se pone PRINCIPIANTE; mejora futura), duración (por defecto 5 min) y kcal (estimadas por tipo y duración).
Nombres: `i18n/es.json` traduce los 5,632 nombres; descripciones y pasos solo están en inglés (los pasos traducidos son 1,324). Se muestra el
nombre en español y, mientras no haya más, la descripción y los pasos en inglés.

### 3. Clips de los entrenadores

- `Ejercicio.clipVersion: Long?` junto a `fotoVersion`. Para publicar solo la foto es obligatoria.
- Editor del entrenador: sección «Ilustración» con la foto de siempre (póster) y «Animación» opcional: se elige un video de la galería o se
  graba; se comprueba en el teléfono (`MediaMetadataRetriever`) que dure **≤ 5 s** y pese **≤ 2 MB**, y se muestra en bucle antes de guardar.
- Worker: `PUT /ejercicio/{id}/clip` (solo `video/mp4`, ≤ 2 MB, uid de la sesión) guarda `ejercicios/{uid}/{id}/clip-{versión}`;
  `GET /ejercicio/{autor}/{id}/clip?v=` lo sirve con `Range` e inmutable. Mismas protecciones que la foto.
- Reglas: `ejercicioValido` acepta `clipVersion` numérico opcional.
- El admin ve el clip reproduciéndose en la revisión.

### 4. Reproductor

`ClipEjercicio` con Media3 (ExoPlayer): sin sonido, en bucle, solo mientras está en pantalla; un toque pausa y muestra la foto fija; el
distintivo «toca para pausar / toca para reproducir» como en openGym. Se usa en el detalle del ejercicio y en la guía de «Entrenar ahora»; las
listas y tarjetas siguen mostrando la foto. Sin clip, se ve la foto (o la imagen por defecto del tipo, como hoy). Dependencia nueva:
`androidx.media3:media3-exoplayer` (+ `media3-ui` solo si hace falta); versión fijada compatible con Kotlin 2.2.

### 5. Nutriólogo: elegir y asignar

- `BancoScreen` (tipo EJERCICIO, al elegir para un plan) gana la pestaña «Globales», como la de platillos: búsqueda sobre el índice y
  filtros por zona, tipo y equipo; mezcla catálogo y banco de entrenadores (publicados). Tocar uno abre su ficha con el clip y «Asignar».
- «Asignar»: hoja con series y repeticiones, o minutos (según el tipo), días de la semana (uno o varios) y una nota opcional.
- El editor del plan gana la sección **«Ejercicios asignados»**, agrupada por día, con quitar y cambiar series/días. Convive con las
  sesiones de texto libre actuales (no se borran: son compatibles y sirven para indicaciones generales).
- `PlanContenido.ejerciciosAsignados: List<EjercicioAsignado>`; tope 35 en total y 10 por día.

```
EjercicioAsignado(dia: Int /*1=lunes … 7=domingo*/, fuente: FuenteEjercicio, id: String, titulo: String,
                  series: Int?, repeticiones: Int?, minutos: Int?, nota: String)
```

`titulo` se copia al asignar para que el plan se vea (y el PDF salga) aunque el ejercicio se retire después. Va dentro de `contenido` del plan
(`pacientes/{id}/planes/{planId}`): las reglas ya aceptan cualquier mapa en `contenido`, así que **no hay cambio de reglas** para esto.

### 6. Paciente

- Los planes enviados no se editan (cada envío es un documento nuevo): lo asignado **vigente** es el del plan más reciente del paciente, y
  el nutriólogo parte de él al armar el siguiente.
- La pantalla del plan muestra «Ejercicios asignados» por día, con foto y, al abrir, el clip y los pasos. Si el ejercicio ya no está
  (retirado del banco), se ve el título guardado y un aviso «Ya no está disponible» en lugar de la ficha.
- `rutinaDeHoy(fecha, asignados, resolver, banco)`: si hay asignados para `fecha.dayOfWeek`, `RutinaDelDia` se arma con ellos en el orden
  del plan (sus series, repeticiones o minutos); si no, el baraje de siempre. El inicio y «Entrenar ahora» no cambian de forma, solo de
  fuente. El catálogo se adapta a `Ejercicio` para entrar a `EntrenarScreen`.
- El PDF del plan (`GeneradorPlanPdf`) añade la sección por día.
- Offline: el plan guarda el título; foto, clip y ficha se bajan cuando hay red y se quedan en caché; sin red se ve el título y los datos
  de la asignación.

### 7. Producción (cada paso con OK)

Worker (rutas `/catalogo/*` y `/ejercicio/{id}/clip`), reglas (`clipVersion`), subida del catálogo a R2 y, si se quiere, la siembra de
ilustraciones a los 150 ejercicios existentes (hoy sin cuenta de autor: se haría con un script aparte).

## Fases (cada una útil por sí sola)

1. **Clips de entrenador y reproductor.** No depende de la licencia. Sale primero: es lo que el usuario quería ver.
2. **Globales y ejercicios asignados.** Sobre el banco de entrenadores y los 150 sembrados; no depende del catálogo.
3. **Catálogo en R2 e importación.** Espera la licencia de Gym visual por escrito y la aclaración del texto AGPL. Al llegar, la pestaña
   «Globales» ya sabe leer de dos fuentes.

## Lo que queda al constructor

- Nombres de clases, pantallas y cadenas en español; el orden de las pantallas del editor.
- Cómo se agrupa y ordena la lista de «Ejercicios asignados».
- Valores por defecto finos (kcal por minuto por tipo, nivel del catálogo) mientras no haya datos mejores.
- Pruebas: lógica pura (mapeo, búsqueda sobre el índice, `rutinaDeHoy` con asignados, validación de límites del clip) con prueba unitaria al lado.

## Qué decidí yo (el usuario puede cambiarlo)

- Clip de hasta 5 s y 2 MB, MP4; sin compresión en el teléfono por ahora.
- Catálogo en R2 con índice local en vez de Firestore.
- Las sesiones de texto libre se conservan junto a los asignados.
- Tope de 35 asignados por plan y 10 por día.
- Nivel PRINCIPIANTE y duración de 5 min por defecto para el catálogo.
- Media3 como reproductor.
