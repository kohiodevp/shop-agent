package com.shop.agent

import android.app.Application
import androidx.room.Room
import com.shop.agent.data.ShopDb

class ShopApplication : Application() {
    lateinit var db: ShopDb
        private set

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(
            applicationContext,
            ShopDb::class.java,
            "shop.db"
        ).fallbackToDestructiveMigration().build()
    }
}
