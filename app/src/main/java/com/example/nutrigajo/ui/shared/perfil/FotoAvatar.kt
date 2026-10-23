package com.example.nutrigajo.ui.shared.perfil

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrigajo.data.repository.FotoPerfil
import com.example.nutrigajo.domain.model.Perfil

// Las fotos de la cuenta, o null si no tiene o aún no cargan (entonces se dibuja la inicial o la portada de siempre). Las
// comparten el perfil y los inicios: usan el mismo ViewModel, así que un cambio en el perfil se ve al volver al inicio.
@Composable
fun rememberFotoAvatar(perfil: Perfil, vm: PerfilViewModel = viewModel(key = "perfil-${perfil.uid}")): ImageBitmap? =
    rememberFoto(FotoPerfil.AVATAR, perfil, vm)

@Composable
fun rememberFotoPortada(perfil: Perfil, vm: PerfilViewModel = viewModel(key = "perfil-${perfil.uid}")): ImageBitmap? =
    rememberFoto(FotoPerfil.PORTADA, perfil, vm)

@Composable
private fun rememberFoto(foto: FotoPerfil, perfil: Perfil, vm: PerfilViewModel): ImageBitmap? {
    val contexto = LocalContext.current
    val estado by (if (foto == FotoPerfil.AVATAR) vm.avatar else vm.portada).collectAsState()
    val version = if (foto == FotoPerfil.AVATAR) perfil.avatarVersion else perfil.portadaVersion
    LaunchedEffect(version) { vm.cargarFoto(foto, contexto.cacheDir, perfil.uid, version) }
    return remember(estado.imagen) { estado.imagen?.asImageBitmap() }
}
