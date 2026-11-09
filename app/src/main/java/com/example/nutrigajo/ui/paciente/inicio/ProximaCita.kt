package com.example.nutrigajo.ui.paciente.inicio

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.nutrigajo.R
import com.example.nutrigajo.domain.model.Cita
import com.example.nutrigajo.ui.components.FilaAjuste
import com.example.nutrigajo.ui.shared.citas.diaYHoraCorto

// La cita que le espera al paciente, en su inicio: día y hora con el nutriólogo. `nueva` (la agendó su nutriólogo y aún no la ha visto)
// antepone «Nueva». Tocarla abre «Mis citas».
@Composable
fun ProximaCita(cita: Cita, nueva: Boolean, onAbrir: () -> Unit, modifier: Modifier = Modifier) {
    FilaAjuste(
        icono = R.drawable.ic_calendario,
        titulo = diaYHoraCorto(cita.inicio),
        modifier = modifier,
        // solo el primer nombre: la etiqueta cabe en una línea y deja ver el día y la hora completos
        etiqueta = stringResource(if (nueva) R.string.proxima_cita_etiqueta_nueva else R.string.proxima_cita_etiqueta, cita.nutriologoNombre.substringBefore(' ')),
        onClick = onAbrir,
    )
}
