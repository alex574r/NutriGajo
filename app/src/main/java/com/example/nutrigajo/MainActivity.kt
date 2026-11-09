package com.example.nutrigajo

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.data.local.AjustesApp
import com.example.nutrigajo.data.local.Biometria
import com.example.nutrigajo.data.push.Avisos
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.ui.actualizacion.ActualizacionHost
import com.example.nutrigajo.ui.actualizacion.ActualizacionViewModel
import com.example.nutrigajo.ui.admin.inicio.InicioAdminScreen
import com.example.nutrigajo.ui.components.PantallaCarga
import com.example.nutrigajo.ui.entrenador.inicio.InicioEntrenadorScreen
import com.example.nutrigajo.ui.nutriologo.inicio.InicioNutriologoScreen
import com.example.nutrigajo.ui.paciente.inicio.InicioPacienteScreen
import com.example.nutrigajo.ui.shared.ajustes.PantallaBloqueo
import com.example.nutrigajo.ui.shared.auth.AuthViewModel
import com.example.nutrigajo.ui.shared.login.LoginScreen
import com.example.nutrigajo.ui.shared.motivacion.MotivacionScreen
import com.example.nutrigajo.ui.shared.onboarding.BienvenidaScreen
import com.example.nutrigajo.ui.shared.onboarding.OnboardingScreen
import com.example.nutrigajo.ui.shared.registro.RegistroScreen
import com.example.nutrigajo.ui.shared.registro.RolScreen
import com.example.nutrigajo.ui.shared.splash.SplashScreen
import com.example.nutrigajo.ui.theme.NutriGajoTheme
import com.example.nutrigajo.widgets.AlmacenWidget
import com.example.nutrigajo.widgets.actualizarWidgets
import com.google.android.gms.maps.MapsInitializer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private const val CLAVE_ONBOARDING_VISTO = "onboarding_visto"
private var emuladorConectado = false

private enum class Paso { SPLASH, MOTIVACION, BIENVENIDA, ONBOARDING, ROL, REGISTRO, LOGIN, INICIO }

// Pasos previos a la sesión: con una sesión activa no tiene sentido seguir en ellos.
private val PASOS_DE_ACCESO = setOf(Paso.BIENVENIDA, Paso.ONBOARDING, Paso.ROL, Paso.REGISTRO, Paso.LOGIN)

// Con la huella o el rostro al abrir, la app se tapa al abrirla y al volver tras más de este tiempo fuera.
private const val ESPERA_BLOQUEO_MS = 30_000L

// FragmentActivity porque la ventana de huella del sistema (BiometricPrompt) la exige; en lo demás es la ComponentActivity de siempre.
class MainActivity : FragmentActivity() {
    // ponytail: bandera en SharedPreferences; pasa a DataStore cuando haya más preferencias locales
    private val prefs by lazy { getSharedPreferences("nutrigajo", MODE_PRIVATE) }
    private val onboardingVisto get() = prefs.getBoolean(CLAVE_ONBOARDING_VISTO, false)

    private fun marcarOnboardingVisto() = prefs.edit().putBoolean(CLAVE_ONBOARDING_VISTO, true).apply()

    // se tocó el aviso de un plan nuevo: el inicio del paciente lo abre y lo da por atendido
    private var abrirPlan by mutableStateOf(false)

    private fun Intent?.esAvisoDePlan() = this?.getStringExtra("tipo") == Avisos.TIPO_PLAN_NUEVO

    // se tocó el aviso de una cita (confirmada, agendada o cancelada): el inicio del paciente abre «Mis citas»
    private var abrirCitas by mutableStateOf(false)

    private fun Intent?.esAvisoDeCita() = this?.getStringExtra("tipo")?.startsWith(Avisos.PREFIJO_TIPO_CITA) == true

    // se tocó el aviso de un mensaje nuevo: el inicio abre ese chat (el id del paciente de la conversación)
    private var abrirChat by mutableStateOf<String?>(null)

