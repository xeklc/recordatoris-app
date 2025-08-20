package com.example.recordatoris


import java.time.LocalTime
import java.util.Date
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordatoris")
data class RecordatoriEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dateMillis: Long,   // Date -> millis
    val timeIso: String,    // LocalTime -> "HH:mm:ss" (ISO)
    val recurring: Boolean
)
data class Recordatori(
    var id: Int,
    var title: String,
    var date: Date,
    var hour: LocalTime,
    var recurring: Boolean
)
fun RecordatoriEntity.toDomain() = Recordatori(
    id, title, Date(dateMillis), LocalTime.parse(timeIso), recurring
)
fun Recordatori.toEntity() = RecordatoriEntity(
    id = if (id == 0) 0 else id,
    title = title,
    dateMillis = date.time,
    timeIso = hour.toString(),
    recurring = recurring
)
