# Actualizaciones internas de la app — diseño

Estado: **aprobado por el usuario el 6 nov 2026** (diseño y plan, con las propuestas de «Para validar» tal como están: APK en el R2 detrás
del Worker y público, actualización obligatoria incluida y la limitación de las firmas aceptada). Lo marcado *(mi decisión)* lo propuse yo.

## Para qué

La app se reparte como un APK firmado (el de Descargas) y no está en una tienda. Cada versión nueva obliga a pasar el archivo a mano a cada
teléfono, no hay forma de saber quién sigue con una versión vieja ni de retirar una que trae un fallo. El pedido es un **sistema de
actualizaciones dentro de la app**: que avise cuando hay una versión nueva, la descargue y la instale.

## Lo que ya existe y se reutiliza

- El release firmado y comprimido (`./gradlew :app:assembleRelease`, R8, 12,6 MB), firmado con `nutrigajo-release.jks`; el certificado
  (SHA-256 `87807ca1…`) es el del `NutriGajo-1.0.apk` ya repartido.
- El Worker `nutrigajo-api` y el bucket de R2 `appsmobile` (binding `FOTOS`): ya sirven fotos con rutas públicas de lectura.
- «Acerca de» (Configuración → Ayuda y soporte) ya muestra la versión de la app.
- `BuildConfig.WORKER_URL` y el patrón de los repositorios que llaman al Worker con `HttpURLConnection`.

Lo que **no** existe: versionado real (`versionCode = 1` y `versionName = "1.0"` fijos), el permiso de instalar paquetes, nada que publique
un APK ni que diga cuál es la última versión.

## El flujo

```
TÚ (computadora)                                     LA APP (cada teléfono)
1. compilas el release firmado con versionCode N
2. `npm run publicar-apk -- app-release.apk`   ──►   al abrir la sesión (como mucho cada 12 h) o en «Buscar actualizaciones»:
   · comprueba la firma y la versión                   GET /app/ultima  →  {versionCode, versionName, sha256, bytes, notas…}
   · sube apk/N.apk y apk/ultima.json a R2             ¿N > mi versión?  no → «Estás al día» (en silencio si fue automática)
                                                      sí → hoja «Hay una versión nueva» con las notas: [Actualizar] [Más tarde]
Worker (solo lectura)                                 Actualizar → descarga con avance → comprueba tamaño y SHA-256
· GET /app/ultima                                     → PackageInstaller → ventana de confirmación de Android → la app se reinicia
· GET /app/apk/N                                      nueva
```

Si `ultima.json` trae una **versión mínima** mayor que la instalada, la actualización es **obligatoria**: pantalla completa, sin «Más tarde».

## Metas

1. Publicar una versión es un solo comando y no se puede publicar por error un APK firmado con otra llave.
2. El usuario se entera de una versión nueva sin buscarla, la instala con dos toques y no pierde su sesión ni sus datos.
3. Un archivo corrupto, cortado o que no coincide con lo publicado nunca llega al instalador.
4. Se puede forzar una actualización cuando una versión vieja ya no debe usarse.

## Fuera de alcance (esta versión)

- Google Play (las actualizaciones dentro de la app de Play usan otra API; ver «Si un día va a Play»).
- Publicar desde la propia app o con el rol ADMIN; la subida va solo por la computadora, con tu cuenta de Cloudflare.
- Actualizaciones diferenciales, descarga que se reanuda, canales beta, despliegue por porcentajes y estadísticas de quién actualizó.
- Instalación silenciosa: Android pide confirmación siempre en la primera actualización (ver «Lo que Android no deja»).
- Firmar el manifiesto aparte: la firma del APK ya la comprueba Android (ver «Seguridad»).

## Lo que haría fracasar esto aunque «funcione»

- Publicar un APK con la llave de debug o con otra: ningún teléfono lo aceptaría y la gente se quedaría con el aviso.
- Que un usuario sin red, o con el Worker caído, vea errores o ventanas al abrir la app.
- Que el aviso insista cada vez que se abre la app con la misma versión, o que una actualización obligatoria deje a alguien sin salida.
- Que una instalación a medias deje la app rota o pierda la sesión.

## Decisiones

