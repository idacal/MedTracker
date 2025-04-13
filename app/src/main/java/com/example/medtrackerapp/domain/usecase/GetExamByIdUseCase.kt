package com.example.medtrackerapp.domain.usecase

import com.example.medtrackerapp.domain.model.MedicalExam
import com.example.medtrackerapp.domain.repository.MedicalExamRepository
import javax.inject.Inject

/**
 * Caso de uso para obtener un examen médico por su ID
 */
class GetExamByIdUseCase @Inject constructor(
    private val repository: MedicalExamRepository
) {
    /**
     * Obtiene un examen médico por su ID
     * @param examId ID del examen a obtener
     * @return El examen si existe, null en caso contrario
     */
    suspend operator fun invoke(examId: String): MedicalExam? {
        return repository.getExamById(examId)
    }
} 