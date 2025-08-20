package com.example.recordatoris


import android.content.Context

suspend fun getFakeTodo(context: Context): MutableList<Recordatori> {
    val repo = RecordatoriRepo(DbProvider.get(context).recordatoriDao())
    return repo.getAll()
}