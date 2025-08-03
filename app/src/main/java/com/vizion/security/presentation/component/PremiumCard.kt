package com.vizion.security.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Composant de carte premium avec dégradé de couleurs
 * 
 * Cette carte offre un design haut de gamme avec:
 * - Dégradé de couleurs personnalisable pour un aspect premium
 * - Coins arrondis prononcés pour un design moderne
 * - Couleur principale configurable selon le contexte
 * - Parfait pour mettre en valeur les informations importantes
 * 
 * Utilisation:
 * - Carte du score de sécurité principal
 * - Éléments d'interface nécessitant une attention particulière
 * - Composants avec statut critique ou important
 * - Headers et sections principales
 * 
 * @param modifier Modificateur Compose pour la personnalisation
 * @param primaryColor Couleur principale pour le dégradé
 * @param content Contenu à afficher dans la carte premium
 */
@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    primaryColor: Color,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            // Coins très arrondis (24dp) pour un aspect premium
            .clip(RoundedCornerShape(24.dp))
            // Dégradé diagonal pour un effet visuel dynamique
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        // Couleur principale à pleine intensité
                        primaryColor,
                        // Version légèrement plus sombre pour créer la profondeur
                        primaryColor.copy(alpha = 0.8f)
                    )
                )
            ),
        content = content
    )
}