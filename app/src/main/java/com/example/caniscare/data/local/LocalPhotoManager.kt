package com.example.caniscare.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object LocalPhotoManager {

    private const val DOG_PHOTOS_DIR = "dog_photos"
    private const val PRODUCT_PHOTOS_DIR = "product_photos"

    /**
     * Guarda una imagen localmente desde una URI (galería o cámara) en el almacenamiento interno privado.
     * Retorna la ruta absoluta del archivo guardado.
     * Esto evita 100% el uso de Firebase Storage, garantizando costo cero y privacidad.
     */
    suspend fun saveImageLocally(
        context: Context,
        imageUri: Uri,
        subDirName: String = DOG_PHOTOS_DIR
    ): String? = withContext(Dispatchers.IO) {
        try {
            val directory = File(context.filesDir, subDirName).apply {
                if (!exists()) mkdirs()
            }

            val fileName = "img_${UUID.randomUUID()}.jpg"
            val destinationFile = File(directory, fileName)

            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                // Decodificamos y comprimimos para ahorrar espacio
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    FileOutputStream(destinationFile).use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    }
                    return@withContext destinationFile.absolutePath
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Verifica si el archivo existe en el almacenamiento local.
     */
    fun fileExists(filePath: String?): Boolean {
        if (filePath.isNullOrBlank()) return false
        return File(filePath).exists()
    }
}
