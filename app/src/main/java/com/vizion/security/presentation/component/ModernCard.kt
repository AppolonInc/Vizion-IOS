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
 * Composant de carte moderne avec design premium
 * 
 * Cette carte utilise un design moderne avec:
 * - Coins arrondis pour un aspect doux et moderne
 * - Support des dégradés de couleurs personnalisés
 * - Couleur de fond par défaut basée sur le thème Material
 * - Flexibilité pour le contenu via BoxScope
 * 
 * Utilisation:
 * - Cartes d'information sur le dashboard
 * - Conteneurs pour les métriques de sécurité
 * - Éléments d'interface avec design cohérent
 * 
 * @param modifier Modificateur Compose pour la personnalisation
 * @param gradient Liste de couleurs pour créer un dégradé (optionnel)
 * @param content Contenu à afficher dans la carte
 */
@Composable
fun ModernCard(
    modifier: Modifier = Modifier,
    gradient: List<Color>? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            // Coins arrondis de 16dp pour un aspect moderne
            .clip(RoundedCornerShape(16.dp))
            // Arrière-plan avec dégradé personnalisé ou couleur de surface par défaut
            .background(
                if (gradient != null) {
                    // Création d'un dégradé vertical avec les couleurs fournies
                    Brush.verticalGradient(gradient)
                } else {
                    // Utilisation de la couleur de surface du thème Material
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ),
        content = content
    )
}