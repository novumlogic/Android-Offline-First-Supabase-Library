package com.example.supabaseofflinesupport.helpers

import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class eq

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class gt

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class lt

/*** Custom Converter.Factory for handling custom query parameter annotations.
 * Used to add query parameter in Supabase API calls.
 * Used in Retrofit Client
 */
class QueryParamConverter: Converter.Factory(){
    override fun stringConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, String>? {
        return when {
            annotations.any { it is eq } -> {
                Converter<Any, String> { value ->
                    "eq.$value"
                }
            }
            annotations.any { it is gt } -> {
                Converter<Any, String> { value ->
                    "gt.$value"
                }
            }
            annotations.any { it is lt } -> {
                Converter<Any, String> { value ->
                    "lt.$value"
                }
            }
            else -> null
        }
    }
}