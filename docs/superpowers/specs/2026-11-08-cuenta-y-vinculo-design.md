# Eliminar la cuenta, desvincularse y cambiar de nutriólogo — diseño

Fecha: 2026-11-08. Estado: **borrador para revisión del usuario**; nada de esto está construido.

## Para qué

Hoy no existe ninguna forma de borrar la cuenta ni de dejar a un nutriólogo. `VinculacionRepository.canjear` solo crea el vínculo con el código de 4 dígitos, `vinculoActual` toma el primero que encuentra y las reglas de Firestore prohíben borrar usuarios, pacientes, mensajes, planes y códigos desde la app (`allow delete: if false`). Un paciente que se equivoca de nutriólogo, o que quiere irse, no puede.

Se quieren tres cosas:

1. **Eliminar la cuenta** (paciente, nutriólogo o entrenador), confirmándolo desde el correo de la cuenta.
2. **Desvincularse** del nutriólogo (el paciente, y también el nutriólogo puede dar de baja a un paciente).
3. **Cambiar de nutriólogo**: dejar al actual y vincularse a otro con su código.

## Metas, fuera de alcance y lo que sería un fracaso

**Metas.** Las tres cosas funcionan de punta a punta. Nadie se queda sin poder volver a vincularse. Lo clínico no se pierde por decisión de una sola persona.

**Fuera de alcance.** Borrar la cuenta de ADMIN (se sigue haciendo a mano en la consola). Recuperar una cuenta borrada. Exportar los datos del usuario. Límite de pacientes por nutriólogo. Cambiar la plantilla del correo de Firebase (la consola no deja editarla en este proyecto).

**Sería un fracaso aunque «funcione».** Borrar el expediente, los planes o el chat de un paciente porque se fue. Mandar correos desde un servidor propio (Resend, Gmail, etc.). Que una prueba borre una cuenta real (Kelly, Alejandro o cualquiera del usuario). Dejar una cuenta a medio borrar que no pueda entrar ni volver a registrarse.

## Lo que decidió el usuario

- **Se conserva cerrado.** Al desvincularse, cambiar de nutriólogo o borrar la cuenta, el expediente, los planes, las mediciones y el chat **no se borran**: el registro queda cerrado y de solo lectura.
- **Cambiar de nutriólogo.** Al cambiar, una casilla «Compartir mi expediente con el nuevo nutriólogo», apagada por defecto. Encendida, se copian al vínculo nuevo solo los datos del expediente; el chat, los planes y las mediciones nunca pasan. El nuevo nutriólogo no ve el historial del anterior.
- **Lo publicado se queda.** Al borrar su cuenta, los platillos y ejercicios ya publicados en los bancos globales se mantienen, con autor «Cuenta eliminada».
- **Confirmar con el correo de Firebase.** Se usa el correo propio de Firebase, que manda **enlaces, no códigos**. Sin servidor de correo propio ni plan Blaze (el proyecto está en Spark; ya lo había descartado el 25 de octubre de 2026). El usuario aceptó confirmar con un enlace en lugar de un código.
- **Desvincularse y cambiar piden solo una confirmación en pantalla**, sin correo (esto lo propuse yo y el usuario no lo objetó).

## Restricciones del proyecto

- Firebase Auth con correo y contraseña; Firestore en plan Spark; Worker de Cloudflare con la cuenta de servicio de Firebase (token con alcance `cloud-platform`) y R2.
- El Worker hoy solo sabe **leer y actualizar** un documento y mandar push (`backend/src/lib/firestore.ts`); borrar y consultar es nuevo.
- Reglas de Firestore y despliegue del Worker: solo con el visto bueno explícito del usuario cada vez. Sin push de git; sin commit hasta que lo pida.
- Interfaz: naranja `#F97316`, Inter, Esqueleto (no spinner) al cargar, textos en español de México.

## Mis llamadas (el usuario puede cambiarlas)

