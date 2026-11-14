# Traducción del catálogo de ejercicios al español

Fecha: 12 nov 2026 · Estado: aprobado por el usuario («Sí, armar plan») · Relacionado: `2026-11-07-ejercicios-en-la-nube-design.md` (catálogo openGym en R2).

## Problema

El catálogo en la nube (5,632 ejercicios) llega a la app con el **título** ya en español, pero la **descripción** está en inglés en los 5,632 y los **pasos** están en inglés en 4,308 (solo 1,324 vienen en español de `i18n/es.json`). Medido sobre el repo de openGym: 559 mil caracteres de descripciones y 1.07 millones de caracteres de pasos en inglés, unos **400 mil tokens** de entrada en total.

## Decisión

Traducir **una sola vez, antes de subir a R2**, y guardar el español en la misma ficha (`descripcion`, `pasos`). **La app no cambia:** ya lee esos dos campos, y cuando el catálogo sube de versión borra sus fichas guardadas y baja las nuevas (`CatalogoRepository.indice()`).

- **Cómo se traduce:** Claude por la API de Mensajes, con la **Message Batches API** (la mitad de precio, asíncrona, sin ajustar límites de velocidad). El SDK oficial de TypeScript (`@anthropic-ai/sdk`) como dependencia de desarrollo del `backend/`; el Worker no la importa.
- **Lotes de 15 ejercicios** por petición (≈ 376 peticiones): el glosario va una vez por lote y no por ejercicio. La respuesta es JSON con esquema (`output_config.format`), por id, para poder validar cada ejercicio por separado.
- **Contexto por ejercicio:** su título ya en español, para que los términos coincidan con los del resto del catálogo.
- **Lo que no se envía ni se toca:** los 1,324 conjuntos de pasos que ya están en español; `musculos` (la app no lo muestra); fotos y clips (la licencia de Gym visual no permite alimentar una IA con las animaciones: **solo viaja texto**).
- **Salida a una carpeta nueva**, no sobre la del importador: `ficha/*.json` traducidas (las que fallen quedan en inglés), `indice-{N}.json` (copia del índice, que no cambia) y `version.json` con la versión nueva. `subir-catalogo.mjs` sube esa carpeta tal cual (≈ 5,634 archivos, unos 40 min) y los clips y fotos de R2 no se tocan.
- **Versión nueva** = fecha `YYYYMMDD` (> 20261107). Con ella los teléfonos bajan el índice nuevo y descartan las fichas guardadas.

## Costo (estimado; el precio del lote es la mitad del normal)

| Modelo | Costo aproximado |
|---|---|
| `claude-haiku-5-5` (**elegido por el usuario**, por defecto del CLI) | < US$ 1 |
| `claude-sonnet-5-5` | ≈ US$ 3.7 |
| `claude-opus-5-5` | ≈ US$ 7 |

(Del simulacro del CLI sobre las 5,632 fichas: ≈ 655 mil tokens de entrada, con instrucciones y estructura JSON, y ≈ 600 mil de salida; estimación por caracteres, no el conteo real del tokenizador.)

El modelo se elige con `--modelo`; el simulacro imprime el cálculo y **nada se gasta sin el OK del usuario**. Antes de la corrida completa se hace una prueba de 2 lotes y se revisa una muestra con el usuario.

## Variante sin clave de API: subagentes de Claude Code (elegida el 12 nov 2026)

El usuario no quiso sacar una clave de API, así que la traducción la hacen **subagentes Haiku de Claude Code** (cuenta como su petición de subagentes) y gasta el cupo de su plan (Pro), no saldo. El script lo soporta con `--exportar` (escribe `entrada/lote-NNNN.json` e `INSTRUCCIONES.md`), `--importar` (lee `traducido/lote-NNNN.json` y lo valida con la misma `leerResultadoDeLote`), `--reintentar` (re-exporta solo los lotes con fallidos) y `--desde K`. La salida y la subida son las mismas. Se hace primero un piloto de unos 4 lotes midiendo el cupo (`get_usage`) y la corrida completa va por olas de subagentes de 10 a 20 lotes cada uno; si el cupo no alcanza, se parte en varios días. El modo con clave (`--enviar` / `--recoger`) sigue disponible.

## Fuera de alcance

- Atribución de las fuentes (los textos son MIT o AGPL de la comunidad): decidirla con el usuario antes de publicar la app.
- Traducir ejercicios que se agreguen después: se vuelve a correr el mismo script sobre la carpeta nueva del importador.
- Traducir en el teléfono (ML Kit) o al vuelo en el Worker: descartadas, ver la conversación del 12 nov.
