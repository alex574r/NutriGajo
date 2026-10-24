package com.example.nutrigajo.domain.model

// Lo que el nutriólogo cuenta de su consultorio; todo es opcional y se muestra solo lo que llenó.
// Los límites son los mismos de firebase/firestore.rules.
data class DatosConsultorio(
    val especialidad: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val horario: String = "",
    val descripcion: String = "",
) {
    companion object {
        const val MAX_ESPECIALIDAD = 80
        const val MAX_DIRECCION = 160
        const val MAX_TELEFONO = 20
        const val MAX_HORARIO = 120
        const val MAX_DESCRIPCION = 280
    }
}

// Nutriólogo verificado con su consultorio en el mapa; vive en directorio/{uid}.
// La cédula sale de su solicitud aprobada (las reglas exigen que coincida); vacía en los puntos publicados antes de la ficha.
data class NutriologoEnMapa(
    val id: String,
    val nombre: String,
    val lat: Double,
    val lng: Double,
    val cedula: String = "",
    val consultorio: DatosConsultorio = DatosConsultorio(),
)
