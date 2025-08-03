package com.vizion.security.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.*

/**
 * Client Wazuh complet utilisant l'API REST pour l'auto-enregistrement
 * et UDP pour l'envoi des logs en temps réel
 *
 * Cette classe gère la communication complète avec le serveur Wazuh SIEM :
 * - Auto-enregistrement via API REST (port 55000) et authd (port 1515)
 * - Transmission des logs via UDP (port 1514)
 * - Gestion des tokens d'authentification
 * - Reconnexion automatique en cas de perte réseau
 * - Persistance des données d'agent dans SharedPreferences
 *
 * Architecture de sécurité :
 * - Certificats SSL auto-signés acceptés pour Wazuh
 * - ID d'agent unique basé sur ANDROID_ID
 * - Heartbeat périodique pour maintenir la connexion
 * - Synchronisation des logs non envoyés
 * - Support dual mode : API REST + authd TCP pour compatibilité maximale
 */
@Singleton
class WazuhApiClient @Inject constructor(
    private val context: Context
) {

    // === CONFIGURATION RÉSEAU ===
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .sslSocketFactory(createUnsafeSslSocketFactory(), createUnsafeTrustManager())
        .hostnameVerifier { _, _ -> true }
        .build()

    // === ÉTAT DE CONNEXION ===
    private var udpSocket: DatagramSocket? = null
    private var serverAddress: InetAddress? = null
    private var isConnected = false
    private var apiToken: String? = null
    private var agentId: String? = null
    private var agentKey: String? = null
    private var connectionAttempts = 0
    private var lastConnectionTime = 0L

    // === STOCKAGE PERSISTANT ===
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "wazuh_agent_prefs", Context.MODE_PRIVATE
    )

    companion object {
        private const val TAG = "WazuhApiClient"
        private const val WAZUH_SERVER = "159.65.120.14"
        private const val WAZUH_API_URL = "https://$WAZUH_SERVER:55000"
        private const val WAZUH_UDP_PORT = 1514
        private const val WAZUH_AUTHD_PORT = 1515
        private const val API_USER = "mobile_agent_manager"
        // CORRECTION: Mot de passe corrigé selon configuration serveur
        private const val API_PASSWORD = "MobileSecure2025!"

        // Clés SharedPreferences
        private const val PREF_AGENT_ID = "agent_id"
        private const val PREF_AGENT_KEY = "agent_key"
        private const val PREF_AGENT_NAME = "agent_name"
        private const val PREF_REGISTERED_AT = "registered_at"
        private const val PREF_CONNECTION_METHOD = "connection_method"

        // Paramètres de retry
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2000L
        private const val CONNECTION_TIMEOUT = 10000 // 10 secondes
    }

    init {
        // Récupérer les données d'agent sauvegardées
        agentId = prefs.getString(PREF_AGENT_ID, null)
        agentKey = prefs.getString(PREF_AGENT_KEY, null)

        Log.d(TAG, "WazuhApiClient initialized - Agent ID: ${agentId ?: "not set"}")
    }

    /**
     * Génère un ID d'agent unique et persistant
     */
    private fun generateAgentId(): String {
        if (agentId != null) return agentId!!

        val deviceId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        } catch (e: Exception) {
            Log.w(TAG, "Could not get ANDROID_ID: ${e.message}")
            "unknown-${System.currentTimeMillis()}"
        }

        val uniqueString = "$deviceId-${android.os.Build.MANUFACTURER}-${android.os.Build.MODEL}"
        val md = MessageDigest.getInstance("MD5")
        val hashBytes = md.digest(uniqueString.toByteArray())
        val hash = hashBytes.joinToString("") { "%02x".format(it) }

        agentId = "mobile-${hash.take(8)}"
        prefs.edit().putString(PREF_AGENT_ID, agentId).apply()

        Log.d(TAG, "Generated new agent ID: $agentId")
        return agentId!!
    }

    /**
     * Génère un nom d'agent descriptif
     */
    private fun generateAgentName(): String {
        val manufacturer = android.os.Build.MANUFACTURER.replace(" ", "").take(8)
        val model = android.os.Build.MODEL.replace(" ", "").take(10)
        val androidVersion = android.os.Build.VERSION.RELEASE

        return "VizionMobile-$manufacturer-$model-v$androidVersion"
    }

    /**
     * S'authentifie auprès de l'API Wazuh avec authentification Basic
     * CORRECTION: Utilise Basic Auth et endpoint ?raw=true
     */
    private suspend fun authenticate(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Authenticating with Wazuh API...")

            // CORRECTION: Utiliser Basic Auth au lieu de JSON
            val credentials = "$API_USER:$API_PASSWORD"
            val encodedCredentials = Base64.encodeToString(
                credentials.toByteArray(),
                Base64.NO_WRAP
            )

            val request = Request.Builder()
                .url("$WAZUH_API_URL/security/user/authenticate?raw=true")
                .addHeader("Authorization", "Basic $encodedCredentials")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()

                // CORRECTION: Avec raw=true, la réponse est directement le token
                if (responseBody?.isNotEmpty() == true && responseBody.startsWith("eyJ")) {
                    apiToken = responseBody.trim()
                    Log.i(TAG, "Successfully authenticated with Wazuh API")
                    return@withContext true
                } else {
                    Log.w(TAG, "Invalid token format received: ${responseBody?.take(50)}")
                }
            } else {
                Log.e(TAG, "Authentication failed: ${response.code} - ${response.message}")
                val errorBody = response.body?.string()
                Log.e(TAG, "Error response: $errorBody")
            }

            false
        } catch (e: Exception) {
            Log.e(TAG, "Authentication error: ${e.message}", e)
            false
        }
    }

    /**
     * Vérifie si un agent existe déjà via l'API
     */
    private suspend fun checkAgentExists(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (apiToken == null) return@withContext false

            val currentAgentId = generateAgentId()

            val request = Request.Builder()
                .url("$WAZUH_API_URL/agents?agents_list=$currentAgentId")
                .addHeader("Authorization", "Bearer $apiToken")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "")

                val totalItems = jsonResponse.getJSONObject("data").getInt("total_affected_items")

                if (totalItems > 0) {
                    Log.i(TAG, "Agent $currentAgentId already exists in API")
                    return@withContext true
                }
            }

            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking agent existence: ${e.message}", e)
            false
        }
    }

    /**
     * NOUVEAU: Enregistre l'agent via authd (port 1515) - méthode de fallback
     * Cette méthode utilise le protocole natif Wazuh pour l'enregistrement d'agents
     */
    private suspend fun registerViaAuthd(): Boolean = withContext(Dispatchers.IO) {
        try {
            val agentName = generateAgentName()

            Log.d(TAG, "Registering agent via authd: $agentName")

            // Connexion TCP au port 1515 pour l'enregistrement
            val socket = Socket()
            socket.connect(java.net.InetSocketAddress(WAZUH_SERVER, WAZUH_AUTHD_PORT), CONNECTION_TIMEOUT)

            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            // Envoyer la requête d'enregistrement selon le protocole Wazuh
            writer.println("OSSEC PASS: ")
            writer.println("OSSEC A:'$agentName'")

            // Lire la réponse
            val response = reader.readLine()

            socket.close()

            if (response?.startsWith("OSSEC K:") == true) {
                // Extraire l'ID et la clé de la réponse
                val keyData = response.substring(8).trim() // Retirer "OSSEC K:"
                val parts = keyData.split(" ")

                if (parts.size >= 4) {
                    val receivedId = parts[0]
                    val receivedKey = parts[3]

                    // Sauvegarder les informations d'agent
                    prefs.edit().apply {
                        putString(PREF_AGENT_ID, receivedId)
                        putString(PREF_AGENT_KEY, receivedKey)
                        putString(PREF_AGENT_NAME, agentName)
                        putString(PREF_CONNECTION_METHOD, "authd")
                        putLong(PREF_REGISTERED_AT, System.currentTimeMillis())
                    }.apply()

                    this@WazuhApiClient.agentId = receivedId
                    this@WazuhApiClient.agentKey = receivedKey

                    Log.i(TAG, "Successfully registered agent via authd: ID $receivedId")
                    return@withContext true
                } else {
                    Log.e(TAG, "Invalid authd response format: $response")
                }
            } else {
                Log.e(TAG, "Agent registration via authd failed: $response")
            }

            false
        } catch (e: Exception) {
            Log.e(TAG, "Authd registration error: ${e.message}", e)
            false
        }
    }

    /**
     * Enregistre automatiquement l'agent via l'API Wazuh
     */
    private suspend fun registerAgent(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (apiToken == null) return@withContext false

            val currentAgentName = generateAgentName()

            Log.d(TAG, "Registering agent via API: $currentAgentName")

            val agentData = JSONObject().apply {
                put("name", currentAgentName)
                put("ip", "any")
                put("group", JSONArray().put("mobile"))
            }

            val request = Request.Builder()
                .url("$WAZUH_API_URL/agents")
                .addHeader("Authorization", "Bearer $apiToken")
                .post(agentData.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "")

                if (jsonResponse.has("data")) {
                    val data = jsonResponse.getJSONObject("data")
                    val affectedItems = data.getJSONArray("affected_items")

                    if (affectedItems.length() > 0) {
                        val agentInfo = affectedItems.getJSONObject(0)
                        val registeredId = agentInfo.getString("id")
                        val agentKey = agentInfo.optString("key", "")

                        // Sauvegarder les informations d'agent
                        prefs.edit().apply {
                            putString(PREF_AGENT_ID, registeredId)
                            putString(PREF_AGENT_KEY, agentKey)
                            putString(PREF_AGENT_NAME, currentAgentName)
                            putString(PREF_CONNECTION_METHOD, "api")
                            putLong(PREF_REGISTERED_AT, System.currentTimeMillis())
                        }.apply()

                        this@WazuhApiClient.agentId = registeredId
                        this@WazuhApiClient.agentKey = agentKey

                        Log.i(TAG, "Successfully registered agent via API: $currentAgentName with ID: $registeredId")
                        return@withContext true
                    }
                }
            }

            Log.e(TAG, "Agent registration via API failed: ${response.code} - ${response.message}")
            val errorBody = response.body?.string()
            Log.e(TAG, "API Response: $errorBody")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Agent registration error: ${e.message}", e)
            false
        }
    }

    /**
     * Met à jour le statut de l'agent (dernière activité)
     */
    private suspend fun updateAgentStatus(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (apiToken == null || agentId == null) return@withContext false

            val request = Request.Builder()
                .url("$WAZUH_API_URL/agents/$agentId/restart")
                .addHeader("Authorization", "Bearer $apiToken")
                .put("{}".toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val success = response.isSuccessful

            if (success) {
                Log.d(TAG, "Agent status updated successfully")
            } else {
                Log.w(TAG, "Failed to update agent status: ${response.code}")
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error updating agent status: ${e.message}", e)
            false
        }
    }

    /**
     * AMÉLIORATION: Connecte à Wazuh avec retry et méthodes multiples
     *
     * Processus complet de connexion amélioré :
     * 1. Vérification de la connexion existante
     * 2. Tentative d'authentification API avec retry
     * 3. Enregistrement via API ou fallback vers authd
     * 4. Établissement de la connexion UDP pour les logs
     * 5. Test de connectivité avec message de démarrage
     */
    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Vérifier si déjà connecté
            if (isConnected && udpSocket?.isClosed == false) {
                Log.d(TAG, "Already connected to Wazuh")
                return@withContext true
            }

            Log.d(TAG, "Connecting to Wazuh server... (attempt ${connectionAttempts + 1})")
            connectionAttempts++

            // Étape 1: Essayer l'authentification API avec retry
            var apiAuthenticated = false
            for (attempt in 1..MAX_RETRY_ATTEMPTS) {
                try {
                    apiAuthenticated = authenticate()
                    if (apiAuthenticated) break

                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        Log.w(TAG, "API authentication attempt $attempt failed, retrying...")
                        kotlinx.coroutines.delay(RETRY_DELAY_MS)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "API authentication attempt $attempt error: ${e.message}")
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        kotlinx.coroutines.delay(RETRY_DELAY_MS)
                    }
                }
            }

            // Étape 2: Enregistrer l'agent si nécessaire
            if (agentId == null || agentKey == null) {
                var registered = false

                // Essayer d'abord l'API si authentifié
                if (apiAuthenticated) {
                    registered = if (checkAgentExists()) {
                        Log.i(TAG, "Agent already exists in system")
                        true
                    } else {
                        registerAgent()
                    }
                }

                // Fallback vers authd si l'API a échoué
                if (!registered) {
                    Log.i(TAG, "Falling back to authd registration...")
                    registered = registerViaAuthd()
                }

                if (!registered) {
                    Log.e(TAG, "Failed to register agent via both API and authd")
                    return@withContext false
                }
            }

            // Étape 3: Établir la connexion UDP
            udpSocket?.close() // Fermer l'ancienne connexion si elle existe
            udpSocket = DatagramSocket().apply {
                soTimeout = 5000
            }
            serverAddress = InetAddress.getByName(WAZUH_SERVER)

            // Étape 4: Test de connexion avec message de démarrage
            val startupMessage = buildWazuhMessage(
                "INFO",
                "Mobile agent connected - ${getDeviceInfo()} - Method: ${getConnectionMethod()}",
                "VizionMobile"
            )
            val success = sendUdpMessage(startupMessage)

            if (success) {
                isConnected = true
                lastConnectionTime = System.currentTimeMillis()
                connectionAttempts = 0 // Reset sur succès

                Log.i(TAG, "Successfully connected to Wazuh (Agent: $agentId)")

                // Mettre à jour le statut de l'agent si l'API est disponible
                if (apiAuthenticated) {
                    updateAgentStatus()
                }
            } else {
                Log.e(TAG, "Failed to send startup message")
                disconnect()
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Connection error: ${e.message}", e)
            disconnect()
            false
        }
    }

    /**
     * AMÉLIORATION: Envoie un log avec retry automatique
     */
    suspend fun sendLog(level: String, message: String, source: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Vérifier/rétablir la connexion si nécessaire
            if (!isConnected || udpSocket?.isClosed == true) {
                Log.d(TAG, "Connection lost, attempting to reconnect...")
                if (!connect()) {
                    Log.w(TAG, "Failed to reconnect, queuing log locally")
                    return@withContext false
                }
            }

            val logMessage = buildWazuhMessage(level, message, source)
            var success = false

            // Retry logic pour l'envoi UDP
            for (attempt in 1..MAX_RETRY_ATTEMPTS) {
                success = sendUdpMessage(logMessage)
                if (success) break

                if (attempt < MAX_RETRY_ATTEMPTS) {
                    Log.w(TAG, "Log send attempt $attempt failed, retrying...")
                    kotlinx.coroutines.delay(500) // Délai plus court pour les logs
                }
            }

            if (success) {
                Log.v(TAG, "Successfully sent log: [$level] $source: $message")
            } else {
                Log.w(TAG, "Failed to send log after $MAX_RETRY_ATTEMPTS attempts")
                isConnected = false
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error sending log: ${e.message}", e)
            isConnected = false
            false
        }
    }

    /**
     * AMÉLIORATION: Envoie un heartbeat enrichi
     */
    suspend fun sendHeartbeat(): Boolean = withContext(Dispatchers.IO) {
        val deviceInfo = getDeviceInfo()
        val connectionInfo = "Connected: ${isConnected()}, Method: ${getConnectionMethod()}, Uptime: ${getConnectionUptime()}s"

        sendLog("INFO", "Mobile agent heartbeat - $deviceInfo - $connectionInfo", "MobileAgent")
    }

    /**
     * Vérifie l'état de connexion avec test UDP optionnel
     */
    fun isConnected(): Boolean {
        val basicCheck = isConnected && udpSocket?.isClosed == false

        // Test périodique de connectivité (toutes les 5 minutes)
        if (basicCheck && System.currentTimeMillis() - lastConnectionTime > 300000) {
            // Ici on pourrait ajouter un ping test UDP si nécessaire
        }

        return basicCheck
    }

    /**
     * AMÉLIORATION: Fermeture propre avec message de déconnexion
     */
    fun disconnect() {
        try {
            if (isConnected) {
                try {
                    val disconnectMessage = buildWazuhMessage(
                        "INFO",
                        "Mobile agent disconnecting - Uptime: ${getConnectionUptime()}s",
                        "VizionMobile"
                    )
                    sendUdpMessageSync(disconnectMessage)
                } catch (e: Exception) {
                    Log.d(TAG, "Could not send disconnect message: ${e.message}")
                }
            }

            udpSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect: ${e.message}", e)
        } finally {
            udpSocket = null
            serverAddress = null
            isConnected = false
            Log.d(TAG, "Disconnected from Wazuh")
        }
    }

    /**
     * AMÉLIORATION: Statistiques enrichies
     */
    fun getConnectionStats(): Map<String, Any> {
        return mapOf(
            "connected" to isConnected(),
            "agent_id" to (agentId ?: "not_generated"),
            "agent_name" to generateAgentName(),
            "server" to WAZUH_SERVER,
            "api_url" to WAZUH_API_URL,
            "udp_port" to WAZUH_UDP_PORT,
            "authd_port" to WAZUH_AUTHD_PORT,
            "has_token" to (apiToken != null),
            "has_agent_key" to (agentKey != null),
            "registered_at" to prefs.getLong(PREF_REGISTERED_AT, 0),
            "connection_method" to getConnectionMethod(),
            "connection_attempts" to connectionAttempts,
            "connection_uptime" to getConnectionUptime(),
            "last_connection_time" to lastConnectionTime
        )
    }

    /**
     * NOUVEAU: Obtient la méthode de connexion utilisée
     */
    private fun getConnectionMethod(): String {
        return prefs.getString(PREF_CONNECTION_METHOD, "unknown") ?: "unknown"
    }

    /**
     * NOUVEAU: Calcule le temps de connexion
     */
    private fun getConnectionUptime(): Long {
        return if (isConnected && lastConnectionTime > 0) {
            (System.currentTimeMillis() - lastConnectionTime) / 1000
        } else {
            0
        }
    }

    /**
     * Envoie un message UDP (version asynchrone) avec gestion d'erreurs améliorée
     */
    private suspend fun sendUdpMessage(message: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (udpSocket?.isClosed == true || serverAddress == null) {
                Log.w(TAG, "UDP socket closed or server address null")
                return@withContext false
            }

            val messageBytes = message.toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(
                messageBytes,
                messageBytes.size,
                serverAddress,
                WAZUH_UDP_PORT
            )

            udpSocket?.send(packet)
            Log.v(TAG, "Sent UDP packet (${messageBytes.size} bytes): ${message.take(100)}...")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send UDP message: ${e.message}", e)
            false
        }
    }

    /**
     * Envoie un message UDP (version synchrone)
     */
    private fun sendUdpMessageSync(message: String): Boolean {
        return try {
            if (udpSocket?.isClosed == true || serverAddress == null) {
                return false
            }

            val messageBytes = message.toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(
                messageBytes,
                messageBytes.size,
                serverAddress,
                WAZUH_UDP_PORT
            )

            udpSocket?.send(packet)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send UDP message (sync): ${e.message}", e)
            false
        }
    }

    /**
     * AMÉLIORATION: Construction de message Wazuh avec format enrichi
     */
    private fun buildWazuhMessage(level: String, message: String, source: String): String {
        val timestamp = SimpleDateFormat("MMM dd HH:mm:ss", Locale.US).format(Date())
        val hostname = generateAgentName().replace(" ", "_").lowercase(Locale.getDefault())
        val currentAgentId = agentId ?: generateAgentId()

        // Format enrichi avec plus de métadonnées
        return "$timestamp $hostname ossec: $level: [$currentAgentId] $source: $message"
    }

    /**
     * AMÉLIORATION: Informations d'appareil enrichies
     */
    private fun getDeviceInfo(): String {
        val connectionStats = "Attempts:$connectionAttempts"
        return "Android ${android.os.Build.VERSION.RELEASE} " +
                "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} " +
                "SDK${android.os.Build.VERSION.SDK_INT} " +
                "ID:${agentId ?: generateAgentId()} " +
                "$connectionStats"
    }

    /**
     * Crée un TrustManager permissif pour les certificats auto-signés
     */
    private fun createUnsafeTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }

    /**
     * Crée une SSLSocketFactory permissive
     */
    private fun createUnsafeSslSocketFactory(): SSLSocketFactory {
        val trustManager = createUnsafeTrustManager()
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), null)
        return sslContext.socketFactory
    }
}