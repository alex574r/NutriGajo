package com.example.nutrigajo.ui.shared.perfil

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.data.local.AjustesApp
import com.example.nutrigajo.data.repository.FotoPerfil
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.domain.model.Rol
import com.example.nutrigajo.ui.components.AccionPerfil
import com.example.nutrigajo.ui.components.TransicionPerfil
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.components.TarjetaAjustes
import com.example.nutrigajo.ui.components.PerfilCabecera
import com.example.nutrigajo.ui.components.SelectorPestanas
import com.example.nutrigajo.ui.shared.ajustes.Ajuste
import com.example.nutrigajo.ui.shared.ajustes.AjustePantalla
import com.example.nutrigajo.ui.shared.ajustes.GrupoAjustes
import com.example.nutrigajo.ui.shared.ajustes.alternarBloqueo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MARGEN = 16.dp
private val PESTANAS = listOf(R.string.perfil_datos, R.string.perfil_ajustes)

class DatoPerfil(
    @param:DrawableRes val icono: Int,
    @param:StringRes val etiqueta: Int,
    val valor: String,
)

// La fila sube desde 10 dp mientras se hace visible, con un retraso por posición para que lleguen en cascada.
@Composable
private fun Aparece(indice: Int, animar: Boolean, contenido: @Composable () -> Unit) {
    val progreso = remember { Animatable(if (animar) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animar) progreso.animateTo(1f, tween(260, delayMillis = 20 + indice * 40, easing = FastOutSlowInEasing))
    }
    Box(
        Modifier.graphicsLayer {
            alpha = progreso.value
            translationY = (1f - progreso.value) * 10.dp.toPx()
        },
    ) { contenido() }
}

// Mes y año del alta ("oct 2026"), o null si la cuenta no lo trae.
fun Perfil.altaCorta(): String? = fechaAlta?.let {
    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))
}

// Los datos que tiene toda cuenta; cada rol añade los suyos detrás.
@Composable
fun datosBase(perfil: Perfil, @StringRes rol: Int): List<DatoPerfil> = listOfNotNull(
    DatoPerfil(R.drawable.ic_person, R.string.perfil_dato_nombre, perfil.nombre),
    DatoPerfil(R.drawable.ic_mail, R.string.perfil_dato_correo, perfil.correo),
    DatoPerfil(R.drawable.ic_informacion, R.string.perfil_dato_rol, stringResource(rol)),
    perfil.altaCorta()?.let { DatoPerfil(R.drawable.ic_calendario, R.string.perfil_dato_alta, it) },
)

