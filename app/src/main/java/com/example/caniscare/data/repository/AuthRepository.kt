package com.example.caniscare.data.repository

import com.example.caniscare.data.model.Usuario
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = try { auth.currentUser } catch (e: Exception) { null }

    val isUserLoggedIn: Boolean
        get() = currentUser != null

    suspend fun registerUser(
        nombre: String,
        email: String,
        password: String,
        telefono: String = "",
        direccion: String = ""
    ): Result<Usuario> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val uid = authResult.user?.uid ?: throw Exception("No se pudo obtener el UID del usuario")

            val nuevoUsuario = Usuario(
                id = uid,
                nombre = nombre.trim(),
                email = email.trim(),
                telefono = telefono.trim(),
                direccion = direccion.trim(),
                rol = if (email.contains("admin")) "admin" else "cliente"
            )

            try {
                db.collection("usuarios").document(uid).set(nuevoUsuario).await()
            } catch (fe: Exception) {
                // Si Firestore aún no está habilitado en consola, retornamos el usuario de auth
            }
            Result.success(nuevoUsuario)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Ya existe una cuenta registrada con este correo electrónico."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("El formato del correo o contraseña es inválido."))
        } catch (e: Exception) {
            val msg = if (e.message?.contains("API key not valid", ignoreCase = true) == true ||
                          e.message?.contains("configuration", ignoreCase = true) == true) {
                "Firebase no configurado: Reemplaza app/google-services.json con el de tu consola Firebase."
            } else {
                e.localizedMessage ?: "Error al registrar usuario"
            }
            Result.failure(Exception(msg))
        }
    }

    suspend fun loginUser(email: String, password: String): Result<Usuario> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val uid = authResult.user?.uid ?: throw Exception("Error al autenticar usuario")

            val snapshot = try {
                db.collection("usuarios").document(uid).get().await()
            } catch (fe: Exception) {
                null
            }

            val usuario = snapshot?.toObject(Usuario::class.java)
                ?: Usuario(id = uid, email = email, nombre = email.substringBefore("@"))
            Result.success(usuario)
        } catch (e: Exception) {
            val msg = if (e.message?.contains("API key not valid", ignoreCase = true) == true ||
                          e.message?.contains("configuration", ignoreCase = true) == true) {
                "Firebase no configurado: Reemplaza app/google-services.json con el de tu consola Firebase."
            } else {
                e.localizedMessage ?: "Credenciales incorrectas o error de conexión"
            }
            Result.failure(Exception(msg))
        }
    }

    suspend fun getCurrentUserProfile(): Result<Usuario> {
        return try {
            val uid = currentUser?.uid ?: throw Exception("No hay sesión activa")
            val snapshot = db.collection("usuarios").document(uid).get().await()
            val usuario = snapshot.toObject(Usuario::class.java)
                ?: Usuario(id = uid, email = currentUser?.email ?: "")
            Result.success(usuario)
        } catch (e: Exception) {
            val fallback = Usuario(
                id = currentUser?.uid ?: "user_default",
                email = currentUser?.email ?: "",
                nombre = currentUser?.email?.substringBefore("@") ?: "Usuario CanisCare"
            )
            Result.success(fallback)
        }
    }

    suspend fun updateProfile(usuario: Usuario): Result<Unit> {
        return try {
            db.collection("usuarios").document(usuario.id).set(usuario).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
