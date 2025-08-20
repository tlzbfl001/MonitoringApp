package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class FallsResponse(
    @SerializedName("alerts")
    var alerts: ArrayList<Alerts> = ArrayList()
)

data class Alerts(
    @SerializedName("id")
    var id: String = "",

    @SerializedName("detectedAt")
    var detectedAt: String = "",

    @SerializedName("confidenceScore")
    var confidenceScore: Float = 0f,

    @SerializedName("fallType")
    var fallType: String = "",

    @SerializedName("severityLevel")
    var severityLevel: String = "",

    @SerializedName("alertSent")
    var alertSent: String = ""
)