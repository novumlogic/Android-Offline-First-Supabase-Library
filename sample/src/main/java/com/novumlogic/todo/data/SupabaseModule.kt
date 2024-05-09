package com.novumlogic.todo.data

import com.novumlogic.todo.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseModule {
//    private val supabase = createSupabaseClient(BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_KEY){
//        install(Postgrest)
//    }
//    fun provideSupabaseClient(): SupabaseClient = supabase

//                          OR                             \\

    private var supabase: SupabaseClient? = null
    fun provideSupabaseClient(): SupabaseClient = supabase ?: createSupabaseClient(BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_KEY){install(Postgrest){
//        defaultSchema = "version_two"
        serializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        })
    }  }.also { supabase = it  }
}