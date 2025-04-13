package com.example.medtrackerapp.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.medtrackerapp.domain.model.ExamCategory
import com.example.medtrackerapp.domain.model.ParameterStatus
import com.example.medtrackerapp.ui.common.ErrorView
import com.example.medtrackerapp.ui.common.LoadingView
import com.example.medtrackerapp.ui.navigation.Screen
import com.example.medtrackerapp.ui.theme.MedicalColors

/**
 * Pantalla que muestra las categorías de un examen específico
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    navController: NavController,
    examId: String,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    // Cargar el examen
    viewModel.loadExam(examId)
    
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorías") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
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
                        onRetry = { viewModel.loadExam(examId) }
                    )
                }
                uiState.exam != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Información básica del examen
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Laboratorio: ${uiState.exam?.laboratoryName ?: ""}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                uiState.exam?.doctorName?.let { doctor ->
                                    if (doctor.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Doctor: $doctor",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Fecha: ${uiState.formattedDate}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        
                        // Lista de categorías
                        if (uiState.categories.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No se encontraron categorías en este examen",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.categories) { category ->
                                    CategoryCard(
                                        category = category,
                                        onClick = {
                                            val categoryName = category.name.replace(" ", "_")
                                            navController.navigate(
                                                Screen.CategoryDetail.createRoute(
                                                    "examId=$examId",
                                                    "categoryName=$categoryName"
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta para mostrar una categoría
 */
@Composable
fun CategoryCard(
    category: ExamCategory,
    onClick: () -> Unit
) {
    val statusCounts = category.getStatusCount()
    val normalCount = statusCounts[ParameterStatus.NORMAL] ?: 0
    val watchCount = statusCounts[ParameterStatus.WATCH] ?: 0
    val attentionCount = statusCounts[ParameterStatus.ATTENTION] ?: 0
    
    val overallStatus = category.getOverallStatus()
    val statusColor = when (overallStatus) {
        ParameterStatus.NORMAL -> MedicalColors.normal
        ParameterStatus.WATCH -> MedicalColors.watch
        ParameterStatus.ATTENTION -> MedicalColors.attention
        ParameterStatus.UNDEFINED -> MedicalColors.undefined
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicador de estado
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(statusColor, shape = RoundedCornerShape(4.dp))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${category.parameters.size} parámetros",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (normalCount > 0 || watchCount > 0 || attentionCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (normalCount > 0) {
                            StatusChip(
                                count = normalCount,
                                color = MedicalColors.normal,
                                label = "Normal"
                            )
                        }
                        
                        if (watchCount > 0) {
                            StatusChip(
                                count = watchCount,
                                color = MedicalColors.watch,
                                label = "Vigilar"
                            )
                        }
                        
                        if (attentionCount > 0) {
                            StatusChip(
                                count = attentionCount,
                                color = MedicalColors.attention,
                                label = "Atención"
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Chip para mostrar la cantidad de parámetros en cierto estado
 */
@Composable
fun StatusChip(
    count: Int,
    color: androidx.compose.ui.graphics.Color,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, shape = androidx.compose.foundation.shape.CircleShape)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = "$count $label",
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
} 