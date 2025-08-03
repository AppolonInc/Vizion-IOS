package com.vizion.security.presentation.screen

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.vizion.security.presentation.viewmodel.AlertsViewModel

data class Alert(
    val id: String,
    val title: String,
    val description: String,
    val severity: AlertSeverity,
    val timestamp: String
)

enum class AlertSeverity(
    val displayName: String,
    val color: Color,
    val icon: ImageVector
) {
    CRITICAL("Critique", Color(0xFFFF1744), Icons.Default.Error),
    WARNING("Attention", Color(0xFFFF6F00), Icons.Default.Warning),
    INFO("Information", Color(0xFF2196F3), Icons.Default.Info),
    SUCCESS("Succès", Color(0xFF00C853), Icons.Default.CheckCircle)
}

/**
 * Écran de gestion des alertes de sécurité
 * 
 * Cet écran présente toutes les alertes de sécurité détectées par le système :
 * - Classification par niveau de sévérité (Critique, Attention, Info, Succès)
 * - Résumé visuel avec compteurs par type d'alerte
 * - Liste détaillée avec timestamps et descriptions
 * - Filtrage et recherche dans les alertes
 * 
 * Types d'alertes gérées :
 * - CRITICAL : Menaces immédiates nécessitant une action urgente
 * - WARNING : Situations suspectes à surveiller
 * - INFO : Informations de sécurité importantes
 * - SUCCESS : Confirmations d'actions de sécurité réussies
 * 
 * Architecture :
 * - Pattern MVVM avec AlertsViewModel
 * - États réactifs avec StateFlow
 * - Injection de dépendances Hilt
 * - Design moderne avec Material 3
 * 
 * TODO: Ajouter un système de filtrage avancé
 * TODO: Implémenter la recherche textuelle dans les alertes
 * TODO: Créer des actions rapides (marquer comme lu, archiver)
 * TODO: Ajouter des notifications push pour les alertes critiques
 */
