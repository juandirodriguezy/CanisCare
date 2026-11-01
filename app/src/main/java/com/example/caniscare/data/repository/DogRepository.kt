package com.example.caniscare.data.repository

import com.example.caniscare.data.model.Perro
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class DogRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val collection = db.collection("perros")

    fun getDogsForUser(usuarioId: String): Flow<List<Perro>> = callbackFlow {
        val listener = collection
            .whereEqualTo("usuarioId", usuarioId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val dogs = snapshot?.toObjects(Perro::class.java) ?: emptyList()
                trySend(dogs)
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveDog(perro: Perro): Result<String> {
        return try {
            val id = if (perro.id.isBlank()) UUID.randomUUID().toString() else perro.id
            val perroToSave = perro.copy(id = id)
            collection.document(id).set(perroToSave).await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDog(perroId: String): Result<Unit> {
        return try {
            collection.document(perroId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
