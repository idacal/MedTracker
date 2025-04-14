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
        
        if (categories.isEmpty()) {
            // Si no se detectaron categorías, tratar todo el texto como una sola categoría
            val parameters = extractParameters(text)
            if (parameters.isNotEmpty()) {
                result.add(ExamCategory("General", parameters))
            }
            return result
        }
        
        // Ordenar categorías por su posición en el texto
        val categoryPositions = categories.map { 
            it to text.indexOf(it, ignoreCase = true) 
        }.filter { it.second >= 0 }.sortedBy { it.second }
        
        for (i in categoryPositions.indices) {
            val (category, startPos) = categoryPositions[i]
            val endPos = if (i < categoryPositions.size - 1) {
                categoryPositions[i + 1].second
            } else {
                text.length
            }
            
            // Extraer la sección de texto correspondiente a esta categoría
            val categoryText = text.substring(startPos, endPos)
            
            // Extraer parámetros de esta categoría
            val parameters = extractParameters(categoryText)
            
            if (parameters.isNotEmpty()) {
                result.add(ExamCategory(category, parameters))
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
        
        val foundCategories = mutableListOf<String>()
        
        // Primero buscar categorías conocidas
        for (category in commonCategories) {
            if (text.contains(category, ignoreCase = true)) {
                foundCategories.add(category)
            }
        }
        
        // Si no se encontraron categorías conocidas, intentar buscar títulos en mayúsculas
        if (foundCategories.isEmpty()) {
            val titlePattern = Pattern.compile("^\\s*([A-Z][A-Z\\s]{3,})\\s*$", Pattern.MULTILINE)
            val matcher = titlePattern.matcher(text)
            
            while (matcher.find()) {
                val possibleCategory = matcher.group(1).trim()
                // Filtrar posibles falsos positivos
                if (possibleCategory.length >= 4 && !isFalsePositive(possibleCategory)) {
                    foundCategories.add(possibleCategory)
                }
            }
        }
        
        return foundCategories
    }
    
    /**
     * Verifica si un título es un falso positivo (no es una categoría de examen)
     */
    private fun isFalsePositive(text: String): Boolean {
        val falsePositives = listOf(
            "NOMBRE", "PACIENTE", "FECHA", "EDAD", "SEXO", "ORIGEN", "DOCTOR", 
            "INFORME", "OBSERVACION", "METODO", "CASA MATRIZ"
        )
        
        return falsePositives.any { text.contains(it, ignoreCase = true) }
    }
    
    /**
     * Extrae parámetros de un texto de examen
     */
    private fun extractParameters(text: String): List<ExamParameter> {
        val parameters = mutableListOf<ExamParameter>()
        
        try {
            // Filtrar líneas irrelevantes para la extracción
            val filteredText = text.lines()
                .filter { line -> 
                    !line.contains("Observación:", ignoreCase = true) && 
                    !line.contains("Casa Matriz:", ignoreCase = true) && 
                    !line.contains("Email:", ignoreCase = true) && 
                    !line.contains("Call Center:", ignoreCase = true) &&
                    !line.contains("adulteración", ignoreCase = true) &&
                    line.trim().isNotEmpty()
                }
                .joinToString("\n")
            
            // Patrón para parámetros con formato: Nombre : Valor Unidad RangoRef
            val linePatterns = listOf(
                // Patrón 1: Nombre : Valor (Unidad) : Rango
                Pattern.compile(
                    "([\\w\\s\\.\\-\\(\\)]+)\\s*:\\s*([\\d,\\.]+)\\s*([\\w%\\/\\^\\d]*)\\s*([\\d,\\.\\s\\-<>]+|Deseable.*?|Negativo|Positivo|Reactivo|No Reactivo)",
                    Pattern.MULTILINE
                ),
                // Patrón 2: Nombre (Unidad) : Valor : Rango
                Pattern.compile(
                    "([\\w\\s\\.\\-\\(\\)]+)\\s*\\(([\\w%\\/\\^\\d]+)\\)\\s*:\\s*([\\d,\\.]+)\\s*([\\d,\\.\\s\\-<>]+|Deseable.*?|Negativo|Positivo|Reactivo|No Reactivo)",
                    Pattern.MULTILINE
                ),
                // Patrón 3: Formato de tabla sin dos puntos
                Pattern.compile(
                    "^\\s*([\\w\\s\\.\\-\\(\\)]+)\\s+([\\d,\\.]+)\\s+([\\w%\\/\\^\\d]+)\\s+([\\d,\\.\\s\\-<>]+|Deseable.*?|Negativo|Positivo|Reactivo|No Reactivo)",
                    Pattern.MULTILINE
                )
            )
            
            // Probar cada patrón
            for (pattern in linePatterns) {
                val matcher = pattern.matcher(filteredText)
                while (matcher.find()) {
                    try {
                        // Extraer los grupos según el patrón
                        if (pattern == linePatterns[0]) {
                            // Patrón 1: Nombre : Valor Unidad RangoRef
                            val name = matcher.group(1)?.trim() ?: continue
                            if (shouldSkipName(name)) continue
                            
                            val value = matcher.group(2)?.trim() ?: continue
                            val unit = matcher.group(3)?.trim() ?: ""
                            val reference = matcher.group(4)?.trim() ?: ""
                            
                            val status = determineStatus(value, reference)
                            parameters.add(
                                ExamParameter(
                                    name = name,
                                    value = value,
                                    unit = unit,
                                    referenceRange = reference,
                                    status = status
                                )
                            )
                        } else if (pattern == linePatterns[1]) {
                            // Patrón 2: Nombre (Unidad) : Valor : Rango
                            val name = matcher.group(1)?.trim() ?: continue
                            if (shouldSkipName(name)) continue
                            
                            val unit = matcher.group(2)?.trim() ?: ""
                            val value = matcher.group(3)?.trim() ?: continue
                            val reference = matcher.group(4)?.trim() ?: ""
                            
                            val status = determineStatus(value, reference)
                            parameters.add(
                                ExamParameter(
                                    name = name,
                                    value = value,
                                    unit = unit,
                                    referenceRange = reference,
                                    status = status
                                )
                            )
                        } else if (pattern == linePatterns[2]) {
                            // Patrón 3: Formato de tabla
                            val name = matcher.group(1)?.trim() ?: continue
                            if (shouldSkipName(name)) continue
                            
                            val value = matcher.group(2)?.trim() ?: continue
                            val unit = matcher.group(3)?.trim() ?: ""
                            val reference = matcher.group(4)?.trim() ?: ""
                            
                            val status = determineStatus(value, reference)
                            parameters.add(
                                ExamParameter(
                                    name = name,
                                    value = value,
                                    unit = unit,
                                    referenceRange = reference,
                                    status = status
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error procesando parámetro: ${e.message}")
                        continue
                    }
                }
            }
            
            // Si no se encontraron parámetros con los patrones anteriores, intentar con un patrón más específico
            if (parameters.isEmpty()) {
                extractSpecialFormatParameters(filteredText, parameters)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extrayendo parámetros: ${e.message}", e)
        }
        
        return parameters
    }
    
    /**
     * Extrae parámetros con formatos especiales
     */
    private fun extractSpecialFormatParameters(text: String, parameters: MutableList<ExamParameter>) {
        try {
            // Buscar patrones específicos como los que encontramos en los informes de laboratorio
            // Por ejemplo, "Leucocitos", "Hemoglobina", etc.
            val specialPatterns = mapOf(
                "Leucocitos" to "Leucocitos\\s*\\([^)]+\\)\\s*:\\s*([\\d,\\.]+)\\s*([\\d,\\.\\s\\-]+)",
                "Eritrocitos" to "Eritrocitos\\s*\\([^)]+\\)\\s*:\\s*([\\d,\\.]+)\\s*([\\d,\\.\\s\\-]+)",
                "Hemoglobina" to "Hemoglobina\\s*\\([^)]+\\)\\s*:\\s*([\\d,\\.]+)\\s*([\\d,\\.\\s\\-]+)",
                "Hematocrito" to "Hematocrito\\s*\\([^)]+\\)\\s*:\\s*([\\d,\\.]+)\\s*([\\d,\\.\\s\\-]+)",
                "Plaquetas" to "Plaquetas\\s*\\([^)]+\\)\\s*:\\s*([\\d,\\.]+)\\s*([\\d,\\.\\s\\-]+)",
                "Glucosa" to "Glucosa\\s*\\([^)]+\\)\\s*:\\s*([\\d,\\.]+)\\s*([\\d,\\.\\s\\-]+)",
                "Colesterol" to "Colesterol\\s*\\([^)]+\\)\\s*:\\s*([\\d,\\.]+)\\s*([\\d,\\.\\s\\-]+|Deseable.*?)",
                "Triglicéridos" to "Triglicéridos\\s*\\([^)]+\\)\\s*:\\s*([\\d,\\.]+)\\s*([\\d,\\.\\s\\-]+|Deseable.*?)"
            )
            
            specialPatterns.forEach { (paramName, patternStr) ->
                val pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE)
                val matcher = pattern.matcher(text)
                
                if (matcher.find()) {
                    val value = matcher.group(1)?.trim() ?: return@forEach
                    val referenceRange = matcher.group(2)?.trim() ?: ""
                    
                    // Determinar la unidad según el parámetro
                    val unit = when {
                        paramName.contains("Leucocitos", ignoreCase = true) -> "x10^9/Lt"
                        paramName.contains("Eritrocitos", ignoreCase = true) -> "x10^12/Lt"
                        paramName.contains("Hemoglobina", ignoreCase = true) -> "gr/dl"
                        paramName.contains("Hematocrito", ignoreCase = true) -> "%"
                        paramName.contains("Plaquetas", ignoreCase = true) -> "x10^9/Lt"
                        paramName.contains("Glucosa", ignoreCase = true) -> "mg/dl"
                        paramName.contains("Colesterol", ignoreCase = true) -> "mg/dl"
                        paramName.contains("Triglicéridos", ignoreCase = true) -> "mg/dl"
                        else -> ""
                    }
                    
                    val status = determineStatus(value, referenceRange)
                    parameters.add(
                        ExamParameter(
                            name = paramName,
                            value = value,
                            unit = unit,
                            referenceRange = referenceRange,
                            status = status
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extrayendo parámetros especiales: ${e.message}", e)
        }
    }
    
    /**
     * Determina si un nombre de parámetro debe ser ignorado
     */
    private fun shouldSkipName(name: String): Boolean {
        val skipTerms = listOf(
            "nombre", "run", "dni", "sexo", "edad", "origen", "doctor", 
            "folio", "toma", "recepción", "fecha", "observación", "método", 
            "casa matriz", "validado", "comentario"
        )
        
        return skipTerms.any { term -> 
            name.contains(term, ignoreCase = true) 
        }
    }
    
    /**
     * Determina el estado de un parámetro comparando con su rango de referencia
     */
    private fun determineStatus(value: String, reference: String): ParameterStatus {
        // Para valores no numéricos como "Negativo", "No Reactivo", etc.
        if (value.equals("Negativo", ignoreCase = true) || 
            value.equals("No Reactivo", ignoreCase = true)) {
            if (reference.contains("Negativo", ignoreCase = true) || 
                reference.contains("No Reactivo", ignoreCase = true)) {
                return ParameterStatus.NORMAL
            }
            return ParameterStatus.WATCH
        }
        
        if (value.equals("Positivo", ignoreCase = true) || 
            value.equals("Reactivo", ignoreCase = true)) {
            if (reference.contains("Positivo", ignoreCase = true) || 
                reference.contains("Reactivo", ignoreCase = true)) {
                return ParameterStatus.NORMAL
            }
            return ParameterStatus.WATCH
        }
        
        if (reference.isEmpty()) return ParameterStatus.UNDEFINED
        
        try {
            val numericValue = value.replace(",", ".").toDoubleOrNull() ?: return ParameterStatus.UNDEFINED
            
            // Manejar diferentes formatos de rango de referencia
            when {
                reference.contains("-") -> {
                    // Formato: "10 - 20"
                    val cleanReference = reference.replace(Regex("[^0-9,.\\-\\s]"), "").trim()
                    val parts = cleanReference.split("-").map { it.trim().replace(",", ".") }
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
                    val cleanReference = reference.replace("<", "").trim().replace(",", ".")
                                               .replace(Regex("[^0-9,.]"), "")
                    val max = cleanReference.toDoubleOrNull()
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
                    val cleanReference = reference.replace(">", "").trim().replace(",", ".")
                                               .replace(Regex("[^0-9,.]"), "")
                    val min = cleanReference.toDoubleOrNull()
                    if (min != null) {
                        return when {
                            numericValue > min -> ParameterStatus.NORMAL
                            numericValue < min * 0.8 -> ParameterStatus.ATTENTION
                            else -> ParameterStatus.WATCH
                        }
                    }
                }
                reference.contains("hasta", ignoreCase = true) || 
                reference.contains("Deseable", ignoreCase = true) -> {
                    // Formato: "hasta 200" o "Deseable: hasta 200"
                    val numbers = reference.replace(Regex("[^0-9,.]"), " ")
                                          .trim().split("\\s+".toRegex())
                    val max = numbers.firstOrNull { it.isNotEmpty() }?.replace(",", ".")?.toDoubleOrNull()
                    if (max != null) {
                        return when {
                            numericValue <= max -> ParameterStatus.NORMAL
                            numericValue > max * 1.2 -> ParameterStatus.ATTENTION
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
    }
}