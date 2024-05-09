package com.example.supabaseofflinesupport

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.supabaseofflinesupport.helpers.NetworkHelper
import com.example.supabaseofflinesupport.helpers.RetrofitClient
import com.example.supabaseofflinesupport.helpers.RetrofitClient.rClient
import com.example.supabaseofflinesupport.utils.decodeList
import com.example.supabaseofflinesupport.utils.getId
import com.example.supabaseofflinesupport.utils.prepareRequestBodyWithoutId
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.KSerializer

/*** Class responsible for synchronizing data between the local database and Supabase table.
 *
 * @param context The application context.
 * @property supabaseClient The Supabase client instance.
 */
class SyncManager(context: Context, private val supabaseClient: SupabaseClient) :
    BaseDataSyncer(supabaseClient) {
    val TAG = javaClass.simpleName

    private val networkHelper = NetworkHelper(context.applicationContext)

    /***Checks if the network is currently available.*
     * @return True if the network is available, false otherwise.
     */
    fun isNetworkAvailable() = networkHelper.isNetworkAvailable()/*** Observes the network connectivity status.
     *
     * Returns a LiveData object that emits a boolean value indicating the network connectivity status.
     *
     * @return A LiveData object emitting the network connectivity status.
     */
    fun observeNetwork() = networkHelper.getNetworkLiveData()

    init {
        RetrofitClient.setupClient(supabaseClient.supabaseHttpUrl, supabaseClient.supabaseKey)
    }

    private val sharedPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    /*** Retrieves the last synchronized timestamp for thegiven table name.
     *
     * @param tableName The name of the table.
     * @return The lastsynchronized timestamp, or 0 if it has never been synchronized.
     */
    fun getLastSyncedTimeStamp(tableName: String): Long {
        return sharedPreferences.getLong("${tableName}_last_synced_timestamp", 0)
    }

    /***Sets the last synchronized timestamp for the given table name.
     ** @param tableName The name of the table.
     * @param value The timestamp to set.
     */
    private fun setLastSyncedTimeStamp(tableName: String, value: Long) {
        with(sharedPreferences.edit()) {
            putLong("${tableName}_last_synced_timestamp", value)
            apply()
        }
    }

    /**
     * Synchronizes data between a local table and a remote table in Supabase using last-wins conflict resolution strategy algorithm.
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
    override suspend fun <T : BaseSyncableEntity, RDto : BaseRemoteEntity> syncToSupabase(
        localTable: String,
        localDao: GenericDao<T>,
        remoteTable: String,
        toMap: (RDto) -> T,
        toMapWithoutLocal: (T) -> RDto,
        serializer: KSerializer<RDto>,
        currentTimeStamp: Long
    ) {
        if (!networkHelper.isNetworkAvailable()) return

        val lastSyncedTimeStamp = getLastSyncedTimeStamp(localTable)
        val localItems =
            localDao.query(SimpleSQLiteQuery("select * from $localTable where lastUpdatedTimestamp > $lastSyncedTimeStamp"))

        Log.d(TAG, "localItems of $localTable [lastupdatetime > $lastSyncedTimeStamp]: size = ${localItems.size}")

        // (local_id, remote_id) pairs - where after local row is inserted into remote, the local ids are replaced with newly generated remote ids
        val insertedLocalToRemoteIds = mutableMapOf<Int, Int>()

        var remoteItems: List<RDto>? = null
        try {
            remoteItems = supabaseClient.postgrest.from(remoteTable).select().data.decodeList<RDto>(
                serializer
            )
        } catch (ex: Exception) {
            Log.e(TAG, "exception while fetching remote items $ex")
        }

        for (localItem in localItems) {
            var remoteItem: RDto? = null
            try {
                remoteItem = remoteItems?.find { it.id == localItem.id }
            } catch (ex: Exception) {
                Log.e(TAG, "exception for searching remote row for local row = $ex ")
            }

            if (remoteItem != null) {
                when {
                    localItem.lastUpdatedTimestamp == remoteItem.lastUpdatedTimestamp -> {
                        //do nothing both items are same
                    }

                    localItem.lastUpdatedTimestamp > remoteItem.lastUpdatedTimestamp -> {
                        //local data is latest
                        when {
                            (localItem.offlineFieldOpType == OfflineCrudType.INSERT.ordinal) -> {
                                localItem.lastUpdatedTimestamp = currentTimeStamp

                                try {

                                    val generatedRemoteId = rClient.insertReturnId(
                                        remoteTable,
                                        toMapWithoutLocal(localItem).prepareRequestBodyWithoutId(
                                            serializer
                                        )
                                    ).getId()

                                    insertedLocalToRemoteIds[localItem.id] = generatedRemoteId

                                } catch (ex: Exception) {
                                    Log.e(
                                        TAG,
                                        "exception while inserting item from local to remote  = $ex",
                                    )
                                }
                            }

                            (localItem.offlineFieldOpType == OfflineCrudType.UPDATE.ordinal) -> {
                                try {
                                    localItem.lastUpdatedTimestamp = currentTimeStamp
                                    rClient.update(
                                        remoteTable,
                                        remoteItem.id,
                                        toMapWithoutLocal(localItem).prepareRequestBodyWithoutId(
                                            serializer
                                        )
                                    )

                                    localDao.update(SimpleSQLiteQuery("update $localTable set offlineFieldOpType = ${OfflineCrudType.NONE.ordinal}, lastUpdatedTimestamp = ${localItem.lastUpdatedTimestamp} where id = ${localItem.id}"))
                                } catch (ex: Exception) {
                                    Log.e(
                                        TAG,
                                        "exception while updating item from local to remote  = $ex",
                                    )
                                }
                            }

                            (localItem.offlineFieldOpType == OfflineCrudType.DELETE.ordinal) -> {
                                try {
                                    supabaseClient.postgrest.from(remoteTable).delete {
                                        filter { eq(BaseRemoteEntity::id.name, remoteItem.id) }
                                    }

                                    localDao.delete(localItem)
                                } catch (ex: Exception) {
                                    Log.e(
                                        TAG,
                                        "exception while deleting item from local and remote  =  $ex",
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        // remote data is latest
                        // if local item with same id was inserted, it should be considered to be added to remote

                        when {
                            (localItem.offlineFieldOpType == OfflineCrudType.INSERT.ordinal) -> {
                                localItem.lastUpdatedTimestamp = currentTimeStamp
                                try {

                                    val generatedRemoteId = rClient.insertReturnId(
                                        remoteTable,
                                        toMapWithoutLocal(localItem).prepareRequestBodyWithoutId(
                                            serializer
                                        )
                                    ).getId()

                                    insertedLocalToRemoteIds[localItem.id] = generatedRemoteId
                                    //also insert the newly added remote item to local db
                                    try {
                                        localDao.insert(toMap(remoteItem))
                                    } catch (ex: Exception) {
                                        Log.e(
                                            TAG,
                                            "error while inserting latest remote data to local $ex",
                                        )
                                    }

                                } catch (ex: Exception) {
                                    Log.e(
                                        TAG,
                                        "exception while inserting item from local to remote $ex"
                                    )
                                    Log.d(TAG, "localItem = $localItem")
                                    Log.d(TAG, "remoteItem = $remoteItem ")
                                }
                            }

                            (localItem.offlineFieldOpType == OfflineCrudType.DELETE.ordinal) -> {
                                //if local item was deleted, then delete in remote
                                try {
                                    supabaseClient.postgrest.from(remoteTable).delete {
                                        filter {
                                            eq(BaseRemoteEntity::id.name, remoteItem.id)
                                        }
                                    }

                                    localDao.delete(localItem)

                                } catch (ex: Exception) {
                                    Log.e(TAG, "error while deleting item from local to remote $ex")
                                }
                            }

                            else -> {
                                //now update the latest remote data to local db
                                localDao.update(toMap(remoteItem))
                                Log.d(
                                    TAG,
                                    "updating local data with remote data for remote id = ${remoteItem.id}"
                                )
                            }
                        }
                    }
                }
            } else {
                //remote data does not exists, means this local data can be newly inserted
                when {
                    (localItem.offlineFieldOpType == OfflineCrudType.INSERT.ordinal || localItem.offlineFieldOpType == OfflineCrudType.UPDATE.ordinal) -> {
                        localItem.lastUpdatedTimestamp = currentTimeStamp
                        try {
//                            val generatedRemoteId = rClient.upsertReturnId(remoteTable,toMapWithoutLocal(localItem).prepareRequestBody(serializer)).getId()
                            val generatedRemoteId = rClient.insertReturnId(
                                remoteTable,
                                toMapWithoutLocal(localItem).prepareRequestBodyWithoutId(serializer)
                            ).getId()
                            insertedLocalToRemoteIds[localItem.id] = generatedRemoteId

                        } catch (ex: Exception) {
                            Log.e(TAG, "exception while upserting item from local to remote  = $ex")
                            Log.d(TAG, "localItem = $localItem")
                            Log.d(TAG, "remoteItem = $remoteItem")
                        }
                    }

                    (localItem.offlineFieldOpType == OfflineCrudType.DELETE.ordinal) -> {
                        // it was added and deleted from local but never synced with remote
                        // cascade delete from local db on child tables
                        localDao.delete(localItem)
                    }
                }
            }
        }
        // update all old local items with ids generated from adding the items to remote db
        // also update local db to forget insert for this item as it is now synced with remote
        insertedLocalToRemoteIds.forEach { (key, value) ->
            val query =
                "UPDATE $localTable set id = ${value}, offlineFieldOpType = ${OfflineCrudType.NONE.ordinal}, lastUpdatedTimestamp = $currentTimeStamp where id = $key"
            localDao.update(SimpleSQLiteQuery(query))
        }


        if (lastSyncedTimeStamp != 0L) {
            try {
                remoteItems = supabaseClient.postgrest.from(remoteTable).select {
//                    filter { gt(BaseRemoteEntity::lastUpdatedTimestamp.name, lastSyncedTimeStamp) }
                    filter { gt("timestamp", lastSyncedTimeStamp) }
                }.data.decodeList<RDto>(serializer)
            } catch (ex: Exception) {
                Log.e(TAG, "exception while fetching remote items $ex")
            }
        }

        val localItemList = localDao.query(SimpleSQLiteQuery("select * from $localTable"))
        remoteItems?.forEach { remoteItem ->

            val localItem = localItemList.find { it.id == remoteItem.id }

            if (localItem != null) {
                when {
                    (localItem.lastUpdatedTimestamp == remoteItem.lastUpdatedTimestamp) -> {
                        //do nothing, both items are same
                    }

                    (localItem.lastUpdatedTimestamp > remoteItem.lastUpdatedTimestamp) -> {
                        //local data is latest
                        //we are comparing remote data, so for local only update and delete are valid considerations
                        when {
                            (localItem.offlineFieldOpType == OfflineCrudType.UPDATE.ordinal) -> {
                                try {
                                    localItem.lastUpdatedTimestamp = currentTimeStamp
                                    rClient.update(
                                        remoteTable,
                                        remoteItem.id,
                                        toMapWithoutLocal(localItem).prepareRequestBodyWithoutId(
                                            serializer
                                        )
                                    )
                                    localDao.update(SimpleSQLiteQuery("update $localTable set offlineCrudType = ${OfflineCrudType.NONE.ordinal}, lastUpdatedTimestamp = $currentTimeStamp where id = ${localItem.id}"))
                                } catch (ex: Exception) {
                                    Log.e(
                                        TAG,
                                        "error while updating item from local to remote $ex",
                                    )
                                }
                            }

                            (localItem.offlineFieldOpType == OfflineCrudType.DELETE.ordinal) -> {
                                try {
                                    supabaseClient.postgrest.from(remoteTable).delete {
                                        filter { eq(BaseRemoteEntity::id.name, remoteItem.id) }
                                    }

                                    localDao.delete(localItem)
                                } catch (ex: Exception) {
                                    Log.e(
                                        TAG,
                                        "error while deleting item from local to remote $ex",
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        //remote data is latest
                        //update local row with remote data
                        val count = localDao.update(toMap(remoteItem))
                    }
                }
            } else {
                //no local data exists for this remote row, inserting in local table
                try {
                    localDao.insert(toMap(remoteItem))
                } catch (ex: Exception) {
                    Log.e(TAG, "error while inserting new remote data to local $ex")
                    Log.d(TAG, "remoteItem = $remoteItem")
                }
            }
        }

        try {
            //now check whether data is deleted from remote and exists in local
            val listOfLocalItems = localDao.query(SimpleSQLiteQuery("select * from $localTable"))
            val listOfRemoteItems = supabaseClient.postgrest.from(remoteTable)
                .select().data.decodeList<RDto>(serializer)

            val idsOfRemoteItems = listOfRemoteItems.map { it.id }.toSet()
            val toBeDeleted = listOfLocalItems.filter { it.id !in (idsOfRemoteItems) }
            toBeDeleted.forEach { localDao.delete(it) }
        } catch (ex: Exception) {
            Log.e(TAG, "error while deleting extra local entries $ex")
        }

        setLastSyncedTimeStamp(localTable, currentTimeStamp)
        Log.d(TAG, "updating lastSyncedTimestamp for $localTable = $currentTimeStamp")

    }

}

