package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PinManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("offline_pin_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN = "secure_pin"
    }

    fun isPinSet(): Boolean {
        return prefs.getString(KEY_PIN, null) != null
    }

    fun savePin(pin: String): Boolean {
        if (pin.length != 4) return false
        prefs.edit().putString(KEY_PIN, pin).apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val saved = prefs.getString(KEY_PIN, null)
        return saved != null && saved == pin
    }

    fun clearPin() {
        prefs.edit().remove(KEY_PIN).apply()
    }
}
