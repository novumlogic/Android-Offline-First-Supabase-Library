# Supabase-Offline-Support-Library

## Introduction
Supabase Offline Support is an Android library designed to facilitate offline capabilities and data synchronization for Android apps using the Supabase backend and Room for local database. This repository also includes a sample Todo List app that integrates the library to demonstrate offline data synchronization, which automatically updates tables based on network availability changes. The library contains `SyncManager` class which provides method `syncToSupabase()` applying the synchronization algorithm and other utility methods, it also contains `BaseDataSyncer` which you can extend to use your own synchronization algorithm  

## Features
- **Offline Data Synchronization:** Ready-to-use data synchronization algorithms to manage local and remote data consistency.
- **Network Change Detection:** Utilizes the [`SyncManager`](../main/SupabaseOfflineSupport/src/main/java/com/example/supabaseofflinesupport/SyncManager.kt) class to observe and react to network status changes, ensuring data is synchronized when the network becomes available.
- **Sample App Integration:** A fully functional Todo List app demonstrating the practical implementation of offline support and data synchronization within an Android application.
- **Extensible**: Provides base classes like   - [`BaseSyncableEntity`](../main/SupabaseOfflineSupport/src/main/java/com/example/supabaseofflinesupport/BaseSyncableEntity.kt), [`BaseRemoteEntity`](../main/SupabaseOfflineSupport/src/main/java/com/example/supabaseofflinesupport/BaseRemoteEntity.kt), [`GenericDao`](../main/SupabaseOfflineSupport/src/main/java/com/example/supabaseofflinesupport/GenericDao.kt), [`BaseDataSyncer`](../main/SupabaseOfflineSupport/src/main/java/com/example/supabaseofflinesupport/BaseDataSyncer.kt) which can be extended to add your behaviours and properties.


## Usage

1. **Configuring the library:** You need to perform the steps below for `syncToSupabase()` method of [SyncManager](../main/SupabaseOfflineSupport/src/main/java/com/example/supabaseofflinesupport/SyncManager.kt) to work properly.
     ```groovy
     include ':supabaseOfflineSupport'
     project(':supabaseOfflineSupport').projectDir = new File('path/to/supabaseOfflineSupport')
     ```
    Extend all the local entities with [BaseSyncableEntity](../main/SupabaseOfflineSupport/src/main/java/com/example/supabaseofflinesupport/BaseSyncableEntity.kt), Below is the snippet from Todo list sample app:
   ```kotlin
   @Entity(tableName = "categories")
    data class Category(
        @PrimaryKey(autoGenerate = true) override val id: Int,
        val name: String,
        override var lastUpdatedTimestamp: Long,
        override val offlineFieldOpType: Int
    ) : BaseSyncableEntity()
   ```

   Extend all the remote dtos with [BaseRemoteEntity](../main/SupabaseOfflineSupport/src/main/java/com/example/supabaseofflinesupport/BaseRemoteEntity.kt), Below is the snippet from Todo list sample app:
   ```kotlin
   @Serializable
    data class CategoryDto(
        val name: String,
        @SerialName("timestamp") override var lastUpdatedTimestamp: Long,
        override val id: Int
    ) : BaseRemoteEntity()
   ```
   
    Extend all the daos with [GenericDao](../main/SupabaseOfflineSupport/src/main/java/com/example/supabaseofflinesupport/GenericDao.kt) as follows:
    ```kotlin
    @Dao
    interface CategoryDao: GenericDao<Category>
    ```
