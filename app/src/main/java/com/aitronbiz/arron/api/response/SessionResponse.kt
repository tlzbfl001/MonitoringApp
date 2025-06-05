package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("redirect")
    var redirect: String = "",

    @SerializedName("token")
    var sessionToken: String = "",

    @SerializedName("user")
    var user: UserResponse = UserResponse()
)

data class UserResponse(
    @SerializedName("id")
    var id: String = "",

    @SerializedName("email")
    var email: String = "",

    @SerializedName("image")
    var image: String = "",

    @SerializedName("emailVerified")
    var emailVerified: String = "",

    @SerializedName("createdAt")
    var createdAt: String = "",

    @SerializedName("updatedAt")
    var updatedAt: String = ""
)