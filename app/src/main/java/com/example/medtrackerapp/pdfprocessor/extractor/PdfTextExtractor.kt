package com.example.medtrackerapp.pdfprocessor.extractor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

/**
 * Clase para extraer texto de archivos PDF
 */
class PdfTextExtractor @Inject constructor(
    private val context: Context
) {
    init {
        // Inicializar PDFBox
        PDFBoxResourceLoader.init(context)
    }

    /**
     * Extrae texto de un PDF con texto seleccionable
     */
    suspend fun extractText(pdfFile: File): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Intentar extraer con PDFBox
            val text = extractWithPdfBox(pdfFile)
            if (text.isNotBlank()) {
                Result.success(text)
            } else {
                // Si PDFBox no obtiene texto, puede que sea un PDF escaneado
                Result.failure(IOException("No se pudo extraer texto, posiblemente es un PDF escaneado"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al extraer texto: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Extrae texto usando PDFBox
     */
    private fun extractWithPdfBox(pdfFile: File): String {
        return try {
            val document = PDDocument.load(pdfFile)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()
            text
        } catch (e: Exception) {
            Log.e(TAG, "Error con PDFBox: ${e.message}", e)
            ""
        }
    }

    /**
     * Convierte un PDF a imágenes para OCR (para PDFs escaneados)
     */
    suspend fun pdfToImages(pdfFile: File): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val fileDescriptor = ParcelFileDescriptor.open(
                pdfFile,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            val renderer = PdfRenderer(fileDescriptor)
            val imageFiles = mutableListOf<File>()

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(
                    page.width * 2,
                    page.height * 2,
                    Bitmap.Config.ARGB_8888
                )
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                // Guardar el bitmap como archivo temporal
                val imageFile = File(context.cacheDir, "page_${i}.jpg")
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }

                imageFiles.add(imageFile)
                page.close()
            }

            renderer.close()
            fileDescriptor.close()

            Result.success(imageFiles)
        } catch (e: Exception) {
            Log.e(TAG, "Error al convertir PDF a imágenes: ${e.message}", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "PdfTextExtractor"
    }
} 