2. **Integrate with your code:**
   After performing above configuration, you can use the library by referring the below code from [TaskRepository](../main/sample/src/main/java/com/novumlogic/todo/data/TaskRepository.kt) of our sample app:
   ```kotlin
   class TaskRepository(
    private val context: Context,
    ) {
       val client = createSupabaseClient(BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_KEY){install(Postgrest){
            serializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }  }
        private val syncManager = SyncManager(context, client)
        val networkConnected = syncManager.observeNetwork()
    ...
    suspend fun forceUpdateTasks() {
          syncManager.syncToSupabase(
              "tasks",
              taskLocalDataSource.taskDao,
              "task",
              { it.toEntity(0) },
              { it.toDto() },
              TaskDto.serializer(),
              System.currentTimeMillis()
          )
       }

    suspend fun forceUpdateCategories() {
          syncManager.syncToSupabase(
              "categories",
              categoryLocalDataSource.categoryDao,
              "categories",
              { it.toEntity(0) },
              { it.toDto() },
              CategoryDto.serializer(),
              System.currentTimeMillis()
          )   
    }

    }
    //This is extension function which is passed in above forceUpdateCategories() parameters
    fun Category.toDto(): CategoryDto = CategoryDto(
        this.name,
        this.lastUpdatedTimestamp,
        this.id
    )
    
    fun CategoryDto.toEntity(crud: Int): Category = Category(
            this.id,
            this.name,
            this.lastUpdatedTimestamp,
            crud
        )
    ```
3. **Observe Network Changes:**
   Below is the code from [MainActivity](../main/sample/src/main/java/com/novumlogic/todo/ui/MainActivity.kt) where the changes are observed:
   ```kotlin
       viewModel.networkConnected.observe(this@MainActivity, Observer {
        Log.d(TAG, "onCreate: Network connection on ?: $it, trigger point ")
        var str = if(it){
            viewModel.forceUpdate()
            getString(R.string.device_online)
        } else {
            getString(R.string.device_offline)
        }
        Toast.makeText(this@MainActivity, str, Toast.LENGTH_SHORT).show()
    })
   ```
   The code from [MainViewModel](../main/sample/src/main/java/com/novumlogic/todo/ui/viewmodels/MainViewModel.kt) which is handling the `TaskRepository`
   ```kotlin
   class MainViewModel(private val taskRepository: TaskRepository): ViewModel() {
    ...
    val networkConnected = taskRepository.networkConnected.distinctUntilChanged()
    ...
    fun forceUpdate() {
        loadingStatus.postValue(true)
        viewModelScope.launch (Dispatchers.IO){
            taskRepository.forceUpdateCategories()
            taskRepository.forceUpdateTasks()
            loadingStatus.postValue(false)
        }
    }
   }
   ```
   
## Synchronization Algorithm
![1_adDyl4OYeAwvLm0yEd3jig](https://github.com/Dhananjay-Navlani/Supabase-Offline-Support-Library/assets/164985893/efdf15fd-b923-41af-8955-a9e4b2c2cbcb)


1. **Check Network Availability:**
  Verify if the device has an active network connection. If not, exit the synchronization process.

2. **Retrieve Local Changes:**
  Query the local database for items that have been updated since the last synchronization.
  If no items are updated then just skip step 4

3. **Retrieve Remote Data:**
  Fetch data from the remote database to compare with local changes.

4. **Handle Local Changes and Remote Conflict:**
  For each local item, check if a corresponding item exists in the remote database: 
   - If the item is found then:

     - Compare timestamps to determine which version is more recent.
      Apply the "Last Write Wins" strategy:
       - If the local version is newer:
            - Update the remote database with the local changes.
            - Handle insertions, updates, and deletions accordingly.
       - else the remote is newer so:
            - Update the local database with the remote changes.
    - else:
         - Insert the local item into the remote database.
         - Update the local item with the generated remote ID.

5. **Update Local Data with Remote Changes:**
  For each remote item, check if a corresponding item exists in the local database: <br>
   - If item exists then:
   
      - Compare timestamps to determine which version is more recent.
       Apply the "Last Write Wins" strategy:
         - If the remote version is newer then:
              -Update the local database with the remote changes.
         - else the local version is newer:
              - Update the remote database with the local changes.
    - else:   
         - Insert the remote item into the local database.
   
6. **Cleanup:**
  Delete local items that no longer exist in the remote database.

7. **Update Last Synced Timestamp:**
  Store the timestamp of last synchronization for each table for future reference.
    

