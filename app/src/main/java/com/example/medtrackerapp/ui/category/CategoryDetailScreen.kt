package com.example.medtrackerapp.ui.category

import androidx.compose.foundation.background
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
import com.example.medtrackerapp.domain.model.ExamParameter
import com.example.medtrackerapp.domain.model.ParameterStatus
import com.example.medtrackerapp.ui.common.ErrorView
import com.example.medtrackerapp.ui.common.LoadingView
import com.example.medtrackerapp.ui.common.ParameterCard
import com.example.medtrackerapp.ui.navigation.Screen
import com.example.medtrackerapp.ui.theme.MedicalColors

/**
 * Pantalla que muestra los parámetros de una categoría específica
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    navController: NavController,
    examId: String,
    categoryName: String,
    viewModel: CategoryDetailViewModel = hiltViewModel()
) {
    // Cargar los detalles de la categoría
    viewModel.loadCategoryDetails(examId, categoryName)
    
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = uiState.categoryName) },
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
                        onRetry = { viewModel.loadCategoryDetails(examId, categoryName) }
                    )
                }
                uiState.parameters.isEmpty() -> {
                    // No hay parámetros
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron parámetros en esta categoría",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                else -> {
                    // Mostrar lista de parámetros
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Información del examen
                        item {
                            ExamInfoCard(
                                laboratoryName = uiState.exam?.laboratoryName ?: "",
                                doctorName = uiState.exam?.doctorName,
                                date = uiState.formattedDate
                            )
                        }
                        
                        // Lista de parámetros
                        items(uiState.parameters) { parameter ->
                            ParameterCard(
                                parameter = parameter,
                                onClick = {
                                    navController.navigate(
                                        Screen.ParameterDetail.createRoute(
                                            "parameterName=${parameter.name}"
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

/**
 * Tarjeta con información básica del examen
 */
@Composable
fun ExamInfoCard(
    laboratoryName: String,
    doctorName: String?,
    date: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Laboratorio: $laboratoryName",
                style = MaterialTheme.typography.bodyLarge
            )
            
            doctorName?.let {
                if (it.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Doctor: $it",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Fecha: $date",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
} 