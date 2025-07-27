package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class PresenceResponse(
    @SerializedName("isPresent")
    var isPresent: Boolean = false,

    @SerializedName("confidenceScore")
    var confidenceScore: Double = 0.0,

    @SerializedName("activityScore")
    var activityScore: Int = 0,

    @SerializedName("breathingDataCount")
    var breathingDataCount: Int = 0,

    @SerializedName("sensorDataCount")
    var sensorDataCount: Int = 0,

    @SerializedName("decidedAt")
    var decidedAt: String = ""
)