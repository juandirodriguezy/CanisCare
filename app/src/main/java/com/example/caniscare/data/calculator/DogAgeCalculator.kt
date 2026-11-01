package com.example.caniscare.data.calculator

enum class DogSize(val displayName: String, val weightDescription: String) {
    SMALL("Pequeño", "Menos de 10 kg"),
    MEDIUM("Mediano", "Entre 10 y 25 kg"),
    LARGE("Grande", "Entre 25 y 45 kg"),
    GIANT("Gigante", "Más de 45 kg")
}

enum class LifeStage(val displayName: String, val badgeColorHex: Long) {
    PUPPY("Cachorro", 0xFF4CAF50),
    ADULT("Adulto", 0xFF2196F3),
    SENIOR("Senior / Adulto Mayor", 0xFFFF9800),
    GERIATRIC("Geriátrico", 0xFF9C27B0)
}

data class DogCalculationResult(
    val dogName: String,
    val dogAgeYears: Double,
    val dogSize: DogSize,
    val humanEquivalentAge: Int,
    val stage: LifeStage,
    val careRecommendations: List<String>,
    val nutritionTips: List<String>,
    val veterinarySchedule: String
)

object DogAgeCalculator {

    /**
     * Calcula la edad humana equivalente y entrega recomendaciones detalladas.
     * Si no se ingresa edad o tamaño, toma valores por defecto seguros.
     */
    fun calculate(
        name: String = "Tu perro",
        ageYearsInput: Double? = null,
        size: DogSize = DogSize.MEDIUM
    ): DogCalculationResult {
        // Valor por defecto: si no ingresa nada o es <= 0, asumimos 1 año canino
        val safeAge = if (ageYearsInput == null || ageYearsInput <= 0.0) 1.0 else ageYearsInput
        val safeName = if (name.isBlank()) "Tu compañero canino" else name.trim()

        val humanAge = calculateHumanAge(safeAge, size)
        val stage = determineLifeStage(safeAge, size)
        val recommendations = getCareRecommendations(stage)
        val nutrition = getNutritionTips(stage, size)
        val schedule = getVeterinarySchedule(stage)

        return DogCalculationResult(
            dogName = safeName,
            dogAgeYears = safeAge,
            dogSize = size,
            humanEquivalentAge = humanAge,
            stage = stage,
            careRecommendations = recommendations,
            nutritionTips = nutrition,
            veterinarySchedule = schedule
        )
    }

    private fun calculateHumanAge(age: Double, size: DogSize): Int {
        if (age <= 0.0) return 0
        if (age <= 1.0) {
            // El primer año equivale aproximadamente a 15 años humanos
            return (age * 15.0).toInt().coerceAtLeast(1)
        }
        if (age <= 2.0) {
            // El segundo año suma alrededor de 9 años humanos (24 al cumplir 2 años)
            val fraction = age - 1.0
            return (15.0 + fraction * 9.0).toInt()
        }

        // A partir de los 2 años (base de 24 años humanos), cada año suma según el tamaño
        val extraYears = age - 2.0
        val multiplier = when (size) {
            DogSize.SMALL -> 4.0
            DogSize.MEDIUM -> 5.0
            DogSize.LARGE -> 6.5
            DogSize.GIANT -> 8.0
        }

        return (24.0 + extraYears * multiplier).toInt()
    }

    private fun determineLifeStage(age: Double, size: DogSize): LifeStage {
        return when (size) {
            DogSize.SMALL, DogSize.MEDIUM -> {
                when {
                    age < 1.0 -> LifeStage.PUPPY
                    age < 7.0 -> LifeStage.ADULT
                    age < 11.0 -> LifeStage.SENIOR
                    else -> LifeStage.GERIATRIC
                }
            }
            DogSize.LARGE, DogSize.GIANT -> {
                when {
                    age < 1.5 -> LifeStage.PUPPY
                    age < 5.5 -> LifeStage.ADULT
                    age < 8.5 -> LifeStage.SENIOR
                    else -> LifeStage.GERIATRIC
                }
            }
        }
    }

    private fun getCareRecommendations(stage: LifeStage): List<String> {
        return when (stage) {
            LifeStage.PUPPY -> listOf(
                "Socialización temprana con personas y otros perros vacunados.",
                "Entrenamiento básico de hábitos de higiene en casa.",
                "Proveer juguetes masticables adecuados para el cambio de dentición.",
                "Mantener esquema de vacunación estricto antes de salir a la calle."
            )
            LifeStage.ADULT -> listOf(
                "Paseos diarios de al menos 30 a 60 minutos para salud cardiovascular.",
                "Cepillado regular de pelaje y control periódico de ectoparásitos (pulgas/garrapatas).",
                "Higiene bucal semanal para prevenir sarro y enfermedad periodontal.",
                "Enriquecimiento ambiental y juegos olfativos para evitar estrés."
            )
            LifeStage.SENIOR -> listOf(
                "Paseos de baja intensidad pero constantes para proteger las articulaciones.",
                "Cama ortopédica acolchada y protección contra bajas temperaturas.",
                "Monitoreo de cambios en la visión, oído o patrones de sueño.",
                "Control de peso estricto para evitar sobrecarga en columna y caderas."
            )
            LifeStage.GERIATRIC -> listOf(
                "Adaptar el hogar con alfombras antideslizantes y rampas si hay escalones.",
                "Revisiones geriátricas veterinarias cada 6 meses (perfil renal y hepático).",
                "Dieta blanda de alta palatabilidad si hay pérdida de piezas dentales.",
                "Acompañamiento paciente, respetando sus tiempos de descanso."
            )
        }
    }

    private fun getNutritionTips(stage: LifeStage, size: DogSize): List<String> {
        return when (stage) {
            LifeStage.PUPPY -> listOf(
                "Alimento concentrado para cachorros rico en DHA, proteínas y calcio.",
                "Fraccionar la ración en 3 a 4 comidas al día."
            )
            LifeStage.ADULT -> listOf(
                "Alimento balanceado formulado específicamente para razas ${size.displayName.lowercase()}.",
                "Fraccionar en 2 comidas al día y evitar actividad física pesada justo después de comer."
            )
            LifeStage.SENIOR, LifeStage.GERIATRIC -> listOf(
                "Concentrado Senior con glucosamina, condroitina y ácidos grasos Omega-3.",
                "Aporte moderado de fósforo y sodio para proteger la función renal y cardíaca."
            )
        }
    }

    private fun getVeterinarySchedule(stage: LifeStage): String {
        return when (stage) {
            LifeStage.PUPPY -> "Vacunación cada 3-4 semanas hasta completar esquema (Parvovirus, Moquillo, Rabia) y desparasitación mensual."
            LifeStage.ADULT -> "Chequeo médico preventivo anual, refuerzo anual de vacunas y desparasitación trimestral."
            LifeStage.SENIOR, LifeStage.GERIATRIC -> "Control clínico semestral con hemograma, química sanguínea, perfil renal y ecografía preventiva."
        }
    }
}
