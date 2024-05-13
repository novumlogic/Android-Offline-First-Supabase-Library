# Supabase-Offline-Support-Library

## Introduction
This library offers a comprehensive solution for managing offline data synchronization for Android applications. It seamlessly handles data consistency between a local database and a remote Supabase tables, enabling robust offline capabilities that are crucial for mobile applications operating in environments with intermittent connectivity. The library is equipped with utility methods that automatically trigger data synchronization upon detecting network changes, ensuring your application remains up-to-date effortlessly. Additionally, it provides the flexibility to integrate custom synchronization triggers according to specific application needs, such as upon specific user actions, at application launch, or at predetermined intervals. This feature allows developers to maintain control over the synchronization process and tailor it to fit the application's usage patterns and requirements.

## Tech Stack 
- Language: Kotlin
- Local Database: Room Database
- Remote Database: Supabase

## Features
- **Offline Data Synchronization:** Ready-to-use data synchronization algorithms to manage local and remote data consistency.
- **Network Change Detection:** Utilizes the [`SyncManager`](../main/SupabaseOfflineSupport/src/main/java/com/example/supabaseofflinesupport/SyncManager.kt) class to observe and react to network status changes, ensuring data is synchronized when the network becomes available.
- **Sample App Integration:** A fully functional Todo List app demonstrating the practical implementation of offline support and data synchronization within an Android application.
- **Extensible**: Provides base classes like   - [`BaseSyncableEntity`](../main/SupabaseOfflineSupport/src/main/java/com/example/supabaseofflinesupport/BaseSyncableEntity.kt), [`BaseRemoteEntity`](../main/SupabaseOfflineSupport/src/main/java/com/example/supabaseofflinesupport/BaseRemoteEntity.kt), [`GenericDao`](../main/SupabaseOfflineSupport/src/main/java/com/example/supabaseofflinesupport/GenericDao.kt), [`BaseDataSyncer`](../main/SupabaseOfflineSupport/src/main/java/com/example/supabaseofflinesupport/BaseDataSyncer.kt) which can be extended to add your behaviours and properties.

## Library Classes Explained

1. **BaseDataSyncer:**
BaseDataSyncer is an abstract base class designed to facilitate synchronization between a local database and Supabase. It serves as a blueprint for implementing custom synchronization logic, including conflict resolution and handling of deleted data.

     To utilize BaseDataSyncer, follow these steps:
     
     1. **Extend BaseDataSyncer**: Create a subclass of BaseDataSyncer to implement your custom synchronization logic. Override the `syncToSupabase` method to define how data synchronization should be performed.
     
     2. **Define Synchronization Logic**: Within the `syncToSupabase` method, specify the synchronization algorithm using the Last Write Wins conflict resolution strategy. This method should handle synchronization between a specific local table and its corresponding remote table.
     
     3. **Parameters**:
        - `localTable`: Name of the local table to be synchronized.
        - `localDao`: Data Access Object (DAO) for the local table, used to perform CRUD operations.
        - `remoteTable`: Name of the remote table to be synchronized.
        - `toMap`: Function that converts the remote entity to the local entity.
        - `toMapWithoutLocal`: Function that converts the local entity to the remote entity.
        - `serializer`: Serializer for the remote entity, required for decoding JSON data received from Supabase.
        - `currentTimeStamp`: Current timestamp in milliseconds, necessary for synchronization logic.
     
     4. **Extend or Use SyncManager**: Optionally, extend SyncManager, which is a concrete implementation of BaseDataSyncer provided by the library. SyncManager already implements the synchronization algorithm, allowing you to use it directly for synchronization tasks.

### Example

```kotlin
class CustomDataSyncer(supabaseClient: SupabaseClient) : BaseDataSyncer(supabaseClient) {

    override suspend fun <T: BaseSyncableEntity, RDto: BaseRemoteEntity> syncToSupabase(
        localTable: String,
        localDao: GenericDao<T>,
        remoteTable: String,
        toMap: (RDto) -> T,
        toMapWithoutLocal: (T) -> RDto,
        serializer: KSerializer<RDto>,
        currentTimeStamp: Long
    ) {
        // Custom synchronization logic implementation
    }
}
```

2.**BaseRemoteEntity:**
   BaseRemoteEntity serves as the base class for any data transfer object (DTO) or entity representing a remote table in Supabase. It establishes a consistent structure for remote entities and ensures that necessary columns are present for synchronization with the SyncManager.

   **Properties:**
   - `id`: Primary key for the remote table.
   - `lastUpdatedTimestamp`: Contains the timestamp in milliseconds for comparison with the local row's timestamp in case of conflicts during synchronization.

   **To use BaseRemoteEntity:**
   1. **Extend BaseRemoteEntity:** Create a subclass of BaseRemoteEntity for each DTO or entity representing a remote table in Supabase. Ensure that the subclass includes the required properties, "id" and "lastUpdatedTimestamp".
   2. **Include Properties in Remote Table:** Make sure that the properties defined in BaseRemoteEntity, namely "id" and "lastUpdatedTimestamp", correspond to columns in the remote table within Supabase. These columns are essential for the SyncManager to work effectively.

