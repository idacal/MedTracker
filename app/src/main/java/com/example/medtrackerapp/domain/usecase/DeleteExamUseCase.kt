package com.example.medtrackerapp.domain.usecase

import com.example.medtrackerapp.domain.repository.MedicalExamRepository
import javax.inject.Inject

/**
 * Caso de uso para eliminar un examen médico
 */
class DeleteExamUseCase @Inject constructor(
    private val repository: MedicalExamRepository
) {
    /**
     * Elimina un examen médico por su ID
     */
    suspend operator fun invoke(examId: String) {
        repository.deleteExam(examId)
    }
} 