package com.vizion.security.presentation.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vizion.security.presentation.component.GlassCard
import com.vizion.security.presentation.component.ModernCard
import com.vizion.security.presentation.component.PremiumCard
import com.vizion.security.presentation.theme.VizionColors
import com.vizion.security.presentation.viewmodel.WazuhViewModel

data class WazuhLog(
    val timestamp: String,
    val level: String,
    val message: String,
    val source: String
)

@Composable
fun WazuhScreen(
    viewModel: WazuhViewModel = hiltViewModel()
) {
    val logs by viewModel.logs.collectAsState(initial = emptyList())
    val isConnected by viewModel.isConnected.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    )
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Monitoring SIEM",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Surveillance Wazuh en temps réel",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        item {
            PremiumWazuhStatusCard(isConnected)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ModernWazuhStatCard(
                    title = "Événements",
                    value = "1,247",
                    icon = Icons.Default.Timeline,
                    color = VizionColors.PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
                ModernWazuhStatCard(
                    title = "Données",
                    value = "2.4 MB",
                    icon = Icons.Default.DataUsage,
                    color = VizionColors.StatusSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            ModernWazuhLogsCard(logs)
        }
    }
}

@Composable
fun PremiumWazuhStatusCard(isConnected: Boolean) {
    val statusColor = if (isConnected) VizionColors.AccentGreen else VizionColors.AccentRed
    val statusText = if (isConnected) "Connecté" else "Déconnecté"
    
    val pulseAnimation by animateFloatAsState(
        targetValue = if (isConnected) 1f else 0.5f,
        animationSpec = tween(1000),
        label = "pulse"
    )
    
    PremiumCard(
        modifier = Modifier.fillMaxWidth(),
        primaryColor = statusColor
    ) {
        Column(
            modifier = Modifier.padding(28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.3f * pulseAnimation),
                                    Color.White.copy(alpha = 0.1f * pulseAnimation)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Wazuh Status",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Serveur Wazuh",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "159.65.120.14:1514",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isConnected) "Uptime: 2h 15m" else "Offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernWazuhStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                color.copy(alpha = 0.2f),
                                color.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ModernWazuhLogsCard(logs: List<WazuhLog>) {
    ModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(VizionColors.PrimaryBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Logs",
                        tint = VizionColors.PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Logs en Temps Réel",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (logs.isEmpty()) {
                Text(
                    text = "Aucun log disponible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                logs.forEach { log ->
                    ModernWazuhLogItem(log)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun ModernWazuhLogItem(log: WazuhLog) {
    val levelColor = when (log.level) {
        "ERROR" -> VizionColors.AccentRed
        "WARN" -> VizionColors.SecondaryAmber
        "INFO" -> VizionColors.AccentBlue
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(levelColor)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = log.level,
                    style = MaterialTheme.typography.labelMedium,
                    color = levelColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = log.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Text(
                text = log.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = log.source,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}