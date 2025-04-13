package com.example.medtrackerapp.domain.repository

import com.example.medtrackerapp.domain.model.ExamParameter
import com.example.medtrackerapp.domain.model.MedicalExam
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.Date

/**
 * Repositorio para manejar exámenes médicos
 */
interface MedicalExamRepository {
    
    /**
     * Obtiene todos los exámenes ordenados por fecha
     */
    fun getExams(): Flow<List<MedicalExam>>
    
    /**
     * Obtiene un examen por su id
     */
    suspend fun getExamById(examId: String): MedicalExam?
    
    /**
     * Guarda un examen
     */
    suspend fun saveExam(exam: MedicalExam)
    
    /**
     * Elimina un examen
     */
    suspend fun deleteExam(examId: String)
    
    /**
     * Obtiene el historial de un parámetro específico
     */
    suspend fun getParameterHistory(parameterName: String): List<ParameterHistory>
    
    /**
     * Procesa un archivo PDF y extrae los datos
     */
    suspend fun processPdf(
        pdfFile: File,
        date: Date,
        laboratoryName: String,
        doctorName: String? = null
    ): Result<MedicalExam>
}

/**
 * Representación histórica de un parámetro
 */
data class ParameterHistory(
    val examId: String,
    val examDate: Date,
    val parameter: ExamParameter
) 