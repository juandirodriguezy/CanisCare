package com.example.caniscare.ui.screens.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.caniscare.data.model.Veterinaria
import com.example.caniscare.data.repository.VeterinariaRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VetMapScreen(
    veterinariaRepository: VeterinariaRepository = remember { VeterinariaRepository() }
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedVeterinaria by remember { mutableStateOf<Veterinaria?>(null) }
    var onlyEmergency by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(4.7012, -74.0450), 13f)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasLocationPermission = fineGranted || coarseGranted

        if (hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val latLng = LatLng(loc.latitude, loc.longitude)
                    userLocation = latLng
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 14f)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val latLng = LatLng(loc.latitude, loc.longitude)
                    userLocation = latLng
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 14f)
                }
            }
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Calcular lista dinámica de veterinarias con distancias
    val veterinariesWithDistance = remember(userLocation, onlyEmergency, searchQuery) {
        if (userLocation != null) {
            val list = veterinariaRepository.getVeterinariesNearLocation(
                userLocation!!.latitude,
                userLocation!!.longitude,
                onlyEmergency
            )
            if (searchQuery.isBlank()) list else list.filter {
                it.first.nombre.contains(searchQuery, ignoreCase = true) ||
                it.first.ciudad.contains(searchQuery, ignoreCase = true) ||
                it.first.direccion.contains(searchQuery, ignoreCase = true)
            }
        } else {
            val list = veterinariaRepository.searchVeterinaries(searchQuery, onlyEmergency)
            list.map { Pair(it, -1.0) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Veterinarias y Urgencias", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (onlyEmergency) "Filtrando: Solo Urgencias 24 Horas" else "Clínicas Cercanas Verificadas",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (onlyEmergency) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (hasLocationPermission) {
                            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                if (loc != null) {
                                    val latLng = LatLng(loc.latitude, loc.longitude)
                                    userLocation = latLng
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
                                }
                            }
                        } else {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        }
                    }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Mi Ubicación", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Mapa Google Maps Compose
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
            ) {
                veterinariesWithDistance.forEach { (vet, _) ->
                    Marker(
                        state = MarkerState(position = LatLng(vet.latitud, vet.longitud)),
                        title = vet.nombre,
                        snippet = if (vet.esUrgencias24h) "🚨 Urgencias 24H: ${vet.telefono}" else "Consulta: ${vet.telefono}",
                        icon = if (vet.esUrgencias24h) {
                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                        } else {
                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        },
                        onClick = {
                            selectedVeterinaria = vet
                            false
                        }
                    )
                }
            }

            // Barra superior flotante: Búsqueda y Chips de Filtro
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .padding(12.dp)
                    .align(Alignment.TopCenter)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por ciudad, clínica o dirección...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !onlyEmergency,
                        onClick = { onlyEmergency = false },
                        label = { Text("Todas") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    FilterChip(
                        selected = onlyEmergency,
                        onClick = { onlyEmergency = true },
                        leadingIcon = {
                            Icon(Icons.Default.Emergency, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("🚨 Solo Urgencias 24H", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                }
            }

            // Carrusel inferior de veterinarias o detalle de la seleccionada
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
            ) {
                selectedVeterinaria?.let { vet ->
                    val dist = userLocation?.let { loc ->
                        veterinariaRepository.calculateDistanceKm(loc.latitude, loc.longitude, vet.latitud, vet.longitud)
                    }
                    VeterinariaDetailCard(
                        veterinaria = vet,
                        distanciaKm = dist,
                        onClose = { selectedVeterinaria = null },
                        onCall = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${vet.telefono}"))
                            context.startActivity(intent)
                        },
                        onDirections = {
                            val uri = Uri.parse("google.navigation:q=${vet.latitud},${vet.longitud}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        }
                    )
                } ?: run {
                    // Carrusel de resultados cercanos
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(veterinariesWithDistance) { (vet, dist) ->
                            VeterinariaQuickCard(
                                veterinaria = vet,
                                distanciaKm = if (dist >= 0) dist else null,
                                onClick = {
                                    selectedVeterinaria = vet
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                        LatLng(vet.latitud, vet.longitud), 15f
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VeterinariaDetailCard(
    veterinaria: Veterinaria,
    distanciaKm: Double?,
    onClose: () -> Unit,
    onCall: () -> Unit,
    onDirections: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (veterinaria.esUrgencias24h) Color(0xFFFFEBEE) else Color(0xFFE3F2FD)
                ) {
                    Text(
                        text = if (veterinaria.esUrgencias24h) "🚨 URGENCIAS 24 HORAS" else "CONSULTA VETERINARIA",
                        color = if (veterinaria.esUrgencias24h) Color(0xFFC62828) else Color(0xFF1565C0),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                TextButton(onClick = onClose) {
                    Text("Cerrar")
                }
            }

            Text(
                text = veterinaria.nombre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${veterinaria.direccion} (${veterinaria.ciudad})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            distanciaKm?.let { d ->
                Text(
                    text = "Aproximadamente a $d km de tu ubicación",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Horario: ${veterinaria.horario}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onCall,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Llamar", fontSize = 12.sp)
                }
                Button(
                    onClick = onDirections,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cómo Llegar", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun VeterinariaQuickCard(
    veterinaria: Veterinaria,
    distanciaKm: Double?,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .width(230.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (veterinaria.esUrgencias24h) "🚨 Urgencias 24H" else "Consulta",
                color = if (veterinaria.esUrgencias24h) Color(0xFFC62828) else Color(0xFF1565C0),
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
            Text(
                text = veterinaria.nombre,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = veterinaria.ciudad,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            distanciaKm?.let { d ->
                Text(
                    text = "A $d km",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
