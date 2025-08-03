package com.vizion.security.data.repository

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.vizion.security.BuildConfig
import com.vizion.security.data.local.datastore.SecurePreferences
import com.vizion.security.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository pour la gestion des abonnements et synchronisation
 * 
 * Cette classe centralise toute la logique de synchronisation entre
 * l'application mobile et le site web principal pour :
 * - Vérifier les abonnements utilisateur
 * - Gérer l'authentification
 * - Synchroniser les données d'appareil
 * - Contrôler l'accès aux fonctionnalités premium
 * 
 * Architecture :
 * - Utilise Retrofit pour les appels API
 * - Stockage sécurisé avec EncryptedSharedPreferences
 * - StateFlow pour la réactivité de l'UI
 * - Gestion automatique des tokens et rafraîchissement
 */
@Singleton
class SubscriptionRepository @Inject constructor(
    private val context: Context,
    private val apiService: VizionApiService,
    private val securePreferences: SecurePreferences
) {
    
    companion object {
        private const val TAG = "SubscriptionRepository"
        
        // Clés pour le stockage sécurisé
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_SUBSCRIPTION_STATUS = "subscription_status"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_CACHED_FEATURES = "cached_features"
        
        // Intervalles de synchronisation
        private const val SYNC_INTERVAL_MS = 3600000L // 1 heure
        private const val TOKEN_REFRESH_THRESHOLD_MS = 300000L // 5 minutes avant expiration
    }
    
    // États observables pour l'UI
    private val _subscriptionStatus = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.Unknown)
    val subscriptionStatus: Flow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()
    
    private val _availableFeatures = MutableStateFlow<FeaturesData?>(null)
    val availableFeatures: Flow<FeaturesData?> = _availableFeatures.asStateFlow()
    
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: Flow<Boolean> = _isAuthenticated.asStateFlow()
    
    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: Flow<UserInfo?> = _userInfo.asStateFlow()
    
    /**
     * Initialise le repository et vérifie l'état d'authentification
     * 
     * Cette méthode est appelée au démarrage de l'application pour :
     * - Vérifier si un token valide existe
     * - Charger les données mises en cache
     * - Démarrer la synchronisation si nécessaire
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing subscription repository...")
            
            // Vérifier si l'utilisateur est déjà authentifié
            val accessToken = securePreferences.getString(KEY_ACCESS_TOKEN)
            if (!accessToken.isNullOrEmpty()) {
                _isAuthenticated.value = true
                
                // Charger les données mises en cache
                loadCachedData()
                
                // Vérifier si une synchronisation est nécessaire
                val lastSync = securePreferences.getLong(KEY_LAST_SYNC, 0)
                val currentTime = System.currentTimeMillis()
                
                if (currentTime - lastSync > SYNC_INTERVAL_MS) {
                    Log.d(TAG, "Synchronization needed, last sync: ${Date(lastSync)}")
                    syncSubscriptionStatus()
                } else {
                    Log.d(TAG, "Using cached data, last sync: ${Date(lastSync)}")
                }
            } else {
                Log.d(TAG, "No authentication token found")
                _subscriptionStatus.value = SubscriptionStatus.NotAuthenticated
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing subscription repository: ${e.message}", e)
            _subscriptionStatus.value = SubscriptionStatus.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Authentifie l'utilisateur avec email et mot de passe
     * 
     * Cette méthode :
     * 1. Envoie les identifiants au serveur
     * 2. Stocke les tokens de manière sécurisée
     * 3. Met à jour l'état d'authentification
     * 4. Synchronise immédiatement les données
     * 
     * @param email Email de l'utilisateur
     * @param password Mot de passe
     * @return Résultat de l'authentification
     */
    suspend fun authenticateUser(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Authenticating user: $email")
            
            val deviceInfo = getDeviceBasicInfo()
            val loginRequest = LoginRequest(email, password, deviceInfo)
            
            val response = apiService.authenticateUser(loginRequest)
            
            if (response.isSuccessful) {
                val authResponse = response.body()!!
                
                // Stocker les tokens de manière sécurisée
                securePreferences.putString(KEY_ACCESS_TOKEN, authResponse.access_token)
                securePreferences.putString(KEY_REFRESH_TOKEN, authResponse.refresh_token)
                securePreferences.putString(KEY_USER_EMAIL, email)
                
                // Mettre à jour l'état
                _isAuthenticated.value = true
                _userInfo.value = authResponse.user
                
                Log.i(TAG, "User authenticated successfully: ${authResponse.user.email}")
                
                // Synchroniser immédiatement après l'authentification
                syncSubscriptionStatus()
                
                // Enregistrer l'appareil si nécessaire
                registerCurrentDevice()
                
                AuthResult.Success(authResponse.user)
            } else {
                val errorMessage = parseErrorMessage(response)
                Log.e(TAG, "Authentication failed: $errorMessage")
                AuthResult.Error(errorMessage)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Authentication error: ${e.message}", e)
            AuthResult.Error(e.message ?: "Network error")
        }
    }
    
    /**
     * Synchronise le statut d'abonnement avec le serveur
     * 
     * Cette méthode vérifie l'abonnement actuel et met à jour :
     * - Le statut d'abonnement (active, expired, etc.)
     * - Les fonctionnalités disponibles
     * - Les informations de l'appareil
     * - Le cache local pour utilisation hors ligne
     */
    suspend fun syncSubscriptionStatus(): SyncResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Syncing subscription status...")
            
            val accessToken = securePreferences.getString(KEY_ACCESS_TOKEN)
            if (accessToken.isNullOrEmpty()) {
                Log.w(TAG, "No access token available for sync")
                return@withContext SyncResult.NotAuthenticated
            }
            
            // Vérifier et rafraîchir le token si nécessaire
            val validToken = ensureValidToken() ?: return@withContext SyncResult.AuthenticationExpired
            
            val deviceId = getDeviceId()
            val appVersion = BuildConfig.VERSION_NAME
            
            val response = apiService.checkSubscriptionStatus(
                authorization = "Bearer $validToken",
                deviceId = deviceId,
                appVersion = appVersion
            )
            
            if (response.isSuccessful) {
                val subscriptionResponse = response.body()!!
                
                // Mettre à jour l'état local
                updateSubscriptionState(subscriptionResponse)
                
                // Mettre en cache pour utilisation hors ligne
                cacheSubscriptionData(subscriptionResponse)
                
                // Mettre à jour les informations de l'appareil
                updateDeviceInfo(validToken)
                
                // Marquer la dernière synchronisation
                securePreferences.putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                
                Log.i(TAG, "Subscription sync completed successfully")
                Log.d(TAG, "Status: ${subscriptionResponse.subscription_status}, Plan: ${subscriptionResponse.plan}")
                
                SyncResult.Success(subscriptionResponse)
            } else {
                val errorMessage = parseErrorMessage(response)
                Log.e(TAG, "Subscription sync failed: $errorMessage")
                
                // En cas d'erreur, utiliser les données mises en cache
                loadCachedData()
                SyncResult.Error(errorMessage)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Subscription sync error: ${e.message}", e)
            
            // En cas d'erreur réseau, utiliser les données mises en cache
            loadCachedData()
            SyncResult.Error(e.message ?: "Network error")
        }
    }
    
    /**
     * Vérifie si une fonctionnalité est disponible
     * 
     * @param feature La fonctionnalité à vérifier
     * @return true si la fonctionnalité est accessible
     */
    fun isFeatureAvailable(feature: SecurityFeature): Boolean {
        val features = _availableFeatures.value ?: return false
        
        return when (feature) {
            SecurityFeature.ANTI_SPYING -> features.anti_spying
            SecurityFeature.BANKING_PROTECTION -> features.banking_protection
            SecurityFeature.NETWORK_MONITORING -> features.network_monitoring
            SecurityFeature.APP_ANALYSIS -> features.app_analysis
            SecurityFeature.REAL_TIME_ALERTS -> features.real_time_alerts
            SecurityFeature.WAZUH_INTEGRATION -> features.wazuh_integration
            SecurityFeature.PREMIUM_SUPPORT -> features.premium_support
            SecurityFeature.ADVANCED_REPORTS -> features.advanced_reports
        }
    }
    
    /**
     * Déconnecte l'utilisateur et nettoie les données
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Logging out user...")
            
            // Nettoyer le stockage sécurisé
            securePreferences.clear()
            
            // Réinitialiser les états
            _isAuthenticated.value = false
            _subscriptionStatus.value = SubscriptionStatus.NotAuthenticated
            _availableFeatures.value = null
            _userInfo.value = null
            
            Log.i(TAG, "User logged out successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout: ${e.message}", e)
        }
    }
    
    /**
     * Force une synchronisation immédiate
     */
    suspend fun forceSyncNow(): SyncResult {
        Log.d(TAG, "Forcing immediate synchronization...")
        return syncSubscriptionStatus()
    }
    
    /**
     * Vérifie si l'utilisateur a un abonnement actif
     */
    fun hasActiveSubscription(): Boolean {
        return when (_subscriptionStatus.value) {
            is SubscriptionStatus.Active -> true
            is SubscriptionStatus.GracePeriod -> true
            else -> false
        }
    }
    
    /**
     * Obtient l'URL pour upgrader l'abonnement
     */
    fun getUpgradeUrl(): String {
        return "https://appollon-inc.com/payments/?device_id=${getDeviceId()}"
    }
    
    // === MÉTHODES PRIVÉES ===
    
    /**
     * S'assure que le token d'accès est valide, le rafraîchit si nécessaire
     */
    private suspend fun ensureValidToken(): String? {
        val accessToken = securePreferences.getString(KEY_ACCESS_TOKEN)
        val refreshToken = securePreferences.getString(KEY_REFRESH_TOKEN)
        
        if (accessToken.isNullOrEmpty() || refreshToken.isNullOrEmpty()) {
            return null
        }
        
        // TODO: Vérifier l'expiration du token et le rafraîchir si nécessaire
        // Pour l'instant, on retourne le token existant
        return accessToken
    }
    
    /**
     * Met à jour l'état d'abonnement local
     */
    private fun updateSubscriptionState(response: SubscriptionStatusResponse) {
        val status = when (response.subscription_status) {
            "active" -> SubscriptionStatus.Active(response.plan, response.expires_at)
            "expired" -> {
                if (response.grace_period_days != null && response.grace_period_days > 0) {
                    SubscriptionStatus.GracePeriod(response.grace_period_days)
                } else {
                    SubscriptionStatus.Expired
                }
            }
            "pending" -> SubscriptionStatus.Pending
            "inactive" -> SubscriptionStatus.Inactive
            else -> SubscriptionStatus.Unknown
        }
        
        _subscriptionStatus.value = status
        _availableFeatures.value = response.features
        
        // Sauvegarder le statut
        securePreferences.putString(KEY_SUBSCRIPTION_STATUS, response.subscription_status)
    }
    
    /**
     * Met en cache les données d'abonnement
     */
    private fun cacheSubscriptionData(response: SubscriptionStatusResponse) {
        try {
            // Sauvegarder les données essentielles pour utilisation hors ligne
            securePreferences.putString(KEY_SUBSCRIPTION_STATUS, response.subscription_status)
            securePreferences.putString(KEY_CACHED_FEATURES, 
                "${response.features.anti_spying}," +
                "${response.features.banking_protection}," +
                "${response.features.network_monitoring}," +
                "${response.features.app_analysis}," +
                "${response.features.real_time_alerts}," +
                "${response.features.wazuh_integration}," +
                "${response.features.premium_support}," +
                "${response.features.advanced_reports}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error caching subscription data: ${e.message}", e)
        }
    }
    
    /**
     * Charge les données mises en cache
     */
    private fun loadCachedData() {
        try {
            val cachedStatus = securePreferences.getString(KEY_SUBSCRIPTION_STATUS)
            val cachedFeatures = securePreferences.getString(KEY_CACHED_FEATURES)
            
            if (!cachedStatus.isNullOrEmpty()) {
                val status = when (cachedStatus) {
                    "active" -> SubscriptionStatus.Active("cached", null)
                    "expired" -> SubscriptionStatus.Expired
                    "pending" -> SubscriptionStatus.Pending
                    "inactive" -> SubscriptionStatus.Inactive
                    else -> SubscriptionStatus.Unknown
                }
                _subscriptionStatus.value = status
            }
            
            if (!cachedFeatures.isNullOrEmpty()) {
                val features = cachedFeatures.split(",")
                if (features.size == 8) {
                    _availableFeatures.value = FeaturesData(
                        anti_spying = features[0].toBoolean(),
                        banking_protection = features[1].toBoolean(),
                        network_monitoring = features[2].toBoolean(),
                        app_analysis = features[3].toBoolean(),
                        real_time_alerts = features[4].toBoolean(),
                        wazuh_integration = features[5].toBoolean(),
                        premium_support = features[6].toBoolean(),
                        advanced_reports = features[7].toBoolean()
                    )
                }
            }
            
            Log.d(TAG, "Cached data loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cached data: ${e.message}", e)
        }
    }
    
    /**
     * Met à jour les informations de l'appareil sur le serveur
     */
    private suspend fun updateDeviceInfo(accessToken: String) {
        try {
            val deviceInfo = getDetailedDeviceInfo()
            val response = apiService.updateDeviceInfo("Bearer $accessToken", deviceInfo)
            
            if (response.isSuccessful) {
                Log.d(TAG, "Device info updated successfully")
            } else {
                Log.w(TAG, "Failed to update device info: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating device info: ${e.message}", e)
        }
    }
    
    /**
     * Enregistre l'appareil actuel
     */
    private suspend fun registerCurrentDevice() {
        try {
            val accessToken = securePreferences.getString(KEY_ACCESS_TOKEN) ?: return
            
            val deviceRegistration = DeviceRegistrationRequest(
                device_id = getDeviceId(),
                device_name = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                device_model = android.os.Build.MODEL,
                android_version = android.os.Build.VERSION.RELEASE,
                app_version = BuildConfig.VERSION_NAME,
                registration_token = null // TODO: Implémenter FCM si nécessaire
            )
            
            val response = apiService.registerDevice("Bearer $accessToken", deviceRegistration)
            
            if (response.isSuccessful) {
                Log.i(TAG, "Device registered successfully")
            } else {
                Log.w(TAG, "Device registration failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering device: ${e.message}", e)
        }
    }
    
    /**
     * Obtient l'ID unique de l'appareil
     */
    private fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }
    
    /**
     * Obtient les informations de base de l'appareil
     */
    private fun getDeviceBasicInfo(): DeviceBasicInfo {
        return DeviceBasicInfo(
            device_id = getDeviceId(),
            device_model = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            android_version = android.os.Build.VERSION.RELEASE,
            app_version = BuildConfig.VERSION_NAME
        )
    }
    
    /**
     * Obtient les informations détaillées de l'appareil
     */
    private fun getDetailedDeviceInfo(): DeviceInfoRequest {
        val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        
        return DeviceInfoRequest(
            device_id = getDeviceId(),
            device_model = android.os.Build.MODEL,
            manufacturer = android.os.Build.MANUFACTURER,
            android_version = android.os.Build.VERSION.RELEASE,
            api_level = android.os.Build.VERSION.SDK_INT,
            app_version = BuildConfig.VERSION_NAME,
            app_build = BuildConfig.VERSION_CODE,
            last_activity = currentTime,
            security_score = null, // TODO: Obtenir depuis SecurityRepository
            threats_detected = null, // TODO: Obtenir depuis SecurityRepository
            wazuh_connected = false // TODO: Obtenir depuis WazuhRepository
        )
    }
    
    /**
     * Parse le message d'erreur depuis la réponse API
     */
    private fun parseErrorMessage(response: Response<*>): String {
        return try {
            response.errorBody()?.string() ?: "Unknown error (${response.code()})"
        } catch (e: Exception) {
            "Network error (${response.code()})"
        }
    }
}

