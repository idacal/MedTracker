package com.example.medtrackerapp.domain.usecase

import com.example.medtrackerapp.domain.repository.MedicalExamRepository
import com.example.medtrackerapp.domain.repository.ParameterHistory
import javax.inject.Inject

/**
 * Caso de uso para obtener el historial de un parámetro específico
 */
class GetParameterHistoryUseCase @Inject constructor(
    private val repository: MedicalExamRepository
) {
    /**
     * Obtiene el historial de un parámetro específico a lo largo del tiempo
     */
    suspend operator fun invoke(parameterName: String): List<ParameterHistory> {
        return repository.getParameterHistory(parameterName)
    }
} 