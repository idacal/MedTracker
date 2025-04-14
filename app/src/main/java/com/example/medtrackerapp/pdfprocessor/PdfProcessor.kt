package com.example.medtrackerapp.pdfprocessor

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.medtrackerapp.domain.model.MedicalExam
import java.io.File
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Procesador principal de archivos PDF
 */
class PdfProcessor @Inject constructor(
    private val context: Context,
    private val medicalExamPdfProcessor: MedicalExamPdfProcessor
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
            // Usar el procesador mejorado para extraer datos del PDF
            val report = medicalExamPdfProcessor.processPdfFromFile(pdfFile)
            
            // Verificar si se encontraron parámetros
            if (report.categories.isEmpty()) {
                return Result.failure(Exception("No se pudieron identificar parámetros médicos"))
            }
            
            // Convertir el reporte a un objeto MedicalExam del dominio
            val exam = medicalExamPdfProcessor.convertToMedicalExam(
                report = report,
                pdfPath = pdfFile.absolutePath,
                laboratoryName = laboratoryName
            )
            
            // Establecer la fecha correcta y el nombre del doctor si se proporcionó
            val finalExam = exam.copy(
                date = date,
                doctorName = doctorName ?: exam.doctorName
            )
            
            Log.d(TAG, "PDF procesado exitosamente: ${finalExam.categories.size} categorías con parámetros")
            Result.success(finalExam)
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando PDF: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Procesa un PDF desde una URI
     */
    suspend fun processPdfFromUri(uri: Uri): Result<MedicalExam> = withContext(Dispatchers.IO) {
        try {
            // Usar el MedicalExamPdfProcessor ya inyectado
            val medicalExam = medicalExamPdfProcessor.processPdfFromUri(uri)
            
            Result.success(medicalExam)
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar PDF: ${e.message}", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "PdfProcessor"
    }
}