package com.example.nutrigajo.domain.model

// Nutriólogo verificado con su consultorio en el mapa; vive en directorio/{uid}.
data class NutriologoEnMapa(
    val id: String,
    val nombre: String,
    val lat: Double,
    val lng: Double,
)
