package com.aitronbiz.arron.api

import com.aitronbiz.arron.api.dto.HomeDTO
import com.aitronbiz.arron.api.dto.LoginDTO
import com.aitronbiz.arron.api.response.HomeResponse
import com.aitronbiz.arron.api.response.LoginResponse
import com.aitronbiz.arron.api.response.SessionResponse
import com.aitronbiz.arron.api.response.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface APIService {
    @POST("api/auth/sign-in/social")
    suspend fun loginWithGoogle(
        @Body dto: LoginDTO
    ): Response<LoginResponse>

    @GET("api/auth/token")
    suspend fun getToken(
        @Header("Authorization") sessionToken: String
    ): Response<TokenResponse>

    @GET("api/auth/get-session")
    suspend fun checkSession(
        @Header("Authorization") sessionToken: String
    ): Response<SessionResponse>

    @POST("homes")
    suspend fun createHome(
        @Header("Authorization") token: String,
        @Body dto: HomeDTO
    ): Response<HomeResponse>
}