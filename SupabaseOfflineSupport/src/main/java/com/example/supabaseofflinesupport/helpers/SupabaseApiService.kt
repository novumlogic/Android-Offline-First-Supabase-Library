package com.example.supabaseofflinesupport.helpers

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
/*** Interface for the Supabase API service.
 */
interface SupabaseApiService {

    /**
     * Inserts a new record into the specified table.
     *
     * @param tableName The name of the table to insert the record into.
     * @param data The data to insert.
     * @return A Response object containing the result of the network operation.
     */

    @Headers("Content-Type: application/json", "Prefer: return=minimal")
    @POST("/rest/v1/{table}")
    suspend fun insert(
        @Path("table") tableName: String,
        @Body data: RequestBody
    ): Response<Unit>

    /**
     * Inserts a new record into the specified table and returns the ID of the inserted record.
     *
     * @param tableName The name of the table to insert the record into.
     * @param data The table data to insert.
     * @return A Response object containing the ID of the inserted record.
     */
    @Headers("Content-Type: application/json", "Prefer: return=representation")
    @POST("/rest/v1/{table}?select=id")
    suspend fun insertReturnId(
        @Path("table") tableName: String,
        @Body data: RequestBody
    ): Response<ResponseBody>

    /**
     * Updates an existing record in the specified table.
     *
     * @param tableName The name of the table to update the record in.
     * @param id The ID of the record to update.
     * @param data The table data to update.
     * @return A Response object containing the result of the network operation.
     */
    @Headers("Content-Type: application/json", "Prefer: return=minimal")
    @PATCH("/rest/v1/{table}")
    suspend fun update(
        @Path("table") tableName: String,
        @Query("id") @eq id: Int,
        @Body data: RequestBody
    ): Response<Unit>

    /**
     * Upserts a record into the specified table. If the record already exists, it will be updated. Otherwise, a new record will be inserted.
     *
     * @param tableName The name of the table to upsert the record into.
     * @param data The data to upsert.
     * @return A Response object containing the result of the operation.
     */
    @Headers("Content-Type: application/json", "Prefer: resolution=merge-duplicates")
    @POST("/rest/v1/{table}")
    suspend fun upsert(
        @Path("table") tableName: String,
        @Body data: RequestBody
    ): Response<Unit>

    /**
     * Upserts a record into the specified table and returns the ID of the upserted record.
     *
     * @param tableName The name of the table to upsert the record into.
     * @param data The data to upsert.
     * @return A Response object containing the ID of the upserted record.
     */
    @Headers("Content-Type: application/json", "Prefer: resolution=merge-duplicates, return=representation")
    @POST("/rest/v1/{table}?select=id")
    suspend fun upsertReturnId(
        @Path("table") tableName: String,
        @Body data: RequestBody
    ): Response<ResponseBody>


    /**
     * Deletes a record from the specified table.
     *
     * @param authHeader The authorization header.
     * @param tableName The name of the table to delete the record from.
     * @param id The ID of the record to delete.
     * @return A Response object containing the result of the operation.
     */
    @Headers("Content-Type: application/json", "Prefer: return=representation")
    @DELETE("/rest/v1/{table}")
    suspend fun delete(
        @Header("Authorization") authHeader: String,
        @Path("table") tableName: String,
        @Query("id") id: Int
    ): Response<Unit>
}