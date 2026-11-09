import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

// Secretos locales (fuera de git): cada quien pone su MAPS_API_KEY en local.properties.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

ksp {
    // esquemas versionados en git: base para migraciones y para revisar cambios del modelo
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.example.nutrigajo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.nutrigajo"
        minSdk = 29
        targetSdk = 36
        // ./gradlew :app:assembleRelease -PversionCode=2 -PversionName=1.1 (sube con cada APK que se publica: así se sabe cuál es más nuevo, ver
        // backend/scripts/publicar-apk.mjs); sin las propiedades, 1 y «1.0»
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = project.findProperty("versionName") as String? ?: "1.0"
        // sin clave el mapa se dibuja en blanco, pero la app compila
        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY", "")
        // Worker de Cloudflare que guarda los archivos en R2; por defecto el de `npm run dev` en backend/ (con adb reverse tcp:8787 tcp:8787)
        // ./gradlew installDebug -PworkerUrl=http://localhost:8787 lo cambia para una sola compilación (pruebas con un Worker de mentira)
        val workerUrl = project.findProperty("workerUrl") as String? ?: localProperties.getProperty("WORKER_URL", "http://localhost:8787")
        buildConfigField("String", "WORKER_URL", "\"$workerUrl\"")
        // la app busca e instala sus propias actualizaciones (APK repartido a mano, sin tienda); -PsinActualizacionInterna=true la apaga (una
        // versión para Google Play no puede actualizarse por su cuenta)
        buildConfigField("boolean", "ACTUALIZACION_INTERNA", "${project.findProperty("sinActualizacionInterna") != "true"}")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // llave del release: ruta y contraseñas en local.properties (fuera de git); sin ellas el release sale sin firmar
        localProperties.getProperty("KEYSTORE_FILE")?.let { archivo ->
            create("release") {
                storeFile = file(archivo)
                storePassword = localProperties.getProperty("KEYSTORE_PASSWORD")
                keyAlias = localProperties.getProperty("KEY_ALIAS")
                keyPassword = localProperties.getProperty("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // ./gradlew installDebug -PemuladorFirebase=true apunta Auth y Firestore a los emuladores locales
            // (npx firebase emulators:start + adb reverse tcp:9099 y tcp:8080); sin la propiedad usa el Firebase real
            buildConfigField("boolean", "USAR_EMULADOR", "${project.findProperty("emuladorFirebase") == "true"}")
        }
        // como el debug (misma firma, así se instala encima sin cerrar sesión) pero no depurable: Compose en debug va varias veces más lento,
        // y el lag de scroll solo se mide bien así. ./gradlew installRapido
        create("rapido") {
            initWith(getByName("debug"))
            isDebuggable = false
            matchingFallbacks += "debug"
        }
        release {
            buildConfigField("boolean", "USAR_EMULADOR", "false")
            signingConfig = signingConfigs.findByName("release")
            // R8 (código) y recorte de recursos: el APK pasa de unos 48 MB a unos 13 MB
            optimization {
                enable = true
            }
            isShrinkResources = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.health.connect)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.maps.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    // el org.json de Android no funciona en la JVM de las pruebas (lanza «not mocked»): la lectura del manifiesto de actualización lo necesita
    testImplementation("org.json:json:20240303")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}