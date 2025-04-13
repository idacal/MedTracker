package com.example.medtrackerapp.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.medtrackerapp.domain.model.MedicalExam
import com.example.medtrackerapp.ui.common.ErrorView
import com.example.medtrackerapp.ui.common.ExamCard
import com.example.medtrackerapp.ui.common.LoadingView
import com.example.medtrackerapp.ui.common.MedTrackerTopBar
import com.example.medtrackerapp.ui.common.StatusIndicator
import com.example.medtrackerapp.domain.model.ParameterStatus
import com.example.medtrackerapp.ui.navigation.Screen
import com.example.medtrackerapp.ui.theme.MedicalColors

/**
 * Pantalla principal de la aplicación
 */
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            MedTrackerTopBar(title = "MedTracker")
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.Upload.route) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Subir PDF",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingView()
                }
                uiState.error != null -> {
                    ErrorView(
                        message = uiState.error ?: "Error desconocido",
                        onRetry = { viewModel.loadExams() }
                    )
                }
                else -> {
                    DashboardContent(
                        exams = uiState.exams,
                        normalCount = uiState.normalCount,
                        watchCount = uiState.watchCount,
                        attentionCount = uiState.attentionCount,
                        onExamClick = { exam ->
                            navController.navigate("category_list/${exam.id}")
                        }
                    )
                }
            }
        }
    }
}

/**
 * Contenido principal de la pantalla de Dashboard
 */
@Composable
fun DashboardContent(
    exams: List<MedicalExam>,
    normalCount: Int,
    watchCount: Int,
    attentionCount: Int,
    onExamClick: (MedicalExam) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp) // Espacio para el FAB
    ) {
        item {
            HealthSummaryCard(
                normalCount = normalCount,
                watchCount = watchCount,
                attentionCount = attentionCount
            )
        }
        
        item {
            Text(
                text = "Exámenes recientes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        if (exams.isEmpty()) {
            item {
                NoExamsMessage()
            }
        } else {
            items(exams) { exam ->
                ExamCard(
                    date = exam.date,
                    laboratoryName = exam.laboratoryName,
                    categoryCount = exam.categories.size,
                    onClick = { onExamClick(exam) }
                )
            }
        }
    }
}

/**
 * Tarjeta de resumen de salud
 */
@Composable
fun HealthSummaryCard(
    normalCount: Int,
    watchCount: Int,
    attentionCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Resumen de Salud",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sin exámenes aún
            if (normalCount == 0 && watchCount == 0 && attentionCount == 0) {
                Text(
                    text = "No hay datos disponibles. Sube tu primer examen médico para ver un resumen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Parámetros del último examen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        StatusIndicators(
                            normalCount = normalCount,
                            watchCount = watchCount,
                            attentionCount = attentionCount
                        )
                    }
                }
            }
        }
    }
}

/**
 * Indicadores de estado para el resumen de salud
 */
@Composable
fun StatusIndicators(
    normalCount: Int,
    watchCount: Int,
    attentionCount: Int
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
    ) {
        StatusIndicator(
            count = normalCount,
            status = ParameterStatus.NORMAL,
            label = "Normal"
        )
        StatusIndicator(
            count = watchCount,
            status = ParameterStatus.WATCH,
            label = "Vigilar"
        )
        StatusIndicator(
            count = attentionCount,
            status = ParameterStatus.ATTENTION,
            label = "Atención"
        )
    }
}

/**
 * Mensaje cuando no hay exámenes
 */
@Composable
fun NoExamsMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No tienes exámenes guardados. Pulsa el botón + para subir tu primer examen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
} 