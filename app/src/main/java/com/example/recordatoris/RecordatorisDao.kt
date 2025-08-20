package com.example.recordatoris

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId



@Dao
interface RecordatoriDao {
    @Query("SELECT * FROM recordatoris ORDER BY dateMillis ASC")
    suspend fun getAll(): List<RecordatoriEntity>

    @Query("SELECT * FROM recordatoris where id = :id")
    suspend fun getById(id: Int): RecordatoriEntity

    @Update
    suspend fun update(item: RecordatoriEntity): Int
    @Transaction
    suspend fun updateHourById(id: Int) {
        val e = getById(id)             // make a @Query("SELECT * FROM … WHERE id=:id")
        val zone = ZoneId.systemDefault()
        val ldt = LocalDateTime.now().plusHours(1)
        update(
            e.copy(
                dateMillis = ldt.atZone(zone).toInstant().toEpochMilli(),
                timeIso = ldt.toLocalTime().toString()
            )
        )
    }


    @Query("SELECT * FROM recordatoris ORDER BY dateMillis ASC")
    fun observeAll(): Flow<List<RecordatoriEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RecordatoriEntity): Long

    @Query("DELETE FROM recordatoris WHERE id = :id")
    suspend fun deleteById(id: Int)
}