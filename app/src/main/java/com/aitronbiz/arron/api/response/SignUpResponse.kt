package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class SignUpResponse(
    @SerializedName("token")
    var sessionToken: String = "",

    @SerializedName("user")
    var user: User = User()
)

data class SignInResponse(
    @SerializedName("redirect")
    var redirect: Boolean = false,

    @SerializedName("token")
    var sessionToken: String = "",

    @SerializedName("user")
    var user: User = User()
)

data class User(
    @SerializedName("id")
    var id: String = "",

    @SerializedName("email")
    var email: String = "",

    @SerializedName("name")
    var name: String = "",

    @SerializedName("image")
    var image: String? = "",

    @SerializedName("emailVerified")
    var emailVerified: Boolean = false,

    @SerializedName("createdAt")
    var createdAt: String = "",

    @SerializedName("updatedAt")
    var updatedAt: String = ""
)