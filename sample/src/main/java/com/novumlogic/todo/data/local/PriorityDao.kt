package com.novumlogic.todo.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.supabaseofflinesupport.GenericDao

@Dao
interface PriorityDao: GenericDao<Priority> {
    @Insert
    suspend fun insert(vararg priority: Priority): Array<Long>
    @Query("select * from priorities")
    fun getAll(): LiveData<List<Priority>>

    @Query("select id from priorities where priority = :priority")
    suspend fun getPriorityId(priority: Int): Int?
}