### Example
```kotlin
@Serializable
data class RemoteEntity(
    override val id: Int,
    override val lastUpdatedTimestamp: Long,
    // Additional properties specific to the remote entity
) : BaseRemoteEntity()
```

3. **BaseSyncableEntity:**
   BaseSyncableEntity serves as the base class for all entities stored in the local Room database that require synchronization with Supabase. It provides essential properties for synchronization and must be extended by local entities to work seamlessly with the syncToSupabase function of SyncManager.

   **Properties:**
   - `id`: Acts as the primary key for the local table.
   - `lastUpdatedTimestamp`: Contains the timestamp in milliseconds to compare with the remote row's timestamp in case of conflicts during synchronization.
   - `offlineFieldOpType`: Indicates the type of CRUD operation performed on the record when the device was offline. It should be set to one of the predefined integer values:
     1. `0`: No changes.
     2. `1`: New insertion.
     3. `2`: Update on the row.
     4. `3`: Deletion.

   **To use BaseSyncableEntity:**
   1. **Extend BaseSyncableEntity:** Create a subclass of BaseSyncableEntity for each entity stored in the local Room database that requires synchronization with Supabase.
   2. **Include Properties in Local Entity:** Ensure that the subclass includes all properties defined in BaseSyncableEntity, namely "id", "lastUpdatedTimestamp", and "offlineFieldOpType".
   3. **Set OfflineFieldOpType:** When performing CRUD operations on the local entity while the device is offline, set the `offlineFieldOpType` property to the appropriate value based on the operation performed.

### Example
```kotlin
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) override val id: Int,
    val name: String,
    override var lastUpdatedTimestamp: Long,
    override val offlineFieldOpType: Int
) : BaseSyncableEntity()
```

4. **GenericDao:**
   GenericDao is an interface that provides a set of generic CRUD (Create, Read, Update, Delete) methods required for data manipulation within Room entities. These methods are essential for the functioning of the SyncManager in performing data synchronization tasks.

   **To use GenericDao:**
   1. **Extend GenericDao:** Implement this interface in your DAO (Data Access Object) implementations. This allows your DAO to provide the necessary data manipulation methods required by the SyncManager for synchronization tasks.
   2. **Implement CRUD Methods:** Implement the following CRUD methods within your DAO implementation:
      - `query`: Executes a raw SQL query with a dynamic table name at runtime using the `@RawQuery` annotation.
      - `update`: Updates a specific entity in the database.
      - `delete`: Deletes a specific entity from the database.
      - `insert`: Inserts a new entity into the database.

   ### Example

   ```kotlin
   interface taskDao : GenericDao<Task> {
       // Additional methods specific to the User entity
   }
   ```

5. **SyncManager:** SyncManager is a concrete class that extends BaseDataSyncer and provides specific logic for synchronizing data with Supabase. It implements the syncToSupabase method to manage data synchronization processes, utilizing the SupabaseApiService for data transmission to and from Supabase.

- **Constructor Parameters**
   - `context`: The application context.
   - `supabaseClient`: The SupabaseClient instance used for communication with Supabase.

- **Properties**
   - `networkHelper`: An instance of NetworkHelper used to check network availability and observe network changes.
   - `sharedPreferences`: SharedPreferences instance used to store the timestamp of last synchronization for each table.

- **Methods**
   - `isNetworkAvailable()`: Checks if the network is available.
   - `observeNetwork()`: Observes network changes.
   - `getLastSyncedTimeStamp(tableName: String)`: Retrieves the timestamp when a particular table was last synced with the remote table.
   - `setLastSyncedTimeStamp(tableName: String, value: Long)`: Sets the timestamp of last synchronization for a given table.
   - `syncToSupabase(...)`: Overrides the syncToSupabase method from BaseDataSyncer to provide specific synchronization logic. This method handles when and how to sync data based on network availability and data state.

- **Usage**
   - To use SyncManager:
       1. **Instantiate SyncManager**: Create an instance of SyncManager by passing the application context and SupabaseClient instance to the constructor.
       2. **Check Network Availability**: Use the `isNetworkAvailable()` method to determine if the network is available before initiating synchronization.
       3. **Observing Network Changes**: Utilize the `observeNetwork()` method to observe network changes and trigger synchronization accordingly.
       4. **Perform Data Synchronization**: Call the `syncToSupabase(...)` method to start the data synchronization process. This method handles synchronization logic, including conflict resolution and data transmission to and from Supabase.

