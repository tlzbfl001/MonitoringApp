package com.aitronbiz.arron.api

import com.aitronbiz.arron.api.dto.DeviceDTO
import com.aitronbiz.arron.api.dto.FcmTokenDTO
import com.aitronbiz.arron.api.dto.FindPasswordDTO
import com.aitronbiz.arron.api.dto.HomeDTO
import com.aitronbiz.arron.api.dto.LoginDTO
import com.aitronbiz.arron.api.dto.RoomDTO
import com.aitronbiz.arron.api.dto.SendNotificationDTO
import com.aitronbiz.arron.api.dto.SignInDTO
import com.aitronbiz.arron.api.dto.SignUpDTO
import com.aitronbiz.arron.api.dto.SubjectDTO
import com.aitronbiz.arron.api.dto.UpdateRoomDTO
import com.aitronbiz.arron.api.response.ActivityResponse
import com.aitronbiz.arron.api.response.DeviceResponse
import com.aitronbiz.arron.api.response.DevicesResponse
import com.aitronbiz.arron.api.response.FcmTokenResponse
import com.aitronbiz.arron.api.response.HomeResponse
import com.aitronbiz.arron.api.response.HomesResponse
import com.aitronbiz.arron.api.response.LoginResponse
import com.aitronbiz.arron.api.response.PresenceResponse
import com.aitronbiz.arron.api.response.RespirationResponse
import com.aitronbiz.arron.api.response.RoomResponse
import com.aitronbiz.arron.api.response.RoomsResponse
import com.aitronbiz.arron.api.response.SendNotificationResponse
import com.aitronbiz.arron.api.response.SessionResponse
import com.aitronbiz.arron.api.response.SignInResponse
import com.aitronbiz.arron.api.response.SignUpResponse
import com.aitronbiz.arron.api.response.StatusResponse
import com.aitronbiz.arron.api.response.SubjectResponse
import com.aitronbiz.arron.api.response.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface APIService {
    @POST("api/auth/sign-in/social")
    suspend fun loginWithGoogle(
        @Body dto: LoginDTO
    ): Response<LoginResponse>

    @POST("api/auth/sign-up/email")
    suspend fun signUpEmail(
        @Body dto: SignUpDTO
    ): Response<SignUpResponse>

    @POST("api/auth/sign-in/email")
    suspend fun signInEmail(
        @Body dto: SignInDTO
    ): Response<SignInResponse>

    @GET("api/auth/token")
    suspend fun getToken(
        @Header("Authorization") sessionToken: String
    ): Response<TokenResponse>

    @GET("api/auth/get-session")
    suspend fun checkSession(
        @Header("Authorization") sessionToken: String
    ): Response<SessionResponse>

    @GET("homes")
    suspend fun getAllHome(
        @Header("Authorization") token: String
    ): Response<HomesResponse>

    @GET("homes/{id}")
    suspend fun getHome(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<HomeResponse>

    @POST("homes")
    suspend fun createHome(
        @Header("Authorization") token: String,
        @Body dto: HomeDTO
    ): Response<HomeResponse>

    @PATCH("homes/{id}")
    suspend fun updateHome(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body dto: HomeDTO
    ): Response<HomeResponse>

    @DELETE("homes/{id}")
    suspend fun deleteHome(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Void>

    @POST("subjects")
    suspend fun createSubject(
        @Header("Authorization") token: String,
        @Body dto: SubjectDTO
    ): Response<SubjectResponse>

    @PATCH("subjects/{id}")
    suspend fun updateSubject(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body dto: SubjectDTO
    ): Response<SubjectResponse>

    @DELETE("subjects/{id}")
    suspend fun deleteSubject(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Void>

    @GET("rooms")
    suspend fun getAllRoom(
        @Header("Authorization") token: String,
        @Query("homeId") homeId: String
    ): Response<RoomsResponse>

    @GET("rooms/{id}")
    suspend fun getRoom(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<RoomResponse>

    @POST("rooms")
    suspend fun createRoom(
        @Header("Authorization") token: String,
        @Body dto: RoomDTO
    ): Response<RoomResponse>

    @PATCH("rooms/{id}")
    suspend fun updateRoom(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body dto: UpdateRoomDTO
    ): Response<RoomResponse>

    @DELETE("rooms/{id}")
    suspend fun deleteRoom(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Void>

    @GET("devices")
    suspend fun getAllDevice(
        @Header("Authorization") token: String,
        @Query("roomId") roomId: String
    ): Response<DevicesResponse>

    @GET("devices/{id}")
    suspend fun getDevice(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<DeviceResponse>

    @POST("devices")
    suspend fun createDevice(
        @Header("Authorization") token: String,
        @Body dto: DeviceDTO
    ): Response<DeviceResponse>

    @PATCH("devices/{id}")
    suspend fun updateDevice(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body dto: DeviceDTO
    ): Response<DeviceResponse>

    @DELETE("devices/{id}")
    suspend fun deleteDevice(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Void>

    @POST("notifications/register-token")
    suspend fun saveFcmToken(
        @Header("Authorization") token: String,
        @Body dto: FcmTokenDTO
    ): Response<FcmTokenResponse>

    @POST("notifications/send")
    suspend fun sendNotification(
        @Header("Authorization") token: String,
        @Body request: SendNotificationDTO
    ): Response<SendNotificationResponse>

    @POST("api/auth/forget-password")
    suspend fun findPassword(
        @Body request: FindPasswordDTO
    ): Response<StatusResponse>

    @GET("activity-scores/rooms/{roomId}")
    suspend fun getActivity(
        @Header("Authorization") token: String,
        @Path("roomId") roomId: String,
        @Query("startTime") startTime: String,
        @Query("endTime") endTime: String
    ): Response<ActivityResponse>

    @GET("breathing/rooms/{roomId}")
    suspend fun getRespiration(
        @Header("Authorization") token: String,
        @Path("roomId") roomId: String
    ): Response<RespirationResponse>

    @GET("presence/rooms/{roomId}/current")
    suspend fun getPresence(
        @Header("Authorization") token: String,
        @Path("roomId") roomId: String
    ): Response<PresenceResponse>
}