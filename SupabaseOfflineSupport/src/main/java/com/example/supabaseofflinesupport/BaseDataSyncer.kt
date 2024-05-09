package com.example.supabaseofflinesupport

import androidx.room.RoomDatabase
import com.google.gson.Gson
import io.github.jan.supabase.SupabaseClient
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy

/*** Base class for synchronizing data between a local database and Supabase.
 *
 * This base class needs to be extended to provide base logic for synchronization, including conflict resolution and handling of deleted data.
 *
 * @param supabaseClient The Supabase client instance.
 */
abstract class BaseDataSyncer(private val supabaseClient: SupabaseClient){

    /**
     * Syncs the local table with the provided remote table
     *
     * Applies the synchronization algorithm uses Last-write wins Conflict resolution strategy
     *
     * @param T The type of the local entity, should be subclass of BaseSyncableEntity
     * @param RDto The type of the remote entity, should be subclass of BaseRemoteEntity
     * @param localTable The name of the local table needs to by synced
     * @param localDao  Dao of that local table required to perform CRUD operations
     * @param remoteTable The name of the remote table needs to be synced
     * @param toMap A function that converts the remote DTO/Entity to the local DTO/Entity
     * @param toMapWithoutLocal A function that converts the local DTO/Entity to the remote DTO/Entity
     * @param serializer Serializer of remote entity required to perform decoding logic on Json of the entity received
     * @param currentTimeStamp The current timestamp in milliseconds required to perform synchronization logic
     * */
    abstract suspend fun <T: BaseSyncableEntity,RDto: BaseRemoteEntity> syncToSupabase(
        localTable: String,
        localDao: GenericDao<T>,
        remoteTable: String,
        toMap: (RDto) -> T,
        toMapWithoutLocal: (T) -> RDto,
        serializer: KSerializer<RDto>,
        currentTimeStamp: Long
    )

}
