package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class RespirationResponse(
    @SerializedName("breathing")
    var breathing: ArrayList<Breathing> = ArrayList()
)

data class RealTimeRespirationResponse(
    @SerializedName("type")
    var type: String = "",

    @SerializedName("roomId")
    var roomId: String = "",

    @SerializedName("deviceId")
    var deviceId: String = "",

    @SerializedName("data")
    var data: Data,

    @SerializedName("timestamp")
    var timestamp: String? = ""
)

data class Data(
    @SerializedName("id")
    var id: String = "",

    @SerializedName("breathingRate")
    var breathingRate: Float = 0f,

    @SerializedName("confidenceScore")
    var confidenceScore: Float = 0f,

    @SerializedName("sensorDataCount")
    var sensorDataCount: Int = 0,

    @SerializedName("startTime")
    var startTime: String? = "",

    @SerializedName("endTime")
    var endTime: String? = "",

    @SerializedName("deviceId")
    var deviceId: String = "",

    @SerializedName("spectrumData")
    var spectrumData: SpectrumData? = null
)

data class SpectrumData(
    @SerializedName("magnitudes")
    var magnitudes: List<Float> = listOf(),

    @SerializedName("frequencies")
    var frequencies: List<Float> = listOf()
)

data class Breathing(
    @SerializedName("id")
    var id: String = "",

    @SerializedName("breathingRate")
    var breathingRate: Float = 0f,

    @SerializedName("startTime")
    var startTime: String = "",

    @SerializedName("endTime")
    var endTime: String = ""
)