package com.novumlogic.todo.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.supabaseofflinesupport.GenericDao

@Dao
interface TaskDao: GenericDao<Task> {
    @Query("select * from tasks where is_complete = :complete")
    suspend fun getTasks(complete: Boolean): List<Task>

    @Query("select * from tasks where date = :date and is_delete = :isDelete")
    fun getTasksByDate(date: String,isDelete: Boolean = false): LiveData<List<Task>>


    @Query(
        """
        SELECT tasks.*, categories.name AS category_name, priorities.priority AS priority
        FROM tasks
        INNER JOIN categories ON tasks.category_id = categories.id
        INNER JOIN priorities ON tasks.priority_id = priorities.id
        where date = :date and is_delete = :isDelete
        """
    )
    fun getTasksWithCategoryAndPriorityNames(date: String,isDelete: Boolean = false): LiveData<List<TaskWithCategoryAndPriorityNames>>
    @Insert
    suspend fun insert(vararg task: Task): Array<Long>
    @Update
    suspend fun updateTask(task: Task): Int

    @Query("select * from tasks where id = :id")
    suspend fun getTaskById(id: Int): Task?
    @Query("SELECT id FROM tasks ORDER BY id DESC LIMIT 1")
    suspend fun getLastLocalId(): Int?
    @Query("update tasks set id = :newId where id = :oldId")
    suspend fun updateTaskIdAfterSync(newId: Long, oldId: Long): Int
    @Query("update tasks set offlineFieldOpType = :crud where id = :id ")
    suspend fun setCrudById(id: Int, crud: Int): Int
    @Query("update tasks set offlineFieldOpType = :crud where id in (:idList) ")
    suspend fun setCrudById(idList: List<Int>, crud: Int): Int
    @Query("select * from tasks where offlineFieldOpType != 0 ")
    suspend fun getLocalChanges(): List<Task>?

}
