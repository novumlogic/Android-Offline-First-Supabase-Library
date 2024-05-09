package com.example.supabaseofflinesupport.helpers

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Object class for creating a Retrofit client for the Supabase API.
 */
object RetrofitClient {
    private var BASE_URL = ""
    private var apikey = ""

    /**
     * Sets up the Retrofit client with the provided base URL and API key.
     *
     * @param baseUrl The base URL of the Supabase API.
     * @param apikey The API key for the Supabase API.
     */
    fun setupClient(baseUrl: String,apikey: String) {
        this.BASE_URL = baseUrl
        this.apikey = apikey
    }

    /**
     * Creates a Retrofit client for the Supabase API.
     *
     * @throws Exception If the API key or base URL is not set.
     */
    val rClient: SupabaseApiService by lazy {
        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(Interceptor {
            val original = it.request()
            if(apikey.isEmpty() || BASE_URL.isEmpty()) throw Exception("The apikey/setupClient for Retroclient is not set. Use setupClient(baseUrl: String,apikey: String) to setup the client")
            val request = original.newBuilder()
                .header("apikey", apikey)
                .header("Authorization", "Bearer $apikey")
                .method(original.method, original.body)
                .build()

            it.proceed(request)
        })
        val client = httpClient.build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addConverterFactory(QueryParamConverter())
            .client(client)
            .build()

        retrofit.create(SupabaseApiService::class.java)
    }
}
