package com.example.medtrackerapp.pdfprocessor.parser

import android.util.Log
import com.example.medtrackerapp.domain.model.ExamCategory
import com.example.medtrackerapp.domain.model.ExamParameter
import com.example.medtrackerapp.domain.model.ParameterStatus
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Clase para parsear resultados de laboratorio desde texto extraído
 */
class LabResultParser @Inject constructor() {

    /**
     * Extrae categorías y parámetros de un texto de examen de laboratorio
     */
    fun parseLabResult(text: String): List<ExamCategory> {
        // Detectar categorías en el texto
        val categories = detectCategories(text)
        val result = mutableListOf<ExamCategory>()
        
        for (category in categories) {
            // Extraer la sección de texto correspondiente a esta categoría
            val categoryText = extractCategoryText(text, category, categories)
            
            // Extraer parámetros de esta categoría
            val parameters = extractParameters(categoryText)
            
            if (parameters.isNotEmpty()) {
                result.add(ExamCategory(category, parameters))
            }
        }

        // Si no se detectaron categorías, tratar todo el texto como una sola categoría
        if (result.isEmpty()) {
            val parameters = extractParameters(text)
            if (parameters.isNotEmpty()) {
                result.add(ExamCategory("General", parameters))
            }
        }
        
        return result
    }
    
    /**
     * Detecta posibles categorías en el texto
     */
    private fun detectCategories(text: String): List<String> {
        val commonCategories = listOf(
            "HEMATOLOGIA", "BIOQUIMICA", "ORINAS", "HORMONAS", 
            "INMUNOLOGIA", "LIPIDOS", "COAGULACION", "HEMOGRAMA",
            "QUIMICA", "QUIMICA SANGUINEA", "ORINA"
        )
        
        return commonCategories.filter { category ->
            text.contains(category, ignoreCase = true)
        }
    }
    
    /**
     * Extrae la sección de texto correspondiente a una categoría
     */
    private fun extractCategoryText(fullText: String, category: String, allCategories: List<String>): String {
        val categoryIndex = fullText.indexOf(category, ignoreCase = true)
        if (categoryIndex == -1) return ""
        
        // Encontrar la siguiente categoría o el final del texto
        var endIndex = fullText.length
        for (nextCategory in allCategories) {
            if (nextCategory == category) continue
            
            val nextCategoryIndex = fullText.indexOf(nextCategory, categoryIndex + category.length, ignoreCase = true)
            if (nextCategoryIndex != -1 && nextCategoryIndex < endIndex) {
                endIndex = nextCategoryIndex
            }
        }
        
        return fullText.substring(categoryIndex, endIndex)
    }
    
    /**
     * Extrae parámetros de un texto de examen
     */
    private fun extractParameters(text: String): List<ExamParameter> {
        val parameters = mutableListOf<ExamParameter>()
        
        try {
            // Probar con patrón de línea
            val lineMatcher = LINE_PATTERN.matcher(text)
            while (lineMatcher.find()) {
                val paramName = lineMatcher.group(1)?.trim() ?: continue
                val value = lineMatcher.group(2)?.trim() ?: continue
                val unit = lineMatcher.group(3)?.trim() ?: ""
                val reference = lineMatcher.group(4)?.trim() ?: ""
                
                val status = determineStatus(value, reference)
                
                parameters.add(
                    ExamParameter(
                        name = paramName,
                        value = value,
                        unit = unit,
                        referenceRange = reference,
                        status = status
                    )
                )
            }
            
            // Si no se encontraron con el patrón de línea, probar con patrón de tabla
            if (parameters.isEmpty()) {
                val tableMatcher = TABLE_PATTERN.matcher(text)
                while (tableMatcher.find()) {
                    val paramName = tableMatcher.group(1)?.trim() ?: continue
                    val value = tableMatcher.group(2)?.trim() ?: continue
                    val unit = tableMatcher.group(3)?.trim() ?: ""
                    val reference = tableMatcher.group(4)?.trim() ?: ""
                    
                    val status = determineStatus(value, reference)
                    
                    parameters.add(
                        ExamParameter(
                            name = paramName,
                            value = value,
                            unit = unit,
                            referenceRange = reference,
                            status = status
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extrayendo parámetros: ${e.message}", e)
        }
        
        return parameters
    }
    
    /**
     * Determina el estado de un parámetro comparando con su rango de referencia
     */
    private fun determineStatus(value: String, reference: String): ParameterStatus {
        if (reference.isEmpty()) return ParameterStatus.UNDEFINED
        
        try {
            val numericValue = value.replace(",", ".").toDoubleOrNull() ?: return ParameterStatus.UNDEFINED
            
            // Manejar diferentes formatos de rango de referencia
            when {
                reference.contains("-") -> {
                    // Formato: "10 - 20"
                    val parts = reference.split("-").map { it.trim().replace(",", ".") }
                    if (parts.size == 2) {
                        val min = parts[0].toDoubleOrNull()
                        val max = parts[1].toDoubleOrNull()
                        
                        if (min != null && max != null) {
                            return when {
                                numericValue < min -> {
                                    if (numericValue < min * 0.8) ParameterStatus.ATTENTION else ParameterStatus.WATCH
                                }
                                numericValue > max -> {
                                    if (numericValue > max * 1.2) ParameterStatus.ATTENTION else ParameterStatus.WATCH
                                }
                                else -> ParameterStatus.NORMAL
                            }
                        }
                    }
                }
                reference.contains("<") -> {
                    // Formato: "< 10"
                    val max = reference.replace("<", "").trim().replace(",", ".").toDoubleOrNull()
                    if (max != null) {
                        return when {
                            numericValue < max -> ParameterStatus.NORMAL
                            numericValue > max * 1.2 -> ParameterStatus.ATTENTION
                            else -> ParameterStatus.WATCH
                        }
                    }
                }
                reference.contains(">") -> {
                    // Formato: "> 10"
                    val min = reference.replace(">", "").trim().replace(",", ".").toDoubleOrNull()
                    if (min != null) {
                        return when {
                            numericValue > min -> ParameterStatus.NORMAL
                            numericValue < min * 0.8 -> ParameterStatus.ATTENTION
                            else -> ParameterStatus.WATCH
                        }
                    }
                }
            }
            
            return ParameterStatus.UNDEFINED
        } catch (e: Exception) {
            Log.e(TAG, "Error determinando estado: ${e.message}", e)
            return ParameterStatus.UNDEFINED
        }
    }
    
    companion object {
        private const val TAG = "LabResultParser"
        
        // Patrones para diferentes formatos de laboratorio
        private val LINE_PATTERN = Pattern.compile(
            "([A-Za-zÀ-ÿ\\s\\(\\)]+)\\s*:\\s*([0-9,.]+)\\s*([a-zA-Z%\\/]+)?\\s*(?:\\(?([0-9,.\\s\\-<>]+)\\)?)?",
            Pattern.MULTILINE
        )
        
        private val TABLE_PATTERN = Pattern.compile(
            "([A-Za-zÀ-ÿ\\s\\(\\)]+)\\s+([0-9,.]+)\\s+([a-zA-Z%\\/]+)?\\s+([0-9,.\\s\\-<>]+)",
            Pattern.MULTILINE
        )
    }
} 