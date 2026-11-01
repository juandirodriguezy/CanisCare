package com.example.caniscare.ui.screens.calculator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.caniscare.data.calculator.DogAgeCalculator
import com.example.caniscare.data.calculator.DogCalculationResult
import com.example.caniscare.data.calculator.DogSize
import com.example.caniscare.ui.components.AppInfoDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    onNavigateToSaveDog: ((String, Double, String) -> Unit)? = null
) {
    var dogName by remember { mutableStateOf("") }
    var ageInput by remember { mutableStateOf("7") }
    var selectedSize by remember { mutableStateOf(DogSize.MEDIUM) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val ageValue = ageInput.toDoubleOrNull() ?: 1.0
    var calculationResult by remember {
        mutableStateOf(
            DogAgeCalculator.calculate(
                name = dogName,
                ageYearsInput = ageValue,
                size = selectedSize
            )
        )
    }

    if (showInfoDialog) {
        AppInfoDialog(onDismissRequest = { showInfoDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "CanisCare",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Información de la App",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Header
                Text(
                    text = "Calculadora de Edad Canina",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Conoce la equivalencia biológica de tu mascota y recibe recomendaciones según su etapa de vida.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Formulario Responsivo
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = dogName,
                            onValueChange = { dogName = it },
                            label = { Text("Nombre del perro (opcional)") },
                            placeholder = { Text("Ej: Firulais, Max, Luna") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = ageInput,
                            onValueChange = { ageInput = it },
                            label = { Text("Edad en años caninos") },
                            placeholder = { Text("Ej: 1, 2, 7 (Vacío = 1 año)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Text(
                            text = "Tamaño del Perro:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Selector de tamaño en cuadrícula 2x2 responsiva
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val sizes = DogSize.values()
                            // Fila 1: Pequeño y Mediano
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (i in 0..1) {
                                    val size = sizes[i]
                                    FilterChip(
                                        selected = selectedSize == size,
                                        onClick = {
                                            selectedSize = size
                                            calculationResult = DogAgeCalculator.calculate(
                                                name = dogName,
                                                ageYearsInput = ageInput.toDoubleOrNull(),
                                                size = size
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = "${size.displayName} (${size.weightDescription})",
                                                fontSize = 11.sp,
                                                fontWeight = if (selectedSize == size) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Fila 2: Grande y Gigante
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (i in 2..3) {
                                    val size = sizes[i]
                                    FilterChip(
                                        selected = selectedSize == size,
                                        onClick = {
                                            selectedSize = size
                                            calculationResult = DogAgeCalculator.calculate(
                                                name = dogName,
                                                ageYearsInput = ageInput.toDoubleOrNull(),
                                                size = size
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = "${size.displayName} (${size.weightDescription})",
                                                fontSize = 11.sp,
                                                fontWeight = if (selectedSize == size) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val parsedAge = ageInput.toDoubleOrNull()
                                calculationResult = DogAgeCalculator.calculate(
                                    name = dogName,
                                    ageYearsInput = parsedAge,
                                    size = selectedSize
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Calcular Equivalencia")
                        }
                    }
                }

                // Resultado animado
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically()
                ) {
                    ResultCard(
                        result = calculationResult,
                        onSaveDog = {
                            onNavigateToSaveDog?.invoke(
                                calculationResult.dogName,
                                calculationResult.dogAgeYears,
                                calculationResult.dogSize.displayName
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ResultCard(
    result: DogCalculationResult,
    onSaveDog: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.dogName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(result.stage.badgeColorHex),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = result.stage.displayName,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Destacado de Edad Humana
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${result.dogAgeYears} años caninos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "≈ ${result.humanEquivalentAge} años humanos",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Categoría: ${result.dogSize.displayName} (${result.dogSize.weightDescription})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Recomendaciones de Cuidado
            Text(
                text = "Recomendaciones Generales de Cuidado:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            result.careRecommendations.forEach { tip ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Nutrición
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Alimentación Sugerida:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            result.nutritionTips.forEach { tip ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Cronograma Veterinario
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.MedicalServices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Control Veterinario:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = result.veterinarySchedule,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
