package com.example.nutrigajo.ui.shared.perfil

import android.content.Intent
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.R
import com.example.nutrigajo.data.repository.FotoPerfil
import com.example.nutrigajo.domain.model.Perfil
import com.example.nutrigajo.ui.components.AccionPerfil
import com.example.nutrigajo.ui.components.TransicionPerfil
import com.example.nutrigajo.ui.components.AlertaError
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.components.PerfilCabecera
import com.example.nutrigajo.ui.components.SelectorPestanas
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
    var cambiandoContrasena by rememberSaveable { mutableStateOf(false) }
    val version = remember { runCatching { contexto.packageManager.getPackageInfo(contexto.packageName, 0).versionName }.getOrNull().orEmpty() }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(colores.background),
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
        item { SelectorPestanas(PESTANAS, pestana, { pestana = it }, Modifier.padding(horizontal = MARGEN, vertical = 8.dp)) }
        if (pestana == 0) {
            items(datos) {
                FilaAjuste(it.icono, titulo = it.valor, modifier = Modifier.padding(horizontal = MARGEN), etiqueta = stringResource(it.etiqueta))
            }
            item { datosExtra() }
        } else {
            item {
                FilaAjuste(
                    icono = R.drawable.ic_campana,
                    titulo = stringResource(R.string.inicio_notificaciones),
                    modifier = Modifier.padding(horizontal = MARGEN),
                    onClick = {
                        contexto.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName),
                        )
                    },
                )
            }
            item {
                FilaAjuste(
                    icono = R.drawable.ic_lock,
                    titulo = stringResource(R.string.perfil_contrasena_titulo),
                    modifier = Modifier.padding(horizontal = MARGEN),
                    onClick = { cambiandoContrasena = true },
                )
            }
            item {
                FilaAjuste(
                    icono = R.drawable.ic_informacion,
                    titulo = version,
                    modifier = Modifier.padding(horizontal = MARGEN),
                    etiqueta = stringResource(R.string.perfil_version),
                )
            }
            item {
                FilaAjuste(
                    icono = R.drawable.ic_flecha_atras,
                    titulo = stringResource(R.string.inicio_cerrar_sesion),
                    modifier = Modifier.padding(horizontal = MARGEN),
                    destructiva = true,
                    onClick = onCerrarSesion,
                )
            }
        }
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
