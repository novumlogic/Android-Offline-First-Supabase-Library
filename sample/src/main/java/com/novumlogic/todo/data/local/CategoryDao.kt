package com.novumlogic.todo.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.supabaseofflinesupport.GenericDao

@Dao
interface CategoryDao: GenericDao<Category> {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg category: Category): Array<Long>


//    @Query("Select * from categories")
//    suspend fun getCategories(): List<Category>
    @Query("Select * from categories")
    fun getCategories(): LiveData<List<Category>>
    @Query("select * from categories where offlineFieldOpType != 0")
    suspend fun getLocalChanges(): List<Category>?

    @Query("update categories set offlineFieldOpType = :crud where name in (:nameList)")
    suspend fun setCrudByNames(nameList: List<String>, crud: Int): Int
    @Query("update categories set offlineFieldOpType = :crud where id = :id")
    suspend fun setCrudById(id: Int, crud: Int): Int
    @Query("select * from categories where name = :name")
    suspend fun getCategoryByName(name: String): Category?

    @Query("select id from categories where name = :name")
    suspend fun getCategoryIdByName(name: String): Int?
    @Query("select id from categories order by id desc limit 1 ")
    suspend fun getLastCategoryId(): Int?

}