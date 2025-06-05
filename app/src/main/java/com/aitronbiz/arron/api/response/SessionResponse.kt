package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class SessionResponse(
    @SerializedName("session")
    var session: Session = Session()
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