package com.aitronbiz.arron.api.dto

import com.google.gson.annotations.SerializedName

data class SignUpDTO (
    @SerializedName("name")
    var name: String = "",

    @SerializedName("email")
    var email: String = "",

    @SerializedName("password")
    var password: String = ""
)

data class SignInDTO (
    @SerializedName("email")
    var email: String = "",

    @SerializedName("password")
    var password: String = ""
)