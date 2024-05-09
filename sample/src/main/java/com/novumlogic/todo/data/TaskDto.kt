package com.novumlogic.todo.data

import com.example.supabaseofflinesupport.BaseRemoteEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * TaskDto represents the task table in supabase
 * @SerialName tag is used to map the column name to the property name
 * We need to assign lastUpdatedTimestamp the SerialName "timestamp" as it is hardcoded in SyncManager
 * */
@Serializable
data class TaskDto(
    override val id: Int,
    val name: String,
    val category_id: Int,
    val emoji: String,
    @SerialName("is_complete") val isComplete: Boolean,
    val date: String,
    val priority_id: Int,
    @SerialName("timestamp")override var lastUpdatedTimestamp: Long,
    @SerialName("is_delete") val isDelete: Boolean
): BaseRemoteEntity()
