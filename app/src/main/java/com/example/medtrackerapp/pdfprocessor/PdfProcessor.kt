package com.example.medtrackerapp.pdfprocessor

import android.util.Log
import com.example.medtrackerapp.domain.model.MedicalExam
import java.io.File
import java.util.Date
import javax.inject.Inject

/**
 * Procesador principal de archivos PDF
 */
class PdfProcessor @Inject constructor(
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
            // Usar el nuevo procesador para extraer datos del PDF
            val reportResult = medicalExamPdfProcessor.processPdfFromFile(pdfFile)
            
            if (reportResult.isFailure) {
                return Result.failure(reportResult.exceptionOrNull() ?: Exception("Error al procesar el PDF"))
            }
            
            val report = reportResult.getOrNull()
            if (report == null || report.categories.isEmpty()) {
                return Result.failure(Exception("No se pudieron identificar parámetros médicos"))
            }
            
            // Convertir el reporte a un objeto MedicalExam del dominio
            val exam = medicalExamPdfProcessor.convertToMedicalExam(
                report = report,
                pdfPath = pdfFile.absolutePath,
                laboratoryName = laboratoryName
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