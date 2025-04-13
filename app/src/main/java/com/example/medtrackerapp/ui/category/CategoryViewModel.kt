package com.example.medtrackerapp.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medtrackerapp.domain.model.ExamCategory
import com.example.medtrackerapp.domain.model.MedicalExam
import com.example.medtrackerapp.domain.usecase.GetExamByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Estado de la UI para la pantalla de categorías
 */
data class CategoryUiState(
    val isLoading: Boolean = true,
    val exam: MedicalExam? = null,
    val categories: List<ExamCategory> = emptyList(),
    val formattedDate: String = "",
    val error: String? = null
)

/**
 * ViewModel para la pantalla de categorías
 */
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val getExamByIdUseCase: GetExamByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    /**
     * Carga un examen por su ID
     */
    fun loadExam(examId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val exam = getExamByIdUseCase(examId)
                if (exam != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            exam = exam,
                            categories = exam.categories,
                            formattedDate = formatDate(exam.date)
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No se encontró el examen"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error desconocido al cargar el examen"
                    )
                }
            }
        }
    }
    
    /**
     * Formatea una fecha para mostrarla en la UI
     */
    private fun formatDate(date: Date): String {
        return dateFormat.format(date)
    }
} 