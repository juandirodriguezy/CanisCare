package com.example.caniscare.ui.screens.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.caniscare.data.model.Pago
import com.example.caniscare.data.model.Pedido
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ReceiptDialog(
    pedido: Pedido,
    pago: Pago,
    onDismiss: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val fechaStr = dateFormat.format(pago.fechaPago.toDate())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (pago.estado == "APROBADO") Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                    contentDescription = null,
                    tint = if (pago.estado == "APROBADO") Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (pago.estado == "APROBADO") "¡Pago Aprobado!" else "Pago Pendiente",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (pago.estado == "APROBADO") Color(0xFF2E7D32) else Color(0xFFE65100)
                )
                Text(
                    text = "Comprobante de Transacción Digital",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReceiptRow(label = "N° Pedido:", value = pedido.id)
                    ReceiptRow(label = "Ref. Transacción:", value = pago.transaccionId)
                    ReceiptRow(label = "Pasarela:", value = pago.pasarela)
                    ReceiptRow(label = "Método:", value = "${pago.metodoPago} (Terminada en ${pago.ultimosDigitosTarjeta})")
                    ReceiptRow(label = "Fecha y Hora:", value = fechaStr)
                    ReceiptRow(label = "Dirección de Entrega:", value = pedido.direccionEntrega)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    ReceiptRow(label = "Subtotal:", value = currencyFormat.format(pedido.subtotal))
                    ReceiptRow(label = "Costo de Envío:", value = currencyFormat.format(pedido.costoEnvio))
                    ReceiptRow(
                        label = "Total Pagado:",
                        value = currencyFormat.format(pago.monto),
                        isBold = true,
                        highlightColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Aceptar y Finalizar")
            }
        }
    )
}

@Composable
private fun ReceiptRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    highlightColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End
        )
    }
}
