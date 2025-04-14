package com.example.medtrackerapp.pdfprocessor

import android.content.Context
import android.net.Uri
import android.util.Log
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.File
import java.util.regex.Pattern
import javax.inject.Inject
import java.util.Date
import com.example.medtrackerapp.domain.model.ExamCategory
import com.example.medtrackerapp.domain.model.ExamParameter
import com.example.medtrackerapp.domain.model.ParameterStatus
import com.example.medtrackerapp.domain.model.MedicalExam
import java.util.UUID

/**
 * Procesador de PDFs de exámenes médicos siguiendo exactamente la lógica del código Python
 */
class MedicalExamPdfProcessor @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "MedicalExamPdfProcessor"
        
        // Macro_Marcadores del código Python
        private val MACRO_MARCADORES = mapOf(
            "HEMATOLOGIA" to setOf(
                "Leucocitos", "Eritrocitos", "Hemoglobina", "Hematocrito", "V.C.M.", "H.C.M.", "C.H.C.M.", "A.D.E.",
                "Plaquetas", "V.H.S.", "Eosinófilos", "Basófilos", "Neutrófilos", "Linfocitos", "Monocitos"
            ),
            "ORINAS" to setOf(
                "Aspecto", "Color", "Densidad", "pH", "Leucocitos", "Nitritos", "Proteína", "Glucosa",
                "Cuerpos cetónicos", "Urobilinógeno", "Bilirrubina", "Sangre (Hb)", "Hematíes", "Leucocitos",
                "Piocitos", "Células epiteliales", "Bacterias", "Mucus", "Levaduras", "Cristales oxalato cálcico",
                "Cristales amorfos", "Hialinos", "Granulosos gruesos"
            ),
            "Estudio de lípidos" to setOf(
                "Colesterol Total", "Colesterol HDL", "Colesterol No HDL", "Colesterol LDL (Friedewald)",
                "Colesterol VLDL", "Triglicéridos", "Colesterol total / HDL"
            ),
            "Perfil Hepático" to setOf(
                "Bilirrubina total", "Bilirrubina directa", "A.S.A.T. (GOT)", "A.L.A.T. (GPT)", "Fosfatasa alcalina",
                "Gama-Glutamiltransp", "Tpo de protrombina", "I.N.R. (Razón Intern. Normal.)", "FIB-4"
            ),
            "Vitaminas" to setOf(
                "Vitamina B12", "25-hidroxi-vitamina D"
            ),
            "Perfil Bioquímico" to setOf(
                "Bilirrubina total", "Bilirrubina directa", "Creatinina", "Glucosa", "Acido úrico", "Urea", "Calcio",
                "Fósforo", "Colesterol", "Triglicéridos", "Proteínas totales", "Albúmina", "Globulinas",
                "Índice Alb/Glob", "A.S.A.T. (GOT)", "A.L.A.T. (GPT)", "Fosfatasa alcalina", "Lactato Deshidrogenasa"
            ),
            "tiroídeas" to setOf(
                "Tirotropina (TSH ultrasensible)"
            )
        )
    }

    // Modelo de datos para almacenar resultados
    data class PatientInfo(
        val name: String = "",
        val id: String = "",
        val age: String = "",
        val gender: String = "",
        val doctor: String = "",
        val examDate: String = "",
        val reportDate: String = ""
    )

    enum class Status {
        NORMAL, HIGH, LOW, UNDEFINED
    }

    data class ExamParameter(
        val name: String,
        val value: String,
        val unit: String,
        val referenceRange: String,
        val status: Status = Status.NORMAL
    )

    data class ExamCategory(
        val name: String,
        val parameters: MutableList<ExamParameter> = mutableListOf()
    )

    data class MedicalReport(
        var patientInfo: PatientInfo = PatientInfo(),
        val categories: MutableList<ExamCategory> = mutableListOf(),
        var fecha: String = ""
    )

    /**
     * Procesa un PDF desde un Uri (entrada principal de la app)
     */
    suspend fun processPdfFromUri(uri: Uri): MedicalExam = withContext(Dispatchers.IO) {
        Log.d(TAG, "Procesando PDF desde Uri")
        
        // 1. Guardar el archivo en memoria interna (similar a handle_uploaded_file en Python)
        val pdfFileName = "exam_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(context.filesDir, pdfFileName)
        
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            pdfFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        
        Log.d(TAG, "PDF guardado en: ${pdfFile.absolutePath}")
        
        // 2. Procesar el PDF (esta es la función faltante)
        val result = processPdfFromFile(pdfFile)
        
        // 3. Convertir el reporte a la clase de dominio
        val medicalExam = convertToMedicalExam(
            report = result, 
            pdfPath = pdfFile.absolutePath,
            laboratoryName = "Laboratorio" // Puedes cambiarlo o pasarlo como parámetro
        )
        
        return@withContext medicalExam
    }
    
    /**
     * Procesa un archivo PDF directamente (implementación faltante)
     */
    fun processPdfFromFile(pdfFile: File): MedicalReport {
        Log.d(TAG, "Procesando archivo PDF: ${pdfFile.absolutePath}")
        
        // 1. Extraer texto del PDF
        val text = extractTextFromPdf(pdfFile)
        Log.d(TAG, "Texto extraído: ${text.length} caracteres")
        
        // 2. Procesar el texto
        val report = procesarTexto(text)
        
        return report
    }
    
    /**
     * Extrae texto de un archivo PDF (equivalente a extract_text_from_pdf en Python)
     */
    private fun extractTextFromPdf(pdfFile: File): String {
        val text = StringBuilder()
        
        try {
            val reader = PdfReader(pdfFile.absolutePath)
            val numPages = reader.numberOfPages
            
            for (i in 1..numPages) {
                text.append(PdfTextExtractor.getTextFromPage(reader, i))
                text.append("\n")
            }
            
            reader.close()
            
            // Verificar si se extrajo texto
            if (text.isEmpty()) {
                Log.e(TAG, "No se pudo extraer texto del PDF")
            } else {
                Log.d(TAG, "Texto extraído exitosamente: primeros 100 caracteres: ${text.take(100)}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al extraer texto del PDF: ${e.message}", e)
        }
        
        return text.toString()
    }
    
    /**
     * Procesa el texto extraído del PDF (combinación de funciones Python)
     */
    private fun procesarTexto(text: String): MedicalReport {
        Log.d(TAG, "Procesando texto extraído")
        val report = MedicalReport()
        
        // Extraer información básica del paciente
        extractPatientInfo(text, report)
        
        // Buscar parámetros y valores (equivalente a find_keywords_and_values)
        findKeywordsAndValues(text, report)
        
        // Verificar resultados
        if (report.categories.isEmpty()) {
            Log.e(TAG, "No se encontraron categorías o parámetros en el texto")
        } else {
            Log.d(TAG, "Se encontraron ${report.categories.size} categorías")
            for (category in report.categories) {
                Log.d(TAG, "Categoría: ${category.name} - ${category.parameters.size} parámetros")
            }
        }
        
        return report
    }
    
    /**
     * Extrae información básica del paciente
     */
    private fun extractPatientInfo(text: String, report: MedicalReport) {
        // Patrones para extraer información del paciente
        val namePattern = Pattern.compile("Nombre\\s*:\\s*([^\\n\\r]+)")
        val idPattern = Pattern.compile("RUN/DNI\\.?\\s*:\\s*([^\\n\\r]+)")
        val agePattern = Pattern.compile("Edad\\s*:\\s*([^\\n\\r]+)")
        val genderPattern = Pattern.compile("Sexo\\s*:\\s*([^\\n\\r]+)")
        val doctorPattern = Pattern.compile("Dr\\(a\\)\\s*:\\s*([^\\n\\r]+)")
        val examDatePattern = Pattern.compile("Toma de Muestra\\s*:\\s*([^\\n\\r]+)")
        val reportDatePattern = Pattern.compile("Fecha de informe\\s*:\\s*([^\\n\\r]+)")

        val nameMatcher = namePattern.matcher(text)
        val idMatcher = idPattern.matcher(text)
        val ageMatcher = agePattern.matcher(text)
        val genderMatcher = genderPattern.matcher(text)
        val doctorMatcher = doctorPattern.matcher(text)
        val examDateMatcher = examDatePattern.matcher(text)
        val reportDateMatcher = reportDatePattern.matcher(text)

        // Extraer datos de forma segura
        val patientInfo = PatientInfo(
            name = if (nameMatcher.find()) nameMatcher.group(1)?.trim() ?: "" else "",
            id = if (idMatcher.find()) idMatcher.group(1)?.trim() ?: "" else "",
            age = if (ageMatcher.find()) ageMatcher.group(1)?.trim() ?: "" else "",
            gender = if (genderMatcher.find()) genderMatcher.group(1)?.trim() ?: "" else "",
            doctor = if (doctorMatcher.find()) doctorMatcher.group(1)?.trim() ?: "" else "",
            examDate = if (examDateMatcher.find()) examDateMatcher.group(1)?.trim() ?: "" else "",
            reportDate = if (reportDateMatcher.find()) reportDateMatcher.group(1)?.trim() ?: "" else ""
        )

        report.patientInfo = patientInfo
        
        // Buscar fecha de muestra como en el código Python
        val lines = text.split("\n")
        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.replace(" ", "").trim() == "TomadeMuestra") {
                try {
                    if (i + 2 < lines.size) {
                        val fechaMuestra = listOf(lines[i + 1].trim(), lines[i + 2].trim())
                        val fechaCompleta = fechaMuestra[1]
                        val fechaDdmmyy = fechaCompleta.split(" ")[0]
                        report.fecha = fechaDdmmyy
                        Log.d(TAG, "Fecha de muestra encontrada: $fechaDdmmyy")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al extraer fecha de muestra: ${e.message}")
                }
                break
            }
        }
    }
    
    /**
     * Busca palabras clave y valores en el texto (equivalente a find_keywords_and_values)
     * Implementación EXACTA del código Python
     */
    private fun findKeywordsAndValues(text: String, report: MedicalReport) {
        val categorias = MACRO_MARCADORES.keys
        var keywords = emptySet<String>()
        val leucocitos = listOf("Eosinófilos", "Basófilos", "Neutrófilos", "Linfocitos", "Monocitos")
        
        // Para tracking
        var currentCategory: ExamCategory? = null
        val categoriesMap = mutableMapOf<String, ExamCategory>()
        
        Log.d(TAG, "Buscando palabras clave y valores")
        
        // Dividir el texto en líneas
        val lines = text.split("\n")
        Log.d(TAG, "Total de líneas en el texto: ${lines.size}")
        
        // Debuggear primeras 20 líneas
        for (lineIndex in 0 until minOf(20, lines.size)) {
            Log.d(TAG, "Línea $lineIndex: ${lines[lineIndex]}")
        }
        
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i].trim()
            
            if (line.replace(" ", "").trim() == "TomadeMuestra") {
                try {
                    if (i + 2 < lines.size) {
                        val fechaMuestra = listOf(lines[i + 1].trim(), lines[i + 2].trim())
                        val fechaCompleta = fechaMuestra[1]
                        val fechaDdmmyy = fechaCompleta.split(" ")[0]
                        Log.d(TAG, "Fecha de muestra detectada: $fechaDdmmyy")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al extraer fecha de muestra: ${e.message}")
                }
            }
            
            // Buscar categorías exactamente como en Python
            for (categoria in categorias) {
                if (line.uppercase().contains(categoria.uppercase())) {
                    val cat = categoria
                    Log.d(TAG, "Categoría encontrada: $cat en línea: '$line'")
                    keywords = MACRO_MARCADORES[categoria] ?: emptySet()
                    Log.d(TAG, "Palabras clave a buscar: ${keywords.joinToString(", ")}")
                    
                    // Crear o recuperar categoría
                    currentCategory = categoriesMap.getOrPut(cat) {
                        ExamCategory(name = cat)
                    }
                    
                    break
                }
            }
            
            // Buscar keywords dentro de la línea actual con comparación más flexible
            for (keyword in keywords) {
                // Normalizar línea y keyword para comparación
                val normalizedLine = normalizeString(line)
                val normalizedKeyword = normalizeString(keyword)
                
                // Comparar de tres formas:
                // 1. Exactamente como en Python (sin espacios)
                // 2. Si la línea contiene la palabra clave completa
                // 3. Si la línea comienza con la palabra clave
                if (keyword.replace(" ", "").trim() == line.replace(" ", "").trim() ||
                    normalizedLine.contains(normalizedKeyword) ||
                    normalizedLine.startsWith(normalizedKeyword)) {
                    
                    Log.d(TAG, "Palabra clave encontrada: '$keyword' en línea: '$line'")
                    
                    // Verificar si ya existe este parámetro en la categoría actual
                    val alreadyExists = currentCategory?.parameters?.any { it.name == keyword } ?: false
                    if (alreadyExists) {
                        Log.d(TAG, "Parámetro '$keyword' ya existe, omitiendo")
                        break
                    }
                    
                    try {
                        // Exactamente como Python: obtener líneas siguientes según el tipo de parámetro
                        val keywordLines: List<String>
                        
                        if (keyword in leucocitos) {
                            // En Python: keyword_lines = [lines[i + j].strip() for j in range(3, 5)]
                            keywordLines = (3 until 5).mapNotNull { j -> 
                                if (i + j < lines.size) lines[i + j].trim() else null 
                            }
                        } else if (keyword == "Tpo de protrombina") {
                            // En Python: keyword_lines = [lines[i + j].strip() for j in range(5, 8)]
                            keywordLines = (5 until 8).mapNotNull { j -> 
                                if (i + j < lines.size) lines[i + j].trim() else null 
                            }
                        } else {
                            // En Python: keyword_lines = [lines[i + j].strip() for j in range(1, 5)]
                            keywordLines = (1 until 5).mapNotNull { j -> 
                                if (i + j < lines.size) lines[i + j].trim() else null 
                            }
                        }
                        
                        Log.d(TAG, "Líneas para '$keyword': ${keywordLines.joinToString(" | ")}")
                        
                        // Reemplazar comas por puntos (como en Python)
                        val processedLines = keywordLines.map { it.replace(",", ".") }
                        
                        // Buscar valor numérico en las líneas (exactamente como en Python)
                        var value: String? = null
                        var referenceValue: String? = null
                        
                        // Verificar si el valor está en la misma línea (formato "Parámetro: valor")
                        if (line.contains(":")) {
                            val parts = line.split(":")
                            if (parts.size > 1) {
                                val possibleValue = parts[1].trim()
                                if (isNumber(possibleValue) || possibleValue.isNotEmpty()) {
                                    value = possibleValue
                                    Log.d(TAG, "Valor encontrado en la misma línea: $value")
                                }
                            }
                        }
                        
                        // Si no hay valor en la línea actual, buscar en líneas siguientes
                        if (value == null) {
                            // Buscar el valor numérico primero
                            for (keywordLine in processedLines) {
                                if (isNumber(keywordLine)) {
                                    value = keywordLine
                                    Log.d(TAG, "Valor numérico encontrado: $value")
                                    break
                                }
                                
                                // Buscar en formato "valor unidad"
                                val numPattern = Pattern.compile("(\\d+[,\\.]?\\d*)")
                                val matcher = numPattern.matcher(keywordLine)
                                if (matcher.find()) {
                                    value = matcher.group(1)
                                    Log.d(TAG, "Valor numérico extraído con regex: $value")
                                    break
                                }
                            }
                        }
                        
                        // Si no hay valor numérico, usar la primera línea
                        if (value == null && processedLines.isNotEmpty()) {
                            value = processedLines[0]
                            Log.d(TAG, "Usando primera línea como valor: $value")
                        }
                        
                        // Buscar valores de referencia
                        for (keywordLine in processedLines) {
                            // Caso 1: Menor que (<)
                            if (keywordLine.contains("<")) {
                                if (keywordLine.length > 1) {
                                    val refValues = keywordLine.split("<")
                                    if (refValues.size == 2 && isNumber(refValues[1])) {
                                        referenceValue = keywordLine
                                        Log.d(TAG, "Referencia encontrada con <: $referenceValue")
                                        break
                                    }
                                }
                            } 
                            // Caso 2: Mayor que (>)
                            else if (keywordLine.contains(">")) {
                                if (keywordLine.length > 1) {
                                    val refValues = keywordLine.split(">")
                                    if (refValues.size == 2 && isNumber(refValues[1])) {
                                        referenceValue = keywordLine
                                        Log.d(TAG, "Referencia encontrada con >: $referenceValue")
                                        break
                                    }
                                }
                            } 
                            // Caso 3: Rango con guion (-)
                            else if (keywordLine.contains("-")) {
                                if (keywordLine.length > 1 && !keywordLine.contains("V.N") && !keywordLine.contains("(")) {
                                    val refValues = keywordLine.split("-")
                                    if (refValues.size == 2 && isNumber(refValues[0]) && isNumber(refValues[1])) {
                                        referenceValue = keywordLine
                                        Log.d(TAG, "Referencia encontrada con -: $referenceValue")
                                        break
                                    }
                                } else {
                                    referenceValue = keywordLine
                                    Log.d(TAG, "Referencia genérica encontrada: $referenceValue")
                                    break
                                }
                            }
                        }
                        
                        // En caso de no encontrar valor, usar un valor predeterminado
                        if (value == null) {
                            value = "N/A"
                            Log.d(TAG, "Usando valor predeterminado N/A para $keyword")
                        }
                        
                        // Añadir el parámetro a la categoría actual
                        if (currentCategory != null) {
                            // Agregar parámetro a la categoría actual
                            currentCategory.parameters.add(
                                ExamParameter(
                                    name = keyword,
                                    value = value,
                                    unit = "",  // Por ahora vacío como solicitó el usuario
                                    referenceRange = referenceValue ?: "",
                                    status = Status.UNDEFINED  // Estado por defecto
                                )
                            )
                            
                            Log.d(TAG, "Parámetro agregado: '$keyword' = '$value', ref: '${referenceValue ?: ""}'")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error procesando palabra clave '$keyword': ${e.message}", e)
                    }
                }
            }
            
            i++
        }
        
        // Agregar todas las categorías que tienen parámetros al reporte
        for ((_, category) in categoriesMap) {
            if (category.parameters.isNotEmpty()) {
                report.categories.add(category)
                Log.d(TAG, "Agregada categoría '${category.name}' con ${category.parameters.size} parámetros")
            }
        }
        
        if (report.categories.isEmpty()) {
            for ((cat, _) in categoriesMap) {
                Log.d(TAG, "Categoría '$cat' no tiene parámetros")
            }
        }
    }
    
    /**
     * Normaliza una cadena para comparación (elimina acentos, espacios, y convierte a minúsculas)
     */
    private fun normalizeString(input: String): String {
        val normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
            .replace(Regex("[^\\p{ASCII}]"), "")
            .lowercase()
            .replace(" ", "")
            .replace(",", "")
            .replace(".", "")
        return normalized
    }
    
    /**
     * Verifica si una cadena es un número (equivalente a is_number en Python)
     */
    private fun isNumber(s: String): Boolean {
        return try {
            s.replace(",", ".").toDouble()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }
    
    /**
     * Convierte el informe médico a la clase de dominio
     */
    fun convertToMedicalExam(
        report: MedicalReport, 
        pdfPath: String, 
        laboratoryName: String
    ): MedicalExam {
        val categories = report.categories.map { category ->
            val parameters = category.parameters.map { param ->
                // Por ahora todos los parámetros tienen estado NORMAL
                ExamParameter(
                    name = param.name,
                    value = param.value,
                    unit = param.unit,
                    referenceRange = param.referenceRange,
                    status = ParameterStatus.NORMAL
                )
            }
            
            ExamCategory(
                name = category.name,
                parameters = parameters
            )
        }
        
        return MedicalExam(
            id = UUID.randomUUID().toString(),
            date = Date(),
            laboratoryName = laboratoryName,
            doctorName = report.patientInfo.doctor,
            pdfFilePath = pdfPath,
            categories = categories
        )
    }
}