package com.vizion.security.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vizion.security.data.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel pour la navigation principale de l'application
 * 
 * Ce ViewModel gère l'état global de navigation et d'authentification :
 * - Vérification de l'état de connexion au démarrage
 * - Gestion de la transition entre écran de login et app principale
 * - Synchronisation automatique des données utilisateur
 * - Contrôle d'accès aux fonctionnalités selon l'abonnement
 */
@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {
    
    // État d'authentification observable par l'UI
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    // État de chargement initial
    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()
    
    init {
        // Vérifier l'état d'authentification au démarrage
        checkAuthenticationStatus()
    }
    
    /**
     * Vérifie l'état d'authentification au démarrage de l'app
     */
    private fun checkAuthenticationStatus() {
        viewModelScope.launch {
            try {
                // Initialiser le repository de subscription
                subscriptionRepository.initialize()
                
                // Observer l'état d'authentification
                subscriptionRepository.isAuthenticated.collect { authenticated ->
                    _isAuthenticated.value = authenticated
                    _isInitializing.value = false
                }
            } catch (e: Exception) {
                _isAuthenticated.value = false
                _isInitializing.value = false
            }
        }
    }
    
    /**
     * Appelé quand l'utilisateur se connecte avec succès
     */
    fun onUserAuthenticated() {
        _isAuthenticated.value = true
    }
    
    /**
     * Déconnecte l'utilisateur et retourne à l'écran de login
     */
    fun logout() {
        viewModelScope.launch {
            subscriptionRepository.logout()
            _isAuthenticated.value = false
        }
    }
}