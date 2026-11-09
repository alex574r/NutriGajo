# Permisos: cuáles son obligatorios, cómo se piden y una pantalla para verlos — diseño

Fecha: 2026-11-08. Estado: **borrador para revisión del usuario**; nada de esto está construido.

## Para qué

Hoy cada pantalla pide su permiso a su manera, y ninguna explica qué pasa si se niega ni cómo arreglarlo después:

- **Modo carrera** (`CarreraScreens.kt`, `rememberArranque`) pide la ubicación al tocar «Comenzar»; si se niega, solo sale un aviso corto (`carrera_permiso`).
- **Mapa de nutriólogos** (`MapaUtil.kt`, `rememberUbicacionConcedida`) la pide cada vez que se abre, aunque sea opcional.
- **Notificaciones** se piden solas al abrir el inicio del paciente (`InicioPacienteScreen.kt`) y al solicitar una cita (`AgendarCitaSheet.kt`).
- **Tu reloj** (`RelojScreen.kt`) ya tiene sus fases (instalar Health Connect, dar permiso, error) con su propio texto.
- **Instalar actualizaciones** tiene su flujo en `InstaladorApk.kt` y la hoja de actualización.

Se quiere un solo criterio: cada permiso se pide en su momento y con su explicación, los obligatorios bloquean **solo la función que los necesita** diciendo qué falta y cómo darlo, y hay una pantalla «Permisos» en Configuración con el estado de cada uno.

## Metas, fuera de alcance y lo que sería un fracaso

**Metas.** Quien niega un permiso nunca se queda sin saber por qué una función no abre ni cómo arreglarlo. Quien lo niega «para siempre» llega a los ajustes del sistema con un botón. Ningún permiso se pide dos veces seguidas sin que la persona haya hecho algo.

**Fuera de alcance.** Ubicación en segundo plano (el servicio en primer plano de Modo carrera no la necesita). Cámara y almacenamiento (la app no los usa: las fotos salen del selector del sistema). Bloquear toda la app por un permiso. La huella o el rostro al abrir (es un ajuste de la app, no un permiso que se pida al sistema). Cambiar los permisos que declara el manifiesto.

**Sería un fracaso aunque «funcione».** Pedir varios permisos juntos al abrir la app. Insistir con recordatorios que molestan. Decir que algo «es obligatorio» cuando la función sí abre sin él, o dejar abrir una función que no puede trabajar sin él.

## Lo que decidió el usuario

- **Obligatorios solo para su función:** la ubicación en Modo carrera y Health Connect en «Tu reloj». Sin ellos, esa función no arranca.
- **Notificaciones: recomendadas.** Nunca bloquean nada.
- **Hay una pantalla «Permisos» en Configuración** con el estado de cada permiso.

## Mis llamadas (el usuario puede cambiarlas)

1. **Tres niveles.** `OBLIGATORIO` (bloquea su función), `RECOMENDADO` y `OPCIONAL`. Cada permiso del catálogo lleva el suyo.
2. **Catálogo (un enum `PermisoApp`):** `UBICACION_CARRERA` (obligatorio; exige la precisa, la aproximada no basta), `UBICACION_MAPA` (opcional; el mapa abre igual y solo no se centra en la persona), `NOTIFICACIONES` (recomendado), `RELOJ` (Health Connect; obligatorio para «Tu reloj») y `ACTUALIZACIONES` (instalar la actualización de la app; se pide en el momento de actualizar, y aquí solo se muestra su estado).
3. **Cuatro estados:** `CONCEDIDO`; `FALTA` (se puede pedir y el sistema mostrará su cuadro); `BLOQUEADO` (la persona ya lo negó y Android no volverá a preguntar: solo se arregla en los ajustes); `NO_DISPONIBLE` (Health Connect sin instalar, o un teléfono que no lo admite). Android no dice directamente que algo quedó bloqueado, así que se deduce: la app guarda si **ya lo pidió alguna vez** y, si ya lo pidió y el sistema ya no ofrece explicar (`shouldShowRequestPermissionRationale` falso), está `BLOQUEADO`. La decisión es una función pura sin dependencias de Android, con pruebas.
4. **Un solo componente.** `rememberPermiso(permiso)` devuelve el estado y una acción `pedir()`, y vuelve a calcular el estado cada vez que la app regresa al frente (`ON_RESUME`), que es como se entera de que se arregló en los ajustes. Un panel `PanelPermiso` (explicación + botón) lo usan todas las pantallas, con los textos del catálogo.
5. **Qué hace el botón según el estado.** `FALTA` → «Dar permiso» (cuadro del sistema). `BLOQUEADO` → «Abrir ajustes» (la pantalla de la app en el sistema; para Health Connect, su propia pantalla de permisos). `NO_DISPONIBLE` → «Instalar» (Play) para Health Connect. `CONCEDIDO` → nada.
6. **Obligatorios, función por función.**
   - *Modo carrera:* al tocar «Comenzar» sin el permiso aparece el panel con la explicación en lugar de lanzar directo el cuadro del sistema; al concederlo, la actividad que se iba a empezar sigue sola (es lo que ya hace `pendiente`). Si el GPS del teléfono está apagado, el aviso que ya existe gana un botón que abre los ajustes de ubicación.
   - *Tu reloj:* conserva sus fases; el texto de la fase de permiso pasa a decir que es necesario para esta pantalla, y el estado «bloqueado» abre los ajustes de Health Connect.
