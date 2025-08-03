// Chemin: app/src/main/java/com/vizion/security/presentation/viewmodel/SecurityViewModel.kt

package com.vizion.security.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.vizion.security.data.repository.SecurityRepository
import com.vizion.security.data.local.entity.SecurityEventEntity
import com.vizion.security.presentation.model.SecurityUiState
import com.vizion.security.presentation.model.SecurityEventUI
import com.vizion.security.presentation.model.PermissionState
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel pour la gestion de l'état UI de sécurité
 *
 * Ce ViewModel centralise toute la logique métier et la gestion d'état
 * pour les écrans de sécurité de l'application Vizion Security.
 *
 * Architecture :
 * - MVVM avec StateFlow pour la réactivité
 * - Injection Hilt pour les dépendances
 * - Gestion d'erreurs centralisée
 * - Actions utilisateur et états loading
 * - Observateurs temps réel des données
 */
@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val securityRepository: SecurityRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SecurityViewModel"
        private const val REFRESH_INTERVAL_MS = 30_000L // 30 secondes
    }

    // États internes
    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    // Formatage des dates
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    init {
        Log.d(TAG, "SecurityViewModel initialized")
        startPeriodicRefresh()
        observeSecurityEvents()
    }

    // === OBSERVATEURS TEMPS RÉEL ===

    /**
     * Observe les événements de sécurité en temps réel
     */
    private fun observeSecurityEvents() {
        viewModelScope.launch {
            try {
                // Observer les événements récents
                securityRepository.getRecentEvents(20).collect { events ->
                    updateRecentEvents(events)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing security events: ${e.message}", e)
                updateErrorState("Erreur lors de la surveillance des événements")
            }
        }

        viewModelScope.launch {
            try {
                // Observer le nombre de menaces critiques
                securityRepository.getActiveCriticalCount().collect { count ->
                    updateThreatLevel(count)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing critical threats: ${e.message}", e)
            }
        }
    }

    /**
     * Rafraîchissement périodique des données
     */
    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                try {
                    delay(REFRESH_INTERVAL_MS)
                    if (!_uiState.value.isLoading) {
                        refreshSecurityData()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic refresh: ${e.message}", e)
                }
            }
        }
    }

    // === CHARGEMENT DES DONNÉES ===

    /**
     * Charge les événements de sécurité
     */
    fun loadSecurityEvents() {
        viewModelScope.launch {
            try {
                updateLoadingState(true)

                // Charger les statistiques générales
                val totalEvents = securityRepository.getTotalEventCount()
                val criticalEvents = securityRepository.getEventCountBySeverity("CRITICAL")
                val warningEvents = securityRepository.getEventCountBySeverity("WARNING")
                val infoEvents = securityRepository.getEventCountBySeverity("INFO")

                _uiState.update { currentState ->
                    currentState.copy(
                        totalEvents = totalEvents,
                        criticalEvents = criticalEvents,
                        warningEvents = warningEvents,
                        infoEvents = infoEvents,
                        lastUpdated = System.currentTimeMillis()
                    )
                }

                Log.d(TAG, "Security events loaded: $totalEvents total, $criticalEvents critical")

            } catch (e: Exception) {
                Log.e(TAG, "Error loading security events: ${e.message}", e)
                updateErrorState("Erreur lors du chargement des événements")
            } finally {
                updateLoadingState(false)
            }
        }
    }

    /**
     * Charge le résumé de sécurité
     */
    fun loadSecuritySummary() {
        viewModelScope.launch {
            try {
                // Calculer le score de risque moyen
                val avgRiskScore = securityRepository.getAverageRiskScore(7)

                // Obtenir les statistiques de synchronisation
                val syncedEvents = securityRepository.getSyncedEventsCount()
                val unsyncedEvents = securityRepository.getUnsyncedEventsCount()

                // Mettre à jour l'état
                _uiState.update { currentState ->
                    currentState.copy(
                        averageRiskScore = avgRiskScore,
                        syncedEvents = syncedEvents,
                        unsyncedEvents = unsyncedEvents,
                        isWazuhConnected = unsyncedEvents < 10, // Heuristique simple
                        lastCheckTime = timeFormat.format(Date())
                    )
                }

                Log.d(TAG, "Security summary loaded: avg risk $avgRiskScore, $syncedEvents synced")

            } catch (e: Exception) {
                Log.e(TAG, "Error loading security summary: ${e.message}", e)
                updateErrorState("Erreur lors du chargement du résumé")
            }
        }
    }

    // === ACTIONS UTILISATEUR ===

    /**
     * Effectue un scan manuel de sécurité
     */
    fun performManualScan() {
        viewModelScope.launch {
            try {
                updateScanningState(true)

                // Simuler un scan (dans une vraie implémentation, déclencher le service)
                delay(3000)

                // Créer un événement de scan
                val scanEvent = SecurityEventEntity(
                    id = "scan_${System.currentTimeMillis()}",
                    eventType = "MANUAL_SCAN",
                    severity = "INFO",
                    message = "Scan de sécurité manuel effectué",
                    source = "USER_ACTION",
                    timestamp = System.currentTimeMillis()
                )

                securityRepository.insertSecurityEvent(scanEvent)

                updateSuccessState("Scan de sécurité terminé avec succès")
                Log.i(TAG, "Manual security scan completed")

            } catch (e: Exception) {
                Log.e(TAG, "Error performing manual scan: ${e.message}", e)
                updateErrorState("Erreur lors du scan manuel")
            } finally {
                updateScanningState(false)
            }
        }
    }

    /**
     * Exporte les logs de sécurité
     */
    fun exportSecurityLogs() {
        viewModelScope.launch {
            try {
                updateExportingState(true)

                val result = securityRepository.exportEventsToJson(30)

                if (result.isSuccess) {
                    updateSuccessState("Logs exportés avec succès")
                    Log.i(TAG, "Security logs exported successfully")
                } else {
                    updateErrorState("Erreur lors de l'export des logs")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error exporting security logs: ${e.message}", e)
                updateErrorState("Erreur lors de l'export")
            } finally {
                updateExportingState(false)
            }
        }
    }

    /**
     * Synchronise avec Wazuh
     */
    fun syncWithWazuh() {
        viewModelScope.launch {
            try {
                updateSyncingState(true)

                val result = securityRepository.syncPendingEventsWithWazuh()

                if (result.isSuccess) {
                    val syncCount = result.getOrNull() ?: 0
                    updateSuccessState("$syncCount événements synchronisés")

                    // Mettre à jour les statistiques
                    loadSecuritySummary()

                    Log.i(TAG, "Synced $syncCount events with Wazuh")
                } else {
                    updateErrorState("Erreur de synchronisation Wazuh")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error syncing with Wazuh: ${e.message}", e)
                updateErrorState("Erreur de synchronisation")
            } finally {
                updateSyncingState(false)
            }
        }
    }

    /**
     * Sélectionne un événement pour affichage détaillé
     */
    fun selectEvent(event: SecurityEventUI) {
        _uiState.update { currentState ->
            currentState.copy(selectedEvent = event)
        }
        Log.d(TAG, "Event selected: ${event.id}")
    }

    /**
     * Résout un événement de sécurité
     */
    fun resolveEvent(eventId: String) {
        viewModelScope.launch {
            try {
                securityRepository.resolveSecurityEvent(eventId, "user")
                updateSuccessState("Événement résolu")

                // Rafraîchir les données
                loadSecurityEvents()

                Log.i(TAG, "Event resolved: $eventId")

            } catch (e: Exception) {
                Log.e(TAG, "Error resolving event: ${e.message}", e)
                updateErrorState("Erreur lors de la résolution")
            }
        }
    }

    // === GESTION DES ÉTATS ===

    /**
     * Met à jour le statut du service
     */
    fun updateServiceStatus(isRunning: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isServiceRunning = isRunning)
        }
        Log.d(TAG, "Service status updated: $isRunning")
    }

    /**
     * Met à jour le statut des permissions
     */
    fun updatePermissionStatus(granted: List<String>, denied: List<String>) {
        _permissionState.update { currentState ->
            currentState.copy(
                grantedPermissions = granted,
                deniedPermissions = denied,
                lastPermissionCheck = System.currentTimeMillis()
            )
        }
        Log.d(TAG, "Permissions updated: ${granted.size} granted, ${denied.size} denied")
    }

    /**
     * Efface les messages d'état
     */
    fun clearMessages() {
        _uiState.update { currentState ->
            currentState.copy(
                errorMessage = null,
                successMessage = null
            )
        }
    }

    // === MÉTHODES PRIVÉES ===

    /**
     * Met à jour les événements récents dans l'UI
     */
    private fun updateRecentEvents(events: List<SecurityEventEntity>) {
        val uiEvents = events.map { entity ->
            SecurityEventUI(
                id = entity.id,
                eventType = entity.eventType,
                severity = entity.severity,
                message = entity.message,
                source = entity.source,
                timestamp = timeFormat.format(Date(entity.timestamp)),
                riskScore = entity.riskScore,
                isResolved = entity.isResolved,
                recommendedAction = entity.recommendedAction,
                confidenceScore = entity.confidenceScore
            )
        }

        _uiState.update { currentState ->
            currentState.copy(recentEvents = uiEvents)
        }
    }

    /**
     * Met à jour le niveau de menace basé sur les événements critiques
     */
    private fun updateThreatLevel(criticalCount: Int) {
        val threatLevel = when {
            criticalCount > 5 -> "CRITICAL"
            criticalCount > 0 -> "WARNING"
            else -> "NORMAL"
        }

        _uiState.update { currentState ->
            currentState.copy(
                threatLevel = threatLevel,
                activeThreats = criticalCount
            )
        }
    }

    /**
     * Rafraîchit toutes les données de sécurité
     */
    private suspend fun refreshSecurityData() {
        try {
            loadSecurityEvents()
            loadSecuritySummary()
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing security data: ${e.message}", e)
        }
    }

    /**
     * Met à jour l'état de chargement
     */
    private fun updateLoadingState(isLoading: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isLoading = isLoading)
        }
    }

    /**
     * Met à jour l'état de scan
     */
    private fun updateScanningState(isScanning: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isScanning = isScanning)
        }
    }

    /**
     * Met à jour l'état d'export
     */
    private fun updateExportingState(isExporting: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isExporting = isExporting)
        }
    }

    /**
     * Met à jour l'état de synchronisation
     */
    private fun updateSyncingState(isSyncing: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isSyncing = isSyncing)
        }
    }

    /**
     * Met à jour l'état d'erreur
     */
    private fun updateErrorState(message: String) {
        _uiState.update { currentState ->
            currentState.copy(
                errorMessage = message,
                successMessage = null
            )
        }
    }

    /**
     * Met à jour l'état de succès
     */
    private fun updateSuccessState(message: String) {
        _uiState.update { currentState ->
            currentState.copy(
                successMessage = message,
                errorMessage = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "SecurityViewModel cleared")
    }
}