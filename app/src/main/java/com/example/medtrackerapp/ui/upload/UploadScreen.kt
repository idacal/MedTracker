package com.example.medtrackerapp.ui.upload

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.medtrackerapp.ui.common.ErrorView
import com.example.medtrackerapp.ui.common.LoadingView
import com.example.medtrackerapp.ui.common.MedTrackerTopBar
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla para subir archivos PDF y procesarlos
 */
@Composable
fun UploadScreen(
    navController: NavController,
    viewModel: UploadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Lanzador para seleccionar PDFs
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.updateSelectedPdf(uri)
    }
    
    Scaffold(
        topBar = {
            MedTrackerTopBar(
                title = "Subir examen",
                showBackButton = true,
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                LoadingView(
                    message = when (uiState.processingStatus) {
                        ProcessingStatus.PROCESSING -> "Procesando PDF..."
                        ProcessingStatus.SAVING -> "Guardando examen..."
                        else -> "Cargando..."
                    }
                )
            } else if (uiState.error != null) {
                ErrorView(
                    message = uiState.error ?: "Error desconocido",
                    onRetry = { viewModel.resetState() }
                )
            } else {
                when (uiState.processingStatus) {
                    ProcessingStatus.SAVED -> {
                        SuccessView(
                            message = "¡Examen guardado correctamente!",
                            onContinue = { navController.popBackStack() }
                        )
                    }
                    ProcessingStatus.PROCESSED -> {
                        ProcessedPdfView(
                            exam = uiState.processedExam,
                            onSave = { viewModel.saveExam() },
                            onCancel = { viewModel.resetState() }
                        )
                    }
                    else -> {
                        UploadForm(
                            selectedPdfUri = uiState.selectedPdfUri,
                            laboratoryName = uiState.laboratoryName,
                            examDate = uiState.examDate,
                            doctorName = uiState.doctorName,
                            onSelectPdf = { pdfLauncher.launch("application/pdf") },
                            onLaboratoryNameChange = { viewModel.updateLaboratoryName(it) },
                            onExamDateChange = { viewModel.updateExamDate(it) },
                            onDoctorNameChange = { viewModel.updateDoctorName(it) },
                            onProcess = { pdfFile -> 
                                uiState.selectedPdfUri?.let { uri ->
                                    val file = createTempFileFromUri(context, uri)
                                    if (file != null) {
                                        viewModel.processPdf(file)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Formulario para subir y procesar PDF
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadForm(
    selectedPdfUri: Uri?,
    laboratoryName: String,
    examDate: Date,
    doctorName: String,
    onSelectPdf: () -> Unit,
    onLaboratoryNameChange: (String) -> Unit,
    onExamDateChange: (Date) -> Unit,
    onDoctorNameChange: (String) -> Unit,
    onProcess: (File) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = examDate.time)
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onExamDateChange(Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Selección de PDF
        PdfSelector(
            selectedPdfUri = selectedPdfUri,
            onSelectPdf = onSelectPdf
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Nombre del laboratorio
        OutlinedTextField(
            value = laboratoryName,
            onValueChange = onLaboratoryNameChange,
            label = { Text("Nombre del laboratorio *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Fecha del examen
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true }
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "Fecha",
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column {
                Text(
                    text = "Fecha del examen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = dateFormat.format(examDate),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Nombre del doctor (opcional)
        OutlinedTextField(
            value = doctorName,
            onValueChange = onDoctorNameChange,
            label = { Text("Nombre del doctor (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Doctor"
                )
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Botón de procesar
        Button(
            onClick = { onProcess(File("")) },
            enabled = selectedPdfUri != null && laboratoryName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Procesar PDF",
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

/**
 * Componente para seleccionar un PDF
 */
@Composable
fun PdfSelector(
    selectedPdfUri: Uri?,
    onSelectPdf: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelectPdf() },
        contentAlignment = Alignment.Center
    ) {
        if (selectedPdfUri != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "PDF seleccionado",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "PDF seleccionado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Toca para cambiar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = "Seleccionar PDF",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Toca para seleccionar un PDF",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Vista para mostrar el PDF procesado
 */
@Composable
fun ProcessedPdfView(
    exam: Any?,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabecera
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(8.dp)
                )
                .padding(16.dp)
        ) {
            Text(
                text = "PDF procesado correctamente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Se han procesado los datos del PDF.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Botones
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text("Cancelar")
            }
            
            Button(
                onClick = onSave,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text("Guardar examen")
            }
        }
    }
}

/**
 * Vista de éxito después de guardar el examen
 */
@Composable
fun SuccessView(
    message: String,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Éxito",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(96.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continuar")
        }
    }
}

/**
 * Función para crear un archivo temporal a partir de un URI
 */
fun createTempFileFromUri(context: Context, uri: Uri): File? {
    return try {
        // Crear archivo temporal
        val tempFile = File.createTempFile("pdf_", ".pdf", context.cacheDir)
        
        // Copiar contenido del URI al archivo temporal
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
} 