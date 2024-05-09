package com.novumlogic.todo.data

import com.novumlogic.todo.data.local.Category
import io.github.jan.supabase.postgrest.postgrest

class CategoryRemoteDataSource {
    private val TAG = javaClass.simpleName
    private val supabase = SupabaseModule.provideSupabaseClient()

    suspend fun getAll(): Result<List<CategoryDto>>{
        return try {
            val list = supabase.postgrest.from("categories").select().decodeList<CategoryDto>()
            Result.Success(list)
        }catch (ex:Exception){
            Result.Failure(ex)
        }
    }

    suspend fun insert(category: CategoryDto): Result<CategoryDto>{
        return try {
            val c = supabase.postgrest.from("categories").insert(category){
                select()
            }.decodeSingle<CategoryDto>()
            Result.Success(c)
        }catch (ex: Exception){
            Result.Failure(ex)
        }
    }

    suspend fun insert(categoryList: List<CategoryDto>): Result<Boolean>{
        return try {
            supabase.postgrest.from("categories").insert(categoryList)
            Result.Success(true)
        }catch (ex: Exception){
            Result.Failure(ex)
        }
    }

    suspend fun upsertCategories(list: List<CategoryDto>): Result<Boolean> {
        return try {
            supabase.postgrest.from("categories").upsert(list)
            Result.Success(true)
        }catch (ex: Exception){
            Result.Failure(ex)
        }
    }

    suspend fun update(category: CategoryDto): Result<Boolean> {
        return try {
            supabase.postgrest.from("categories").update(category){
                filter { CategoryDto::name eq category.name }
            }
            Result.Success(true)
        }catch (ex: Exception){
            Result.Failure(ex)
        }

    }

    suspend fun getRemoteChanges(timestamp: Long): Result<List<CategoryDto>> {
        return try {
            val list = supabase.postgrest.from("categories").select { filter { CategoryDto::lastUpdatedTimestamp gt timestamp } }.decodeList<CategoryDto>()
            Result.Success(list)
        }catch (ex: Exception){
            Result.Failure(ex)
        }

    }


}