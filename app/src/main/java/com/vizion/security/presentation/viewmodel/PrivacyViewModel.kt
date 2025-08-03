package com.vizion.security.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vizion.security.presentation.screen.AppPermission
import com.vizion.security.presentation.screen.RiskLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacyViewModel @Inject constructor() : ViewModel() {
    
    private val _apps = MutableStateFlow<List<AppPermission>>(emptyList())
    val apps: StateFlow<List<AppPermission>> = _apps.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadApps()
    }
    
    private fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Sample data - in a real implementation, this would scan installed apps
                val sampleApps = listOf(
                    AppPermission(
                        appName = "WhatsApp",
                        permissions = listOf("CAMERA", "MICROPHONE", "CONTACTS", "STORAGE"),
                        riskLevel = RiskLevel.MEDIUM
                    ),
                    AppPermission(
                        appName = "Banking App",
                        permissions = listOf("INTERNET", "ACCESS_NETWORK_STATE"),
                        riskLevel = RiskLevel.LOW
                    ),
                    AppPermission(
                        appName = "Unknown App",
                        permissions = listOf("CAMERA", "LOCATION", "CONTACTS", "SMS", "PHONE"),
                        riskLevel = RiskLevel.HIGH
                    ),
                    AppPermission(
                        appName = "Chrome",
                        permissions = listOf("INTERNET", "STORAGE", "CAMERA", "MICROPHONE"),
                        riskLevel = RiskLevel.MEDIUM
                    ),
                    AppPermission(
                        appName = "Calculator",
                        permissions = listOf(),
                        riskLevel = RiskLevel.LOW
                    )
                )
                
                _apps.value = sampleApps
            } catch (e: Exception) {
                _apps.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshApps() {
        loadApps()
    }
}