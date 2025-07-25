package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class RespirationResponse(
    @SerializedName("breathing")
    var breathing: ArrayList<Breathing> = ArrayList()
)

data class Breathing(
    @SerializedName("id")
    var id: String = "",

    @SerializedName("breathingRate")
    var breathingRate: Int = 0,

    @SerializedName("confidenceScore")
    var confidenceScore: Double = 0.0,

    @SerializedName("sensorDataCount")
    var sensorDataCount: Int = 0,

    @SerializedName("startTime")
    var startTime: String = "",

    @SerializedName("endTime")
    var endTime: String = "",

    @SerializedName("createdAt")
    var createdAt: String = "",

    @SerializedName("updatedAt")
    var updatedAt: String = "",

    @SerializedName("deviceId")
    var deviceId: String = ""
)