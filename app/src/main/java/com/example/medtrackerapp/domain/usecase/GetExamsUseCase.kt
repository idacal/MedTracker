package com.example.medtrackerapp.domain.usecase

import com.example.medtrackerapp.domain.model.MedicalExam
import com.example.medtrackerapp.domain.repository.MedicalExamRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Caso de uso para obtener la lista de exámenes
 */
class GetExamsUseCase @Inject constructor(
    private val repository: MedicalExamRepository
) {
    /**
     * Obtiene todos los exámenes ordenados por fecha
     */
    operator fun invoke(): Flow<List<MedicalExam>> {
        return repository.getExams()
    }
} 