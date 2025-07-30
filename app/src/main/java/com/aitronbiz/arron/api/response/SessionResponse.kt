package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class SessionResponse(
    @SerializedName("session")
    var session: Session = Session(),

    @SerializedName("user")
    var user: UserData = UserData()
)

data class Session(
    @SerializedName("expiresAt")
    var expiresAt: String = "",

    @SerializedName("token")
    var token: String = "",

    @SerializedName("ipAddress")
    var ipAddress: String = "",

    @SerializedName("userAgent")
    var userAgent: String = "",

    @SerializedName("userId")
    var userId: String = "",

    @SerializedName("id")
    var id: String = ""
)

data class UserData(
    @SerializedName("name")
    var name: String = "",

    @SerializedName("email")
    var email: String = "",

    @SerializedName("emailVerified")
    var emailVerified: String = "",

    @SerializedName("image")
    var image: String = "",

    @SerializedName("createdAt")
    var createdAt: String = "",

    @SerializedName("updatedAt")
    var updatedAt: String = ""
)