/**
 * États possibles de l'abonnement
 */
sealed class SubscriptionStatus {
    object Unknown : SubscriptionStatus()
    object NotAuthenticated : SubscriptionStatus()
    data class Active(val plan: String, val expiresAt: String?) : SubscriptionStatus()
    data class GracePeriod(val daysRemaining: Int) : SubscriptionStatus()
    object Expired : SubscriptionStatus()
    object Inactive : SubscriptionStatus()
    object Pending : SubscriptionStatus()
    data class Error(val message: String) : SubscriptionStatus()
}

/**
 * Résultats d'authentification
 */
sealed class AuthResult {
    data class Success(val user: UserInfo) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Résultats de synchronisation
 */
sealed class SyncResult {
    data class Success(val data: SubscriptionStatusResponse) : SyncResult()
    data class Error(val message: String) : SyncResult()
    object NotAuthenticated : SyncResult()
    object AuthenticationExpired : SyncResult()
}

/**
 * Fonctionnalités de sécurité disponibles
 */
enum class SecurityFeature {
    ANTI_SPYING,
    BANKING_PROTECTION,
    NETWORK_MONITORING,
    APP_ANALYSIS,
    REAL_TIME_ALERTS,
    WAZUH_INTEGRATION,
    PREMIUM_SUPPORT,
    ADVANCED_REPORTS
}