// Perfil de paciente y nutriólogo con el mismo molde: cabecera y dos pestañas, "Datos" de la cuenta y "Configuración".
// `datosExtra` va bajo los datos (el estado de la verificación del nutriólogo) y `espacioInferior` deja libre la barra flotante.
@Composable
fun PerfilScreen(
    perfil: Perfil,
    @StringRes rol: Int,
    verificado: Boolean,
    estadisticas: List<String>,
    principal: AccionPerfil,
    secundaria: AccionPerfil?,
    datos: List<DatoPerfil>,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
    onVolver: (() -> Unit)? = null,
    espacioInferior: Dp = 0.dp,
    // el avatar viaja desde la pantalla anterior si la hay
    transicion: TransicionPerfil? = null,
    // clave propia: el uid ya la usan otros ViewModels del mismo usuario
    vm: PerfilViewModel = viewModel(key = "perfil-${perfil.uid}"),
    // solo el paciente: la pantalla «Mi nutriólogo» (cambiar de nutriólogo o desvincularse); recibe cómo volver al perfil
    miNutriologo: (@Composable (onVolver: () -> Unit) -> Unit)? = null,
    // paciente y nutriólogo: sus vínculos ya cerrados, solo para leer; recibe cómo volver al perfil
    anteriores: (@Composable (onVolver: () -> Unit) -> Unit)? = null,
    datosExtra: @Composable () -> Unit = {},
) {
    val colores = MaterialTheme.colorScheme
    val contexto = LocalContext.current
    val estado by vm.estado.collectAsState()
    val portada by vm.portada.collectAsState()
    val avatar by vm.avatar.collectAsState()
    val imagenAvatar = rememberFotoAvatar(perfil, vm)
    // el selector del sistema no pide permisos: solo entrega la foto que la persona elige
    val elegirPortada = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.cambiarFoto(FotoPerfil.PORTADA, perfil.uid, contexto.contentResolver, contexto.cacheDir, uri)
    }
    val elegirAvatar = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.cambiarFoto(FotoPerfil.AVATAR, perfil.uid, contexto.contentResolver, contexto.cacheDir, uri)
    }
    LaunchedEffect(perfil.portadaVersion) { vm.cargarFoto(FotoPerfil.PORTADA, contexto.cacheDir, perfil.uid, perfil.portadaVersion) }
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    // las filas solo se animan al cambiar de pestaña, no al abrir el perfil
    var huboCambio by remember { mutableStateOf(false) }
    var cambiandoContrasena by rememberSaveable { mutableStateOf(false) }
    // la pantalla de ajustes abierta desde Configuración; cubre el perfil sin perder su pestaña ni su desplazamiento
    var ajuste by rememberSaveable { mutableStateOf<Ajuste?>(null) }
    var enMiNutriologo by rememberSaveable { mutableStateOf(false) }
    var enAnteriores by rememberSaveable { mutableStateOf(false) }
    val version = remember { runCatching { contexto.packageManager.getPackageInfo(contexto.packageName, 0).versionName }.getOrNull().orEmpty() }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(colores.background),
            contentPadding = PaddingValues(bottom = espacioInferior + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                PerfilCabecera(
                    nombre = perfil.nombre,
                    correo = perfil.correo,
                    rol = rol,
                    verificado = verificado,
                    estadisticas = estadisticas,
                    principal = principal,
                    secundaria = secundaria,
                    onVolver = onVolver,
                    imagenPortada = portada.imagen?.asImageBitmap(),
                    subiendoPortada = portada.subiendo,
                    onCambiarPortada = { elegirPortada.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    imagenAvatar = imagenAvatar,
                    subiendoAvatar = avatar.subiendo,
                    onCambiarAvatar = { elegirAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    transicion = transicion,
                )
            }
            (portada.error ?: avatar.error)?.let { item { AlertaError(stringResource(it), Modifier.padding(horizontal = MARGEN)) } }
            item {
                SelectorPestanas(
                    PESTANAS,
                    pestana,
                    {
                        huboCambio = true
                        pestana = it
                    },
                    Modifier.padding(horizontal = MARGEN, vertical = 8.dp),
                )
            }
            // El contenido de la pestaña entra desde el lado hacia el que se va (y el anterior sale al contrario) y sus filas
            // aparecen una tras otra; al abrir el perfil las filas ya salen puestas.
            item {
                AnimatedContent(
                    targetState = pestana,
                    transitionSpec = {
                        val sentido = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { sentido * it / 5 } + fadeIn(tween(220, delayMillis = 30))) togetherWith
                            (slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) { -sentido * it / 5 } + fadeOut(tween(120)))
                    },
                    label = "pestana",
                ) { actual ->
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (actual == 0) {
                            datos.forEachIndexed { indice, dato ->
                                Aparece(indice, huboCambio) {
                                    FilaAjuste(dato.icono, titulo = dato.valor, modifier = Modifier.padding(horizontal = MARGEN), etiqueta = stringResource(dato.etiqueta))
                                }
                            }
                            Aparece(datos.size, huboCambio) { datosExtra() }
                        } else {
                            Configuracion(
                                huboCambio,
                                onAjuste = { ajuste = it },
                                onCerrarSesion = onCerrarSesion,
                                onMiNutriologo = if (miNutriologo != null) ({ enMiNutriologo = true }) else null,
                                onAnteriores = if (anteriores != null) ({ enAnteriores = true }) else null,
                                // la cuenta de administrador no se borra desde la app
                                puedeEliminar = perfil.rol != Rol.ADMIN,
                            )
                        }
                    }
                }
            }
        }
        ajuste?.let {
            AjustePantalla(
                ajuste = it,
                perfil = perfil,
                versionApp = version,
                onCambiarContrasena = { cambiandoContrasena = true },
                onVolver = { ajuste = null },
                espacioInferior = espacioInferior,
                onCuentaEliminada = onCerrarSesion,
            )
        }
        if (enMiNutriologo) miNutriologo?.invoke { enMiNutriologo = false }
        if (enAnteriores) anteriores?.invoke { enAnteriores = false }
    }

    if (cambiandoContrasena) {
        val cerrar = {
            cambiandoContrasena = false
            vm.reiniciar()
        }
        AlertDialog(
            onDismissRequest = cerrar,
            title = { Text(stringResource(R.string.perfil_contrasena_titulo)) },
            text = {
                Text(
                    when {
                        estado.enviado -> stringResource(R.string.perfil_contrasena_enviado, perfil.correo)
                        estado.error != null -> stringResource(estado.error!!)
                        else -> stringResource(R.string.perfil_contrasena_ayuda, perfil.correo)
                    },
                )
            },
            confirmButton = {
                if (estado.enviado) {
                    TextButton(onClick = cerrar) { Text(stringResource(R.string.perfil_listo)) }
                } else {
                    TextButton(onClick = { vm.restablecerContrasena(perfil.correo) }, enabled = !estado.enviando) {
                        Text(stringResource(R.string.perfil_contrasena_enviar))
                    }
                }
            },
            dismissButton = {
                if (!estado.enviado) TextButton(onClick = cerrar) { Text(stringResource(R.string.verificacion_cancelar)) }
            },
        )
    }
}

