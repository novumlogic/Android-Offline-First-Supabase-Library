package com.example.supabaseofflinesupport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Base class that needs to be extends by all remote Dtos/Entities that needs to participate in synchronization
 *
 * @property id Primary key for remote table
 * @property lastUpdatedTimestamp Contains milliseconds as timestamp to compare
 * with local row's timestamp in case of conflict and perform synchronization
 *
 * */
@Serializable
abstract class BaseRemoteEntity {
    abstract val id: Int
    @SerialName("timestamp")abstract val lastUpdatedTimestamp: Long
}
