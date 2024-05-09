package com.novumlogic.todo.data

import com.example.supabaseofflinesupport.BaseRemoteEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val name: String,
    @SerialName("timestamp") override var lastUpdatedTimestamp: Long,
    override val id: Int
) : BaseRemoteEntity()