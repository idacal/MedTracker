package com.example.medtrackerapp.pdfprocessor

import android.content.Context
import android.net.Uri
import androidx.core.text.HtmlCompat
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.File
import java.util.regex.Pattern
import javax.inject.Inject
import java.util.Date

/**
 * Clase principal para procesar PDFs de exámenes médicos
 */
class MedicalExamPdfProcessor @Inject constructor(
    private val context: Context
) {

    // Clases de datos para almacenar la información extraída
    data class PatientInfo(
        val name: String = "",
        val id: String = "",
        val age: String = "",
        val gender: String = "",
        val doctor: String = "",
        val examDate: String = "",
        val reportDate: String = ""
    )

    data class ExamParameter(
        val name: String,
        val value: String,
        val unit: String,
        val referenceRange: String,
        val status: Status = Status.NORMAL
    )

    enum class Status {
        NORMAL, HIGH, LOW, UNDEFINED
    }

    data class ExamCategory(
        val name: String,
        val parameters: MutableList<ExamParameter> = mutableListOf()
    )

    data class MedicalReport(
        var patientInfo: PatientInfo = PatientInfo(),
        val categories: MutableList<ExamCategory> = mutableListOf()
    )

    /**
     * Procesa un PDF de exámenes médicos desde un Uri
     */
    suspend fun processPdfFromUri(uri: Uri): MedicalReport = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
        return@withContext processPdfFromInputStream(inputStream)
    }
    
    /**
     * Procesa un PDF de exámenes médicos desde un archivo
     */
    suspend fun processPdfFromFile(pdfFile: File): Result<MedicalReport> = withContext(Dispatchers.IO) {
        return@withContext try {
            val report = processPdfFromInputStream(pdfFile.inputStream())
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Procesa un PDF de exámenes médicos desde un InputStream
     */
    suspend fun processPdfFromInputStream(inputStream: InputStream?): MedicalReport = withContext(Dispatchers.IO) {
        if (inputStream == null) {
            return@withContext MedicalReport()
        }

        val pdfReader = PdfReader(inputStream)
        val pageCount = pdfReader.numberOfPages
        val textContent = StringBuilder()

        // Extraer todo el texto del PDF
        for (i in 1..pageCount) {
            textContent.append(PdfTextExtractor.getTextFromPage(pdfReader, i))
            textContent.append("\n")
        }

        pdfReader.close()
        inputStream.close()

        // Procesar el contenido del PDF
        return@withContext processTextContent(textContent.toString())
    }

    /**
     * Procesa el contenido de texto extraído del PDF
     */
    private fun processTextContent(content: String): MedicalReport {
        val report = MedicalReport()

        // Extraer información del paciente
        extractPatientInfo(content, report)

        // Detectar categorías de exámenes y extraer parámetros
        detectExamCategories(content, report)

        return report
    }

    /**
     * Extrae la información del paciente del contenido del PDF
     */
    private fun extractPatientInfo(content: String, report: MedicalReport) {
        val namePattern = Pattern.compile("Nombre\\s*:\\s*([^\\n]+)")
        val idPattern = Pattern.compile("RUN/DNI\\.?\\s*:\\s*([^\\n]+)")
        val agePattern = Pattern.compile("Edad\\s*:\\s*([^\\n]+)")
        val genderPattern = Pattern.compile("Sexo\\s*:\\s*([^\\n]+)")
        val doctorPattern = Pattern.compile("Dr\\(a\\)\\s*:\\s*([^\\n]+)")
        val examDatePattern = Pattern.compile("Toma de Muestra\\s*:\\s*([^\\n]+)")
        val reportDatePattern = Pattern.compile("Fecha de informe\\s*:\\s*([^\\n]+)")

        val nameMatcher = namePattern.matcher(content)
        val idMatcher = idPattern.matcher(content)
        val ageMatcher = agePattern.matcher(content)
        val genderMatcher = genderPattern.matcher(content)
        val doctorMatcher = doctorPattern.matcher(content)
        val examDateMatcher = examDatePattern.matcher(content)
        val reportDateMatcher = reportDatePattern.matcher(content)

        // Crear un nuevo objeto PatientInfo en lugar de modificar el existente
        val newPatientInfo = PatientInfo(
            name = if (nameMatcher.find()) nameMatcher.group(1).trim() else "",
            id = if (idMatcher.find()) idMatcher.group(1).trim() else "",
            age = if (ageMatcher.find()) ageMatcher.group(1).trim() else "",
            gender = if (genderMatcher.find()) genderMatcher.group(1).trim() else "",
            doctor = if (doctorMatcher.find()) doctorMatcher.group(1).trim() else "",
            examDate = if (examDateMatcher.find()) examDateMatcher.group(1).trim() else "",
            reportDate = if (reportDateMatcher.find()) reportDateMatcher.group(1).trim() else ""
        )

        // Crear un nuevo objeto MedicalReport con la información actualizada
        report.patientInfo = newPatientInfo
    }

    /**
     * Detecta las categorías de exámenes y extrae los parámetros
     */
    private fun detectExamCategories(content: String, report: MedicalReport) {
        // Categorías conocidas de exámenes
        val knownCategories = listOf(
            "HEMATOLOGIA", "HEMOGRAMA", "ORINAS", "BIOQUIMICA", "HORMONAS", 
            "INMUNOLOGIA", "MICROBIOLOGIA", "COAGULACION", "SEROLOGIA"
        )

        // Crear un patrón para buscar las categorías
        val categoryPattern = knownCategories.joinToString("|") { "($it)" }
        val pattern = Pattern.compile(categoryPattern, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(content)

        // Lista para almacenar las posiciones de las categorías encontradas
        val categoryPositions = mutableListOf<Pair<String, Int>>()

        // Encontrar todas las categorías y sus posiciones
        while (matcher.find()) {
            val categoryName = matcher.group(0)
            val position = matcher.start()
            categoryPositions.add(Pair(categoryName, position))
        }

        // Ordenar por posición
        categoryPositions.sortBy { it.second }

        // Procesar cada categoría
        for (i in categoryPositions.indices) {
            val (categoryName, startPos) = categoryPositions[i]
            val endPos = if (i < categoryPositions.size - 1) {
                categoryPositions[i + 1].second
            } else {
                content.length
            }

            // Extraer el contenido de la categoría
            val categoryContent = content.substring(startPos, endPos)

            // Crear una nueva categoría de exámenes
            val category = ExamCategory(name = categoryName)

            // Extraer parámetros según la categoría
            when {
                categoryName.contains("HEMATOLOGIA", ignoreCase = true) -> {
                    extractHematologyParameters(categoryContent, category)
                }
                categoryName.contains("ORINAS", ignoreCase = true) -> {
                    extractUrineParameters(categoryContent, category)
                }
                categoryName.contains("BIOQUIMICA", ignoreCase = true) -> {
                    extractBiochemistryParameters(categoryContent, category)
                }
                categoryName.contains("HORMONAS", ignoreCase = true) -> {
                    extractHormoneParameters(categoryContent, category)
                }
                categoryName.contains("INMUNOLOGIA", ignoreCase = true) -> {
                    extractImmunologyParameters(categoryContent, category)
                }
                // Añadir más casos según sea necesario
            }

            // Añadir la categoría al informe si contiene parámetros
            if (category.parameters.isNotEmpty()) {
                report.categories.add(category)
            }
        }
    }

    /**
     * Extrae parámetros de hematología del contenido
     */
    private fun extractHematologyParameters(content: String, category: ExamCategory) {
        // Patrón general para parámetros de hematología
        // Busca: Nombre del parámetro : Valor (Unidad) : Rango de referencia
        val pattern = Pattern.compile("([\\w\\.\\s]+)\\s*:\\s*([\\d,\\.]+)\\s*([\\w%\\/\\^\\d]+)?\\s*([\\d,\\.\\s\\-<>]+)")
        val matcher = pattern.matcher(content)

        while (matcher.find()) {
            val name = matcher.group(1)?.trim() ?: continue
            val value = matcher.group(2)?.trim() ?: continue
            val unit = matcher.group(3)?.trim() ?: ""
            val reference = matcher.group(4)?.trim() ?: ""

            // Determinar el estado (normal, alto, bajo)
            val status = determineStatus(value, reference)

            // Crear y añadir el parámetro
            val parameter = ExamParameter(name, value, unit, reference, status)
            category.parameters.add(parameter)
        }

        // También buscar parámetros específicos (fórmula leucocitaria, etc.)
        extractSpecificHematologyParameters(content, category)
    }

    /**
     * Extrae parámetros específicos de hematología (fórmula leucocitaria, etc.)
     */
    private fun extractSpecificHematologyParameters(content: String, category: ExamCategory) {
        // Buscar porcentajes de leucocitos (eosinófilos, neutrófilos, etc.)
        val leucocytesPattern = Pattern.compile("(Eosinófilos|Basófilos|Neutrófilos|Linfocitos|Monocitos)\\s*:\\s*([\\d,\\.]+)\\s*%\\s*([\\d,\\.]+)\\s*([\\d,\\.\\s\\-<>]+)")
        val matcher = leucocytesPattern.matcher(content)

        while (matcher.find()) {
            val name = matcher.group(1)?.trim() ?: continue
            val percentValue = matcher.group(2)?.trim() ?: continue
            val absoluteValue = matcher.group(3)?.trim() ?: continue
            val reference = matcher.group(4)?.trim() ?: ""

            // Determinar el estado (normal, alto, bajo)
            val status = determineStatus(percentValue, reference)

            // Crear y añadir el parámetro para el porcentaje
            val parameter = ExamParameter("$name (%)", percentValue, "%", reference, status)
            category.parameters.add(parameter)

            // Crear y añadir el parámetro para el valor absoluto
            val absoluteParameter = ExamParameter("$name (abs)", absoluteValue, "x10^9/Lt", reference, status)
            category.parameters.add(absoluteParameter)
        }
    }

    /**
     * Extrae parámetros de orina del contenido
     */
    private fun extractUrineParameters(content: String, category: ExamCategory) {
        // Patrones para exámenes de orina
        val physicalPattern = Pattern.compile("(Aspecto|Color)\\s*:\\s*([^\\n:]+)")
        val chemicalPattern = Pattern.compile("(Densidad|pH|Leucocitos|Nitritos|Proteína|Glucosa|Cuerpos cetónicos|Urobilinógeno|Bilirrubina|Sangre)\\s*:\\s*([^\\n:]+)\\s*([\\d,\\.\\s\\-<>]+|Negativo)")

        // Extraer parámetros físicos
        val physicalMatcher = physicalPattern.matcher(content)
        while (physicalMatcher.find()) {
            val name = physicalMatcher.group(1)?.trim() ?: continue
            val value = physicalMatcher.group(2)?.trim() ?: continue

            val parameter = ExamParameter(name, value, "", "", Status.UNDEFINED)
            category.parameters.add(parameter)
        }

        // Extraer parámetros químicos
        val chemicalMatcher = chemicalPattern.matcher(content)
        while (chemicalMatcher.find()) {
            val name = chemicalMatcher.group(1)?.trim() ?: continue
            val value = chemicalMatcher.group(2)?.trim() ?: continue
            val reference = chemicalMatcher.group(3)?.trim() ?: ""

            val status = if (value.equals("Negativo", ignoreCase = true) && 
                         reference.contains("Negativo", ignoreCase = true)) {
                Status.NORMAL
            } else {
                determineStatus(value, reference)
            }

            val parameter = ExamParameter(name, value, "", reference, status)
            category.parameters.add(parameter)
        }

        // Extraer parámetros del sedimento
        extractUrineSedimentParameters(content, category)
    }

    /**
     * Extrae parámetros del sedimento urinario
     */
    private fun extractUrineSedimentParameters(content: String, category: ExamCategory) {
        // Patrón para elementos del sedimento
        val sedimentPattern = Pattern.compile("(Hematíes|Leucocitos|Células epiteliales|Bacterias|Cristales|Cilindros)\\s*:\\s*([^\\n:]+)\\s*([\\d<>\\s\\-]+|No se observa)")
        val matcher = sedimentPattern.matcher(content)

        while (matcher.find()) {
            val name = matcher.group(1)?.trim() ?: continue
            val value = matcher.group(2)?.trim() ?: continue
            val reference = matcher.group(3)?.trim() ?: ""

            val status = if (value.contains("No se observa", ignoreCase = true)) {
                Status.NORMAL
            } else {
                determineStatus(value, reference)
            }

            val parameter = ExamParameter(name, value, "", reference, status)
            category.parameters.add(parameter)
        }
    }

    /**
     * Extrae parámetros bioquímicos del contenido
     */
    private fun extractBiochemistryParameters(content: String, category: ExamCategory) {
        // Patrón para parámetros bioquímicos
        val pattern = Pattern.compile("([\\w\\s\\.\\-\\(\\)]+)\\s*\\(?([\\w%\\/\\^\\d]+)?\\)?\\s*:\\s*([\\d,\\.]+)\\s*([\\w%\\/\\^\\d]*)\\s*([\\d,\\.\\s\\-<>]+|Deseable.*)")
        val matcher = pattern.matcher(content)

        while (matcher.find()) {
            val fullMatch = matcher.group(0) ?: continue
            var name = matcher.group(1)?.trim() ?: continue
            var unitFromPattern = matcher.group(2)?.trim() ?: ""
            val value = matcher.group(3)?.trim() ?: continue
            var unitAfterValue = matcher.group(4)?.trim() ?: ""
            var reference = matcher.group(5)?.trim() ?: ""

            // Si la unidad está después del valor, usarla
            val unit = if (unitAfterValue.isNotEmpty()) unitAfterValue else unitFromPattern

            // Limpiar nombre (en caso de que tenga la unidad entre paréntesis)
            name = name.replace("\\([^)]*\\)".toRegex(), "").trim()

            // Determinar el estado
            val status = determineStatus(value, reference)

            val parameter = ExamParameter(name, value, unit, reference, status)
            category.parameters.add(parameter)
        }

        // Extraer perfil lipídico específicamente
        extractLipidProfileParameters(content, category)
    }

    /**
     * Extrae parámetros del perfil lipídico
     */
    private fun extractLipidProfileParameters(content: String, category: ExamCategory) {
        if (!content.contains("Estudio de lípidos", ignoreCase = true)) {
            return
        }

        // Patrón específico para el perfil lipídico
        val pattern = Pattern.compile("(Colesterol Total|Colesterol HDL|Colesterol No HDL|Colesterol LDL|Colesterol VLDL|Triglicéridos|Colesterol total / HDL)\\s*\\(([\\w\\/]+)\\)\\s*:\\s*([\\d,\\.]+)\\s*(.*)")
        val matcher = pattern.matcher(content)

        while (matcher.find()) {
            val name = matcher.group(1)?.trim() ?: continue
            val unit = matcher.group(2)?.trim() ?: ""
            val value = matcher.group(3)?.trim() ?: continue
            var reference = matcher.group(4)?.trim() ?: ""

            // Extraer el rango de referencia del texto que sigue
            val refPattern = Pattern.compile("(Deseable|Óptimo|Riesgo estándar)\\s*:\\s*([^\\n]+)")
            val refMatcher = refPattern.matcher(reference)
            
            if (refMatcher.find()) {
                reference = "${refMatcher.group(1)}: ${refMatcher.group(2)}"
            }

            // Determinar el estado
            val status = determineStatusForLipids(name, value, reference)

            val parameter = ExamParameter(name, value, unit, reference, status)
            category.parameters.add(parameter)
        }
    }

    /**
     * Extrae parámetros hormonales del contenido
     */
    private fun extractHormoneParameters(content: String, category: ExamCategory) {
        // Patrón para hormonas
        val pattern = Pattern.compile("([\\w\\s\\-\\(\\)]+)\\s*\\(([\\w\\/µ]+)\\)\\s*:\\s*([\\d,\\.]+)\\s*([\\d,\\.\\s\\-<>]+)")
        val matcher = pattern.matcher(content)

        while (matcher.find()) {
            val name = matcher.group(1)?.trim() ?: continue
            val unit = matcher.group(2)?.trim() ?: ""
            val value = matcher.group(3)?.trim() ?: continue
            val reference = matcher.group(4)?.trim() ?: ""

            // Determinar el estado
            val status = determineStatus(value, reference)

            val parameter = ExamParameter(name, value, unit, reference, status)
            category.parameters.add(parameter)
        }
    }

    /**
     * Extrae parámetros inmunológicos del contenido
     */
    private fun extractImmunologyParameters(content: String, category: ExamCategory) {
        // Patrón para pruebas inmunológicas
        val pattern = Pattern.compile("(VIH test|Anticuerpos anti Treponémicos|V\\.D\\.R\\.L\\.)\\s*:\\s*(No Reactivo|Reactivo)\\s*(No Reactivo|Reactivo)")
        val matcher = pattern.matcher(content)

        while (matcher.find()) {
            val name = matcher.group(1)?.trim() ?: continue
            val value = matcher.group(2)?.trim() ?: continue
            val reference = matcher.group(3)?.trim() ?: ""

            // Determinar el estado
            val status = if (value == reference) Status.NORMAL else Status.HIGH

            val parameter = ExamParameter(name, value, "", reference, status)
            category.parameters.add(parameter)
        }
    }

    /**
     * Determina el estado (normal, alto, bajo) para parámetros numéricos
     */
    private fun determineStatus(value: String, reference: String): Status {
        // Si el valor no es un número, no podemos determinar el estado
        val numericValue = value.replace(",", ".").toDoubleOrNull() ?: return Status.UNDEFINED
        
        // Varios formatos posibles para rangos de referencia
        if (reference.contains("-")) {
            // Rango típico: "min - max"
            val rangeParts = reference.split("-").map { it.trim().replace(",", ".").toDoubleOrNull() }
            if (rangeParts.size == 2 && rangeParts[0] != null && rangeParts[1] != null) {
                val min = rangeParts[0]!!
                val max = rangeParts[1]!!
                return when {
                    numericValue < min -> Status.LOW
                    numericValue > max -> Status.HIGH
                    else -> Status.NORMAL
                }
            }
        } else if (reference.contains("<")) {
            // Formato: "< max"
            val maxValueStr = reference.replace("<", "").trim().replace(",", ".")
            val maxValue = maxValueStr.toDoubleOrNull()
            if (maxValue != null) {
                return if (numericValue < maxValue) Status.NORMAL else Status.HIGH
            }
        } else if (reference.contains(">")) {
            // Formato: "> min"
            val minValueStr = reference.replace(">", "").trim().replace(",", ".")
            val minValue = minValueStr.toDoubleOrNull()
            if (minValue != null) {
                return if (numericValue > minValue) Status.NORMAL else Status.LOW
            }
        }
        
        return Status.UNDEFINED
    }

    /**
     * Determina el estado específicamente para parámetros del perfil lipídico
     */
    private fun determineStatusForLipids(name: String, value: String, reference: String): Status {
        val numericValue = value.replace(",", ".").toDoubleOrNull() ?: return Status.UNDEFINED

        // Lógica específica según el tipo de lípido
        return when {
            name.contains("Colesterol Total", ignoreCase = true) -> {
                if (numericValue <= 200) Status.NORMAL else Status.HIGH
            }
            name.contains("Colesterol HDL", ignoreCase = true) -> {
                // Para hombres
                if (numericValue < 35) Status.LOW
                else if (numericValue > 55) Status.HIGH
                else Status.NORMAL
            }
            name.contains("Colesterol LDL", ignoreCase = true) -> {
                if (numericValue < 100) Status.NORMAL else Status.HIGH
            }
            name.contains("Triglicéridos", ignoreCase = true) -> {
                if (numericValue <= 150) Status.NORMAL else Status.HIGH
            }
            else -> determineStatus(value, reference)
        }
    }

    /**
     * Genera una tabla HTML con los resultados
     */
    fun generateHtmlTable(report: MedicalReport): String {
        val htmlBuilder = StringBuilder()
        
        // Información del paciente
        htmlBuilder.append("<h2>Información del Paciente</h2>")
        htmlBuilder.append("<table border='1' cellpadding='5'>")
        htmlBuilder.append("<tr><td><strong>Nombre:</strong></td><td>${report.patientInfo.name}</td></tr>")
        htmlBuilder.append("<tr><td><strong>ID:</strong></td><td>${report.patientInfo.id}</td></tr>")
        htmlBuilder.append("<tr><td><strong>Edad:</strong></td><td>${report.patientInfo.age}</td></tr>")
        htmlBuilder.append("<tr><td><strong>Género:</strong></td><td>${report.patientInfo.gender}</td></tr>")
        htmlBuilder.append("<tr><td><strong>Médico:</strong></td><td>${report.patientInfo.doctor}</td></tr>")
        htmlBuilder.append("<tr><td><strong>Fecha de Examen:</strong></td><td>${report.patientInfo.examDate}</td></tr>")
        htmlBuilder.append("<tr><td><strong>Fecha de Informe:</strong></td><td>${report.patientInfo.reportDate}</td></tr>")
        htmlBuilder.append("</table>")
        
        // Resultados de exámenes por categoría
        for (category in report.categories) {
            htmlBuilder.append("<h2>${category.name}</h2>")
            htmlBuilder.append("<table border='1' cellpadding='5'>")
            htmlBuilder.append("<tr>")
            htmlBuilder.append("<th>Parámetro</th>")
            htmlBuilder.append("<th>Valor</th>")
            htmlBuilder.append("<th>Unidad</th>")
            htmlBuilder.append("<th>Valores de Referencia</th>")
            htmlBuilder.append("<th>Estado</th>")
            htmlBuilder.append("</tr>")
            
            for (parameter in category.parameters) {
                val statusColor = when (parameter.status) {
                    Status.NORMAL -> "#4CAF50" // Verde
                    Status.HIGH -> "#F44336"   // Rojo
                    Status.LOW -> "#2196F3"    // Azul
                    Status.UNDEFINED -> "#9E9E9E" // Gris
                }
                
                val statusText = when (parameter.status) {
                    Status.NORMAL -> "Normal"
                    Status.HIGH -> "Alto"
                    Status.LOW -> "Bajo"
                    Status.UNDEFINED -> "Sin definir"
                }
                
                htmlBuilder.append("<tr>")
                htmlBuilder.append("<td>${parameter.name}</td>")
                htmlBuilder.append("<td>${parameter.value}</td>")
                htmlBuilder.append("<td>${parameter.unit}</td>")
                htmlBuilder.append("<td>${parameter.referenceRange}</td>")
                htmlBuilder.append("<td style='color:${statusColor}'>${statusText}</td>")
                htmlBuilder.append("</tr>")
            }
            
            htmlBuilder.append("</table>")
        }
        
        return htmlBuilder.toString()
    }

    /**
     * Genera un texto de resumen con los resultados anormales
     */
    fun generateAbnormalValuesSummary(report: MedicalReport): String {
        val abnormalBuilder = StringBuilder()
        abnormalBuilder.append("Resumen de valores anormales:\n\n")
        
        var hasAbnormalValues = false
        
        for (category in report.categories) {
            val abnormalParams = category.parameters.filter { 
                it.status == Status.HIGH || it.status == Status.LOW 
            }
            
            if (abnormalParams.isNotEmpty()) {
                hasAbnormalValues = true
                abnormalBuilder.append("${category.name}:\n")
                
                for (param in abnormalParams) {
                    val statusText = if (param.status == Status.HIGH) "Alto" else "Bajo"
                    abnormalBuilder.append("- ${param.name}: ${param.value} ${param.unit} ($statusText)\n")
                    abnormalBuilder.append("  Valor de referencia: ${param.referenceRange}\n")
                }
                
                abnormalBuilder.append("\n")
            }
        }
        
        if (!hasAbnormalValues) {
            abnormalBuilder.append("Todos los valores están dentro de los rangos normales.\n")
        }
        
        return abnormalBuilder.toString()
    }
    
    /**
     * Convierte el reporte médico a un objeto de dominio MedicalExam
     */
    fun convertToMedicalExam(
        report: MedicalReport, 
        pdfPath: String, 
        laboratoryName: String
    ): com.example.medtrackerapp.domain.model.MedicalExam {
        val categories = report.categories.map { category ->
            val parameters = category.parameters.map { param ->
                val status = when (param.status) {
                    Status.NORMAL -> com.example.medtrackerapp.domain.model.ParameterStatus.NORMAL
                    Status.HIGH, Status.LOW -> com.example.medtrackerapp.domain.model.ParameterStatus.WATCH
                    Status.UNDEFINED -> com.example.medtrackerapp.domain.model.ParameterStatus.UNDEFINED
                }
                
                com.example.medtrackerapp.domain.model.ExamParameter(
                    name = param.name,
                    value = param.value,
                    unit = param.unit,
                    referenceRange = param.referenceRange,
                    status = status
                )
            }
            
            com.example.medtrackerapp.domain.model.ExamCategory(
                name = category.name,
                parameters = parameters
            )
        }
        
        // El objeto MedicalExam no tiene el campo patientInfo, así que lo eliminamos
        return com.example.medtrackerapp.domain.model.MedicalExam(
            id = java.util.UUID.randomUUID().toString(),
            date = Date(),
            laboratoryName = laboratoryName,
            doctorName = report.patientInfo.doctor,
            pdfFilePath = pdfPath,
            categories = categories
        )
    }
} 