package com.example.caniscare.ui.screens.cart

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.caniscare.data.local.LocalPhotoManager
import com.example.caniscare.data.model.CarritoItem
import com.example.caniscare.data.model.Pago
import com.example.caniscare.data.model.Pedido
import com.example.caniscare.data.repository.CartRepository
import com.example.caniscare.data.repository.PaymentRepository
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    currentUserId: String,
    currentUserName: String,
    userInitialAddress: String,
    cartRepository: CartRepository,
    paymentRepository: PaymentRepository,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cartItems by cartRepository.getCartItemsFlow(currentUserId).collectAsState(initial = emptyList())
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 } }

    var direccionEntrega by remember { mutableStateOf(userInitialAddress) }
    var ciudadEntrega by remember { mutableStateOf("Bogotá") }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var completedOrder by remember { mutableStateOf<Pair<Pedido, Pago>?>(null) }

    val subtotal = remember(cartItems) {
        cartItems.sumOf { it.precioUnitario * it.cantidad }
    }
    // Envío gratis si la compra supera $150.000 COP, sino $9.500 COP
    val costoEnvio = remember(subtotal) {
        if (subtotal == 0.0 || subtotal >= 150000.0) 0.0 else 9500.0
    }
    val total = remember(subtotal, costoEnvio) {
        subtotal + costoEnvio
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Carrito de Compras", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "Historial de Pedidos")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tu carrito está vacío",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Explora el catálogo de alimentos caninos y añade los productos favoritos de tu mascota.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ver Pedidos Anteriores")
                    }
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
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                items(cartItems, key = { it.id }) { item ->
                    CartItemRow(
                        item = item,
                        currencyFormat = currencyFormat,
                        onIncrease = {
                            scope.launch {
                                cartRepository.updateQuantity(item.id, item.cantidad + 1, 99)
                            }
                        },
                        onDecrease = {
                            scope.launch {
                                cartRepository.updateQuantity(item.id, item.cantidad - 1, 99)
                            }
                        },
                        onRemove = {
                            scope.launch {
                                cartRepository.removeFromCart(item.id)
                            }
                        }
                    )
                }

                item {
                    // Dirección de Envío
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Dirección de Entrega:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = direccionEntrega,
                                onValueChange = { direccionEntrega = it },
                                label = { Text("Calle, Carrera, N° de Casa/Apto *") },
                                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = ciudadEntrega,
                                onValueChange = { ciudadEntrega = it },
                                label = { Text("Ciudad *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    // Resumen de Compra
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Resumen de la Compra", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal:")
                                Text(currencyFormat.format(subtotal), fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Costo de Envío:")
                                Text(
                                    if (costoEnvio == 0.0) "GRATIS" else currencyFormat.format(costoEnvio),
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (costoEnvio == 0.0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total:", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    currencyFormat.format(total),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (direccionEntrega.isBlank()) {
                                        Toast.makeText(context, "Por favor ingrese la dirección de entrega", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    showCheckoutDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Confirmar e Iniciar Pago", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

        if (showCheckoutDialog) {
            CheckoutDialog(
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                direccionEntrega = direccionEntrega,
                ciudadEntrega = ciudadEntrega,
                cartItems = cartItems,
                subtotal = subtotal,
                costoEnvio = costoEnvio,
                total = total,
                paymentRepository = paymentRepository,
                onDismiss = { showCheckoutDialog = false },
                onPaymentSuccessOrPending = { pedido, pago ->
                    showCheckoutDialog = false
                    completedOrder = Pair(pedido, pago)
                }
            )
        }

        completedOrder?.let { (ped, pag) ->
            ReceiptDialog(
                pedido = ped,
                pago = pag,
                onDismiss = { completedOrder = null }
            )
        }
    }
}

@Composable
private fun CartItemRow(
    item: CarritoItem,
    currencyFormat: NumberFormat,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto local o Icono
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (LocalPhotoManager.fileExists(item.fotoLocalPath)) {
                    AsyncImage(
                        model = File(item.fotoLocalPath),
                        contentDescription = item.nombreProducto,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Pets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.nombreProducto,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "${item.marca} • ${item.presentacion}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currencyFormat.format(item.precioUnitario),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Selector de Cantidad
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrease, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "Menos", modifier = Modifier.size(16.dp))
                }
                Text(
                    text = "${item.cantidad}",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                IconButton(onClick = onIncrease, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Más", modifier = Modifier.size(16.dp))
                }
            }

            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
