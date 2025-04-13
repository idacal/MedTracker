package com.example.medtrackerapp.pdfprocessor

import android.util.Log
import com.example.medtrackerapp.domain.model.ExamCategory
import com.example.medtrackerapp.domain.model.MedicalExam
import com.example.medtrackerapp.pdfprocessor.extractor.PdfTextExtractor
import com.example.medtrackerapp.pdfprocessor.ocr.OcrProcessor
import com.example.medtrackerapp.pdfprocessor.parser.LabResultParser
import java.io.File
import java.util.Date
import java.util.UUID
import javax.inject.Inject

/**
 * Procesador principal de archivos PDF
 */
class PdfProcessor @Inject constructor(
    private val textExtractor: PdfTextExtractor,
    private val ocrProcessor: OcrProcessor,
    private val labResultParser: LabResultParser
) {
    /**
     * Procesa un archivo PDF y extrae los datos médicos
     */
    suspend fun processPdf(
        pdfFile: File,
        date: Date,
        laboratoryName: String,
        doctorName: String? = null
    ): Result<MedicalExam> {
        return try {
            // Paso 1: Intentar extraer texto directamente
            val textResult = textExtractor.extractText(pdfFile)
            
            // Texto extraído del PDF (ya sea directamente o por OCR)
            val extractedText = if (textResult.isSuccess) {
                textResult.getOrNull() ?: ""
            } else {
                // Paso 2: Si no se pudo extraer texto directamente, intentar con OCR
                val imagesResult = textExtractor.pdfToImages(pdfFile)
                if (imagesResult.isSuccess) {
                    val images = imagesResult.getOrNull() ?: emptyList()
                    val ocrResult = ocrProcessor.processImages(images)
                    ocrResult.getOrNull() ?: ""
                } else {
                    return Result.failure(Exception("No se pudo procesar el PDF"))
                }
            }

            if (extractedText.isBlank()) {
                return Result.failure(Exception("No se pudo extraer texto del PDF"))
            }

            // Paso 3: Parsear el texto extraído
            val categories = labResultParser.parseLabResult(extractedText)

            if (categories.isEmpty()) {
                return Result.failure(Exception("No se pudieron identificar parámetros médicos"))
            }

            // Paso 4: Crear el objeto MedicalExam
            val exam = MedicalExam(
                id = UUID.randomUUID().toString(),
                date = date,
                laboratoryName = laboratoryName,
                doctorName = doctorName,
                pdfFilePath = pdfFile.absolutePath,
                categories = categories
            )

            Result.success(exam)
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando PDF: ${e.message}", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "PdfProcessor"
    }
} 