1. **Cerrar un vínculo** = el documento `pacientes/{id}` pasa a `activo = false` con `cerradoEn` y `cerradoPor` (`PACIENTE`, `NUTRIOLOGO` o `CUENTA_ELIMINADA`). No se borra. Es el «archivado» que las reglas ya anunciaban («Sin baja por ahora (llega con el archivado)»).
2. **Las reglas de escritura sobre un vínculo exigen `activo == true`** (expediente, metas, último mensaje, mensajes nuevos, planes nuevos). Las de lectura no cambian: los dos siguen leyendo el registro cerrado.
3. **Una cuenta eliminada la borra el Worker**, no la app, porque las reglas no dejan borrar y porque son muchos documentos. La app solo pide y confirma.
4. **Citas.** Al cerrar un vínculo se cancelan las citas vigentes entre los dos (con la lógica y las reglas de cancelación que ya existen). Al borrar la cuenta de un **paciente** se borran todas sus citas y su candado `citasActivas`. Al borrar la de un **nutriólogo** se cancelan sus citas pendientes y confirmadas y se avisa a los pacientes.
5. **Comentarios** que el usuario envió desde Configuración se borran con su cuenta.
6. **Borradores del autor.** Al borrar la cuenta de un nutriólogo o entrenador, sus ejercicios y platillos en BORRADOR, EN_REVISION o RECHAZADO se borran (con sus archivos en R2). Los PUBLICADOS se quedan con `autorNombre = "Cuenta eliminada"` y **conservan `autorId`**, porque las fotos y clips en R2 están bajo el uid del autor.
7. **Avisos push** a la otra parte al cerrar un vínculo («X dejó de ser tu paciente» / «Tu nutriólogo cerró el vínculo»), por el mismo camino que los avisos de citas: el Worker con su cuenta de servicio. Qué canal de notificación usar lo decide quien construya.
8. **Un solo vínculo activo por paciente** se sigue garantizando como hoy: la app solo ofrece canjear código a quien no tiene vínculo activo. No hay candado en las reglas (techo conocido; se añadiría uno parecido a `citasActivas` si hiciera falta).
9. **Qué se ve de lo cerrado en la primera versión.** Las reglas dejan leerlo; las pantallas son simples: el paciente ve «Planes anteriores» (solo lectura) y el nutriólogo ve una lista «Pacientes anteriores» (solo lectura). Si esto crece, se puede dejar para una segunda versión sin romper el resto.

## Flujo 1 — Eliminar la cuenta

Para paciente, nutriólogo y entrenador. Ajustes → «Eliminar mi cuenta». El ADMIN no ve esa opción.

1. **Aviso.** La pantalla dice qué se borra y qué se conserva según el rol. Al nutriólogo le dice además cuántos pacientes tiene vinculados y que cada vínculo se cierra.
2. **Pedir el correo.** El botón «Enviarme el enlace de confirmación» llama a Firebase (`sendSignInLinkToEmail`) con el correo de la cuenta. Firebase manda el correo con su plantilla.
3. **Abrir el enlace.** El enlace abre la app (enlaces de la app sobre el dominio de Hosting del proyecto) y la app lo usa para **volver a confirmar la identidad** (`reauthenticateWithCredential` con `EmailAuthProvider.getCredentialWithLink`). Si el enlace no abre la app, hay una casilla «Pega aquí el enlace del correo» como plan B.
4. **Confirmación final.** Tras la reautenticación se muestra un último paso, «Eliminar definitivamente», con un botón en rojo.
5. **Borrado.** La app llama `POST /cuenta/eliminar` al Worker con su token de sesión. El Worker comprueba que el token es válido y que la confirmación de identidad es **reciente** (`auth_time` de hace 5 minutos o menos); si no, responde 401 y la app vuelve al paso 2.
6. **Después.** La app cierra sesión (borra su estado local, incluido el token de notificaciones) y vuelve al inicio de sesión.

### Qué borra el Worker, por rol

El orden importa: **primero lo de adentro, y al final `usuarios/{uid}` y la cuenta de Auth**. Así, si algo falla a la mitad, la cuenta sigue entrando y se puede reintentar; el endpoint es idempotente.

**Paciente**
- Cierra sus vínculos (todos los `pacientes` con su `usuarioId`, también los que ya estaban cerrados): `activo = false`, `cerradoPor = CUENTA_ELIMINADA` (los ya cerrados conservan su motivo original) y **quita `usuarioId`, `correoVinculacion` y `codigoVinculacion`** del registro (queda con el `nombre`, para que no apunte a una cuenta que ya no existe). El expediente, los planes, las mediciones y el chat se quedan.
- Borra sus citas y su candado `citasActivas`, libera las horas en `ocupados`, y borra sus comentarios.
- Borra `usuarios/{uid}` con sus subcolecciones (`entrenamientos`, `carreras`, `modoCarrera`, `banco`) y su avatar y portada en R2.
- Borra la cuenta de Auth.
- Avisa a cada nutriólogo cuyo vínculo se cerró.

**Nutriólogo**
- Cierra los vínculos de sus pacientes (`cerradoPor = CUENTA_ELIMINADA`). Cada paciente conserva su registro en solo lectura y puede volver a vincularse.
- Cancela sus citas pendientes y confirmadas, libera sus horas, y avisa a los pacientes (cierre de vínculo y cancelación en un solo aviso).
- Borra su consultorio (`directorio/{uid}` y `ocupados`), sus códigos de vinculación sin usar, su solicitud de verificación y **libera la reserva de su cédula** (`cedulas/{numero}`) para que otra cuenta pueda usarla.
- Borrados de autor, como en «Mis llamadas» 6; borra `usuarios/{uid}` con subcolecciones, avatar y portada, y la cuenta de Auth.

**Entrenador**
- Borrados de autor (llamada 6), su solicitud de verificación, `usuarios/{uid}` con subcolecciones, avatar y portada, y la cuenta de Auth.

## Flujo 2 — Desvincularse

**Paciente.** Ajustes → «Mi nutriólogo» → «Desvincularme». Confirmación en pantalla que dice lo que pasa. Un solo lote: cierra el vínculo, cancela sus citas vigentes con ese nutriólogo (y libera su candado y la hora). Después la app avisa al nutriólogo (push) y el paciente vuelve a la pantalla de «Vincular» que ya existe.