    private fun Intent?.chatDelAviso(): String? =
        this?.takeIf { it.getStringExtra("tipo") == Avisos.TIPO_MENSAJE_NUEVO }?.getStringExtra("pacienteId")

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.esAvisoDePlan()) abrirPlan = true
        if (intent.esAvisoDeCita()) abrirCitas = true
        intent.chatDelAviso()?.let { abrirChat = it }
    }

    // la última vez que la app salió de la pantalla; estático para que girar el teléfono (que recrea la actividad) no cuente como salir
    private companion object {
        var ultimaActividad = Long.MIN_VALUE / 2
    }

    override fun onStart() {
        super.onStart()
        if (!AjustesApp.bloqueo.value || FirebaseAuth.getInstance().currentUser == null) return
        if (!Biometria.disponible(this)) {
            // ya no hay huella, rostro ni PIN con qué desbloquear: se quita el bloqueo para no dejar la app cerrada
            AjustesApp.cambiarBloqueo(this, false)
        } else if (SystemClock.elapsedRealtime() - ultimaActividad > ESPERA_BLOQUEO_MS) {
            AjustesApp.bloqueada.value = true
        }
    }

    override fun onStop() {
        ultimaActividad = SystemClock.elapsedRealtime()
        super.onStop()
    }

    private fun pedirDesbloqueo() {
        Biometria.pedir(
            actividad = this,
            titulo = getString(R.string.bloqueo_titulo),
            subtitulo = getString(R.string.bloqueo_texto),
            alExito = {
                ultimaActividad = SystemClock.elapsedRealtime()
                AjustesApp.bloqueada.value = false
            },
            alFallar = {},
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AjustesApp.iniciar(this)
        // al girar la pantalla la intención es la misma de antes: solo cuenta la de un arranque nuevo
        if (savedInstanceState == null && intent.esAvisoDePlan()) abrirPlan = true
        if (savedInstanceState == null && intent.esAvisoDeCita()) abrirCitas = true
        if (savedInstanceState == null) intent.chatDelAviso()?.let { abrirChat = it }
        enableEdgeToEdge()
        // CameraUpdateFactory (usado antes de que el mapa exista) falla si el SDK de Maps no está inicializado
        MapsInitializer.initialize(applicationContext)
        Avisos.crearCanales(this)
        if (BuildConfig.USAR_EMULADOR && !emuladorConectado) {
            // debe ir antes del primer uso de Auth y Firestore; solo una vez por proceso (la actividad se recrea al girar)
            FirebaseAuth.getInstance().useEmulator("localhost", 9099)
            FirebaseFirestore.getInstance().useEmulator("localhost", 8080)
            emuladorConectado = true
        }
        setContent {
            val oscuro by AjustesApp.oscuro
            NutriGajoTheme(darkTheme = oscuro) {
                // los iconos de la barra de estado siguen al tema de la app, no al del sistema
                val vista = LocalView.current
                SideEffect {
                    WindowCompat.getInsetsController(window, vista).apply {
                        isAppearanceLightStatusBars = !oscuro
                        isAppearanceLightNavigationBars = !oscuro
                    }
                }
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
                    // con sesión, la frase da paso al inicio; sin ella, se muestra la bienvenida
                    Paso.SPLASH -> SplashScreen(
                        onTerminar = { paso = if (estado.sesion) Paso.MOTIVACION else Paso.BIENVENIDA },
                    )
                    Paso.MOTIVACION -> MotivacionScreen(onContinuar = { paso = Paso.INICIO })
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
                        onRegistrar = { nombres, apellidos, correo, contrasena, confirmacion ->
                            auth.registrar(nombres, apellidos, correo, contrasena, confirmacion, rol)
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
                        val perfil = estado.perfil
                        LaunchedEffect(Unit) { if (perfil == null) auth.cargarPerfil() }
                        val cerrarSesion = {
                            auth.cerrarSesion()
                            AlmacenWidget.borrar(applicationContext)
                            actualizarWidgets(applicationContext)
                            paso = Paso.LOGIN
                        }
                        // el inicio depende del rol del perfil guardado en Firestore
                        when {
                            perfil == null && estado.perfilFallo -> PerfilNoDisponible(auth::cargarPerfil, cerrarSesion)
                            perfil == null -> PantallaCarga()
                            perfil.rol == Rol.NUTRIOLOGO -> InicioNutriologoScreen(
                                perfil,
                                cerrarSesion,
                                abrirChat = abrirChat,
                                onChatConsumido = { abrirChat = null },
                            )
                            perfil.rol == Rol.ENTRENADOR -> InicioEntrenadorScreen(perfil, cerrarSesion)
                            perfil.rol == Rol.PACIENTE -> InicioPacienteScreen(
                                perfil,
                                cerrarSesion,
                                abrirPlan = abrirPlan,
                                onPlanConsumido = { abrirPlan = false },
                                abrirChat = abrirChat,
                                onChatConsumido = { abrirChat = null },
                                abrirCitas = abrirCitas,
                                onCitasConsumido = { abrirCitas = false },
                            )
                            // ADMIN
                            else -> InicioAdminScreen(perfil, cerrarSesion)
                        }
                        // las actualizaciones de la app: se busca al entrar a la sesión (como mucho cada 12 horas) y la hoja, o la pantalla
                        // obligatoria, va encima del inicio
                        val actualizacion: ActualizacionViewModel = viewModel()
                        LaunchedEffect(Unit) { actualizacion.verificar(manual = false) }
                        ActualizacionHost(actualizacion, onCerrarApp = { finish() })
                    }
                }
                // con el bloqueo puesto, la ventana de huella sale sola y esta pantalla tapa la app hasta desbloquear
                val bloqueada by AjustesApp.bloqueada
                LaunchedEffect(bloqueada) { if (bloqueada) pedirDesbloqueo() }
                if (bloqueada) {
                    PantallaBloqueo(
                        onDesbloquear = ::pedirDesbloqueo,
                        onCerrarSesion = {
                            auth.cerrarSesion()
                            AlmacenWidget.borrar(applicationContext)
                            actualizarWidgets(applicationContext)
                            AjustesApp.bloqueada.value = false
                            paso = Paso.LOGIN
                        },
                    )
                }
            }
        }
    }
}

// El perfil no se pudo leer (sin conexión o cuenta sin perfil): se puede reintentar o salir.
@Composable
private fun PerfilNoDisponible(onReintentar: () -> Unit, onCerrarSesion: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.perfil_error), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onReintentar) { Text(stringResource(R.string.perfil_reintentar)) }
        TextButton(onClick = onCerrarSesion) { Text(stringResource(R.string.inicio_cerrar_sesion)) }
    }
}
