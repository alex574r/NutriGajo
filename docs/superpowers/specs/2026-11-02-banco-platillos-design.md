# Banco global de platillos (dietas) — diseño

Estado: **borrador para aprobar**. Nada de esto está construido. Lo marcado como *(mi decisión)* lo propuse yo; el resto sale de lo que
pidió el usuario o de lo que ya existe en la app.

## Para qué

El usuario pidió «mejorar todo el banco de las dietas» y que **el entrenador también pueda crear dietas**, con el aspecto de la
tarjeta de «Dieta & nutrición» (foto, proteína, grasas, kcal y flecha naranja).

Hoy hay dos cosas separadas y ninguna sirve de verdad:

- El banco del **nutriólogo** (`usuarios/{uid}/banco`, `ItemBanco`) es privado y solo de texto (nombre, preparación, tiempo de comida).
  Sirve para rellenar los menús de un plan.
- «Dieta & nutrición» del **paciente** (`DatosMuestra.kt`, `platos`) es todo de muestra: 6 platillos fijos con la misma foto, iguales
  todos los días. Las pantallas de Mis comidas, agregar a mano y escáner con IA también son de muestra.

La idea es repetir con las comidas lo que ya se hizo con los ejercicios: un **banco global** donde crean los profesionales verificados,
revisa el admin y explora el paciente.

*Dieta = platillo* *(mi decisión)*: una receta suelta como «Ensalada con pechuga». Un menú completo de un día o de una semana queda
fuera de esta primera versión (ver «Fuera de alcance»). Si el usuario quiere menús completos, hay que volver aquí antes de construir.

## Metas

1. Un nutriólogo o un entrenador verificado crea un platillo con foto, macros, ingredientes y pasos, lo manda a revisión y el admin lo
   publica o lo rechaza con motivo.
2. El paciente ve en su inicio platillos reales publicados (la tarjeta del diseño) y, con «Ver todas», explora el banco con búsqueda y
   filtros y abre la receta paso a paso.
3. El nutriólogo, al armar un plan, puede elegir platillos del banco global además de los suyos.
4. Lo publicado respeta las alergias y la dieta que el paciente puso en su expediente.

## Fuera de alcance (esta versión)

- Menús de un día o de una semana, y que el plan del nutriólogo aparezca en «Mis comidas» por día.
- El escáner con IA, agregar una comida a mano y el conteo de calorías del día (siguen de muestra).
- Video, favoritos, reportes, métricas del autor, y una base de alimentos con valores nutricionales.
- Que el nutriólogo o el entrenador publiquen sin revisión.

## Lo que haría fracasar esto aunque «funcione»

- Que un platillo con kcal o macros inventados llegue al paciente: es información de salud. Por eso todo pasa por el admin y el
  formulario avisa si las kcal no cuadran con los macros.
- Que el paciente crea que la receta de un entrenador sustituye el plan de su nutriólogo: cada platillo dice quién lo hizo y la pantalla
  repite el aviso de «no sustituye las indicaciones de tu nutriólogo» (el mismo de Explorar ejercicios).
- Que un paciente con alergias vea platillos con ese ingrediente sin ninguna señal.

## Flujo

```
Autor (nutriólogo o entrenador verificado)         Admin                         Paciente / Nutriólogo
Mis platillos → Nuevo → borrador
   │  foto, macros, ingredientes, pasos
   ▼
Mandar a revisión ───────────────────────────►  Contenido → Platillos → revisar
                                                  ├─ Publicar ───────────────►  Inicio: fila «Dieta & nutrición»
                                                  └─ Rechazar (con motivo) ─┐    «Ver todas» = Explorar platillos
                                                                            │    Nutriólogo: «Globales» en el selector del plan
Autor: ve «Rechazado» + motivo ◄────────────────────────────────────────────┘
Autor: edita y vuelve a mandar · duplica un publicado · retira uno publicado
```

Estados (los mismos de los ejercicios): `BORRADOR → EN_REVISION → PUBLICADO | RECHAZADO → RETIRADO`. Lo publicado es de solo lectura:
se retira o se duplica para editarlo.

## Qué lleva un platillo *(mi decisión, calcado de `Ejercicio` y de la tarjeta de muestra `Comida`)*

