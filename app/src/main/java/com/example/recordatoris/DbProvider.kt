package com.example.recordatoris

import android.content.Context
import androidx.room.Room

object DbProvider {
    @Volatile private var INSTANCE: AppDb? = null
    fun get(context: Context): AppDb =
        INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDb::class.java,
                "recordatoris.db"
            ).build().also { INSTANCE = it }
        }
}