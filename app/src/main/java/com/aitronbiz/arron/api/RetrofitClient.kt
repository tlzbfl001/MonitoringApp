package com.aitronbiz.arron.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val AUTH_URL = "https://dev.auth.arron.aitronbiz.com/"
    private const val BASE_URL = "https://dev.arron.aitronbiz.com/api/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY  // 로그 확인용
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val authRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(AUTH_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApiService: APIService = authRetrofit.create(APIService::class.java)
    val apiService: APIService = retrofit.create(APIService::class.java)
}