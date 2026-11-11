# NutriGajo

App Android para que un nutriólogo (o un entrenador) acompañe a sus pacientes: plan de alimentación y de ejercicio, citas, chat, seguimiento de carreras y actividad. Proyecto de prácticas profesionales.

| Carpeta | Qué hay |
|---|---|
| `app/` | La app Android (Kotlin, Jetpack Compose, Material 3). |
| `backend/` | Worker de Cloudflare (TypeScript): avisos push, borrado de cuenta, fotos y clips en R2, publicación de actualizaciones. |
| `firebase/` | Reglas de seguridad de Firestore y sus pruebas contra el emulador. |
| `docs/superpowers/` | Especificaciones y planes de trabajo de cada función. |

## Qué hace

- **Cuatro roles:** paciente, nutriólogo, entrenador y administrador (`Rol` en `domain/model/Enums.kt`).
- **Paciente:** menú del día y platillos, rutinas y ejercicios asignados, citas, chat con su nutriólogo, calorías, rachas, Modo carrera (ruta GPS en el mapa), reloj por Health Connect y directorio de nutriólogos en el mapa.
- **Nutriólogo / entrenador:** expediente de cada paciente, plan de N días en PDF, agenda de citas, banco de platillos y de ejercicios.
- **Cuenta:** vínculo cerrado con el nutriólogo (desvincularse, cambiar de nutriólogo), eliminar la cuenta con confirmación por enlace de correo, permisos pedidos solo cuando la función los necesita, actualizaciones dentro de la app.

## Tecnologías

Kotlin 2.2 · Compose (BoM 2026.02.01) · AGP 9.3 y Gradle 9.5 · Firebase (Auth, Firestore, Cloud Messaging; BoM 34.19) · Maps Compose · Health Connect · Cloudflare Workers + R2 · `jose` para firmar el token de la cuenta de servicio.

`minSdk` 29, `targetSdk` 36. Se abre con Android Studio (JDK 21).

## Antes de compilar: lo que no está en el repositorio

Estos archivos son secretos o de cada quien y están en `.gitignore`. Sin ellos la app no compila:

1. **`app/google-services.json`**: crea un proyecto en [Firebase](https://console.firebase.google.com), registra la app con el paquete `com.example.nutrigajo` y descarga el archivo a `app/`. Activa Authentication (correo y contraseña, y «Enlace de correo» para confirmar la eliminación de cuenta) y Firestore, y publica `firebase/firestore.rules`.
2. **`local.properties`**: copia `local.properties.example` y llena lo que uses (`MAPS_API_KEY` para el mapa, `WORKER_URL` si el Worker corre en otra dirección, y las cuatro `KEY*` solo para firmar un APK release).

El Worker apunta al proyecto `nutrigajo` y al bucket `appsmobile` de R2 (`backend/wrangler.toml`): cámbialos por los tuyos.

## Pruebas

```bash
# App: pruebas unitarias JVM (no necesitan teléfono ni cuenta)
./gradlew :app:testDebugUnitTest

# Worker: tipos y pruebas con un fetch simulado (no tocan Cloudflare ni Firebase)
cd backend
npm install
npm run check
npm run test:vinculos     # también test:cuenta, test:aviso, test:plan, test:chat, test:app,
                          # test:publicar, test:foto, test:clip, test:catalogo y test:catalogoRutas
# `npm test` es otra cosa: una prueba de humo contra un Worker en marcha (`npm run dev` en otra terminal)

# Reglas de Firestore: contra el emulador (necesita Java; levántalo en otra terminal)
npx firebase emulators:start --only auth,firestore --project nutrigajo
node firebase/test/vinculos.mjs     # una por archivo de firebase/test/
```

Si el puerto 8080 está ocupado, cambia el de Firestore en `firebase.json` y pásalo a las pruebas con `FS_PUERTO`.

## Worker de Cloudflare

```bash
cd backend
npm run dev                                    # http://localhost:8787, con R2 simulado
npx wrangler secret put FIREBASE_SERVICE_ACCOUNT   # JSON de la cuenta de servicio; sin él los avisos responden 503
npm run deploy
```

La clave de la cuenta de servicio, la llave de firma (`*.jks`) y `google-services.json` nunca se suben a git. Respalda el `.jks`: sin él no se pueden publicar actualizaciones de un APK ya compartido.
