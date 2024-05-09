package com.example.supabaseofflinesupport

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlin.reflect.KMutableProperty

/***Generic DAO interface for performing database operations.*/
interface GenericDao<T> {

    /**
     * Executes a raw SQL query and returns a list of entities.*
     * @param query The SQL query to execute.
     * @return A list of entities.
     */
    @RawQuery
    suspend fun query(query: SupportSQLiteQuery): List<T>

    /**
     * Updates an entity in the database.
     *
     * @param entity The entity to update.
     * @return The number of rows affected.
     */
    @Update
    suspend fun update(entity: T): Int

    /**
     * Executes a raw SQL query to update entities.
     *
     * @param query The SQL query to execute.
     * @return The number of rows affected.
     */
    @RawQuery
    suspend fun update(query: SupportSQLiteQuery): Int

    /**
     * Deletes an entity from the database.
     *
     * @param entity The entity to delete.
     * @return The number of rows affected.
     */
    @Delete
    suspend fun delete(entity: T): Int

    /**
     * Inserts an entityinto the database.
     *
     * @param entity The entity to insert.
     * @return The row ID of the inserted entity.
     */
    @Insert
    suspend fun insert(entity: T): Long

    /**
     * Executes a raw SQL query to delete entities.
     *
     * @param query The SQL query to execute.
     * @return The number of rows affected.
     */
    @RawQuery
    suspend fun delete(query: SupportSQLiteQuery): Int
}