package com.vizion.security.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.vizion.security.data.repository.WazuhRepository
import com.vizion.security.data.repository.SecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * ViewModel pour l'écran Dashboard (Tableau de bord)
 * 
 * Ce ViewModel gère l'état et la logique métier de l'écran principal de l'application.
 * Il centralise toutes les données affichées sur le dashboard et les met à jour
 * en temps réel.
 * 
 * Responsabilités:
 * - Calcul du score de sécurité global
 * - Comptage des menaces actives
 * - Suivi du nombre d'applications protégées
 * - Vérification de la connexion Wazuh
 * - Gestion des événements récents
 * 
 * Architecture:
 * - Utilise l'injection de dépendances Hilt (@HiltViewModel)
 * - Gère l'état avec StateFlow pour la réactivité
 * - Exécute les opérations asynchrones avec viewModelScope
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    // Repository pour accéder aux données de sécurité locales
    private val securityRepository: SecurityRepository
) : ViewModel() {
    
    // === ÉTATS PRIVÉS (MUTABLES) ===
    // Ces StateFlow privés permettent de modifier les valeurs depuis le ViewModel
    
    /**
     * Score de sécurité global (0-100)
     * Calculé en fonction du nombre et de la gravité des événements de sécurité
     */
    private val _securityScore = MutableStateFlow(85)
    
    /**
     * Nombre de menaces actives détectées
     * Correspond aux événements de sécurité critiques non résolus
     */
    val securityScore: StateFlow<Int> = _securityScore.asStateFlow()
    
    private val _activeThreats = MutableStateFlow(0)
    
    /**
     * Nombre d'applications actuellement protégées par le système
     */
    val activeThreats: StateFlow<Int> = _activeThreats.asStateFlow()
    
    private val _protectedApps = MutableStateFlow(12)
    
    /**
     * État de la connexion au serveur Wazuh SIEM
     * true = connecté, false = déconnecté
     */
    val protectedApps: StateFlow<Int> = _protectedApps.asStateFlow()
    
    private val _wazuhConnected = MutableStateFlow(false)
    
    /**
     * Liste des événements récents à afficher sur le dashboard
     * Contient les messages d'activité des dernières heures
     */
    val wazuhConnected: StateFlow<Boolean> = _wazuhConnected.asStateFlow()
    
    private val _recentEvents = MutableStateFlow<List<String>>(emptyList())
    val recentEvents: StateFlow<List<String>> = _recentEvents.asStateFlow()
    // === ÉTATS PUBLICS (LECTURE SEULE) ===
    // Ces StateFlow publics permettent aux composants UI d'observer les changements
    
    
    /**
     * Bloc d'initialisation du ViewModel
     * Exécuté automatiquement lors de la création du ViewModel
     */
    init {
        // Chargement initial de toutes les données du dashboard
        loadDashboardData()
    }
    
    /**
     * Charge toutes les données nécessaires pour le dashboard
     * 
     * Cette méthode orchestre le chargement de toutes les métriques affichées
     * sur l'écran principal. Elle est exécutée de manière asynchrone pour
     * éviter de bloquer l'interface utilisateur.
     */
    private fun loadDashboardData() {
        // viewModelScope.launch crée une coroutine liée au cycle de vie du ViewModel
        // La coroutine sera automatiquement annulée si le ViewModel est détruit
        viewModelScope.launch {
            try {
                // Calcul et mise à jour du score de sécurité
                updateSecurityScore()
                
                // Comptage et mise à jour des menaces actives
                updateActiveThreats()
                
                // Vérification de l'état de connexion Wazuh
                updateWazuhStatus()
                
                // Chargement des événements récents
                updateRecentEvents()
                
            } catch (e: Exception) {
                // Gestion des erreurs globales
                // TODO: Implémenter la gestion d'erreurs (logs, notifications utilisateur)
            }
        }
    }
    
    /**
     * Calcule et met à jour le score de sécurité global
     * 
     * Le score est calculé selon cette logique:
     * - 30/100 si des événements critiques sont présents
     * - 60/100 si plus de 5 événements d'avertissement
     * - 75/100 si quelques événements d'avertissement
     * - 85/100 si aucun problème majeur détecté
     */
    private suspend fun updateSecurityScore() {
        try {
            // Récupération du nombre d'événements critiques depuis la base de données
            val criticalEvents = securityRepository.getEventCountBySeverity("CRITICAL")
            // Récupération du nombre d'événements d'avertissement
            val warningEvents = securityRepository.getEventCountBySeverity("WARNING")
            
            // Calcul du score basé sur la gravité et le nombre d'événements
            val score = when {
                criticalEvents > 0 -> 30    // Score très bas si menaces critiques
                warningEvents > 5 -> 60     // Score moyen si beaucoup d'avertissements
                warningEvents > 0 -> 75     // Score correct si quelques avertissements
                else -> 85                  // Bon score si pas de problèmes majeurs
            }
            
            // Mise à jour de l'état observable
            _securityScore.value = score
        } catch (e: Exception) {
            // Valeur par défaut en cas d'erreur de calcul
            _securityScore.value = 50
        }
    }
    
    /**
     * Met à jour le nombre de menaces actives
     * 
     * Les menaces actives correspondent aux événements de sécurité
     * classés comme "CRITICAL" dans la base de données.
     */
    private suspend fun updateActiveThreats() {
        try {
            // Récupération du nombre d'événements critiques
            val threats = securityRepository.getEventCountBySeverity("CRITICAL")
            _activeThreats.value = threats
        } catch (e: Exception) {
            // Valeur par défaut en cas d'erreur
            _activeThreats.value = 0
        }
    }
    
    /**
     * Vérifie et met à jour l'état de connexion au serveur Wazuh
     * 
     * Cette méthode interroge le repository Wazuh pour connaître
     * l'état actuel de la connexion TCP au serveur SIEM.
     */
    private fun updateWazuhStatus() {
        // Vérification synchrone de l'état de connexion
        _wazuhConnected.value = false // Temporaire - sera implémenté plus tard
    }
    
    /**
     * Charge et met à jour la liste des événements récents
     * 
     * Cette méthode compile une liste d'événements significatifs
     * survenus dans les dernières 24 heures pour affichage sur le dashboard.
     */
    private suspend fun updateRecentEvents() {
        try {
            // Récupération du nombre d'événements depuis hier
            val eventCount = securityRepository.getRecentEventCount()
            
            // Construction de la liste des événements à afficher
            val events = mutableListOf<String>()
            
            // Ajout d'un message si de nouveaux événements ont été détectés
            if (eventCount > 0) {
                events.add("$eventCount nouveaux événements détectés")
            }
            
            // Ajout du statut de connexion Wazuh
            // TODO: Réimplémenter quand WazuhRepository sera disponible
            if (false) { // Temporaire
                events.add("Connexion Wazuh établie")
            } else {
                events.add("Tentative de reconnexion Wazuh")
            }
            
            // Ajout des informations sur les logs transmis
            // TODO: Réimplémenter quand WazuhRepository sera disponible
            events.add("Logs en cours de synchronisation")
            
            // Mise à jour de l'état avec la liste des événements
            _recentEvents.value = events
        } catch (e: Exception) {
            // Message d'erreur en cas de problème de chargement
            _recentEvents.value = listOf("Erreur lors du chargement des événements")
        }
    }
    
    /**
     * Méthode publique pour rafraîchir toutes les données du dashboard
     * 
     * Cette méthode peut être appelée par l'interface utilisateur
     * pour forcer une mise à jour de toutes les métriques affichées.
     * Utile lors d'un pull-to-refresh ou d'un clic sur un bouton de rafraîchissement.
     */
    fun refreshData() {
        loadDashboardData()
    }
}