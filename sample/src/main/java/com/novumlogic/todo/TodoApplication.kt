package com.novumlogic.todo

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.novumlogic.todo.data.CategoryRemoteDataSource
import com.novumlogic.todo.data.TaskRemoteDataSource
import com.novumlogic.todo.data.TaskRepository
import com.novumlogic.todo.data.local.CategoryLocalDataSource
import com.novumlogic.todo.data.local.PriorityLocalDataSource
import com.novumlogic.todo.data.local.TaskLocalDataSource
import com.novumlogic.todo.data.local.TodoDatabase

class TodoApplication : Application() {
    private val TAG = javaClass.simpleName
    private val _networkConnected = MutableLiveData<Boolean>()
    private val networkRequest = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            _networkConnected.postValue(true)
            Log.d(TAG, "onAvailable: net on ")
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            _networkConnected.postValue(false)
            Log.d(TAG, "onLost: net off")
        }
    }
    private lateinit var connectivityManager: ConnectivityManager
    val taskRepository by lazy {
        val db = TodoDatabase.getDatabase(this)

        val taskLocalDataSource = TaskLocalDataSource(db.taskDao())
        val categoryLocalDataSource = CategoryLocalDataSource(db.categoryDao())
        val taskRemoteDataSource = TaskRemoteDataSource()
        val categoryRemoteDataSource = CategoryRemoteDataSource()
        val priorityLocalDataSource = PriorityLocalDataSource(db.priorityDao())

        TaskRepository(this,
            taskLocalDataSource,
            taskRemoteDataSource,
            categoryLocalDataSource,
            categoryRemoteDataSource,
            priorityLocalDataSource,
        )

    }

    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(android.net.ConnectivityManager::class.java)
        connectivityManager.let {
            it.getNetworkCapabilities(it.activeNetwork)?.let { netCap ->
                _networkConnected.value = netCap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } ?: _networkConnected.also { net -> net.value = false }
        }
        Log.d(TAG, "onCreate: netconnnection: ${_networkConnected.value}")
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        Log.d(TAG, "onCreate: _networkConnected -- $_networkConnected")
    }


    override fun onTerminate() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        super.onTerminate()
    }

}
