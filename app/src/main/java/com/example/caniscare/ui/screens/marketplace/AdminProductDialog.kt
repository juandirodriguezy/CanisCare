package com.example.caniscare.ui.screens.marketplace

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
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
import com.example.caniscare.data.local.LocalPhotoManager
import com.example.caniscare.data.model.Producto
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductDialog(
    productoToEdit: Producto? = null,
    onDismiss: () -> Unit,
    onSaveProduct: (Producto) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nombre by remember { mutableStateOf(productoToEdit?.nombre ?: "") }
    var marca by remember { mutableStateOf(productoToEdit?.marca ?: "") }
    var categoria by remember { mutableStateOf(productoToEdit?.categoriaNombre ?: "Concentrados") }
    var descripcion by remember { mutableStateOf(productoToEdit?.descripcion ?: "") }
    var presentacion by remember { mutableStateOf(productoToEdit?.presentacion ?: "Bolsa 15 kg") }
    var precioStr by remember { mutableStateOf(productoToEdit?.precio?.toInt()?.toString() ?: "120000") }
    var stockStr by remember { mutableStateOf(productoToEdit?.stockDisponible?.toString() ?: "10") }
    var localPhotoPath by remember { mutableStateOf(productoToEdit?.fotoLocalPath ?: "") }
    var isSavingPhoto by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isSavingPhoto = true
            scope.launch {
                val saved = LocalPhotoManager.saveImageLocally(context, uri, "product_photos")
                isSavingPhoto = false
                if (saved != null) {
                    localPhotoPath = saved
                } else {
                    Toast.makeText(context, "Error al guardar imagen local", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val categoriasDisponibles = listOf("Concentrados", "Alimentos Húmedos", "Snacks Caninos")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (productoToEdit == null) "Agregar Alimento Canino" else "Editar Producto")
        },
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
                        .size(90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (LocalPhotoManager.fileExists(localPhotoPath)) {
                        AsyncImage(
                            model = File(localPhotoPath),
                            contentDescription = "Foto Alimento",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (isSavingPhoto) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Foto Local", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del Producto *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = marca,
                    onValueChange = { marca = it },
                    label = { Text("Marca Canina *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Categoría Canina:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.Start)
                )

                // Chips responsivos con scroll horizontal para no comprimirse
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoriasDisponibles.forEach { cat ->
                        FilterChip(
                            selected = categoria == cat,
                            onClick = { categoria = cat },
                            label = { Text(cat, fontSize = 11.sp, maxLines = 1) }
                        )
                    }
                }

                OutlinedTextField(
                    value = presentacion,
                    onValueChange = { presentacion = it },
                    label = { Text("Presentación (Ej: Bolsa 15 kg, Lata 370g)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = precioStr,
                        onValueChange = { precioStr = it },
                        label = { Text("Precio ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text("Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción Nutricional") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (nombre.isBlank() || marca.isBlank()) {
                    Toast.makeText(context, "Nombre y marca son obligatorios", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val prod = Producto(
                    id = productoToEdit?.id ?: "",
                    categoriaId = "cat_${categoria.lowercase().replace(" ", "_")}",
                    categoriaNombre = categoria,
                    nombre = nombre.trim(),
                    marca = marca.trim(),
                    descripcion = descripcion.trim(),
                    presentacion = presentacion.trim(),
                    precio = precioStr.toDoubleOrNull() ?: 10000.0,
                    fotoLocalPath = localPhotoPath,
                    stockDisponible = stockStr.toIntOrNull() ?: 10
                )
                onSaveProduct(prod)
            }) {
                Text("Guardar Producto")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
