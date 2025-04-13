package com.example.medtrackerapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medtrackerapp.domain.model.MedicalExam
import com.example.medtrackerapp.domain.model.ParameterStatus
import com.example.medtrackerapp.domain.usecase.GetExamsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado UI para la pantalla de Dashboard
 */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val exams: List<MedicalExam> = emptyList(),
    val error: String? = null,
    val normalCount: Int = 0,
    val watchCount: Int = 0,
    val attentionCount: Int = 0
)

/**
 * ViewModel para la pantalla principal
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getExamsUseCase: GetExamsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadExams()
    }

    /**
     * Carga los exámenes médicos
     */
    fun loadExams() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                getExamsUseCase().collect { exams ->
                    // Calcular conteos de parámetros
                    val statusCounts = calculateStatusCounts(exams)
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            exams = exams,
                            normalCount = statusCounts[ParameterStatus.NORMAL] ?: 0,
                            watchCount = statusCounts[ParameterStatus.WATCH] ?: 0,
                            attentionCount = statusCounts[ParameterStatus.ATTENTION] ?: 0
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error desconocido al cargar exámenes"
                    )
                }
            }
        }
    }

    /**
     * Calcula los conteos de parámetros por estado
     */
    private fun calculateStatusCounts(exams: List<MedicalExam>): Map<ParameterStatus, Int> {
        val result = mutableMapOf<ParameterStatus, Int>()
        
        if (exams.isEmpty()) {
            return result
        }
        
        // Tomamos solo el examen más reciente para el resumen
        val latestExam = exams.maxByOrNull { it.date }
        
        if (latestExam != null) {
            latestExam.categories.forEach { category ->
                category.parameters.forEach { parameter ->
                    val count = result.getOrDefault(parameter.status, 0)
                    result[parameter.status] = count + 1
                }
            }
        }
        
        return result
    }
} 