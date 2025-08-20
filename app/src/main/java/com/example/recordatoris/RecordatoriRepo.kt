package com.example.recordatoris

import kotlinx.coroutines.flow.map

class RecordatoriRepo(private val dao: RecordatoriDao) {
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<Recordatori>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun updateHourById(id: Int) = dao.updateHourById(id)

    suspend fun getById(id: Int): Recordatori {
        return dao.getById(id).toDomain()
    }
    suspend fun getAll(): MutableList<Recordatori> =
        dao.getAll().map { it.toDomain() }.toMutableList()

    suspend fun add(item: Recordatori): Int =
        dao.insert(item.toEntity()).toInt()

    suspend fun deleteById(id: Int) = dao.deleteById(id)
}