// La pestaña Configuración en grupos, como el diseño: General, Seguridad y privacidad, Ayuda y soporte y la sesión. Las filas con
// flecha abren su pantalla; las de interruptor cambian al tocarlas.
@Composable
private fun Configuracion(
    animar: Boolean,
    onAjuste: (Ajuste) -> Unit,
    onCerrarSesion: () -> Unit,
    onMiNutriologo: (() -> Unit)? = null,
    onAnteriores: (() -> Unit)? = null,
    puedeEliminar: Boolean = false,
) {
    val contexto = LocalContext.current
    val oscuro by AjustesApp.oscuro
    val bloqueo by AjustesApp.bloqueo
    val margen = Modifier.padding(horizontal = MARGEN)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GrupoAjustes(stringResource(R.string.ajustes_general), margen)
        Aparece(0, animar) {
            TarjetaAjustes(margen) {
                FilaAjuste(icono = R.drawable.ic_campana, titulo = stringResource(R.string.inicio_notificaciones), plana = true, onClick = { onAjuste(Ajuste.NOTIFICACIONES) })
                FilaAjuste(icono = R.drawable.ic_person, titulo = stringResource(R.string.ajustes_info_personal), plana = true, onClick = { onAjuste(Ajuste.INFORMACION) })
                FilaAjuste(
                    icono = R.drawable.ic_luna,
                    titulo = stringResource(R.string.ajustes_modo_oscuro),
                    plana = true,
                    activa = oscuro,
                    onActivaChange = { AjustesApp.cambiarOscuro(contexto, it) },
                )
                // el paciente cambia de nutriólogo o se desvincula desde aquí
                onMiNutriologo?.let { abrir -> FilaAjuste(icono = R.drawable.ic_pacientes, titulo = stringResource(R.string.ajustes_mi_nutriologo), plana = true, onClick = abrir) }
                onAnteriores?.let { abrir -> FilaAjuste(icono = R.drawable.ic_documento, titulo = stringResource(R.string.ajustes_vinculos_anteriores), plana = true, onClick = abrir) }
            }
        }
        GrupoAjustes(stringResource(R.string.ajustes_seguridad_grupo), margen)
        Aparece(1, animar) {
            TarjetaAjustes(margen) {
                FilaAjuste(icono = R.drawable.ic_lock, titulo = stringResource(R.string.ajustes_seguridad), plana = true, onClick = { onAjuste(Ajuste.SEGURIDAD) })
                FilaAjuste(
                    icono = R.drawable.ic_verificado,
                    titulo = stringResource(R.string.ajustes_biometria),
                    plana = true,
                    activa = bloqueo,
                    onActivaChange = { alternarBloqueo(contexto, it) },
                )
                FilaAjuste(icono = R.drawable.ic_ajustes, titulo = stringResource(R.string.ajustes_permisos), plana = true, onClick = { onAjuste(Ajuste.PERMISOS) })
            }
        }
        GrupoAjustes(stringResource(R.string.ajustes_ayuda_grupo), margen)
        Aparece(2, animar) {
            TarjetaAjustes(margen) {
                FilaAjuste(icono = R.drawable.ic_informacion, titulo = stringResource(R.string.ajustes_acerca), plana = true, onClick = { onAjuste(Ajuste.ACERCA) })
                FilaAjuste(icono = R.drawable.ic_bombilla, titulo = stringResource(R.string.ajustes_centro_ayuda), plana = true, onClick = { onAjuste(Ajuste.AYUDA) })
                FilaAjuste(icono = R.drawable.ic_mensaje, titulo = stringResource(R.string.ajustes_comentarios), plana = true, onClick = { onAjuste(Ajuste.COMENTARIOS) })
            }
        }
        // salir y borrar la cuenta, en su propia tarjeta roja
        Aparece(3, animar) {
            TarjetaAjustes(margen.padding(top = 8.dp), destructiva = true) {
                FilaAjuste(icono = R.drawable.ic_flecha_atras, titulo = stringResource(R.string.inicio_cerrar_sesion), plana = true, destructiva = true, onClick = onCerrarSesion)
                if (puedeEliminar) {
                    FilaAjuste(icono = R.drawable.ic_lock, titulo = stringResource(R.string.ajustes_eliminar_cuenta), plana = true, destructiva = true, onClick = { onAjuste(Ajuste.ELIMINAR_CUENTA) })
                }
            }
        }
    }
}