**Dónde vive** *(mi decisión)*: el APK y un `ultima.json` en el bucket que ya tienes (`apk/{versionCode}.apk`, `apk/ultima.json`), servidos
por el Worker en dos rutas **públicas de solo lectura**: `GET /app/ultima` y `GET /app/apk/{versionCode}`. Públicas porque quien no puede
iniciar sesión por un fallo tiene que poder actualizar justo para arreglarlo. El Worker **no acepta subidas** de APK.

**Cómo se publica** *(mi decisión)*: `backend/scripts/publicar-apk.mjs` (`npm run publicar-apk`). Lee del APK su paquete, `versionCode` y
`versionName` (con `aapt2 dump badging` del SDK), calcula SHA-256 y tamaño, **rechaza** un APK que no sea `com.example.nutrigajo`, cuyo
certificado no sea el del release (SHA-256 `87807ca1881078dc124af1a8d6da07cfe585bb5de68e27823bc097423793df3f`) o cuya versión no sea mayor
que la publicada, y sube los dos objetos con `wrangler r2 object put --remote`. `--solo-mostrar` enseña lo que subiría sin subir nada.

**Versionado** *(mi decisión)*: `versionCode` entero que sube de uno en uno con cada APK que se publica; `versionName` libre («1.1»).
Salen de `-PversionCode=N -PversionName=X.Y` al compilar (por defecto 1 y «1.0», como hoy); ya no se duplican a mano en ningún otro lado.

**Cuándo se busca** *(mi decisión)*: al entrar a la sesión, si pasaron 12 horas desde la última consulta, y a mano en «Acerca de → Buscar
actualizaciones». La automática no muestra nada si falla o si está al día. «Más tarde» recuerda esa versión y no vuelve a salir sola hasta
que haya otra; el aviso sigue en «Acerca de». La obligatoria no se pospone.

**Qué ve el usuario** *(mi decisión)*: una hoja inferior con la versión nueva, sus notas y el avance de la descarga; en «Acerca de», una fila
que dice «Estás al día» o «Versión X disponible». La obligatoria es una pantalla completa con el mismo botón; si la descarga falla, ofrece
reintentar y cerrar la app (no deja a nadie atrapado sin red).

## Seguridad

- Todo por HTTPS desde `BuildConfig.WORKER_URL`; la app **nunca** descarga de una dirección que venga en el manifiesto (arma ella misma
  `/app/apk/{versionCode}`, con el número validado).
- **La defensa de fondo es la firma:** Android solo instala una actualización firmada con la misma llave que la app instalada. Un APK
  malicioso, aunque lo sirviera el Worker, no se instala. El SHA-256 y el tamaño del manifiesto protegen de descargas corruptas, no de un
  Worker comprometido (viven en el mismo sitio).
- Se exige `versionCode` del manifiesto mayor que el instalado, y Android rechaza además los retrocesos.
- El archivo se descarga a la caché de la app (`cache/actualizaciones/`), se comprueba antes de entregarlo al instalador y se borra al
  terminar, al fallar o al abrir la app.
- El manifiesto se valida estrictamente (tipos, SHA-256 de 64 caracteres hexadecimales, tamaño entre 1 byte y 200 MB, notas acotadas):
  uno mal formado se ignora.

## Lo que Android no deja (y hay que decirlo)

- **La primera vez pide permiso** «Instalar apps desconocidas» para NutriGajo (`REQUEST_INSTALL_PACKAGES` + ajuste del sistema); la app
  lleva a esa pantalla y retoma al volver. Y cada instalación muestra la ventana de confirmación de Android (Android 12 o más puede
  saltarla en actualizaciones siguientes, pero no hay que contar con ello).
- **Solo se actualizan las instalaciones firmadas con la llave de release.** Los teléfonos con las variantes `debug` o `rapido` (firmadas
  con la llave de debug, como el tuyo hoy) no pueden recibir el APK de release: la app dirá «Esta versión no se puede actualizar desde la
  app» y habrá que instalar el release a mano, **una sola vez**, lo que obliga a desinstalar la de debug (y cierra la sesión local).
- Play Protect puede avisar de una app de «fuente desconocida».
- Si un día va a Google Play: su política prohíbe que una app se actualice por su cuenta; entonces se compila sin esta función
  (`-PsinActualizacionInterna=true` apaga la consulta y el aviso; habría que quitar también el permiso con un *flavor*).

