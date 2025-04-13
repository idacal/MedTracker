package com.example.medtrackerapp.domain.model

import java.util.Date

/**
 * Representa un examen médico completo
 */
data class MedicalExam(
    val id: String,
    val date: Date,
    val laboratoryName: String,
    val doctorName: String? = null,
    val pdfFilePath: String,
    val categories: List<ExamCategory> = emptyList()
)

/**
 * Representa una categoría de examen (Hematología, Bioquímica, etc.)
 */
data class ExamCategory(
    val name: String,
    val parameters: List<ExamParameter> = emptyList()
) {
    /**
     * Calcula el estado general de la categoría basado en los parámetros
     */
    fun getOverallStatus(): ParameterStatus {
        if (parameters.isEmpty()) return ParameterStatus.UNDEFINED
        
        return when {
            parameters.any { it.status == ParameterStatus.ATTENTION } -> ParameterStatus.ATTENTION
            parameters.any { it.status == ParameterStatus.WATCH } -> ParameterStatus.WATCH
            parameters.all { it.status == ParameterStatus.NORMAL } -> ParameterStatus.NORMAL
            else -> ParameterStatus.UNDEFINED
        }
    }
    
    /**
     * Retorna la cantidad de parámetros en cada estado
     */
    fun getStatusCount(): Map<ParameterStatus, Int> {
        return parameters.groupBy { it.status }
            .mapValues { it.value.size }
    }
}

/**
 * Representa un parámetro individual de examen (Glucosa, Colesterol, etc.)
 */
data class ExamParameter(
    val name: String,
    val value: String,
    val unit: String = "",
    val referenceRange: String = "",
    val status: ParameterStatus = ParameterStatus.NORMAL
) {
    /**
     * Intenta obtener el valor numérico del parámetro
     */
    fun getNumericValue(): Double? {
        return value.replace(",", ".").toDoubleOrNull()
    }
}

/**
 * Estados posibles de un parámetro
 */
enum class ParameterStatus {
    NORMAL,     // Dentro del rango normal
    WATCH,      // Fuera de rango pero no crítico
    ATTENTION,  // Requiere atención médica
    UNDEFINED   // No se puede determinar
} 