7. **Notificaciones.** Ya no se piden solas al abrir el inicio: la primera vez aparece una hoja con la explicación («Activar» / «Ahora no»). «Ahora no» no vuelve a insistir; el estado queda visible en «Permisos» como «Falta». En Android 12 y anteriores no hay permiso que pedir y cuenta como concedido. Al pedir una cita se sigue ofreciendo, con el mismo panel.
8. **Mapa.** Pide la ubicación solo la primera vez que se abre (usando el «ya lo pidió»); las siguientes aperturas no vuelven a preguntar. Quien la quiera después la activa desde «Permisos».
9. **Pantalla «Permisos» (Configuración, todos los roles).** Una fila por permiso que le toca a ese rol: el paciente ve notificaciones, ubicación, reloj y actualizaciones; nutriólogo, entrenador y administrador ven notificaciones, ubicación (para el mapa y el consultorio) y actualizaciones. Cada fila dice el nivel («Obligatorio para Modo carrera», «Recomendado», «Opcional»), su estado y el botón que corresponde. Se recalcula al volver de los ajustes.
10. **Quitar el permiso a mitad de una carrera.** Android cierra la app cuando el permiso se retira desde los ajustes. Qué conserva la carrera depende de cuánto guarda mientras avanza; quien construya lo comprueba. Si la ruta no se guarda de a poco, queda como límite conocido y no se arregla aquí.

## Textos (los fija quien construya, en español de México)

Cada permiso del catálogo trae: título, **para qué se pide** («Para dibujar tu ruta y medir la distancia aunque apagues la pantalla»), **qué pasa sin él** y el texto de la acción. Los obligatorios dicen «Necesitamos esto para abrir X»; los recomendados, «Te avisamos de…».

## Cómo se prueba

- **Lógica pura (JVM):** el estado a partir de (concedido, ya lo pidió, el sistema todavía explica), qué permisos le tocan a cada rol y que solo `UBICACION_CARRERA` y `RELOJ` son obligatorios.
- **Teléfono, sin tocar datos:** `adb shell pm revoke` y `pm grant` de cada permiso de ejecución (nunca `pm clear`: borra la llave de la sesión) para ver `FALTA` y `CONCEDIDO`; `BLOQUEADO` se ve negando el cuadro dos veces. Health Connect sin instalar no se puede ver en este teléfono (ya lo tiene): se revisa por el estado `NO_DISPONIBLE` con una prueba de la lógica.
- **Orientación:** mirar que el teléfono no esté en horizontal antes de mandar toques con adb.

## Lo que queda a quien construya

La forma de la hoja y del panel (siguiendo el estilo de la app: naranja, Inter, esqueleto al cargar), los textos finales, dónde guardar el «ya lo pidió» (junto a los demás ajustes de la app, `AjustesApp`) y el nombre de las funciones.
