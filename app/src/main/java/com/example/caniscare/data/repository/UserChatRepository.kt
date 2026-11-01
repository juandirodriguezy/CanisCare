package com.example.caniscare.data.repository

import com.example.caniscare.data.model.Conversacion
import com.example.caniscare.data.model.Mensaje
import com.example.caniscare.data.model.Usuario
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UserChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val usuariosCollection = db.collection("usuarios")
    private val conversacionesCollection = db.collection("conversaciones")
    private val mensajesCollection = db.collection("mensajes")

    /**
     * Obtiene la lista de otros usuarios registrados para iniciar chat.
     */
    suspend fun getAvailableUsers(currentUserId: String): List<Usuario> {
        return try {
            val snapshot = usuariosCollection.get().await()
            snapshot.toObjects(Usuario::class.java).filter { it.id != currentUserId }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Flujo en tiempo real de conversaciones donde participa el usuario actual.
     */
    fun getUserConversationsFlow(currentUserId: String): Flow<List<Conversacion>> = callbackFlow {
        // Consultamos conversaciones donde el usuario es participante 1 o participante 2
        val listener = conversacionesCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val all = snapshot?.toObjects(Conversacion::class.java) ?: emptyList()
                val userConversations = all.filter {
                    it.participante1Id == currentUserId || it.participante2Id == currentUserId
                }.sortedByDescending { it.fechaActualizacion.seconds }
                trySend(userConversations)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Obtiene o crea la conversación 1 a 1 entre dos usuarios.
     */
    suspend fun getOrCreateConversation(
        currentUserId: String,
        currentUserName: String,
        targetUserId: String,
        targetUserName: String
    ): String {
        val snapshot = conversacionesCollection.get().await()
        val existing = snapshot.toObjects(Conversacion::class.java).firstOrNull {
            (it.participante1Id == currentUserId && it.participante2Id == targetUserId) ||
            (it.participante1Id == targetUserId && it.participante2Id == currentUserId)
        }

        if (existing != null) {
            return existing.id
        }

        val newId = UUID.randomUUID().toString()
        val newConv = Conversacion(
            id = newId,
            participante1Id = currentUserId,
            participante1Nombre = currentUserName,
            participante2Id = targetUserId,
            participante2Nombre = targetUserName,
            ultimoMensaje = "Conversación iniciada",
            fechaActualizacion = Timestamp.now()
        )
        conversacionesCollection.document(newId).set(newConv).await()
        return newId
    }

    /**
     * Flujo en tiempo real de los mensajes de una conversación ordenados por fecha.
     */
    fun getMessagesFlow(conversacionId: String): Flow<List<Mensaje>> = callbackFlow {
        val listener = mensajesCollection
            .whereEqualTo("conversacionId", conversacionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val msgs = snapshot?.toObjects(Mensaje::class.java)
                    ?.sortedBy { it.timestamp.seconds }
                    ?: emptyList()
                trySend(msgs)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Envía un mensaje y actualiza el último mensaje de la conversación.
     */
    suspend fun sendMessage(
        conversacionId: String,
        remitenteId: String,
        remitenteNombre: String,
        contenido: String
    ): Result<Unit> {
        return try {
            val msgId = UUID.randomUUID().toString()
            val now = Timestamp.now()
            val mensaje = Mensaje(
                id = msgId,
                conversacionId = conversacionId,
                remitenteId = remitenteId,
                remitenteNombre = remitenteNombre,
                contenido = contenido.trim(),
                timestamp = now,
                leido = false
            )

            val batch = db.batch()
            batch.set(mensajesCollection.document(msgId), mensaje)
            batch.update(
                conversacionesCollection.document(conversacionId),
                mapOf(
                    "ultimoMensaje" to contenido.trim(),
                    "fechaActualizacion" to now
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