| Campo | Notas |
|---|---|
| nombre | hasta 80 caracteres |
| resumen | una o dos frases, hasta 300 |
| momento | desayuno, comida o cena (los de `MomentoComida`); «colación» queda para después |
| proteína, grasas, carbos (g) y kcal | enteros con tope; el formulario sugiere kcal = 4·P + 4·C + 9·G y avisa si difieren más de 15 % |
| minutos | tiempo de preparación |
| ingredientes | lista (hasta 20 renglones de 80) |
| pasos | lista (hasta 12 de 300), en el mismo pager de tarjetas que los ejercicios |
| etiquetas | sin gluten, sin lactosa, vegetariano, vegano, alto en proteína, bajo en carbohidratos (hasta 4) |
| foto | una; mismo camino que la de los ejercicios (Worker + R2) |
| autor | id, nombre y rol, para mostrar «Receta de nutriólogo» o «Receta de entrenador» |
| estado, motivo, fechas | como en `Ejercicio` |

## Quién ve qué

- **Autor:** «Mis platillos» en su inicio (lista con estado, formulario, duplicar, retirar). Para el entrenador es una segunda sección
  junto a «Mis ejercicios»; para el nutriólogo, una entrada nueva.
- **Admin:** la pestaña «Contenido» pasa a tener «Ejercicios» y «Platillos»; misma revisión con vista previa del detalle.
- **Paciente vinculado:** la fila «Dieta & nutrición» del inicio con platillos publicados reales (la tarjeta del diseño, con la foto
  real), «Ver todas» abre Explorar platillos (búsqueda; filtros por momento y etiqueta) y el detalle con receta paso a paso.
- **Filtro por expediente** *(mi decisión)*: se esconden los platillos cuyos ingredientes mencionan una alergia del paciente y, si su
  dieta es de plantas o baja en carbohidratos, se prefieren los que llevan esa etiqueta. Es una coincidencia de texto, no una garantía
  médica; el detalle lo dice.
- **Nutriólogo armando un plan:** el selector del banco gana una pestaña «Globales». Elegir uno escribe en el menú lo mismo que hoy
  escribe `ItemBanco.textoParaPlan()` (nombre, ingredientes y kcal en el tope de 500 caracteres). Su banco privado no cambia.

## Piezas técnicas *(mi decisión: reutilizar lo de ejercicios)*

- Modelo y repositorio nuevos (`Platillo`, `PlatillosRepository`) con la forma de `Ejercicio`/`EjerciciosRepository`; la colección es
  `platillos/{id}`.
- Reglas de Firestore nuevas con la misma lógica que `ejercicios` (crea solo un NUTRIOLOGO o ENTRENADOR verificado, solo el ADMIN
  publica, topes en cada campo) y su prueba contra los emuladores. **Publicarlas necesita el visto bueno del usuario.**
- Ruta nueva del Worker para la foto (`PUT/GET /platillo/{autor}/{id}`) y su prueba de humo. **Desplegarla necesita el visto bueno del
  usuario.**
- Se reutilizan las piezas visuales de `ui/shared/ejercicios` (cabecera oscura, pager de pasos, tarjeta con foto y el botón de volver).
- Siembra opcional: los 6 platillos de muestra, sin foto, escritos a producción como se hicieron los 150 ejercicios (solo con permiso).

## Fases

1. Modelo, reglas, Worker, «Mis platillos» del autor y la revisión del admin.
2. Fila del inicio y Explorar platillos para el paciente, con el filtro por expediente.
3. Pestaña «Globales» en el selector del plan del nutriólogo.
4. (Aparte) Menús completos y el plan del nutriólogo en «Mis comidas».

## Lo que decide el usuario antes de construir

1. ¿«Dietas» son platillos sueltos (lo que supuse) o también menús completos?
2. ¿Los platillos de un nutriólogo verificado también pasan por el admin (lo que supuse, por consistencia con los ejercicios) o se
   publican solos?
3. ¿Se siembran los 6 platillos de muestra para que el inicio no quede vacío hasta que haya reales?

## Lo que queda a quien construya

Orden exacto de las pantallas del formulario, textos, el tope de kcal y de macros, y el diseño de la tarjeta de «Mis platillos» (puede
ser la misma tarjeta con foto de los ejercicios).
