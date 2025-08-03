package com.vizion.security.data.remote

import retrofit2.Response
import retrofit2.http.*

/**
 * Interface API pour la synchronisation avec le site principal Vizion
 * 
 * Cette interface définit tous les endpoints nécessaires pour :
 * - Vérifier les abonnements utilisateur
 * - Synchroniser les données de compte
 * - Gérer l'authentification mobile
 * - Récupérer les informations de fonctionnalités
 */
interface VizionApiService {
    
    /**
     * Vérifie le statut d'abonnement de l'utilisateur
     * 
     * Cet endpoint contacte le serveur principal pour obtenir :
     * - Le statut d'abonnement actuel (active, inactive, expired)
     * - Les fonctionnalités disponibles selon le plan
     * - La date d'expiration de l'abonnement
     * - Les limites d'appareils
     * 
     * @param authorization Token Bearer pour l'authentification
     * @param deviceId ID unique de l'appareil mobile
     * @param appVersion Version de l'application mobile
     * @return Réponse contenant le statut complet de l'abonnement
     */
    @GET("payments/mobile-sync/")
    suspend fun checkSubscriptionStatus(
        @Header("Authorization") authorization: String,
        @Header("X-Device-ID") deviceId: String,
        @Header("X-App-Version") appVersion: String
    ): Response<SubscriptionStatusResponse>
    
    /**
     * Authentifie l'utilisateur avec email/mot de passe
     * 
     * Permet à l'utilisateur de se connecter depuis l'application mobile
     * en utilisant ses identifiants du site web principal.
     * 
     * @param loginRequest Données de connexion (email, password)
     * @return Token JWT pour les requêtes authentifiées
     */
    @POST("accounts/mobile-login/")
    suspend fun authenticateUser(
        @Body loginRequest: LoginRequest
    ): Response<AuthenticationResponse>
    
    /**
     * Rafraîchit le token d'authentification
     * 
     * Permet de renouveler le token JWT avant son expiration
     * pour maintenir la session utilisateur active.
     * 
     * @param refreshRequest Token de rafraîchissement
     * @return Nouveau token JWT valide
     */
    @POST("accounts/token/refresh/")
    suspend fun refreshToken(
        @Body refreshRequest: RefreshTokenRequest
    ): Response<AuthenticationResponse>
    
    /**
     * Met à jour les informations de l'appareil
     * 
     * Synchronise les données de l'appareil mobile avec le serveur :
     * - Modèle et version Android
     * - Version de l'application
     * - Dernière activité
     * - Statut de sécurité
     * 
     * @param authorization Token Bearer
     * @param deviceInfo Informations détaillées de l'appareil
     * @return Confirmation de mise à jour
     */
    @POST("payments/mobile-sync/device/")
    suspend fun updateDeviceInfo(
        @Header("Authorization") authorization: String,
        @Body deviceInfo: DeviceInfoRequest
    ): Response<UpdateResponse>
    
    /**
     * Récupère les fonctionnalités disponibles
     * 
     * Obtient la liste des fonctionnalités de sécurité disponibles
     * selon le plan d'abonnement de l'utilisateur.
     * 
     * @param authorization Token Bearer
     * @return Liste des fonctionnalités et leurs statuts
     */
    @GET("payments/features/")
    suspend fun getAvailableFeatures(
        @Header("Authorization") authorization: String
    ): Response<FeaturesResponse>
    
    /**
     * Enregistre un nouvel appareil
     * 
     * Associe un nouvel appareil mobile au compte utilisateur
     * en vérifiant les limites d'appareils selon l'abonnement.
     * 
     * @param authorization Token Bearer
     * @param deviceRegistration Données d'enregistrement de l'appareil
     * @return Confirmation d'enregistrement
     */
    @POST("accounts/register-device/")
    suspend fun registerDevice(
        @Header("Authorization") authorization: String,
        @Body deviceRegistration: DeviceRegistrationRequest
    ): Response<DeviceRegistrationResponse>
}

/**
 * Modèles de données pour les requêtes API
 */

/**
 * Réponse du statut d'abonnement
 * 
 * Contient toutes les informations sur l'abonnement utilisateur
 * et les fonctionnalités disponibles.
 */
