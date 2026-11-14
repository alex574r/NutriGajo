# Avisos de terceros

El código y la documentación de NutriGajo están bajo la licencia MIT (ver [LICENSE](LICENSE)). **Esa licencia no cubre lo que se lista aquí**: cada cosa conserva la licencia de quien la hizo.

## Catálogo de ejercicios (no está en este repositorio)

La app descarga el catálogo de ejercicios de un bucket de Cloudflare R2 propio, a través del Worker de `backend/`. Los archivos del catálogo no se publican en este repositorio. Los scripts `backend/scripts/importar-catalogo.mjs` y `backend/scripts/traducir-catalogo.mjs` preparan el catálogo a partir de una copia local de [openGym](https://github.com/DuarteSantos8/openGym).

### Textos (nombres, músculos, descripciones y pasos)

- **openGym**, © 2026 Duarte Santos, bajo la **GNU AGPL v3.0**. Su catálogo lo edita su comunidad. Las descripciones, los ejercicios nuevos, las correcciones de músculos y las traducciones se escribieron para openGym y están cubiertos por su AGPL.
- Las entradas marcadas `"textSource": "exercisedb"` (1,324 ejercicios) conservan nombres, músculos y pasos que vienen de [ExerciseDB v1](https://exercisedb.dev/), de AscendAPI, a través de [hasaneyldrm/exercises-dataset](https://github.com/hasaneyldrm/exercises-dataset), bajo la licencia **MIT**, © 2026 Hasan Emir Yıldırım.
- **Traducción al español:** las descripciones y los pasos que estaban en inglés se tradujeron al español con Claude (Anthropic) el 12 nov 2026. Las traducciones derivan de esos textos y siguen sus mismos términos de licencia.

### Fotos y animaciones de los ejercicios

Son de **Gym visual** (gymvisual.com), © Aliaksandr Makatserchyk. **No están cubiertas por la licencia MIT de este repositorio ni por la AGPL de openGym.** Se usan con la autorización que su titular dio al autor de esta app. En este repositorio no hay copias, y nadie más puede reutilizarlas desde aquí: quien las quiera debe obtener su propia licencia en [gymvisual.com](https://gymvisual.com/). Tampoco pueden usarse para alimentar a un sistema de IA.

## Imágenes de la app

Las fotografías de `app/src/main/res/drawable-nodpi/` (por ejemplo `ej_*.jpg`, `exp_*.jpg` y `carrera_tarjeta.webp`) no están cubiertas por la licencia MIT de este repositorio. Sus derechos pertenecen a sus autores o titulares.

## Fuente

**Inter** (Inter Regular 4.001), © 2016 The Inter Project Authors ([github.com/rsms/inter](https://github.com/rsms/inter)), bajo la **SIL Open Font License 1.1** ([texto oficial](https://openfontlicense.org/open-font-license-official-text/)). Archivo: `app/src/main/res/font/inter.ttf`.

## Dependencias

Jetpack Compose, Firebase, Maps Compose, Media3, Health Connect, `jose` y las demás bibliotecas conservan sus propias licencias (en su mayoría Apache-2.0 y MIT). Los servicios de Google (Firebase y Maps) y de Cloudflare se usan bajo sus propios términos.
