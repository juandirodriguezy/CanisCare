package com.example.caniscare.data.repository

import com.example.caniscare.data.model.CategoriaAlimento
import com.example.caniscare.data.model.Inventario
import com.example.caniscare.data.model.Producto
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MarketplaceRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val productosCollection = db.collection("productos")
    private val inventarioCollection = db.collection("inventario")
    private val categoriasCollection = db.collection("categorias")

    /**
     * Flujo en tiempo real de todos los productos disponibles en el catálogo canino.
     */
    fun getProductsFlow(): Flow<List<Producto>> = callbackFlow {
        val listener = productosCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val items = snapshot?.toObjects(Producto::class.java) ?: emptyList()
            trySend(items)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Guarda o actualiza un producto en Firestore y sincroniza su inventario.
     */
    suspend fun saveProduct(producto: Producto): Result<String> {
        return try {
            val id = if (producto.id.isBlank()) UUID.randomUUID().toString() else producto.id
            val toSave = producto.copy(id = id)

            val batch = db.batch()
            batch.set(productosCollection.document(id), toSave)

            val inventario = Inventario(
                id = id,
                productoId = id,
                stockDisponible = toSave.stockDisponible,
                stockMinimo = 2,
                ultimaActualizacion = Timestamp.now()
            )
            batch.set(inventarioCollection.document(id), inventario)

            batch.commit().await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina un producto y su registro de inventario.
     */
    suspend fun deleteProduct(productoId: String): Result<Unit> {
        return try {
            val batch = db.batch()
            batch.delete(productosCollection.document(productoId))
            batch.delete(inventarioCollection.document(productoId))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Precarga alimentos exclusivamente caninos (concentrados, húmedos y snacks) si el catálogo está vacío.
     */
    suspend fun seedInitialCanineProductsIfNeeded() {
        try {
            val snapshot = productosCollection.limit(1).get().await()
            if (!snapshot.isEmpty) return // Ya existen productos

            val initialProducts = listOf(
                Producto(
                    id = "prod_conc_01",
                    categoriaId = "cat_concentrados",
                    categoriaNombre = "Concentrados",
                    nombre = "Alimento Adulto Razas Medianas",
                    marca = "Royal Canin Canino",
                    descripcion = "Fórmula completa para mantenimiento muscular y digestión óptima en perros adultos.",
                    presentacion = "Bolsa 15 kg",
                    precio = 245000.0,
                    stockDisponible = 15
                ),
                Producto(
                    id = "prod_conc_02",
                    categoriaId = "cat_concentrados",
                    categoriaNombre = "Concentrados",
                    nombre = "Pro Plan Puppy OptiStart",
                    marca = "Purina",
                    descripcion = "Nutrición con anticuerpos naturales del calostro para fortalecer el sistema inmune de cachorros.",
                    presentacion = "Bolsa 7.5 kg",
                    precio = 168000.0,
                    stockDisponible = 12
                ),
                Producto(
                    id = "prod_hum_01",
                    categoriaId = "cat_humedos",
                    categoriaNombre = "Alimentos Húmedos",
                    nombre = "Pâté Canino de Cordero & Arroz",
                    marca = "Hill's Science Diet",
                    descripcion = "Alimento húmedo en lata de alta digestibilidad para perros con sensibilidad estomacal.",
                    presentacion = "Lata 370 g",
                    precio = 18500.0,
                    stockDisponible = 40
                ),
                Producto(
                    id = "prod_hum_02",
                    categoriaId = "cat_humedos",
                    categoriaNombre = "Alimentos Húmedos",
                    nombre = "Estofado Canino Gourmet de Pollo y Verduras",
                    marca = "Pedigree Canino",
                    descripcion = "Trozos tiernos cocinados al vapor con vegetales frescos y vitaminas esenciales.",
                    presentacion = "Sobre Pouch 100 g",
                    precio = 4800.0,
                    stockDisponible = 60
                ),
                Producto(
                    id = "prod_snack_01",
                    categoriaId = "cat_snacks",
                    categoriaNombre = "Snacks Caninos",
                    nombre = "Dentastix Cuidado Oral Diario",
                    marca = "Pedigree",
                    descripcion = "Snacks dentales que reducen hasta un 80% la acumulación de sarro canino.",
                    presentacion = "Paquete 7 barras (180 g)",
                    precio = 19500.0,
                    stockDisponible = 30
                ),
                Producto(
                    id = "prod_snack_02",
                    categoriaId = "cat_snacks",
                    categoriaNombre = "Snacks Caninos",
                    nombre = "Galletas Horneadas con Proteína de Res",
                    marca = "Chunky Canino",
                    descripcion = "Golosinas crujientes ideales para entrenamiento y refuerzo positivo en todas las etapas.",
                    presentacion = "Caja 350 g",
                    precio = 14200.0,
                    stockDisponible = 25
                )
            )

            val batch = db.batch()
            for (p in initialProducts) {
                batch.set(productosCollection.document(p.id), p)
                val inv = Inventario(
                    id = p.id,
                    productoId = p.id,
                    stockDisponible = p.stockDisponible,
                    stockMinimo = 2,
                    ultimaActualizacion = Timestamp.now()
                )
                batch.set(inventarioCollection.document(p.id), inv)
            }
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
