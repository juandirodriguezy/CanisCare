package com.example.caniscare.data.repository

import com.example.caniscare.data.model.CarritoItem
import com.example.caniscare.data.model.Producto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CartRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val cartCollection = db.collection("cart_items")

    fun getCartItemsFlow(usuarioId: String): Flow<List<CarritoItem>> = callbackFlow {
        val listener = cartCollection
            .whereEqualTo("usuarioId", usuarioId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(CarritoItem::class.java) ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addToCart(usuarioId: String, producto: Producto): Result<Unit> {
        return try {
            val existingSnapshot = cartCollection
                .whereEqualTo("usuarioId", usuarioId)
                .whereEqualTo("productoId", producto.id)
                .limit(1)
                .get()
                .await()

            if (!existingSnapshot.isEmpty) {
                val doc = existingSnapshot.documents[0]
                val currentItem = doc.toObject(CarritoItem::class.java)
                val newQty = (currentItem?.cantidad ?: 1) + 1
                if (newQty <= producto.stockDisponible) {
                    doc.reference.update("cantidad", newQty).await()
                }
            } else {
                val id = UUID.randomUUID().toString()
                val item = CarritoItem(
                    id = id,
                    usuarioId = usuarioId,
                    productoId = producto.id,
                    nombreProducto = producto.nombre,
                    marca = producto.marca,
                    presentacion = producto.presentacion,
                    precioUnitario = producto.precio,
                    cantidad = 1,
                    fotoLocalPath = producto.fotoLocalPath
                )
                cartCollection.document(id).set(item).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateQuantity(cartItemId: String, newQuantity: Int, stockMax: Int): Result<Unit> {
        return try {
            if (newQuantity <= 0) {
                cartCollection.document(cartItemId).delete().await()
            } else {
                val safeQty = newQuantity.coerceAtMost(stockMax)
                cartCollection.document(cartItemId).update("cantidad", safeQty).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromCart(cartItemId: String): Result<Unit> {
        return try {
            cartCollection.document(cartItemId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
