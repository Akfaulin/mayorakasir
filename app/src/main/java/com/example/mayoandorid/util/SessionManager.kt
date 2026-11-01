package com.example.mayoandorid.util

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val pref: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = pref.edit()

    companion object {
        private const val PREF_NAME = "mayora_courier_pref"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME = "user_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_ROLE = "user_role"
        private const val KEY_BASE_URL = "api_base_url"

        const val DEFAULT_BASE_URL = "http://192.168.0.105:8000/api/"
    }

    fun saveAuth(token: String, userId: Long, name: String, email: String, role: String) {
        editor.putString(KEY_TOKEN, token)
        editor.putLong(KEY_USER_ID, userId)
        editor.putString(KEY_NAME, name)
        editor.putString(KEY_EMAIL, email)
        editor.putString(KEY_ROLE, role)
        editor.apply()
    }

    fun isLoggedIn(): Boolean = pref.getString(KEY_TOKEN, null) != null

    fun getToken(): String = pref.getString(KEY_TOKEN, "") ?: ""

    fun getUserName(): String = pref.getString(KEY_NAME, "Kurir Mayora") ?: "Kurir Mayora"

    fun getUserEmail(): String = pref.getString(KEY_EMAIL, "kurir@mayora.com") ?: "kurir@mayora.com"

    fun getUserRole(): String = pref.getString(KEY_ROLE, "kurir") ?: "kurir"

    fun getBaseUrl(): String {
        val saved = pref.getString(KEY_BASE_URL, null)
        if (saved == null || saved.contains("10.0.2.2") || saved.contains("192.168.100.110") || saved.contains("127.0.0.1") || saved.contains("localhost")) {
            return DEFAULT_BASE_URL
        }
        return saved
    }

    fun setBaseUrl(url: String) {
        var cleanUrl = url.trim()
        if (!cleanUrl.endsWith("/")) {
            cleanUrl += "/"
        }
        editor.putString(KEY_BASE_URL, cleanUrl)
        editor.apply()
    }

    fun logout() {
        editor.remove(KEY_TOKEN)
        editor.remove(KEY_USER_ID)
        editor.remove(KEY_NAME)
        editor.remove(KEY_EMAIL)
        editor.remove(KEY_ROLE)
        editor.apply()
    }
}