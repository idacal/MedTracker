package com.example.medtrackerapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.medtrackerapp.domain.model.ExamParameter
import com.example.medtrackerapp.domain.model.ParameterStatus
import com.example.medtrackerapp.ui.theme.MedicalColors

/**
 * Tarjeta para mostrar un parámetro médico
 */
@Composable
fun ParameterCard(
    parameter: ExamParameter,
    onClick: () -> Unit
) {
    val statusColor = when (parameter.status) {
        ParameterStatus.NORMAL -> MedicalColors.normal
        ParameterStatus.WATCH -> MedicalColors.watch
        ParameterStatus.ATTENTION -> MedicalColors.attention
        ParameterStatus.UNDEFINED -> MedicalColors.undefined
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = statusColor.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Columna izquierda: Nombre del parámetro
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = parameter.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (parameter.referenceRange.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rango: ${parameter.referenceRange}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Espaciador
            Spacer(modifier = Modifier.width(16.dp))
            
            // Columna derecha: Valor y unidad
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = parameter.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                
                if (parameter.unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = parameter.unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
} 