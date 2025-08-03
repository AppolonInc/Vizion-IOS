package com.vizion.security.data.local.datastore

import androidx.core.content.edit
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestionnaire de préférences sécurisées
 * 
 * Cette classe encapsule l'utilisation d'EncryptedSharedPreferences pour
 * stocker de manière sécurisée les données sensibles comme :
 * - Tokens d'authentification JWT
 * - Informations d'abonnement
 * - Données utilisateur sensibles
 * - Cache de synchronisation
 * 
 * Sécurité :
 * - Chiffrement AES-256 pour les clés et valeurs
 * - Utilise Android Keystore pour la gestion des clés
 * - Protection contre l'extraction des données même avec root
 * - Nettoyage automatique en cas de compromission
 */
@Singleton
class SecurePreferences @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val PREFS_NAME = "vizion_secure_prefs"
        private const val TAG = "SecurePreferences"
    }
    
    // Clé maître pour le chiffrement, générée et stockée dans Android Keystore
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    // Instance des préférences chiffrées
    private val sharedPreferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * Stocke une chaîne de caractères de manière sécurisée
     * 
     * @param key Clé de stockage (sera chiffrée)
     * @param value Valeur à stocker (sera chiffrée)
     */
    fun putString(key: String, value: String) {
        sharedPreferences.edit {
            putString(key, value)
        }
    }
    
    /**
     * Récupère une chaîne de caractères stockée de manière sécurisée
     * 
     * @param key Clé de récupération
     * @param defaultValue Valeur par défaut si la clé n'existe pas
     * @return Valeur déchiffrée ou valeur par défaut
     */
    fun getString(key: String, defaultValue: String? = null): String? {
        return sharedPreferences.getString(key, defaultValue)
    }
    
    /**
     * Stocke un entier long de manière sécurisée
     * 
     * @param key Clé de stockage
     * @param value Valeur à stocker
     */
    fun putLong(key: String, value: Long) {
        sharedPreferences.edit {
            putLong(key, value)
        }
    }
    
    /**
     * Récupère un entier long stocké de manière sécurisée
     * 
     * @param key Clé de récupération
     * @param defaultValue Valeur par défaut
     * @return Valeur stockée ou valeur par défaut
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }
    
    /**
     * Stocke un booléen de manière sécurisée
     * 
     * @param key Clé de stockage
     * @param value Valeur à stocker
     */
    fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit {
            putBoolean(key, value)
        }
    }
    
    /**
     * Récupère un booléen stocké de manière sécurisée
     * 
     * @param key Clé de récupération
     * @param defaultValue Valeur par défaut
     * @return Valeur stockée ou valeur par défaut
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }
    
    /**
     * Stocke un entier de manière sécurisée
     * 
     * @param key Clé de stockage
     * @param value Valeur à stocker
     */
    fun putInt(key: String, value: Int) {
        sharedPreferences.edit {
            putInt(key, value)
        }
    }
    
    /**
     * Récupère un entier stocké de manière sécurisée
     * 
     * @param key Clé de récupération
     * @param defaultValue Valeur par défaut
     * @return Valeur stockée ou valeur par défaut
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }
    
    /**
     * Vérifie si une clé existe dans le stockage sécurisé
     * 
     * @param key Clé à vérifier
     * @return true si la clé existe
     */
    fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }
    
    /**
     * Supprime une clé spécifique du stockage sécurisé
     * 
     * @param key Clé à supprimer
     */
    fun remove(key: String) {
        sharedPreferences.edit {
            remove(key)
        }
    }
    
    /**
     * Efface complètement toutes les données du stockage sécurisé
     * 
     * Cette méthode est utilisée lors de la déconnexion ou en cas
     * de compromission de sécurité détectée.
     */
    fun clear() {
        sharedPreferences.edit {
            clear()
        }
    }
    
    /**
     * Obtient toutes les clés stockées (pour debug uniquement)
     * 
     * @return Set des clés disponibles
     */
    fun getAllKeys(): Set<String> {
        return sharedPreferences.all.keys
    }
    
    /**
     * Vérifie l'intégrité du stockage sécurisé
     * 
     * Cette méthode peut être utilisée pour détecter une éventuelle
     * compromission du stockage chiffré.
     * 
     * @return true si l'intégrité est préservée
     */
    fun checkIntegrity(): Boolean {
        return try {
            // Test simple : essayer de lire/écrire une valeur de test
            val testKey = "integrity_test"
            val testValue = "test_${System.currentTimeMillis()}"
            
            putString(testKey, testValue)
            val retrievedValue = getString(testKey)
            remove(testKey)
            
            testValue == retrievedValue
        } catch (e: Exception) {
            false
        }
    }
}