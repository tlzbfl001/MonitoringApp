package com.aitronbiz.arron.api.dto

import com.google.gson.annotations.SerializedName

data class FindPasswordDTO (
    @SerializedName("email")
    var email: String = ""
)

data class ResetPasswordDTO (
    @SerializedName("email")
    var email: String = "",

    @SerializedName("otp")
    var otp: String = "",

    @SerializedName("password")
    var password: String = ""
)