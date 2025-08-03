package com.vizion.security.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Composant de carte avec effet de verre (glassmorphism)
 * 
 * Cette carte implémente l'effet glassmorphism populaire dans le design moderne:
 * - Arrière-plan semi-transparent avec effet de flou visuel
 * - Bordures subtiles pour définir les contours
 * - Aspect "verre dépoli" très tendance dans les interfaces modernes
 * - Parfait pour les éléments flottants et les overlays
 * 
 * Utilisation:
 * - Cartes de statistiques sur le dashboard
 * - Éléments d'interface avec effet de profondeur
 * - Composants nécessitant un aspect premium et moderne
 * 
 * @param modifier Modificateur Compose pour la personnalisation
 * @param content Contenu à afficher dans la carte de verre
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            // Coins arrondis de 20dp pour un aspect plus doux
            .clip(RoundedCornerShape(20.dp))
            // Arrière-plan avec effet de verre semi-transparent
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        // Couleur de surface avec transparence pour l'effet de verre
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        // Dégradé vers une version plus transparente
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )
            ),
        content = content
    )
}