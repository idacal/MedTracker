package com.example.medtrackerapp.domain.usecase

import com.example.medtrackerapp.domain.model.MedicalExam
import com.example.medtrackerapp.domain.repository.MedicalExamRepository
import javax.inject.Inject

/**
 * Caso de uso para guardar un examen médico
 */
class SaveExamUseCase @Inject constructor(
    private val repository: MedicalExamRepository
) {
    /**
     * Guarda un examen médico en la base de datos
     */
    suspend operator fun invoke(exam: MedicalExam) {
        repository.saveExam(exam)
    }
} 