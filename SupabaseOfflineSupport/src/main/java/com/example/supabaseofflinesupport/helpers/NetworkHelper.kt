package com.example.supabaseofflinesupport.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.core.content.getSystemService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * NetworkHelper class to check network connectivityand provide a LiveData object to observe network state changes.
 *
 * @param context The application context.*/

class NetworkHelper(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkLiveData: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>().also {
            it.postValue( connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false)
        }
    }

    private val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    /**
     * Checks if the network is currently available.
     * @return True if the network is available, false otherwise.
     */
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Returns a LiveData object that emits true when the network becomes available and false when it is lost.
     *
     * @return A LiveData object for network state changes.
     */
    fun getNetworkLiveData(): LiveData<Boolean> {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                networkLiveData.postValue(true)
            }

            override fun onLost(network: Network) {
                networkLiveData.postValue(false)
            }
        }

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        return networkLiveData
    }
}
