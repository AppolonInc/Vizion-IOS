package com.vizion.security.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vizion.security.data.repository.AuthResult
import com.vizion.security.data.repository.SubscriptionRepository
import com.vizion.security.data.remote.UserInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel pour l'écran de connexion
 * 
 * Ce ViewModel gère toute la logique de connexion utilisateur :
 * - Validation des champs de saisie
 * - Authentification avec le serveur
 * - Gestion des états de chargement et d'erreur
 * - Synchronisation post-connexion
 * 
 * Architecture :
 * - Utilise StateFlow pour la réactivité de l'UI
 * - Gère les états asynchrones avec des coroutines
 * - Centralise la logique métier de l'authentification
 * - Communique avec SubscriptionRepository pour l'API
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {
    
    // === ÉTATS PRIVÉS (MUTABLES) ===
    
    /**
     * État de l'interface utilisateur de connexion
     * Suit le processus de connexion de l'utilisateur
     */
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Initial)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    /**
     * Indicateur de chargement pour l'UI
     * True pendant la tentative de connexion
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * Message d'erreur à afficher à l'utilisateur
     * Null si aucune erreur
     */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    /**
     * Informations de l'utilisateur connecté
     * Null si pas connecté
     */
    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo.asStateFlow()
    
    /**
     * Tente de connecter l'utilisateur avec email et mot de passe
     * 
     * Cette méthode :
     * 1. Valide les entrées utilisateur
     * 2. Affiche l'état de chargement
     * 3. Appelle l'API d'authentification
     * 4. Gère les réponses de succès et d'erreur
     * 5. Met à jour l'état de l'UI en conséquence
     * 
     * @param email Adresse email de l'utilisateur
     * @param password Mot de passe de l'utilisateur
     */
    fun login(email: String, password: String) {
        // Lancement dans une coroutine pour éviter de bloquer l'UI
        viewModelScope.launch {
            try {
                // === VALIDATION DES ENTRÉES ===
                if (!isValidInput(email, password)) {
                    return@launch
                }
                
                // === DÉBUT DU PROCESSUS DE CONNEXION ===
                _isLoading.value = true
                _errorMessage.value = null
                _uiState.value = LoginUiState.Loading
                
                // === APPEL API D'AUTHENTIFICATION ===
                val result = subscriptionRepository.authenticateUser(email, password)
                
                when (result) {
                    is AuthResult.Success -> {
                        // === CONNEXION RÉUSSIE ===
                        _userInfo.value = result.user
                        _uiState.value = LoginUiState.Success(result.user)
                        _errorMessage.value = null
                        
                        // Log pour le débogage (sans informations sensibles)
                        android.util.Log.i("LoginViewModel", "User logged in successfully: ${result.user.email}")
                    }
                    
                    is AuthResult.Error -> {
                        // === ERREUR DE CONNEXION ===
                        _uiState.value = LoginUiState.Error(result.message)
                        _errorMessage.value = getLocalizedErrorMessage(result.message)
                        
                        // Log pour le débogage
                        android.util.Log.w("LoginViewModel", "Login failed: ${result.message}")
                    }
                }
                
            } catch (e: Exception) {
                // === GESTION DES ERREURS INATTENDUES ===
                val errorMsg = "Erreur de connexion inattendue"
                _uiState.value = LoginUiState.Error(errorMsg)
                _errorMessage.value = errorMsg
                
                // Log détaillé pour le débogage
                android.util.Log.e("LoginViewModel", "Unexpected login error: ${e.message}", e)
                
            } finally {
                // === NETTOYAGE FINAL ===
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Efface le message d'erreur actuel
     * 
     * Utilisé quand l'utilisateur commence à retaper ses identifiants
     * ou quand on veut nettoyer l'état d'erreur.
     */
    fun clearError() {
        _errorMessage.value = null
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Initial
        }
    }
    
    /**
     * Réinitialise complètement l'état du ViewModel
     * 
     * Utilisé lors de la navigation vers l'écran de connexion
     * ou pour nettoyer l'état après une déconnexion.
     */
    fun resetState() {
        _uiState.value = LoginUiState.Initial
        _isLoading.value = false
        _errorMessage.value = null
        _userInfo.value = null
    }
    
    /**
     * Vérifie si l'utilisateur est actuellement connecté
     * 
     * @return true si l'utilisateur est connecté
     */
    fun isUserLoggedIn(): Boolean {
        return _uiState.value is LoginUiState.Success
    }
    
    /**
     * Obtient les informations de l'utilisateur connecté
     * 
     * @return UserInfo si connecté, null sinon
     */
    fun getCurrentUser(): UserInfo? {
        return _userInfo.value
    }
    
    // === MÉTHODES PRIVÉES ===
    
    /**
     * Valide les entrées utilisateur avant l'envoi
     * 
     * Vérifie que :
     * - L'email n'est pas vide et a un format valide
     * - Le mot de passe respecte les critères minimums
     * 
     * @param email Email à valider
     * @param password Mot de passe à valider
     * @return true si les entrées sont valides
     */
    private fun isValidInput(email: String, password: String): Boolean {
        // Validation de l'email
        if (email.isBlank()) {
            _errorMessage.value = "L'adresse email est requise"
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _errorMessage.value = "Format d'email invalide"
            return false
        }
        
        // Validation du mot de passe
        if (password.isBlank()) {
            _errorMessage.value = "Le mot de passe est requis"
            return false
        }
        
        if (password.length < 6) {
            _errorMessage.value = "Le mot de passe doit contenir au moins 6 caractères"
            return false
        }
        
        return true
    }
    
    /**
     * Convertit les messages d'erreur de l'API en messages localisés
     * 
     * Transforme les erreurs techniques en messages compréhensibles
     * par l'utilisateur final en français.
     * 
     * @param apiError Message d'erreur de l'API
     * @return Message d'erreur localisé
     */
    private fun getLocalizedErrorMessage(apiError: String): String {
        return when {
            apiError.contains("invalid credentials", ignoreCase = true) ||
            apiError.contains("unauthorized", ignoreCase = true) -> 
                "Email ou mot de passe incorrect"
            
            apiError.contains("network", ignoreCase = true) ||
            apiError.contains("connection", ignoreCase = true) -> 
                "Erreur de connexion. Vérifiez votre connexion internet."
            
            apiError.contains("timeout", ignoreCase = true) -> 
                "Délai d'attente dépassé. Réessayez."
            
            apiError.contains("server", ignoreCase = true) ||
            apiError.contains("500", ignoreCase = true) -> 
                "Erreur serveur temporaire. Réessayez dans quelques minutes."
            
            apiError.contains("account locked", ignoreCase = true) -> 
                "Compte temporairement verrouillé. Contactez le support."
            
            apiError.contains("email not verified", ignoreCase = true) -> 
                "Email non vérifié. Vérifiez votre boîte mail."
            
            else -> "Erreur de connexion. Vérifiez vos identifiants."
        }
    }
    
    /**
     * États possibles de l'interface utilisateur de connexion
     * 
     * Ces états permettent à l'UI de réagir appropriément selon
     * la phase du processus de connexion.
     */
    sealed class LoginUiState {
        /**
         * État initial - aucune action en cours
         */
        object Initial : LoginUiState()
        
        /**
         * État de chargement - connexion en cours
         */
        object Loading : LoginUiState()
        
        /**
         * État de succès - utilisateur connecté
         * @param user Informations de l'utilisateur connecté
         */
        data class Success(val user: UserInfo) : LoginUiState()
        
        /**
         * État d'erreur - échec de connexion
         * @param message Message d'erreur à afficher
         */
        data class Error(val message: String) : LoginUiState()
    }
}