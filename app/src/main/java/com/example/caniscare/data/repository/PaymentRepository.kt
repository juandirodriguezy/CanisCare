package com.example.caniscare.data.repository

import com.example.caniscare.data.model.CarritoItem
import com.example.caniscare.data.model.DetallePedido
import com.example.caniscare.data.model.Pago
import com.example.caniscare.data.model.Pedido
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

sealed class PaymentSimulationResult {
    data class Success(val pedido: Pedido, val pago: Pago) : PaymentSimulationResult()
    data class Rejected(val motivo: String) : PaymentSimulationResult()
    data class Pending(val pedido: Pedido, val mensaje: String) : PaymentSimulationResult()
}

class PaymentRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val pedidosCollection = db.collection("pedidos")
    private val detallesCollection = db.collection("detalles_pedidos")
    private val pagosCollection = db.collection("pagos")
    private val productosCollection = db.collection("productos")
    private val cartCollection = db.collection("cart_items")

    fun getOrderHistoryFlow(usuarioId: String): Flow<List<Pedido>> = callbackFlow {
        val listener = pedidosCollection
            .whereEqualTo("usuarioId", usuarioId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val orders = snapshot?.toObjects(Pedido::class.java)
                    ?.sortedByDescending { it.fechaPedido.seconds }
                    ?: emptyList()
                trySend(orders)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Valida existencias en tiempo real de cada producto antes de procesar el pago.
     */
    suspend fun validateStockBeforePayment(cartItems: List<CarritoItem>): Result<Unit> {
        return try {
            for (item in cartItems) {
                val prodDoc = productosCollection.document(item.productoId).get().await()
                val currentStock = prodDoc.getLong("stockDisponible")?.toInt() ?: 0
                if (currentStock < item.cantidad) {
                    return Result.failure(Exception("Stock insuficiente para ${item.nombreProducto}. Disponibles: $currentStock"))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Procesa la simulación de pasarela Sandbox (Wompi / Mercado Pago compliant).
     * Simula cobro sin costo real y gestiona los 3 estados: APROBADO, RECHAZADO, PENDIENTE.
     */
    suspend fun processSandboxPayment(
        usuarioId: String,
        usuarioNombre: String,
        direccionEntrega: String,
        ciudadEntrega: String,
        cartItems: List<CarritoItem>,
        subtotal: Double,
        costoEnvio: Double,
        total: Double,
        simulatedState: String, // "APROBADO", "RECHAZADO", "PENDIENTE"
        cardNumber: String
    ): PaymentSimulationResult {
        if (simulatedState == "RECHAZADO") {
            return PaymentSimulationResult.Rejected("Transacción rechazada por el emisor (Fondos insuficientes o tarjeta inválida en sandbox)")
        }

        val pedidoId = "PED-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        val pagoId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        val ultimosDigitos = if (cardNumber.length >= 4) cardNumber.takeLast(4) else "4242"

        val pedido = Pedido(
            id = pedidoId,
            usuarioId = usuarioId,
            usuarioNombre = usuarioNombre,
            fechaPedido = Timestamp.now(),
            estado = if (simulatedState == "APROBADO") "PAGADO" else "PENDIENTE",
            direccionEntrega = direccionEntrega,
            ciudadEntrega = ciudadEntrega,
            subtotal = subtotal,
            costoEnvio = costoEnvio,
            total = total
        )

        val pago = Pago(
            id = pagoId,
            pedidoId = pedidoId,
            pasarela = "Wompi/MercadoPago Sandbox",
            transaccionId = "TXN-" + System.currentTimeMillis(),
            metodoPago = "Tarjeta de Crédito (Sandbox)",
            estado = simulatedState,
            monto = total,
            fechaPago = Timestamp.now(),
            ultimosDigitosTarjeta = ultimosDigitos
        )

        try {
            val batch = db.batch()

            // 1. Guardar Pedido
            batch.set(pedidosCollection.document(pedidoId), pedido)

            // 2. Guardar Detalles de Pedido
            for (item in cartItems) {
                val detalleId = UUID.randomUUID().toString()
                val detalle = DetallePedido(
                    id = detalleId,
                    pedidoId = pedidoId,
                    productoId = item.productoId,
                    nombreProducto = item.nombreProducto,
                    cantidad = item.cantidad,
                    precioUnitario = item.precioUnitario,
                    subtotal = item.precioUnitario * item.cantidad
                )
                batch.set(detallesCollection.document(detalleId), detalle)

                // 3. Descontar stock si fue aprobado
                if (simulatedState == "APROBADO") {
                    val prodRef = productosCollection.document(item.productoId)
                    batch.update(prodRef, "stockDisponible", FieldValue.increment(-item.cantidad.toLong()))
                }
            }

            // 4. Guardar Transacción de Pago
            batch.set(pagosCollection.document(pagoId), pago)

            // 5. Vaciar carrito del usuario
            for (item in cartItems) {
                batch.delete(cartCollection.document(item.id))
            }

            batch.commit().await()

            return if (simulatedState == "APROBADO") {
                PaymentSimulationResult.Success(pedido, pago)
            } else {
                PaymentSimulationResult.Pending(pedido, "La transacción está siendo validada por la entidad financiera de prueba.")
            }
        } catch (e: Exception) {
            return PaymentSimulationResult.Rejected("Error al registrar compra: ${e.localizedMessage}")
        }
    }
}