- **Example**
   ```kotlin
   val context: Context = applicationContext
   val supabaseClient: SupabaseClient = initializeSupabaseClient()

   val syncManager = SyncManager(context, supabaseClient)

   if (syncManager.isNetworkAvailable()) {
       syncManager.syncToSupabase(
           localTable = "local_table",
           localDao = localDao,
           remoteTable = "remote_table",
           toMap = { remoteDto -> mapToLocalEntity(remoteDto) },
           toMapWithoutLocal = { localEntity -> mapToRemoteDto(localEntity) },
           serializer = RemoteDto.serializer(),
           currentTimeStamp = System.currentTimeMillis()
       )
   }
   ```

    
6. **RetrofitClient:**  RetrofitClient is a singleton object responsible for setting up and providing a Retrofit client instance for making HTTP requests to Supabase REST API (Used in `syncTosupabase()` of `SyncManager`) 

    - **Properties**
        - `BASE_URL`: Base URL of the Supabase API.
        - `apikey`: API key used for authentication with Supabase API.

    - **Methods**
        - `setupClient(baseUrl: String, apikey: String)`: Sets up the Retrofit client with the provided base URL and API key.
        - `rClient`: Lazy-initialized property that provides an instance of SupabaseApiService, which is the Retrofit interface for making API calls.

    - **Usage**
        - To use RetrofitClient:
            1. **Setup Retrofit Client**: Call `setupClient(baseUrl: String, apikey: String)` to set up the base URL and API key.
            2. **Access Retrofit Client**: Use the `rClient` property to access the Retrofit client instance and make API calls.

    - **Example**
        ```kotlin
        // Set up RetrofitClient
        RetrofitClient.setupClient(baseUrl = "https://api.supabase.io", apikey = "your_api_key")

        // Access Retrofit client instance
        val apiService = RetrofitClient.rClient

        // Use apiService to make API calls
        apiService.getData()
        ```

        
7. **SupabaseApiService:** SupabaseApiService is an interface that provides asynchronous methods for performing CRUD operations on Supabase REST API.

    - **Methods**
        - `insert(tableName: String, data: RequestBody)`: Performs an insert operation on the specified table.
        - `insertReturnId(tableName: String, data: RequestBody)`: Performs an insert operation on the specified table and returns the ID of the inserted row.
        - `update(tableName: String, id: Int, data: RequestBody)`: Performs an update operation on the specified table for the row with the given ID.
        - Other CRUD methods for delete, select, and upsert operations can be defined similarly.

8. **NetworkHelper:** Class that provides utility methods to check internet connectivity and notify observers about network state changes using LiveData.

    - **Properties**
        - `connectivityManager`: Manages the network connections.
        - `networkLiveData`: LiveData that represents the current network state.

    - **Methods**
        - `isNetworkAvailable()`: Checks if the device is connected to the internet.
        - `getNetworkLiveData()`: Provides LiveData to observe changes in network connectivity.

    - **Usage**
        - To use NetworkHelper:
            1. **Initialize**: Create an instance of NetworkHelper by passing the application context.
            2. **Check Network**: Call `isNetworkAvailable()` to perform instantaneous network checks.
            3. **Observe Network Changes**: Use `getNetworkLiveData()` to get a LiveData object that updates observers about network state changes.

    - **Example**
        ```kotlin
        // Initialization
        val networkHelper = NetworkHelper(context)

        // Check network availability
        val isAvailable = networkHelper.isNetworkAvailable()

        // Observe network changes
        networkHelper.getNetworkLiveData().observe(this, { isConnected ->
            if (isConnected) {
                // Network is connected
            } else {
                // Network is disconnected
            }
        })
        ```

9. **QueryParamConverter:** It is a custom converter factory used in Retrofit to dynamically modify URLs based on query requirements. It contains annotation classes (`eq`, `lt`, `gt`) to define query parameters for Supabase REST API calls.

    - **Annotation Classes**
        - `eq`: Indicates equality comparison in query parameters.
        - `lt`: Indicates less-than comparison in query parameters.
        - `gt`: Indicates greater-than comparison in query parameters.

    - **Functionality**
        - `stringConverter(type: Type, annotations: Array<out Annotation>, retrofit: Retrofit)`: Overrides the stringConverter method to provide a custom converter for query parameters.
            - Returns a Converter to convert query parameter values to string representations based on annotations.
            - Annotations determine the type of comparison (equality, less-than, greater-than) for the query parameter.

    - **Usage**
        - To use QueryParamConverter:
            1. **Define Annotations**: Annotate parameters with `eq`, `lt`, or `gt` to specify query conditions.
            2. **Apply Converter**: Use QueryParamConverter as a converter factory in Retrofit setup to enable dynamic URL modification based on query parameters.

    - **Example**
        ```kotlin
        // Usage of QueryParamConverter annotations
        interface ApiService {
              @Headers("Content-Type: application/json", "Prefer: return=minimal")
              @PATCH("/rest/v1/{table}")
              suspend fun update(
                  @Path("table") tableName: String,
                  @Query("id") @eq id: Int,
                  @Body data: RequestBody
              ): Response<Unit>
        }

        // Retrofit setup with QueryParamConverter
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(QueryParamConverter())
            .build()

        // Create ApiService instance
        val apiService = retrofit.create(ApiService::class.java)
        ```

