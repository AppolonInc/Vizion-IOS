package com.vizion.security.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vizion.security.presentation.screen.Alert
import com.vizion.security.presentation.screen.AlertSeverity
import com.vizion.security.data.repository.SecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val securityRepository: SecurityRepository
) : ViewModel() {
    
    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadAlerts()
    }
    
    private fun loadAlerts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // For now, we'll create some sample alerts
                // In a real implementation, this would load from the repository
                val sampleAlerts = listOf(
                    Alert(
                        id = "1",
                        title = "Tentative d'accès non autorisé",
                        description = "Une application suspecte a tenté d'accéder aux données bancaires",
                        severity = AlertSeverity.CRITICAL,
                        timestamp = formatTimestamp(Date())
                    ),
                    Alert(
                        id = "2",
                        title = "Nouvelle application installée",
                        description = "Une application avec des permissions élevées a été installée",
                        severity = AlertSeverity.WARNING,
                        timestamp = formatTimestamp(Date(System.currentTimeMillis() - 3600000))
                    ),
                    Alert(
                        id = "3",
                        title = "Scan de sécurité terminé",
                        description = "Le scan de sécurité quotidien s'est terminé avec succès",
                        severity = AlertSeverity.SUCCESS,
                        timestamp = formatTimestamp(Date(System.currentTimeMillis() - 7200000))
                    )
                )
                
                _alerts.value = sampleAlerts
            } catch (e: Exception) {
                _alerts.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun formatTimestamp(date: Date): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date)
    }
    
    fun refreshAlerts() {
        loadAlerts()
    }
}