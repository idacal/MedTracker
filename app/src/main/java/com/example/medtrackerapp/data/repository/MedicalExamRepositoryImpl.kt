package com.example.medtrackerapp.data.repository

import android.util.Log
import com.example.medtrackerapp.data.local.database.CategoryDao
import com.example.medtrackerapp.data.local.database.CategoryEntity
import com.example.medtrackerapp.data.local.database.ExamDao
import com.example.medtrackerapp.data.local.database.ExamEntity
import com.example.medtrackerapp.data.local.database.ParameterDao
import com.example.medtrackerapp.data.local.database.ParameterEntity
import com.example.medtrackerapp.domain.model.ExamCategory
import com.example.medtrackerapp.domain.model.ExamParameter
import com.example.medtrackerapp.domain.model.MedicalExam
import com.example.medtrackerapp.domain.model.ParameterStatus
import com.example.medtrackerapp.domain.repository.MedicalExamRepository
import com.example.medtrackerapp.domain.repository.ParameterHistory
import com.example.medtrackerapp.pdfprocessor.PdfProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del repositorio de exámenes médicos
 */
@Singleton
class MedicalExamRepositoryImpl @Inject constructor(
    private val examDao: ExamDao,
    private val categoryDao: CategoryDao,
    private val parameterDao: ParameterDao,
    private val pdfProcessor: PdfProcessor
) : MedicalExamRepository {

    override fun getExams(): Flow<List<MedicalExam>> {
        return examDao.getExams().map { examEntities ->
            examEntities.map { examEntity ->
                mapExamEntityToDomain(examEntity)
            }
        }
    }

    override suspend fun getExamById(examId: String): MedicalExam? = withContext(Dispatchers.IO) {
        val examEntity = examDao.getExamById(examId) ?: return@withContext null
        return@withContext mapExamEntityToDomain(examEntity)
    }

    override suspend fun saveExam(exam: MedicalExam) = withContext(Dispatchers.IO) {
        try {
            // Guardar examen
            val examEntity = ExamEntity(
                id = exam.id,
                date = exam.date,
                laboratoryName = exam.laboratoryName,
                doctorName = exam.doctorName,
                pdfFilePath = exam.pdfFilePath
            )
            examDao.insertExam(examEntity)

            // Eliminar categorías y parámetros anteriores si existen
            categoryDao.deleteByExamId(exam.id)
            parameterDao.deleteByExamId(exam.id)

            // Guardar categorías y parámetros
            exam.categories.forEach { category ->
                val categoryEntity = CategoryEntity(
                    examId = exam.id,
                    name = category.name
                )
                val categoryId = categoryDao.insertCategory(categoryEntity)

                // Guardar parámetros de esta categoría
                category.parameters.forEach { parameter ->
                    val parameterEntity = ParameterEntity(
                        examId = exam.id,
                        categoryId = categoryId,
                        name = parameter.name,
                        value = parameter.value,
                        unit = parameter.unit,
                        referenceRange = parameter.referenceRange,
                        statusValue = mapStatusToInt(parameter.status)
                    )
                    parameterDao.insertParameter(parameterEntity)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando examen: ${e.message}", e)
            throw e
        }
    }

    override suspend fun deleteExam(examId: String) = withContext(Dispatchers.IO) {
        try {
            // Eliminar también registros relacionados
            parameterDao.deleteByExamId(examId)
            categoryDao.deleteByExamId(examId)
            examDao.deleteExam(examId)

            // También podríamos eliminar el archivo PDF si es necesario
            val exam = examDao.getExamById(examId)
            if (exam != null) {
                val pdfFile = File(exam.pdfFilePath)
                if (pdfFile.exists()) {
                    pdfFile.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando examen: ${e.message}", e)
            throw e
        }
    }

    override suspend fun getParameterHistory(parameterName: String): List<ParameterHistory> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Buscar parámetros con nombre similar
            val paramEntities = parameterDao.getParameterHistory("%$parameterName%")
            
            paramEntities.mapNotNull { paramEntity ->
                val exam = examDao.getExamById(paramEntity.examId) ?: return@mapNotNull null
                
                ParameterHistory(
                    examId = paramEntity.examId,
                    examDate = exam.date,
                    parameter = ExamParameter(
                        name = paramEntity.name,
                        value = paramEntity.value,
                        unit = paramEntity.unit,
                        referenceRange = paramEntity.referenceRange,
                        status = mapIntToStatus(paramEntity.statusValue)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo historial: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun processPdf(
        pdfFile: File,
        date: Date,
        laboratoryName: String,
        doctorName: String?
    ): Result<MedicalExam> {
        return pdfProcessor.processPdf(pdfFile, date, laboratoryName, doctorName)
    }

    /**
     * Convierte una entidad de examen a un objeto de dominio
     */
    private suspend fun mapExamEntityToDomain(examEntity: ExamEntity): MedicalExam {
        val categories = mutableListOf<ExamCategory>()
        
        // Obtener categorías para este examen
        val categoryEntities = categoryDao.getCategoriesByExamId(examEntity.id)
        
        for (categoryEntity in categoryEntities) {
            // Obtener parámetros para esta categoría
            val parameterEntities = parameterDao.getParametersByCategoryId(categoryEntity.id)
            
            val parameters = parameterEntities.map { paramEntity ->
                ExamParameter(
                    name = paramEntity.name,
                    value = paramEntity.value,
                    unit = paramEntity.unit,
                    referenceRange = paramEntity.referenceRange,
                    status = mapIntToStatus(paramEntity.statusValue)
                )
            }
            
            categories.add(
                ExamCategory(
                    name = categoryEntity.name,
                    parameters = parameters
                )
            )
        }
        
        return MedicalExam(
            id = examEntity.id,
            date = examEntity.date,
            laboratoryName = examEntity.laboratoryName,
            doctorName = examEntity.doctorName,
            pdfFilePath = examEntity.pdfFilePath,
            categories = categories
        )
    }

    /**
     * Mapea un estado de parámetro a su representación entera
     */
    private fun mapStatusToInt(status: ParameterStatus): Int {
        return when (status) {
            ParameterStatus.NORMAL -> 0
            ParameterStatus.WATCH -> 1
            ParameterStatus.ATTENTION -> 2
            ParameterStatus.UNDEFINED -> 3
        }
    }

    /**
     * Mapea un entero al estado correspondiente
     */
    private fun mapIntToStatus(statusValue: Int): ParameterStatus {
        return when (statusValue) {
            0 -> ParameterStatus.NORMAL
            1 -> ParameterStatus.WATCH
            2 -> ParameterStatus.ATTENTION
            else -> ParameterStatus.UNDEFINED
        }
    }

    companion object {
        private const val TAG = "MedicalExamRepositoryImpl"
    }
} 