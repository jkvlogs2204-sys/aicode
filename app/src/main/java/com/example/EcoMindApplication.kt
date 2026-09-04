package com.example

import android.app.Application
import android.util.Log
import com.example.ai.ChatGptEcoAssistant
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class EcoMindApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ChatGptEcoAssistant.initialize(this)
        initFirebase()
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                try {
                    val app = FirebaseApp.initializeApp(this)
                    if (app != null) {
                        Log.d("EcoMindApplication", "FirebaseApp initialized with default google-services")
                    } else {
                        initFallbackFirebase()
                    }
                } catch (e: Exception) {
                    Log.w("EcoMindApplication", "Default FirebaseApp init failed: ${e.message}. Using fallback options.")
                    initFallbackFirebase()
                }
            }
        } catch (e: Exception) {
            Log.e("EcoMindApplication", "Failed to initialize FirebaseApp: ${e.message}")
        }
    }

    private fun initFallbackFirebase() {
        try {
            val fallbackOptions = FirebaseOptions.Builder()
                .setApplicationId("1:1082257186609:android:com.aistudio.ecomind.ai")
                .setProjectId("ecomind-app")
                .setApiKey("AIzaSyFallbackKeyForLocalDev123456789")
                .build()
            FirebaseApp.initializeApp(this, fallbackOptions)
            Log.d("EcoMindApplication", "FirebaseApp initialized with fallback options")
        } catch (e: Exception) {
            Log.e("EcoMindApplication", "Fallback FirebaseApp init failed: ${e.message}")
        }
    }
}

