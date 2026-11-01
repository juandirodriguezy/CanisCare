package com.example.caniscare.data.repository

import com.example.caniscare.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AIChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" o "assistant"
    val content: String,
    val isEmergency: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class GeminiAIRepository {

    private val systemInstruction = """
        Eres CanisBot, el asistente experto en salud, nutrición, higiene y bienestar canino de la aplicación CanisCare.
        Tus reglas fundamentales son:
        1. Eres exclusivo para temas caninos (perros). Si te preguntan sobre otras especies o temas no relacionados, responde amablemente que tu especialidad es el cuidado de los perros.
        2. Proporciona consejos claros sobre alimentación, cuidados preventivos, higiene y comportamiento canino.
        3. DETECCIÓN DE URGENCIAS: Si el usuario describe síntomas críticos (envenenamiento, vómito con sangre, convulsiones, dificultad extrema para respirar, hinchazón abdominal súbita/torsión gástrica, atropellamiento o letargo grave), DEBES iniciar tu respuesta con la etiqueta [URGENCIA_VETERINARIA] en mayúsculas, seguida de una advertencia enfática para que traslade a su perro inmediatamente a una clínica veterinaria de urgencias 24h, indicando qué precauciones inmediatas tomar mientras llega al centro médico.
        4. Recuerda siempre que tus consejos son de orientación preventiva y NUNCA sustituyen el diagnóstico de un médico veterinario colegiado.
    """.trimIndent()

    private var generativeModel: GenerativeModel? = null

    init {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank()) {
            try {
                generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = apiKey,
                    systemInstruction = com.google.ai.client.generativeai.type.content {
                        text(systemInstruction)
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun sendChatMessage(userText: String): AIChatMessage = withContext(Dispatchers.IO) {
        val lowerText = userText.lowercase()
        // Detección heurística de urgencias caninas de respaldo
        val isEmergencyPrompt = lowerText.contains("veneno") ||
                lowerText.contains("sangre") ||
                lowerText.contains("convulsi") ||
                lowerText.contains("atropell") ||
                lowerText.contains("no respira") ||
                lowerText.contains("torsion") ||
                lowerText.contains("ahog")

        val model = generativeModel
        if (model != null) {
            try {
                val response = model.generateContent(userText)
                val responseText = response.text ?: "No pude procesar tu respuesta en este momento."
                val isEmergency = responseText.contains("[URGENCIA_VETERINARIA]") || isEmergencyPrompt
                val cleanText = responseText.replace("[URGENCIA_VETERINARIA]", "").trim()

                return@withContext AIChatMessage(
                    sender = "assistant",
                    content = cleanText,
                    isEmergency = isEmergency
                )
            } catch (e: Exception) {
                // Si la cuota o red falla, usamos el motor de triage local
            }
        }

        // Motor inteligente local de respaldo (Offline / Sandbox)
        val fallbackResponse = generateLocalIntelligentResponse(userText, isEmergencyPrompt)
        AIChatMessage(
            sender = "assistant",
            content = fallbackResponse,
            isEmergency = isEmergencyPrompt
        )
    }

    private fun generateLocalIntelligentResponse(prompt: String, isEmergency: Boolean): String {
        val lower = prompt.lowercase()
        if (isEmergency) {
            return "¡ATENCIÓN: Situación de posible urgencia médica! Los síntomas descritos requieren valoración profesional inmediata. Por favor mantén la calma, evita medicar a tu perro por tu cuenta y dirígete al centro veterinario de urgencias más cercano disponible en el mapa de CanisCare. Mantén a tu perro abrigado y con vías respiratorias despejadas durante el traslado."
        }
        if (lower.contains("comida") || lower.contains("aliment") || lower.contains("nutricion")) {
            return "Para una nutrición canina óptima, asegúrate de proporcionar un concentrado adecuado para su tamaño y edad (cachorro, adulto o senior). Los perros adultos requieren generalmente 2 raciones al día, agua fresca permanente y evitar alimentos tóxicos como chocolate, cebolla, uvas o huesos cocidos que puedan astillarse."
        }
        if (lower.contains("bano") || lower.contains("baño") || lower.contains("higiene") || lower.contains("pulga")) {
            return "La frecuencia de baño recomendada para perros es cada 3 a 4 semanas con champú formulado para pH canino. El cepillado regular ayuda a retirar pelo muerto y distribuir los aceites naturales. No olvides revisar periódicamente orejas y almohadillas, y mantener al día su pipeta o collar antiparasitario."
        }
        if (lower.contains("vacuna") || lower.contains("desparasit")) {
            return "El esquema básico incluye la vacuna polivalente (parvovirus, moquillo, hepatitis, leptospirosis) y la vacuna antirrábica anual. La desparasitación interna suele realizarse cada 3 meses bajo indicación del médico veterinario."
        }

        return "Hola, soy CanisBot. Como asistente de bienestar canino, te recomiendo siempre mantener una rutina de ejercicio diario, hidratación constante y controles veterinarios preventivos. ¿Deseas consultar sobre alimentación, higiene o cuidados de alguna etapa de vida en particular?"
    }
}
