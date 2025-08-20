package com.example.recordatoris

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RecordatoriEntity::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun recordatoriDao(): RecordatoriDao
}