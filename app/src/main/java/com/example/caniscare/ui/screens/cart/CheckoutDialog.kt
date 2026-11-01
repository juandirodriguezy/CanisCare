package com.example.caniscare.ui.screens.cart

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.caniscare.data.model.CarritoItem
import com.example.caniscare.data.model.Pago
import com.example.caniscare.data.model.Pedido
import com.example.caniscare.data.repository.PaymentRepository
import com.example.caniscare.data.repository.PaymentSimulationResult
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutDialog(
    currentUserId: String,
    currentUserName: String,
    direccionEntrega: String,
    ciudadEntrega: String,
    cartItems: List<CarritoItem>,
    subtotal: Double,
    costoEnvio: Double,
    total: Double,
    paymentRepository: PaymentRepository,
    onDismiss: () -> Unit,
    onPaymentSuccessOrPending: (Pedido, Pago) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 } }

    var cardNumber by remember { mutableStateOf("4242 4242 4242 4242") }
    var cardHolder by remember { mutableStateOf(currentUserName.ifBlank { "Juan Perez" }) }
    var expiryDate by remember { mutableStateOf("12/28") }
    var cvv by remember { mutableStateOf("123") }
    var isProcessing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Pasarela de Pagos Sandbox", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Wompi / Mercado Pago Test", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total a Procesar:", fontWeight = FontWeight.Bold)
                        Text(currencyFormat.format(total), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Text(
                    text = "Datos de Tarjeta (Entorno de Pruebas Seguro):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { cardNumber = it },
                    label = { Text("Número de Tarjeta de Prueba") },
                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cardHolder,
                    onValueChange = { cardHolder = it },
                    label = { Text("Nombre del Titular") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { expiryDate = it },
                        label = { Text("Vence (MM/AA)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cvv,
                        onValueChange = { cvv = it },
                        label = { Text("CVV") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sandbox Activo: No se cobrará dinero real.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Probar Estados de Transacción:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                // Botones para probar los 3 estados exigidos por la guía
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // 1. APROBADO
                    Button(
                        onClick = {
                            isProcessing = true
                            scope.launch {
                                val stockCheck = paymentRepository.validateStockBeforePayment(cartItems)
                                if (stockCheck.isFailure) {
                                    isProcessing = false
                                    Toast.makeText(context, stockCheck.exceptionOrNull()?.message, Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                val result = paymentRepository.processSandboxPayment(
                                    usuarioId = currentUserId,
                                    usuarioNombre = currentUserName,
                                    direccionEntrega = direccionEntrega,
                                    ciudadEntrega = ciudadEntrega,
                                    cartItems = cartItems,
                                    subtotal = subtotal,
                                    costoEnvio = costoEnvio,
                                    total = total,
                                    simulatedState = "APROBADO",
                                    cardNumber = cardNumber
                                )
                                isProcessing = false
                                if (result is PaymentSimulationResult.Success) {
                                    onPaymentSuccessOrPending(result.pedido, result.pago)
                                }
                            }
                        },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("Aprobar", fontSize = 11.sp)
                    }

                    // 2. RECHAZADO
                    Button(
                        onClick = {
                            isProcessing = true
                            scope.launch {
                                val result = paymentRepository.processSandboxPayment(
                                    usuarioId = currentUserId,
                                    usuarioNombre = currentUserName,
                                    direccionEntrega = direccionEntrega,
                                    ciudadEntrega = ciudadEntrega,
                                    cartItems = cartItems,
                                    subtotal = subtotal,
                                    costoEnvio = costoEnvio,
                                    total = total,
                                    simulatedState = "RECHAZADO",
                                    cardNumber = cardNumber
                                )
                                isProcessing = false
                                if (result is PaymentSimulationResult.Rejected) {
                                    Toast.makeText(context, "❌ ${result.motivo}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("Rechazar", fontSize = 11.sp)
                    }

                    // 3. PENDIENTE
                    Button(
                        onClick = {
                            isProcessing = true
                            scope.launch {
                                val result = paymentRepository.processSandboxPayment(
                                    usuarioId = currentUserId,
                                    usuarioNombre = currentUserName,
                                    direccionEntrega = direccionEntrega,
                                    ciudadEntrega = ciudadEntrega,
                                    cartItems = cartItems,
                                    subtotal = subtotal,
                                    costoEnvio = costoEnvio,
                                    total = total,
                                    simulatedState = "PENDIENTE",
                                    cardNumber = cardNumber
                                )
                                isProcessing = false
                                if (result is PaymentSimulationResult.Pending) {
                                    val dummyPago = Pago(
                                        pedidoId = result.pedido.id,
                                        estado = "PENDIENTE",
                                        monto = total,
                                        transaccionId = "TXN-PEND-" + System.currentTimeMillis()
                                    )
                                    onPaymentSuccessOrPending(result.pedido, dummyPago)
                                }
                            }
                        },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("Pendiente", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) {
                Text("Cancelar")
            }
        }
    )
}
