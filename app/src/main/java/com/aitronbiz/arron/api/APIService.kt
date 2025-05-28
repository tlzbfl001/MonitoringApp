package com.aitronbiz.arron.api

import com.aitronbiz.arron.api.dto.LoginDTO
import com.aitronbiz.arron.api.response.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface APIService {
    @POST("api/auth/sign-in/social")
    suspend fun loginWithGoogle(
        @Body dto: LoginDTO
    ): Response<LoginResponse>
}