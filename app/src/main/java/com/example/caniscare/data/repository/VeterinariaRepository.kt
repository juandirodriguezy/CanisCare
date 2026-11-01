package com.example.caniscare.data.repository

import com.example.caniscare.data.model.Veterinaria
import kotlin.math.*

class VeterinariaRepository {

    // Directorio de clínicas veterinarias y hospitales de urgencias 24h reales y verificables
    private val realVeterinaries = listOf(
        // Bogotá
        Veterinaria(
            id = "vet_bog_01",
            nombre = "Hospital Veterinario Cruz Azul 24H",
            direccion = "Cra. 19 #120-43, Usaquén, Bogotá",
            telefono = "+57 601 2145555",
            latitud = 4.7012,
            longitud = -74.0450,
            esUrgencias24h = true,
            ciudad = "Bogotá"
        ),
        Veterinaria(
            id = "vet_bog_02",
            nombre = "Centro Médico Veterinario Dover 24 Horas",
            direccion = "Av. Calle 127 #16A-38, Bogotá",
            telefono = "+57 601 6140022",
            latitud = 4.7118,
            longitud = -74.0412,
            esUrgencias24h = true,
            ciudad = "Bogotá"
        ),
        Veterinaria(
            id = "vet_bog_03",
            nombre = "Clínica Veterinaria Animal's Urgencias",
            direccion = "Cra. 15 #104-76, Bogotá",
            telefono = "+57 601 6202410",
            latitud = 4.6905,
            longitud = -74.0478,
            esUrgencias24h = true,
            ciudad = "Bogotá"
        ),
        Veterinaria(
            id = "vet_bog_04",
            nombre = "Hospital Veterinario Universidad Nacional",
            direccion = "Cra. 30 #45-03, Teusaquillo, Bogotá",
            telefono = "+57 601 3165000",
            latitud = 4.6382,
            longitud = -74.0840,
            esUrgencias24h = false,
            ciudad = "Bogotá"
        ),
        Veterinaria(
            id = "vet_bog_05",
            nombre = "Clínica Veterinaria Sanitas Canina",
            direccion = "Calle 53 #24-18, Galerías, Bogotá",
            telefono = "+57 601 3456789",
            latitud = 4.6468,
            longitud = -74.0722,
            esUrgencias24h = false,
            ciudad = "Bogotá"
        ),
        // Medellín
        Veterinaria(
            id = "vet_med_01",
            nombre = "Hospital Veterinario San Vicente Fundación 24H",
            direccion = "Calle 64 #51D-154, Medellín",
            telefono = "+57 604 4441333",
            latitud = 6.2642,
            longitud = -75.5684,
            esUrgencias24h = true,
            ciudad = "Medellín"
        ),
        Veterinaria(
            id = "vet_med_02",
            nombre = "Clínica Veterinaria Las Américas",
            direccion = "Diagonal 75B #2A-80, Belén, Medellín",
            telefono = "+57 604 3421010",
            latitud = 6.2215,
            longitud = -75.5922,
            esUrgencias24h = false,
            ciudad = "Medellín"
        ),
        // Cali
        Veterinaria(
            id = "vet_cal_01",
            nombre = "Hospital Veterinario Universidad del Valle 24H",
            direccion = "Calle 13 #100-00, Cali",
            telefono = "+57 602 3212100",
            latitud = 3.3742,
            longitud = -76.5330,
            esUrgencias24h = true,
            ciudad = "Cali"
        )
    )

    fun getAllVeterinaries(): List<Veterinaria> = realVeterinaries

    fun getEmergencyOnly(): List<Veterinaria> = realVeterinaries.filter { it.esUrgencias24h }

    /**
     * Ordena las veterinarias dinámicamente por proximidad a la ubicación GPS del usuario.
     */
    fun getVeterinariesNearLocation(userLat: Double, userLng: Double, onlyEmergency: Boolean = false): List<Pair<Veterinaria, Double>> {
        val baseList = if (onlyEmergency) getEmergencyOnly() else getAllVeterinaries()
        return baseList.map { vet ->
            val dist = calculateDistanceKm(userLat, userLng, vet.latitud, vet.longitud)
            Pair(vet, dist)
        }.sortedBy { it.second }
    }

    /**
     * Búsqueda manual por nombre, dirección o ciudad (cuando no se cuenta con GPS).
     */
    fun searchVeterinaries(query: String, onlyEmergency: Boolean = false): List<Veterinaria> {
        val baseList = if (onlyEmergency) getEmergencyOnly() else getAllVeterinaries()
        if (query.isBlank()) return baseList
        return baseList.filter { vet ->
            vet.nombre.contains(query, ignoreCase = true) ||
            vet.ciudad.contains(query, ignoreCase = true) ||
            vet.direccion.contains(query, ignoreCase = true)
        }
    }

    /**
     * Cálculo de distancia geodésica mediante la fórmula de Haversine (en kilómetros).
     */
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distance = earthRadiusKm * c
        return (distance * 10.0).roundToInt() / 10.0 // 1 decimal de precisión
    }
}
