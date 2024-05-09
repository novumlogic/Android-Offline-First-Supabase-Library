package com.novumlogic.todo.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.supabaseofflinesupport.SyncManager
import com.novumlogic.todo.data.local.Category
import com.novumlogic.todo.data.local.CategoryLocalDataSource
import com.novumlogic.todo.data.local.OfflineCrudType
import com.novumlogic.todo.data.local.PriorityLocalDataSource
import com.novumlogic.todo.data.local.Task
import com.novumlogic.todo.data.local.TaskLocalDataSource
import com.novumlogic.todo.util.toDto
import com.novumlogic.todo.util.toEntity
import kotlin.system.measureTimeMillis

class TaskRepository(
    private val context: Context,
    private val taskLocalDataSource: TaskLocalDataSource,
    private val taskRemoteDataSource: TaskRemoteDataSource,
    private val categoryLocalDataSource: CategoryLocalDataSource,
    private val categoryRemoteDataSource: CategoryRemoteDataSource,
    private val priorityLocalDataSource: PriorityLocalDataSource,
) {

    private val TAG = javaClass.simpleName
    private val syncManager = SyncManager(context, SupabaseModule.provideSupabaseClient())
    val networkConnected = syncManager.observeNetwork()

    init {
        Log.d(
            TAG,
            "is networkConnected initizaled: $networkConnected and value = ${networkConnected.value} "
        )
    }

    fun observeTasks(date: String) = taskLocalDataSource.getTasksWithCategoryAndPriorityNames(date)

    suspend fun insertTask(task: Task): Result<Task> {
        Log.d(TAG, "insertTask: value of networkconnected -> ${networkConnected.value}")

        //perform operation based on network connection as local copy of remote data is present
        if (networkConnected.value!!) {
            //insert in local as well as remote
            taskLocalDataSource.insert(task)
            val result = taskRemoteDataSource.insert(task.toDto())
            return if (result is Result.Success) {
                //if remote insertion is successful then reset the crud to 0
                taskLocalDataSource.setCrudById(task.id, 0)
                Result.Success(task)
            } else {
                //remote insertion has failed so let the crud remain 1 in local to push it when synchronizing
                Result.Failure((result as Result.Failure).exception)
            }
        } else {
            //user is offline
            taskLocalDataSource.insert(task)
            return Result.Success(task)
        }

    }

    private fun isSyncFirstRun(tableName: String) =
        syncManager.getLastSyncedTimeStamp(tableName) == 0L

    suspend fun updateTask(task: Task): Boolean {
        if (isSyncFirstRun("tasks")) {
            if (task.offlineFieldOpType == OfflineCrudType.UPDATE.ordinal) task.offlineFieldOpType =
                OfflineCrudType.INSERT.ordinal
        }
        //updating the task in local table first so that changes are reflected fast in the ui
        val count = taskLocalDataSource.updateTask(task)
        if (networkConnected.value!!) {
            val result = taskRemoteDataSource.updateTask(task.toDto())
            if (result.succeeded) {
                //if the task is updated in remote we can set crud to 0
                taskLocalDataSource.setCrudById(task.id, 0)
            } else {
                //let the crud remain 2
                Log.d(TAG, "updateTask: $result")
            }
        }
        return count > 0
    }

    suspend fun forceUpdateTasks() {
        val syncTime = measureTimeMillis {
            syncManager.syncToSupabase(
                "tasks",
                taskLocalDataSource.taskDao,
                "task",
                { it.toEntity(0) },
                { it.toDto() },
                TaskDto.serializer(),
                System.currentTimeMillis()
            )
        }
        Log.d(TAG, "Time taken in task table sync: $syncTime ms ")
    }

    suspend fun forceUpdateCategories() {
        val syncTime = measureTimeMillis {
            syncManager.syncToSupabase(
                "categories",
                categoryLocalDataSource.categoryDao,
                "categories",
                { it.toEntity(0) },
                { it.toDto() },
                CategoryDto.serializer(),
                System.currentTimeMillis()
            )
        }
        Log.d(TAG, "Time taken in categories table sync: $syncTime ms")
    }

    suspend fun getLatestLocalId(): Int {
        return (taskLocalDataSource.getLastLocalId() ?: 0) + 1
    }

    fun observeCategories() = categoryLocalDataSource.getCategories()

    suspend fun getCategoryId(name: String) = categoryLocalDataSource.getCategoryIdByName(name)
    suspend fun insertCategory(category: Category): Result<Category> {
        //perform operation based on network connection as local copy of remote data is present
        if (networkConnected.value!!) {
            //insert in remote as well as local
            val result = categoryRemoteDataSource.insert(category.toDto())
            categoryLocalDataSource.insert(category)
            return if (result is Result.Success) {
                //if remote insertion is successful then reset the crud to 0
                categoryLocalDataSource.setCrudById(category.id, 0)
                Result.Success(category)
            } else {
                //remote insertion has failed so let the crud remain 1 in local to push it when synchronizing
                Result.Failure((result as Result.Failure).exception)
            }
        } else {
            //user is offline
            categoryLocalDataSource.insert(category)
            return Result.Success(category)
        }

    }


    fun getPriorities() = priorityLocalDataSource.getPriorities()
    suspend fun getPriorityId(priority: Int) = priorityLocalDataSource.getPriorityId(priority)
    suspend fun getLatestCategoryId(): Int {
        return (categoryLocalDataSource.getLastCategoryId() ?: 0) + 1
    }

}
