package com.example.medtrackerapp.domain.usecase

import com.example.medtrackerapp.domain.model.MedicalExam
import com.example.medtrackerapp.domain.repository.MedicalExamRepository
import java.io.File
import java.util.Date
import javax.inject.Inject

/**
 * Caso de uso para procesar archivos PDF y extraer datos médicos
 */
class ProcessPdfUseCase @Inject constructor(
    private val repository: MedicalExamRepository
) {
    /**
     * Procesa un PDF para extraer datos médicos
     */
    suspend operator fun invoke(
        pdfFile: File,
        date: Date,
        laboratoryName: String,
        doctorName: String? = null
    ): Result<MedicalExam> {
        return repository.processPdf(pdfFile, date, laboratoryName, doctorName)
    }
} 