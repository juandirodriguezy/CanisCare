package com.example.caniscare.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.caniscare.data.calculator.DogAgeCalculator
import com.example.caniscare.data.calculator.DogSize
import com.example.caniscare.data.local.LocalPhotoManager
import com.example.caniscare.data.model.Perro
import com.example.caniscare.data.repository.DogRepository
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDogsScreen(
    currentUserId: String,
    dogRepository: DogRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dogsList by dogRepository.getDogsForUser(currentUserId).collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Mascotas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Registrar Perro") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { paddingValues ->
        if (dogsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aún no tienes perros registrados",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Toca el botón '+' para registrar a tu compañero canino con su foto local y características.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(dogsList, key = { it.id }) { dog ->
                        DogCard(
                            dog = dog,
                            onDelete = {
                                scope.launch {
                                    dogRepository.deleteDog(dog.id)
                                    Toast.makeText(context, "${dog.nombre} eliminado", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }

        if (showAddDialog) {
            AddDogDialog(
                currentUserId = currentUserId,
                onDismiss = { showAddDialog = false },
                onSave = { nuevoPerro ->
                    scope.launch {
                        val res = dogRepository.saveDog(nuevoPerro)
                        if (res.isSuccess) {
                            Toast.makeText(context, "${nuevoPerro.nombre} registrado con éxito", Toast.LENGTH_SHORT).show()
                            showAddDialog = false
                        } else {
                            Toast.makeText(context, "Error al guardar perro", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun DogCard(
    dog: Perro,
    onDelete: () -> Unit
) {
    val dogSize = when (dog.tamano) {
        "Pequeño" -> DogSize.SMALL
        "Grande" -> DogSize.LARGE
        "Gigante" -> DogSize.GIANT
        else -> DogSize.MEDIUM
    }
    val calc = DogAgeCalculator.calculate(
        name = dog.nombre,
        ageYearsInput = dog.edadAnos,
        size = dogSize
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto local o Icono
            if (LocalPhotoManager.fileExists(dog.fotoLocalPath)) {
                AsyncImage(
                    model = File(dog.fotoLocalPath),
                    contentDescription = dog.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Pets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dog.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${dog.raza.ifBlank { "Mestizo" }} • ${dog.tamano}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${dog.edadAnos} años caninos (≈ ${calc.humanEquivalentAge} humanos)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Etapa: ${calc.stage.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDogDialog(
    currentUserId: String,
    onDismiss: () -> Unit,
    onSave: (Perro) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nombre by remember { mutableStateOf("") }
    var raza by remember { mutableStateOf("") }
    var edadStr by remember { mutableStateOf("1") }
    var pesoStr by remember { mutableStateOf("10") }
    var tamano by remember { mutableStateOf("Mediano") }
    var localPhotoPath by remember { mutableStateOf("") }
    var isSavingPhoto by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isSavingPhoto = true
            scope.launch {
                val savedPath = LocalPhotoManager.saveImageLocally(context, uri, "dog_photos")
                isSavingPhoto = false
                if (savedPath != null) {
                    localPhotoPath = savedPath
                } else {
                    Toast.makeText(context, "No se pudo guardar la imagen local", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Nueva Mascota") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Selector de foto local
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (LocalPhotoManager.fileExists(localPhotoPath)) {
                        AsyncImage(
                            model = File(localPhotoPath),
                            contentDescription = "Foto Mascota",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (isSavingPhoto) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = "Foto Local", tint = MaterialTheme.colorScheme.primary)
                            Text("Foto", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del perro *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = raza,
                    onValueChange = { raza = it },
                    label = { Text("Raza") },
                    placeholder = { Text("Ej: Golden Retriever, Criollo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = edadStr,
                        onValueChange = { edadStr = it },
                        label = { Text("Edad (años)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pesoStr,
                        onValueChange = { pesoStr = it },
                        label = { Text("Peso (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Tamaño:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.Start)
                )

                // 2x2 Cuadrícula para tamaño para no amontonarse en dialogs
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = tamano == "Pequeño",
                            onClick = { tamano = "Pequeño" },
                            label = { Text("Pequeño (<10kg)", fontSize = 11.sp, maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = tamano == "Mediano",
                            onClick = { tamano = "Mediano" },
                            label = { Text("Mediano (10-25kg)", fontSize = 11.sp, maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = tamano == "Grande",
                            onClick = { tamano = "Grande" },
                            label = { Text("Grande (25-45kg)", fontSize = 11.sp, maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = tamano == "Gigante",
                            onClick = { tamano = "Gigante" },
                            label = { Text("Gigante (>45kg)", fontSize = 11.sp, maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (nombre.isBlank()) {
                    Toast.makeText(context, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val perro = Perro(
                    usuarioId = currentUserId,
                    nombre = nombre.trim(),
                    raza = raza.trim(),
                    edadAnos = edadStr.toDoubleOrNull() ?: 1.0,
                    pesoKg = pesoStr.toDoubleOrNull() ?: 10.0,
                    tamano = tamano,
                    fotoLocalPath = localPhotoPath
                )
                onSave(perro)
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
