package com.example.medtrackerapp.pdfprocessor.ocr

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Clase para procesar OCR en imágenes extraídas de PDFs
 */
class OcrProcessor @Inject constructor(
    private val context: Context
) {
    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Realiza OCR en una imagen y devuelve el texto extraído
     */
    suspend fun performOcr(imageFile: File): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val image = InputImage.fromFilePath(context, Uri.fromFile(imageFile))
            val result = recognizer.process(image).await()
            Result.success(result.text)
        } catch (e: Exception) {
            Log.e(TAG, "Error en OCR: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Procesa múltiples imágenes (páginas de PDF) con OCR
     */
    suspend fun processImages(imageFiles: List<File>): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val textBuilder = StringBuilder()

            for (imageFile in imageFiles) {
                try {
                    val pageTextResult = performOcr(imageFile)
                    if (pageTextResult.isSuccess) {
                        textBuilder.append(pageTextResult.getOrNull()).append("\n\n")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando página: ${e.message}", e)
                } finally {
                    // Limpiar archivo temporal después de procesarlo
                    imageFile.delete()
                }
            }

            if (textBuilder.isNotEmpty()) {
                Result.success(textBuilder.toString())
            } else {
                Result.failure(Exception("No se pudo extraer texto de ninguna página"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando imágenes: ${e.message}", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "OcrProcessor"
    }
} 