10. **Utils:** Provides a set of extension functions and data classes to streamline data handling for Supabase API interactions.

    - **Extension Functions**
        - `decodeSingle(serializer: KSerializer<RDto>): RDto`: Decodes a JSON string into a single instance of type RDto.
        - `decodeList(serializer: KSerializer<RDto>): List<RDto>`: Decodes a JSON string into a list of instances of type RDto.
        - `getId()`: Extracts the ID from the response body of a Supabase API call.
        - `prepareRequestBody(serializer: KSerializer<T>): RequestBody`: Prepares a request body for a Supabase API call, including the ID.
        - `prepareRequestBodyWithoutId(serializer: KSerializer<T>): RequestBody`: Prepares a request body for a Supabase API call, excluding the ID.
        - `removeIdFromJson(str: String): String`: Removes the ID field from a JSON string.

    - **Data Classes**
        - `JsonId`: Represents a JSON object containing an ID.
        - `JsonError`: Represents a JSON object containing error details from a Supabase API response.

    - **Usage**
        - These utility functions and data classes are used to simplify data handling for Supabase API interactions. They facilitate parsing JSON responses, preparing request bodies, and extracting IDs, enhancing the efficiency of data exchange with the Supabase backend.


### Todo List Sample App Overview

The Todo List Sample App allows users to manage tasks with various details such as name, category, emoji, priority, and completion status. Users can create new tasks for specific dates, mark tasks as complete or incomplete, delete tasks, and view past tasks. The app follows a simple yet efficient approach to task management.

#### Tables Overview

1. **Categories**
    - Purpose: Stores categories for organizing tasks.
    - Columns:
        - `id`: Primary key for the category.
        - `name`: Name of the category.
        - `lastUpdatedTimestamp`: Timestamp indicating the last update time.
        - `offlineFieldOpType`: Type of offline CRUD operation performed.

2. **Tasks**
    - Purpose: Contains task details including name, category, emoji, date, completion status, priority, and deletion status.
    - Columns:
        - `id`: Primary key for the task.
        - `name`: Name of the task.
        - `categoryId`: Foreign key referencing the category associated with the task.
        - `emoji`: Emoji representing the task.
        - `date`: Date for the task.
        - `isComplete`: Indicates whether the task is completed or not.
        - `priorityId`: Foreign key referencing the priority level of the task.
        - `lastUpdatedTimestamp`: Timestamp indicating the last update time.
        - `isDelete`: Indicates whether the task is marked for deletion.
        - `offlineFieldOpType`: Type of offline CRUD operation performed.

3. **Priorities**
    - Purpose: Stores priority levels for tasks.
    - Columns:
        - `id`: Primary key for the priority.
        - `priority`: Priority level.


The app's data model consists of these tables, each serving a specific purpose in organizing and managing tasks. These tables are integrated with corresponding DAOs and local data source classes, facilitating seamless data management within the app.


## Library's Usage

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
   **Note:** @SerialName("timestamp") is must on the property lastUpdatedTimestamp and also the column named "timestamp" of type bigint / int8 , "id" of bigint/ int8 column as primary key is required in Supabase table in order to work with SyncManager as follows:

   ```Postgres
   create table
       public.categories (
         name text not null,
         timestamp bigint null,
         id bigint generated by default as identity,
         //Add other columns as per your need
       ) tablespace pg_default;
   ```

   

    Extend all the daos with [GenericDao](../main/SupabaseOfflineSupport/src/main/java/com/example/supabaseofflinesupport/GenericDao.kt) as follows:
    ```kotlin
    @Dao
    interface CategoryDao: GenericDao<Category>
    ```
3. **Integrate with your code:**
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
4. **Observe Network Changes:**
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

## Additional Resources

For information and insights like challenge faced during the development, check out our Medium blog post:

- [Supercharge Your Android App with Offline Capability Using Supabase and Room](link-to-your-medium-blog-post)



