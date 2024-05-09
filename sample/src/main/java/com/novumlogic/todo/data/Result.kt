package com.novumlogic.todo.data

import java.lang.Exception

sealed class Result<out R> {
    data class Success<out T>(val data: T): Result<T>()
    data class Failure(val exception: Exception): Result<Nothing>()
    data object Loading: Result<Nothing>()

    override fun toString(): String {
        return when(this){
            is Success<*> -> "Success[data = $data]"
            is Failure -> "Failure[exception=$exception]"
            Loading -> "Data Loading"
        }
    }
}

val Result<*>.succeeded
    get() = this is Result.Success && data != null