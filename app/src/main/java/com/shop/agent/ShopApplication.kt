package com.shop.agent

import android.app.Application

class ShopApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Moteur métier en Kotlin pur (TailorEngine) : aucune initialisation requise.
    }
}
