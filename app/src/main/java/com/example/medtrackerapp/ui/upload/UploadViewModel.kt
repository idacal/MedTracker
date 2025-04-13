package com.example.medtrackerapp.ui.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medtrackerapp.domain.model.MedicalExam
import com.example.medtrackerapp.domain.usecase.ProcessPdfUseCase
import com.example.medtrackerapp.domain.usecase.SaveExamUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import javax.inject.Inject

/**
 * Estado de la UI para la pantalla de subida de PDFs
 */
data class UploadUiState(
    val isLoading: Boolean = false,
    val selectedPdfUri: Uri? = null,
    val laboratoryName: String = "",
    val examDate: Date = Date(),
    val doctorName: String = "",
    val processingStatus: ProcessingStatus = ProcessingStatus.IDLE,
    val processedExam: MedicalExam? = null,
    val error: String? = null
)

/**
 * Estado del procesamiento de PDFs
 */
enum class ProcessingStatus {
    IDLE,             // Sin actividad
    PROCESSING,       // Procesando PDF
    PROCESSED,        // PDF procesado correctamente
    SAVING,           // Guardando en base de datos
    SAVED,            // Guardado completo
    ERROR             // Error en el proceso
}

/**
 * ViewModel para la subida y procesamiento de PDFs
 */
@HiltViewModel
class UploadViewModel @Inject constructor(
    private val processPdfUseCase: ProcessPdfUseCase,
    private val saveExamUseCase: SaveExamUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    /**
     * Actualiza el URI del PDF seleccionado
     */
    fun updateSelectedPdf(uri: Uri?) {
        _uiState.update { it.copy(
            selectedPdfUri = uri,
            processingStatus = ProcessingStatus.IDLE,
            processedExam = null,
            error = null
        ) }
    }

    /**
     * Actualiza el nombre del laboratorio
     */
    fun updateLaboratoryName(name: String) {
        _uiState.update { it.copy(laboratoryName = name) }
    }

    /**
     * Actualiza la fecha del examen
     */
    fun updateExamDate(date: Date) {
        _uiState.update { it.copy(examDate = date) }
    }

    /**
     * Actualiza el nombre del doctor
     */
    fun updateDoctorName(name: String) {
        _uiState.update { it.copy(doctorName = name) }
    }

    /**
     * Procesa el PDF seleccionado
     */
    fun processPdf(pdfFile: File) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isLoading = true,
                    processingStatus = ProcessingStatus.PROCESSING,
                    error = null
                ) 
            }

            try {
                val laboratoryName = uiState.value.laboratoryName
                if (laboratoryName.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            processingStatus = ProcessingStatus.ERROR,
                            error = "El nombre del laboratorio es obligatorio"
                        )
                    }
                    return@launch
                }

                val result = processPdfUseCase(
                    pdfFile = pdfFile,
                    date = uiState.value.examDate,
                    laboratoryName = laboratoryName,
                    doctorName = uiState.value.doctorName.takeIf { it.isNotBlank() }
                )

                if (result.isSuccess) {
                    val exam = result.getOrNull()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            processingStatus = ProcessingStatus.PROCESSED,
                            processedExam = exam
                        )
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Error desconocido al procesar el PDF"
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            processingStatus = ProcessingStatus.ERROR,
                            error = error
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        processingStatus = ProcessingStatus.ERROR,
                        error = e.message ?: "Error desconocido al procesar el PDF"
                    )
                }
            }
        }
    }

    /**
     * Guarda el examen procesado en la base de datos
     */
    fun saveExam() {
        val exam = uiState.value.processedExam ?: return

        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isLoading = true,
                    processingStatus = ProcessingStatus.SAVING
                ) 
            }

            try {
                saveExamUseCase(exam)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        processingStatus = ProcessingStatus.SAVED
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        processingStatus = ProcessingStatus.ERROR,
                        error = e.message ?: "Error desconocido al guardar el examen"
                    )
                }
            }
        }
    }

    /**
     * Reinicia el estado para subir un nuevo PDF
     */
    fun resetState() {
        _uiState.update { 
            UploadUiState(
                laboratoryName = it.laboratoryName,
                examDate = Date()
            )
        }
    }
} 