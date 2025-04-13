package com.example.medtrackerapp.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medtrackerapp.domain.model.ExamCategory
import com.example.medtrackerapp.domain.model.ExamParameter
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
 * Estado de la UI para la pantalla de detalles de categoría
 */
data class CategoryDetailUiState(
    val isLoading: Boolean = true,
    val exam: MedicalExam? = null,
    val categoryName: String = "",
    val parameters: List<ExamParameter> = emptyList(),
    val formattedDate: String = "",
    val error: String? = null
)

/**
 * ViewModel para la pantalla de detalles de categoría
 */
@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    private val getExamByIdUseCase: GetExamByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryDetailUiState())
    val uiState: StateFlow<CategoryDetailUiState> = _uiState.asStateFlow()
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    /**
     * Carga los detalles de una categoría específica de un examen
     */
    fun loadCategoryDetails(examId: String, categoryName: String) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isLoading = true, 
                    error = null,
                    categoryName = categoryName.replace("_", " ")
                )
            }
            
            try {
                val exam = getExamByIdUseCase(examId)
                if (exam != null) {
                    // Buscar la categoría por nombre (ignorando mayúsculas/minúsculas)
                    val category = exam.categories.find { 
                        it.name.equals(categoryName.replace("_", " "), ignoreCase = true)
                    }
                    
                    if (category != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                exam = exam,
                                parameters = category.parameters,
                                formattedDate = formatDate(exam.date)
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "No se encontró la categoría"
                            )
                        }
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
                        error = e.message ?: "Error desconocido al cargar los detalles"
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