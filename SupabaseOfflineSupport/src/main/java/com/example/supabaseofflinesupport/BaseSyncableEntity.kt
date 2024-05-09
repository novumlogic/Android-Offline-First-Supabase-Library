package com.example.supabaseofflinesupport


/**
 * Base class for all local entities
 *
 * Local Entity needs to extend this class in order to work with syncToSupabase() function of SyncManager
 *
 * @property id: Acts as Primary key for local table
 * @property lastUpdatedTimestamp: Contains milliseconds as timestamp to compare
 * with remote row's timestamp in case of conflict and perform synchronization
 * @property offlineFieldOpType: Should be passed any 4 predefined int values to determine state of record
 * on which CRUD operation was performed when device was offline
 * 0 for no changes,
 * 1 for new insertion,
 * 2 for updation on row,
 * 3 for deletion
 * */
abstract class BaseSyncableEntity {
    abstract val id: Int
    abstract var lastUpdatedTimestamp: Long
    abstract val offlineFieldOpType: Int
}
