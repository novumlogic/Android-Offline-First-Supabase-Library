package com.novumlogic.todo.data

import androidx.lifecycle.MutableLiveData
import io.github.jan.supabase.postgrest.postgrest


class TaskRemoteDataSource {
    private val TAG = javaClass.simpleName
    private val supabase = SupabaseModule.provideSupabaseClient()
    private val _networkConnected = MutableLiveData<Boolean>()
//    val networkConnected: LiveData<Boolean> = _networkConnected


    suspend fun insert(taskDto: TaskDto): Result<TaskDto>{
        return try {
            val task = supabase.postgrest.from("task").insert(
//                mapOf(
//                    "id" to taskDto.id,
//                    "name" to taskDto.name,
//                    "category" to taskDto.category,
//                    "emoji" to taskDto.emoji,
//                    "date" to taskDto.date
//                )
                taskDto
            ){
                select()
            }.decodeSingle<TaskDto>()

            Result.Success(task)
        }catch (ex: Exception){
            Result.Failure(ex)
        }
    }

    suspend fun getNextLatestId(android_id: String): Result<Long>{
        return try {
            val count = supabase.postgrest.from("task").select{
                filter { eq("android_id",android_id) }
            }.decodeList<TaskDto>().size.toLong().plus(1)

            Result.Success(count)

        }catch (ex: Exception){
            Result.Failure(ex)
        }
    }
    suspend fun getAll(): Result<List<TaskDto>> {
        return try{
            val list = supabase.postgrest.from("task").select().decodeList<TaskDto>()
            Result.Success(list)
        }catch (ex: Exception){
            Result.Failure(ex)
        }
    }

    suspend fun updateTask(taskDto: TaskDto): Result<Boolean> {
        return try{
            supabase.postgrest.from("task").update(taskDto){
                filter { TaskDto::id eq taskDto.id }
            }
            Result.Success(true)
        }catch (ex: Exception){
            Result.Failure(ex)
        }
    }

    suspend fun upsertTask(task : TaskDto): Result<Boolean> {
        return try{
            val res =  supabase.postgrest.from("task").upsert(task){select()}.decodeSingle<TaskDto>()
            Result.Success(true)
        }catch (ex: Exception){
            Result.Failure(ex)
        }
    }

    suspend fun getRemoteChanges(timestamp: Long): Result<List<TaskDto>> {
        return try {
            val list = supabase.postgrest.from("task").select {
                filter {
                    TaskDto::lastUpdatedTimestamp gt timestamp
//                    TaskDto::is_delete eq false
                }
            }.decodeList<TaskDto>()
            Result.Success(list)

        }catch (ex: Exception){
            Result.Failure(ex)
        }

    }

    suspend fun upsertTaskList(list: List<TaskDto>): Result<Boolean>{
        return try {
            supabase.postgrest.from("task").upsert(list)
            Result.Success(true)
        }catch (ex: Exception){
            Result.Failure(ex)
        }

    }

}