**Nutriólogo da de baja a un paciente.** En el expediente del paciente → «Dar de baja». Confirmación en pantalla. Cierra el vínculo con `cerradoPor = NUTRIOLOGO` y cancela o rechaza las citas con ese paciente. El paciente recibe un aviso y queda sin nutriólogo.

## Flujo 3 — Cambiar de nutriólogo

Ajustes → «Mi nutriólogo» → «Cambiar de nutriólogo».

1. El paciente escribe el **código de 4 dígitos del nuevo** nutriólogo. La app lo valida (lo mismo que `canjear` hoy: inválido, usado, vencido) **antes de tocar nada**.
2. La pantalla muestra «Dejarás a {actual} y pasarás con {nuevo}», la casilla «Compartir mi expediente con el nuevo nutriólogo» (apagada) y la confirmación.
3. Un **solo lote**: cierra el vínculo anterior, marca el código como usado, crea el vínculo nuevo y, si la casilla está encendida, escribe en él solo el mapa `expediente` copiado del anterior (con `actualizadoPor = PACIENTE`). Si algo falla, no pasa nada de lo anterior: el paciente nunca se queda sin nutriólogo a medias.
4. Se cancelan las citas vigentes con el anterior y se le avisa por push.

## Cambios en lo que ya existe

**Reglas de Firestore** (`firebase/firestore.rules`, se publican solo con el visto bueno del usuario):
- `pacientes`: nueva regla de actualización que deja a una de las dos partes cambiar solo `activo` (de verdadero a falso), `cerradoEn`, `cerradoPor` y `ultimaModificacion`. La de crear admite además `expediente` opcional (validado con `expedienteValido`). Las reglas de escritura existentes (expediente, metas, último mensaje, mensajes, planes) agregan `activo == true`.
- `citas`: sin cambios, las reglas de cancelar y rechazar que ya hay alcanzan.

**App.** `vinculoActual()` y `pacientes()` filtran por `activo == true`; **todo lugar que consulta `pacientes`** (agenda, expediente, chat, planes, citas, metas) debe revisarse para no mostrar ni escribir en un vínculo cerrado. `VinculacionRepository` gana `cerrarVinculo` y `cambiarNutriologo`; `AuthRepository`, las dos funciones de la confirmación por correo.

**Worker** (`backend/`): `POST /cuenta/eliminar`; `POST /vinculos/notificar`; `lib/firestore.ts` aprende a consultar y borrar documentos (y a borrar objetos de R2 y la cuenta de Auth por la API de administración). Pruebas sin red, como las de `test:aviso`.

## Riesgo principal y la prueba que va primero

Todo el Flujo 1 depende de que la confirmación por enlace funcione **en este proyecto**. Antes de construir lo demás hay una prueba corta («spike») con una cuenta desechable:

1. «Enlace de correo» (inicio de sesión sin contraseña) activado en la consola de Authentication (lo activa el usuario; yo no cambio la consola).
2. El enlace llega, abre la app o se puede pegar, y `reauthenticateWithCredential` actualiza el `auth_time` del token.
3. Los enlaces con el dominio de Hosting funcionan sin Dynamic Links (Firebase los cerró el 25 de agosto de 2025; las versiones de Firebase de la app ya los soportan). Un proyecto antiguo puede necesitar un ajuste de configuración.

**Si el spike falla, se vuelve a hablar con el usuario**; no se propone otra vía de correo por cuenta propia mientras él no lo pida.

**Dos consecuencias de activar «Enlace de correo» que quien construya debe comprobar.** Se añade una segunda forma de entrar con solo tener el correo (el restablecer contraseña ya lo permitía) y completar un enlace con un correo sin cuenta puede crear una cuenta de Auth sin perfil. Las reglas ya niegan casi todo sin `usuarios/{uid}` con rol, así que el efecto esperado es nulo, pero conviene verificarlo.

## Cómo se prueba

- **Reglas:** pruebas contra el emulador (`firebase/test/`), con casos de cada parte (paciente, nutriólogo, ajeno) cerrando, escribiendo en un vínculo cerrado y cambiando de nutriólogo.
- **Worker:** pruebas sin red de cada paso del borrado por rol y de la idempotencia (reintentar tras un fallo a la mitad), y del rechazo si `auth_time` es viejo.
- **Lógica pura:** las funciones que deciden qué borrar o cerrar por rol, con pruebas de JVM.
- **Teléfono:** solo con cuentas de prueba nuevas (`qa.*` o desechables) en el emulador; **nunca con Kelly, Alejandro ni otra cuenta real**. El borrado real en producción se prueba una vez, con una cuenta desechable, con el visto bueno del usuario.

## Lo que queda a quien construya

La forma exacta de las pantallas de Ajustes («Mi nutriólogo», «Eliminar mi cuenta»), siguiendo el estilo de la app; la librería de enlaces de la app en Android; el canal de notificación del aviso; el orden fino de los borrados dentro de cada rol; y cuántos documentos se borran por lote.
