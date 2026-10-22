package com.example.nutrigajo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.ui.shared.auth.AuthViewModel
import com.example.nutrigajo.ui.shared.login.LoginScreen
import com.example.nutrigajo.ui.shared.motivacion.MotivacionScreen
import com.example.nutrigajo.ui.shared.onboarding.BienvenidaScreen
import com.example.nutrigajo.ui.shared.onboarding.OnboardingScreen
import com.example.nutrigajo.ui.shared.registro.RegistroScreen
import com.example.nutrigajo.ui.shared.registro.RolScreen
import com.example.nutrigajo.ui.shared.splash.SplashScreen
import com.example.nutrigajo.ui.theme.NutriGajoTheme

private const val CLAVE_ONBOARDING_VISTO = "onboarding_visto"

private enum class Paso { SPLASH, MOTIVACION, BIENVENIDA, ONBOARDING, ROL, REGISTRO, LOGIN, INICIO }

// Pasos previos a la sesión: con una sesión activa no tiene sentido seguir en ellos.
private val PASOS_DE_ACCESO = setOf(Paso.BIENVENIDA, Paso.ONBOARDING, Paso.ROL, Paso.REGISTRO, Paso.LOGIN)

class MainActivity : ComponentActivity() {
    // ponytail: bandera en SharedPreferences; pasa a DataStore cuando haya más preferencias locales
    private val prefs by lazy { getSharedPreferences("nutrigajo", MODE_PRIVATE) }
    private val onboardingVisto get() = prefs.getBoolean(CLAVE_ONBOARDING_VISTO, false)

    private fun marcarOnboardingVisto() = prefs.edit().putBoolean(CLAVE_ONBOARDING_VISTO, true).apply()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NutriGajoTheme {
                val auth: AuthViewModel = viewModel()
                val estado by auth.estado.collectAsState()
                // ponytail: pasos locales; el NavHost por rol los reemplaza cuando existan las pantallas de cada rol
                var paso by rememberSaveable { mutableStateOf(Paso.SPLASH) }
                var rol by rememberSaveable { mutableStateOf(Rol.PACIENTE) }

                LaunchedEffect(estado.sesion, paso) {
                    if (estado.sesion && paso in PASOS_DE_ACCESO) {
                        marcarOnboardingVisto()
                        paso = Paso.INICIO
                    }
                }

                when (paso) {
                    Paso.SPLASH -> SplashScreen(onTerminar = { paso = Paso.MOTIVACION })
                    Paso.MOTIVACION -> MotivacionScreen(
                        onContinuar = {
                            paso = when {
                                estado.sesion -> Paso.INICIO
                                onboardingVisto -> Paso.LOGIN
                                else -> Paso.BIENVENIDA
                            }
                        },
                    )
                    Paso.BIENVENIDA -> BienvenidaScreen(
                        onComenzar = { paso = Paso.ONBOARDING },
                        onIngresar = {
                            marcarOnboardingVisto()
                            paso = Paso.LOGIN
                        },
                    )
                    Paso.ONBOARDING -> OnboardingScreen(
                        onVolver = { paso = Paso.BIENVENIDA },
                        onTerminar = { paso = Paso.ROL },
                    )
                    Paso.ROL -> RolScreen(
                        // quien ya vio el onboarding llegó desde "Regístrate" del login
                        onVolver = { paso = if (onboardingVisto) Paso.LOGIN else Paso.ONBOARDING },
                        onContinuar = {
                            rol = it
                            auth.limpiarError()
                            paso = Paso.REGISTRO
                        },
                    )
                    Paso.REGISTRO -> RegistroScreen(
                        rol = rol,
                        estado = estado,
                        onRegistrar = { nombre, correo, contrasena, confirmacion ->
                            auth.registrar(nombre, correo, contrasena, confirmacion, rol)
                        },
                        onIniciarSesion = {
                            auth.limpiarError()
                            paso = Paso.LOGIN
                        },
                        onVolver = {
                            auth.limpiarError()
                            paso = Paso.ROL
                        },
                        onEditar = auth::limpiarError,
                    )
                    Paso.LOGIN -> LoginScreen(
                        estado = estado,
                        onIngresar = auth::ingresar,
                        onRegistrar = {
                            auth.limpiarError()
                            paso = Paso.ROL
                        },
                        onEditar = auth::limpiarError,
                    )
                    Paso.INICIO -> {
                        LaunchedEffect(Unit) { auth.cargarPerfil() }
                        InicioProvisional(
                            perfil = estado.perfil,
                            onCerrarSesion = {
                                auth.cerrarSesion()
                                paso = Paso.LOGIN
                            },
                        )
                    }
                }
            }
        }
    }
}

// ponytail: pantalla de paso hasta que existan los inicios de nutriólogo y paciente; sirve para ver el perfil guardado.
@Composable
private fun InicioProvisional(perfil: Perfil?, onCerrarSesion: () -> Unit) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (perfil != null) {
                Text(stringResource(R.string.inicio_hola, perfil.nombre), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.inicio_rol, perfil.rol.name), style = MaterialTheme.typography.bodyLarge)
                Text(perfil.correo, style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = onCerrarSesion) { Text(stringResource(R.string.inicio_cerrar_sesion)) }
        }
    }
}
