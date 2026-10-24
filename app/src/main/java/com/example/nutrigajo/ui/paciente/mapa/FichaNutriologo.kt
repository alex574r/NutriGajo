package com.example.nutrigajo.ui.paciente.mapa

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.net.toUri
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.NutriologoEnMapa
import com.example.nutrigajo.ui.components.AvatarInicial
import com.example.nutrigajo.ui.components.BotonPrimario

private val MARGEN = 16.dp

// el interlineado de la escala no sirve para texto de varias líneas
private val TEXTO_LARGO @Composable get() = MaterialTheme.typography.bodyMedium.copy(lineHeight = 1.4.em)

// La ficha del nutriólogo que el paciente abre con «Ver más»: su foto, su cédula y lo que contó de su consultorio.
// Solo se muestra lo que el nutriólogo llenó.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FichaNutriologo(nutriologo: NutriologoEnMapa, foto: Bitmap?, onCerrar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val contexto = LocalContext.current
    val consultorio = nutriologo.consultorio
    val imagen = remember(foto) { foto?.asImageBitmap() }
    val hayConsultorio = consultorio.direccion.isNotBlank() || consultorio.horario.isNotBlank() || consultorio.telefono.isNotBlank()

    // sin media altura: la ficha abre completa y, si no cabe, se desplaza
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colores.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MARGEN)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AvatarInicial(nombre = nutriologo.nombre, tamano = 88.dp, estilo = MaterialTheme.typography.headlineMedium, imagen = imagen)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = nutriologo.nombre,
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.headlineMedium,
                            color = colores.onBackground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_verificado),
                            contentDescription = stringResource(R.string.nutri_verificado),
                            modifier = Modifier.size(22.dp),
                            tint = colores.primary,
                        )
                    }
                    if (consultorio.especialidad.isNotBlank()) {
                        Text(consultorio.especialidad, style = MaterialTheme.typography.titleSmall, color = colores.onBackground)
                    }
                    Text(stringResource(R.string.mapa_verificado), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
                    if (nutriologo.cedula.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.ficha_cedula, nutriologo.cedula),
                            style = MaterialTheme.typography.bodySmall,
                            color = colores.onSurfaceVariant,
                        )
                    }
                }
            }

            if (consultorio.descripcion.isNotBlank()) {
                Seccion(R.string.ficha_acerca) {
                    Text(consultorio.descripcion, style = TEXTO_LARGO, color = colores.onBackground)
                }
            }
            if (hayConsultorio) {
                Seccion(R.string.ficha_consultorio) {
                    if (consultorio.direccion.isNotBlank()) FilaDato(R.drawable.ic_ubicacion, consultorio.direccion)
                    if (consultorio.horario.isNotBlank()) FilaDato(R.drawable.ic_reloj, consultorio.horario)
                    if (consultorio.telefono.isNotBlank()) {
                        FilaDato(
                            icono = R.drawable.ic_telefono,
                            texto = consultorio.telefono,
                            modifier = Modifier.clickable(
                                onClickLabel = stringResource(R.string.ficha_llamar, nutriologo.nombre),
                                role = Role.Button,
                            ) { marcar(contexto, consultorio.telefono) },
                        )
                    }
                }
            } else if (consultorio.descripcion.isBlank()) {
                Text(stringResource(R.string.ficha_sin_datos), style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
            }

            BotonPrimario(
                texto = stringResource(R.string.ficha_como_llegar),
                onClick = { comoLlegar(contexto, nutriologo.lat, nutriologo.lng) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colores.onBackground, contentColor = colores.background),
            )
        }
    }
}

@Composable
private fun Seccion(@StringRes titulo: Int, contenido: @Composable () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(titulo), style = MaterialTheme.typography.titleMedium, color = colores.onBackground)
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colores.surfaceVariant).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { contenido() }
    }
}

@Composable
private fun FilaDato(@DrawableRes icono: Int, texto: String, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(24.dp), tint = colores.onBackground)
        Text(texto, style = TEXTO_LARGO, color = colores.onBackground)
    }
}

// Abre el marcador del teléfono con el número escrito; el paciente decide si llama.
private fun marcar(contexto: Context, telefono: String) {
    try {
        contexto.startActivity(Intent(Intent.ACTION_DIAL, "tel:${telefono.filter { it.isDigit() || it == '+' }}".toUri()))
    } catch (_: ActivityNotFoundException) {
    }
}

// La ruta hasta el consultorio en Google Maps (la app, o el navegador si no está).
private fun comoLlegar(contexto: Context, lat: Double, lng: Double) {
    try {
        contexto.startActivity(Intent(Intent.ACTION_VIEW, "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng".toUri()))
    } catch (_: ActivityNotFoundException) {
    }
}
