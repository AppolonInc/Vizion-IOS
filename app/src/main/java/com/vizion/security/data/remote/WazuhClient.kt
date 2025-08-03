package com.vizion.security.data.remote

import android.util.Log
import com.vizion.security.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WazuhClient @Inject constructor() {
    
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var isConnected = false
    
    companion object {
        private const val TAG = "WazuhClient"
        private val WAZUH_SERVER = BuildConfig.WAZUH_SERVER
        private val WAZUH_PORT = BuildConfig.WAZUH_PORT.toInt()
        private const val CONNECTION_TIMEOUT = 10000 // 10 seconds
        private const val AGENT_ID = "android-001"
    }
    
    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isConnected) {
                return@withContext true
            }
            
            Log.d(TAG, "Connecting to Wazuh server: $WAZUH_SERVER:$WAZUH_PORT")
            
            socket = Socket().apply {
                soTimeout = CONNECTION_TIMEOUT
                connect(java.net.InetSocketAddress(WAZUH_SERVER, WAZUH_PORT), CONNECTION_TIMEOUT)
            }
            
            writer = BufferedWriter(OutputStreamWriter(socket?.getOutputStream()))
            isConnected = true
            
            // Send initial connection message
            sendLog("INFO", "Wazuh agent connected", "WazuhAgent")
            
            Log.d(TAG, "Successfully connected to Wazuh server")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Wazuh server", e)
            disconnect()
            false
        }
    }
    
    suspend fun sendLog(level: String, message: String, source: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isConnected) {
                if (!connect()) {
                    return@withContext false
                }
            }
            
            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
            val logMessage = buildWazuhMessage(timestamp, level, message, source)
            
            writer?.apply {
                write(logMessage)
                newLine()
                flush()
            }
            
            Log.d(TAG, "Sent log to Wazuh: $logMessage")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send log to Wazuh", e)
            disconnect()
            false
        }
    }
    
    suspend fun sendHeartbeat(): Boolean = withContext(Dispatchers.IO) {
        sendLog("INFO", "Heartbeat from Android agent", "WazuhAgent")
    }
    
    fun disconnect() {
        try {
            writer?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing Wazuh connection", e)
        } finally {
            writer = null
            socket = null
            isConnected = false
            Log.d(TAG, "Disconnected from Wazuh server")
        }
    }
    
    fun isConnected(): Boolean = isConnected && socket?.isConnected == true
    
    private fun buildWazuhMessage(timestamp: String, level: String, message: String, source: String): String {
        return """
            {
                "timestamp": "$timestamp",
                "agent": {
                    "id": "$AGENT_ID",
                    "name": "VizionSecurity-Android",
                    "type": "android"
                },
                "log": {
                    "level": "$level",
                    "message": "$message",
                    "source": "$source"
                },
                "location": "android-security-monitoring"
            }
        """.trimIndent().replace("\n", "").replace(" ", "")
    }
}