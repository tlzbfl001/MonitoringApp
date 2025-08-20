package com.aitronbiz.arron.api.dto

import com.google.gson.annotations.SerializedName

data class CheckOtpDTO (
    @SerializedName("email")
    var email: String = "",

    @SerializedName("otp")
    var otp: String = "",

    @SerializedName("type")
    var type: String = ""
)