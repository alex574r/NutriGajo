package com.example.nutrigajo.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nutrigajo.R

// CTA principal: naranja, texto en minúsculas y flecha. 56dp en formularios, 64dp en la bienvenida.
@Composable
fun BotonPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    altura: Dp = 56.dp,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    // cambia la flecha por un indicador de progreso; los toques se ignoran mientras dura
    cargando: Boolean = false,
) {
    Button(
        onClick = { if (!cargando) onClick() },
        modifier = modifier.height(altura),
        enabled = enabled,
        colors = colors,
        shape = RoundedCornerShape(21.dp),
        // el relleno vertical solo cabe en alturas de 56dp o más; en un botón más bajo recortaba el texto
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = if (altura >= 56.dp) 16.dp else 0.dp),
    ) {
        Text(text = texto, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(16.dp))
        if (cargando) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