@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel = hiltViewModel()
) {
    // === OBSERVATION DES ÉTATS ===
    // Collecte des données depuis le ViewModel pour mise à jour réactive
    // TODO: Ajouter un état de rafraîchissement (pull-to-refresh)
    // TODO: Implémenter la gestion d'erreurs avec retry
    val alerts by viewModel.alerts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LazyColumn(
        // === CONFIGURATION DU LAYOUT ===
        // Liste scrollable avec espacement uniforme entre les éléments
        // TODO: Ajouter un indicateur de position de scroll
        // TODO: Implémenter le sticky header pour les sections
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                // === TITRE PRINCIPAL ===
                // En-tête de l'écran avec style cohérent
                // TODO: Ajouter un sous-titre avec le nombre total d'alertes
                // TODO: Inclure la date de dernière mise à jour
                text = "Alertes de Sécurité",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
                // TODO: Ajouter une animation d'apparition
                // TODO: Inclure un indicateur de statut global (nombre d'alertes non lues)
                // TODO: Ajouter un bouton de paramètres des alertes
                // TODO: Implémenter un mode sombre/clair pour le texte
                // TODO: Localiser le texte selon la langue de l'utilisateur
            )
        }

        item {
            AlertSummaryCard(alerts)
        }

        items(alerts.size) { index ->
            // === LISTE DES ALERTES ===
            // Affichage de chaque alerte avec sa carte dédiée
            // TODO: Implémenter la virtualisation pour de grandes listes
            // TODO: Ajouter des animations d'apparition/disparition
            // TODO: Créer des actions de swipe (supprimer, archiver)
            AlertCard(alert = alerts[index])
        }
        
        if (isLoading) {
            item {
                Text(
                    text = "Chargement des alertes...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

/**
 * Carte de résumé des alertes par type
 * 
 * Affiche un aperçu visuel du nombre d'alertes par niveau de sévérité
 * avec des compteurs colorés et une présentation claire.
 * 
 * TODO: Rendre les compteurs cliquables pour filtrer
 * TODO: Ajouter des graphiques en secteurs
 * TODO: Inclure des tendances (augmentation/diminution)
 */
@Composable
fun AlertSummaryCard(alerts: List<Alert>) {
    // === CALCUL DES STATISTIQUES ===
    // Comptage automatique des alertes par type de sévérité
    // TODO: Optimiser avec des compteurs pré-calculés dans le ViewModel
    val criticalCount = alerts.count { it.severity == AlertSeverity.CRITICAL }
    val warningCount = alerts.count { it.severity == AlertSeverity.WARNING }
    val infoCount = alerts.count { it.severity == AlertSeverity.INFO }
    val successCount = alerts.count { it.severity == AlertSeverity.SUCCESS }

    Card(
        modifier = Modifier.fillMaxWidth(),
        // === STYLE DE LA CARTE ===
        // Couleur de fond distinctive pour le résumé
        // TODO: Adapter la couleur selon le niveau d'alerte le plus élevé
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                // === TITRE DU RÉSUMÉ ===
                // TODO: Ajouter une icône représentative
                // TODO: Inclure la période couverte (dernières 24h, semaine, etc.)
                // TODO: Ajouter un bouton d'actualisation
                text = "Résumé des Alertes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                // === DISPOSITION DES COMPTEURS ===
                // Répartition équitable de l'espace pour chaque compteur
                // TODO: Adapter la disposition selon la taille d'écran
                // TODO: Implémenter un layout en grille pour plus de compteurs
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AlertSummaryItem("Critique", criticalCount, AlertSeverity.CRITICAL.color)
                AlertSummaryItem("Attention", warningCount, AlertSeverity.WARNING.color)
                AlertSummaryItem("Info", infoCount, AlertSeverity.INFO.color)
                AlertSummaryItem("Succès", successCount, AlertSeverity.SUCCESS.color)
            }
        }
    }
}

/**
 * Élément individuel du résumé d'alertes
 * 
 * Affiche un compteur avec label et couleur associée au type d'alerte.
 * 
 * TODO: Ajouter des animations de compteur
 * TODO: Implémenter des effets de survol/clic
 */
@Composable
fun AlertSummaryItem(label: String, count: Int, color: Color) {
    Column(
        // Centrage horizontal pour un alignement propre
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            // Label descriptif avec style subtil
            // TODO: Localiser les labels
            // TODO: Ajouter des tooltips explicatifs
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Carte individuelle d'alerte
 * 
 * Affiche les détails complets d'une alerte de sécurité :
 * - Icône colorée selon la sévérité
 * - Titre et description de l'alerte
 * - Timestamp de détection
 * - Badge de niveau de sévérité
 */
@Composable
fun AlertCard(alert: Alert) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            // === LAYOUT DE LA CARTE D'ALERTE ===
            // Disposition horizontale avec icône à gauche et contenu à droite
            // TODO: Ajouter des actions contextuelles (menu à 3 points)
            // TODO: Implémenter l'expansion pour plus de détails
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                // === ICÔNE DE SÉVÉRITÉ ===
                // Cercle coloré avec icône représentative du niveau d'alerte
                // TODO: Animer l'icône pour les alertes critiques
                // TODO: Ajouter des effets de pulsation
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(alert.severity.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = alert.severity.icon,
                    contentDescription = alert.severity.displayName,
                    tint = alert.severity.color,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                // === EN-TÊTE DE L'ALERTE ===
                // Titre et badge de sévérité sur la même ligne
                // TODO: Ajouter un indicateur "non lu"
                // TODO: Implémenter la troncature intelligente des titres longs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = alert.title,
                        // TODO: Adapter la taille selon l'importance
                        // TODO: Ajouter des liens cliquables dans le titre
                        // TODO: Implémenter la mise en évidence des mots-clés
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = alert.severity.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = alert.severity.color,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    // === DESCRIPTION DE L'ALERTE ===
                    // Texte détaillé expliquant l'alerte
                    // TODO: Supporter le formatage riche (gras, liens)
                    // TODO: Ajouter la troncature avec "Voir plus"
                    text = alert.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    // === TIMESTAMP DE L'ALERTE ===
                    // Affichage de la date/heure de détection
                    // TODO: Implémenter le formatage relatif ("il y a 2 heures")
                    // TODO: Ajouter la géolocalisation si pertinente
                    text = alert.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}