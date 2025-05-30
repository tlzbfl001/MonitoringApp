package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class TokenResponse(
    @SerializedName("token")
    var token: String = ""
)