## Datos

`apk/ultima.json` (lo escribe el script; lo lee el Worker y la app):

```json
{ "versionCode": 2, "versionName": "1.1", "sha256": "<64 hex>", "bytes": 13349873,
  "notas": ["Citas agendadas por tu nutriólogo", "Arreglos de rendimiento"], "minVersionCode": 0, "publicadaEn": 1793900000000 }
```

`minVersionCode` (0 si no se quiere forzar a nadie): una versión instalada menor que él ve la actualización obligatoria.

## Worker (`backend/`) *(se despliega solo con tu OK)*

- `GET /app/ultima`: devuelve `apk/ultima.json` (`application/json`, `Cache-Control: public, max-age=300`); 404 si aún no hay.
- `GET /app/apk/{versionCode}` (solo dígitos, 1 a 9): devuelve el APK (`application/vnd.android.package-archive`, con su tamaño y `ETag`,
  `Cache-Control: public, max-age=31536000, immutable` porque el número va en la dirección); 404 si no existe o el número no es válido.
- Sin ruta de subida. Lo publicado solo cambia con el script y tu cuenta de Cloudflare.

## App

- **Dominio** (`domain/Actualizacion.kt`): el manifiesto, su lectura estricta y la decisión «al día / disponible / obligatoria».
- **Datos:** `ActualizacionRepository` (consultar y descargar con comprobación) e `InstaladorApk` (sesión de `PackageInstaller`,
  `InstalacionReceiver` que recibe el resultado y lanza la ventana de confirmación).
- **Pantallas:** `ActualizacionViewModel` (una sola instancia para la actividad), hoja de actualización, pantalla obligatoria y la fila
  de «Acerca de». Se enganchan en `MainActivity` cuando hay sesión (`Paso.INICIO`).
- **Textos** en `strings.xml`; mensajes claros para: sin red, archivo que no coincide, falta de espacio, permiso sin dar, firma distinta
  y cancelación.

## Pruebas

- Unitarias: lectura estricta del manifiesto (válido y cada campo malo), decisión de estado (incluida la obligatoria), cada cuándo se
  consulta y la versión pospuesta, comprobación de archivo (tamaño y SHA-256) y el mapeo de resultados del instalador.
- Worker: `backend/test/app.mjs` con un R2 falso (rutas, 404, tipos, caché, números de versión inválidos).
- Script de publicación: `backend/test/publicar.mjs` (armado del manifiesto y lectura de `aapt2 dump badging`).
- En el teléfono (con tu permiso): dos APK de debug con la misma llave (versión 1 y 2) y un Worker de mentira; se ve el aviso, la
  descarga, el permiso, la confirmación y que la app queda en la versión 2 **con tu sesión**.

## Fases

1. Dominio y comprobaciones puras. 2. Worker. 3. Script de publicación y versionado de Gradle. 4. Datos e instalador en la app.
5. ViewModel y pantallas. 6. Verificación en el teléfono. 7. Despliegue del Worker y primera publicación (cada uno con tu OK).

## Para validar

1. **¿Dónde se aloja el APK?** Propongo el R2 que ya tienes, detrás de tu Worker y público. La alternativa es otro sitio (por ejemplo
   GitHub Releases); cambia solo el Worker y el script.
2. **¿Entra la actualización obligatoria en esta versión?** Propongo que sí (un campo más y una pantalla); sin ella, una versión con un
   fallo grave solo se puede avisar, no forzar.
3. **¿Aceptas la limitación de las firmas?** Para que tu teléfono pueda actualizarse hay que instalar una vez el release firmado (hoy
   tienes `debug`/`rapido`), lo que cierra tu sesión local. Los teléfonos que ya usen el APK de Descargas firmado sí podrán actualizarse.

## Mis llamadas (puedes cambiarlas)

Rutas públicas · sin subida desde la red · 12 horas entre consultas · «Más tarde» por versión · hoja inferior para lo opcional y pantalla
completa para lo obligatorio · la firma del APK como defensa principal · el script rechaza lo que no sea el release de la llave conocida ·
`versionCode` por propiedad de Gradle.
