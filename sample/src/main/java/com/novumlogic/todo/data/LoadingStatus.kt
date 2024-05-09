package com.novumlogic.todo.data

import io.github.jan.supabase.exceptions.HttpRequestException
import java.lang.Exception
import java.net.UnknownHostException

enum class Status{
    LOADING,
    SUCCESS,
    FAILURE
}

enum class Error{
    NO_DATA,
    NETWORK_ERROR,
    UNKNOWN_ERROR
}
data class LoadingStatus<T>(val status: Status, val errorCode: Error?, val data: T){
    companion object{
        fun loading(): LoadingStatus<*>{
            return LoadingStatus(Status.LOADING,null,null)
        }

        fun <T>onSuccess(data: T): LoadingStatus<T>{
            return LoadingStatus(Status.SUCCESS,null, data)
        }

        fun onFailure(ex:Exception): LoadingStatus<*>{
            return when(ex){
                is UnknownHostException -> {
                    LoadingStatus(Status.FAILURE,Error.NETWORK_ERROR,ex.message.toString())
                }
                is HttpRequestException -> {
                    LoadingStatus(Status.FAILURE,Error.NETWORK_ERROR,ex.message.toString())
                }

                else -> {
                    LoadingStatus(Status.FAILURE, Error.UNKNOWN_ERROR, ex.message.toString())
                }
            }
        }
    }
}