data class SubscriptionStatusResponse(
    val user_id: Int,
    val email: String,
    val subscription_status: String, // "active", "inactive", "expired", "pending"
    val plan: String, // "vizion_premium", "vizion_free", "vizion_enterprise"
    val expires_at: String?, // Format ISO 8601
    val features: FeaturesData,
    val device_limit: Int,
    val devices_registered: Int,
    val last_sync: String,
    val grace_period_days: Int? // Jours restants en période de grâce
)

/**
 * Données des fonctionnalités disponibles
 * 
 * Chaque booléen indique si la fonctionnalité est accessible
 * avec l'abonnement actuel de l'utilisateur.
 */
data class FeaturesData(
    val anti_spying: Boolean, // Protection contre l'espionnage
    val banking_protection: Boolean, // Protection bancaire avancée
    val network_monitoring: Boolean, // Surveillance réseau
    val app_analysis: Boolean, // Analyse approfondie des applications
    val real_time_alerts: Boolean, // Alertes en temps réel
    val wazuh_integration: Boolean, // Intégration SIEM Wazuh
    val premium_support: Boolean, // Support technique premium
    val advanced_reports: Boolean // Rapports de sécurité avancés
)

/**
 * Requête de connexion utilisateur
 */
data class LoginRequest(
    val email: String,
    val password: String,
    val device_info: DeviceBasicInfo
)

/**
 * Informations de base de l'appareil
 */
data class DeviceBasicInfo(
    val device_id: String,
    val device_model: String,
    val android_version: String,
    val app_version: String
)

/**
 * Réponse d'authentification
 * 
 * Contient les tokens nécessaires pour les requêtes authentifiées
 * et les informations de base de l'utilisateur.
 */
data class AuthenticationResponse(
    val access_token: String, // Token JWT pour l'authentification
    val refresh_token: String, // Token pour renouveler l'accès
    val token_type: String, // Généralement "Bearer"
    val expires_in: Int, // Durée de validité en secondes
    val user: UserInfo
)

/**
 * Informations utilisateur de base
 */
data class UserInfo(
    val id: Int,
    val email: String,
    val first_name: String?,
    val last_name: String?,
    val is_premium: Boolean,
    val subscription_plan: String
)

/**
 * Requête de rafraîchissement de token
 */
data class RefreshTokenRequest(
    val refresh_token: String
)

/**
 * Requête de mise à jour des informations d'appareil
 */
data class DeviceInfoRequest(
    val device_id: String,
    val device_model: String,
    val manufacturer: String,
    val android_version: String,
    val api_level: Int,
    val app_version: String,
    val app_build: Int,
    val last_activity: String, // Timestamp ISO 8601
    val security_score: Int?, // Score de sécurité actuel
    val threats_detected: Int?, // Nombre de menaces détectées
    val wazuh_connected: Boolean // Statut de connexion Wazuh
)

/**
 * Réponse de mise à jour générique
 */
data class UpdateResponse(
    val success: Boolean,
    val message: String,
    val updated_at: String
)

/**
 * Réponse des fonctionnalités disponibles
 */
data class FeaturesResponse(
    val features: FeaturesData,
    val plan_name: String,
    val plan_description: String,
    val upgrade_url: String? // URL pour upgrader l'abonnement
)

/**
 * Requête d'enregistrement d'appareil
 */
data class DeviceRegistrationRequest(
    val device_id: String,
    val device_name: String, // Nom personnalisé de l'appareil
    val device_model: String,
    val android_version: String,
    val app_version: String,
    val registration_token: String? // Token FCM pour notifications
)

/**
 * Réponse d'enregistrement d'appareil
 */
data class DeviceRegistrationResponse(
    val success: Boolean,
    val device_registered: Boolean,
    val device_limit_reached: Boolean,
    val message: String,
    val registered_devices: List<RegisteredDevice>
)

/**
 * Informations d'un appareil enregistré
 */
data class RegisteredDevice(
    val device_id: String,
    val device_name: String,
    val device_model: String,
    val last_seen: String,
    val is_current: Boolean // True si c'est l'appareil actuel
)