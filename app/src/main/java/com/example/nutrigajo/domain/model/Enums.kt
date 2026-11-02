package com.example.nutrigajo.domain.model

// No renombrar valores: Room y Firestore guardan el nombre del enum como texto.

enum class Rol { ADMIN, NUTRIOLOGO, ENTRENADOR, PACIENTE }

enum class EstadoSync { PENDIENTE, SINCRONIZADO }

enum class Sexo { FEMENINO, MASCULINO, OTRO }

enum class TipoComida { DESAYUNO, COLACION, COMIDA, CENA }

enum class TipoCumplimiento { COMIDA, EJERCICIO }
