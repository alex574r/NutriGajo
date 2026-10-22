package com.example.nutrigajo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R

// Pantalla completa de esqueletos, mientras se resuelve algo que decide qué mostrar.
// `conCabecera` imita el inicio con foto (cabecera, tarjetas y una imagen grande); sin ella, un título y tarjetas.
@Composable
fun PantallaCarga(modifier: Modifier = Modifier, conCabecera: Boolean = false) {
    val descripcion = stringResource(R.string.cargando)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .clearAndSetSemantics { contentDescription = descripcion },
    ) {
        if (conCabecera) {
            val barra = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Esqueleto(
                modifier = Modifier.fillMaxWidth().height(barra + 215.dp),
                forma = RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (conCabecera) Modifier else Modifier.statusBarsPadding())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Esqueleto(Modifier.fillMaxWidth(0.5f).height(24.dp))
            if (conCabecera) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(2) { Esqueleto(Modifier.size(135.dp, 176.dp), RoundedCornerShape(34.dp)) }
                }
                Esqueleto(Modifier.fillMaxWidth(0.4f).height(24.dp))
                Esqueleto(Modifier.fillMaxWidth().height(234.dp), RoundedCornerShape(31.dp))
            } else {
                repeat(3) { Esqueleto(Modifier.fillMaxWidth().height(100.dp), RoundedCornerShape(24.dp)) }
            }
        }
    }
}
