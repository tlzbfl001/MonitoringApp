package com.aitronbiz.arron.api.dto

import com.google.gson.annotations.SerializedName

data class LoginDTO (
    @SerializedName("provider")
    var provider: String = "",

    @SerializedName("idToken")
    var idToken: IdTokenDTO = IdTokenDTO()
)


data class IdTokenDTO(
    @SerializedName("token")
    var token: String = ""
)