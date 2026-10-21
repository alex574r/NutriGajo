package com.example.nutrigajo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.nutrigajo.ui.shared.motivacion.MotivacionScreen
import com.example.nutrigajo.ui.shared.onboarding.BienvenidaScreen
import com.example.nutrigajo.ui.shared.onboarding.OnboardingScreen
import com.example.nutrigajo.ui.shared.registro.RolScreen
import com.example.nutrigajo.ui.shared.splash.SplashScreen
import com.example.nutrigajo.ui.theme.NutriGajoTheme

private const val CLAVE_ONBOARDING_VISTO = "onboarding_visto"

private enum class Paso { SPLASH, MOTIVACION, BIENVENIDA, ONBOARDING, ROL, INICIO }

class MainActivity : ComponentActivity() {
    // ponytail: bandera en SharedPreferences; pasa a DataStore/repositorio de sesión cuando exista el login
    private val prefs by lazy { getSharedPreferences("nutrigajo", MODE_PRIVATE) }
    private val onboardingVisto get() = prefs.getBoolean(CLAVE_ONBOARDING_VISTO, false)

    private fun marcarOnboardingVisto() = prefs.edit().putBoolean(CLAVE_ONBOARDING_VISTO, true).apply()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NutriGajoTheme {
                // ponytail: pasos locales; el NavHost (sesión y rol) los reemplaza cuando exista el login
                var paso by rememberSaveable { mutableStateOf(Paso.SPLASH) }
                when (paso) {
                    Paso.SPLASH -> SplashScreen(onTerminar = { paso = Paso.MOTIVACION })
                    Paso.MOTIVACION -> MotivacionScreen(
                        onContinuar = { paso = if (onboardingVisto) Paso.INICIO else Paso.BIENVENIDA },
                    )
                    Paso.BIENVENIDA -> BienvenidaScreen(
                        onComenzar = { paso = Paso.ONBOARDING },
                        onIngresar = {
                            marcarOnboardingVisto()
                            paso = Paso.INICIO
                        },
                    )
                    Paso.ONBOARDING -> OnboardingScreen(
                        onVolver = { paso = Paso.BIENVENIDA },
                        onTerminar = { paso = Paso.ROL },
                    )
                    // ponytail: el rol elegido se descarta; pasa a la pantalla de registro cuando exista
                    Paso.ROL -> RolScreen(
                        onVolver = { paso = Paso.ONBOARDING },
                        onContinuar = {
                            marcarOnboardingVisto()
                            paso = Paso.INICIO
                        },
                    )
                    Paso.INICIO -> Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Greeting(
                            name = "Android",
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NutriGajoTheme {
        Greeting("Android")
    }
}