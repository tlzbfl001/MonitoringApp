package com.aitronbiz.arron.api.dto

import com.google.gson.annotations.SerializedName

data class FindPasswordDTO (
    @SerializedName("email")
    var email: String = ""
)