package com.example.caniscare.data.model

data class Veterinaria(
    val id: String,
    val nombre: String,
    val direccion: String,
    val telefono: String,
    val latitud: Double,
    val longitud: Double,
    val esUrgencias24h: Boolean,
    val ciudad: String,
    val horario: String = if (esUrgencias24h) "Atención Continua 24 Horas" else "Lunes a Sábado: 8:00 AM - 6:00 PM"
)
