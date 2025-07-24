package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class ActivityResponse(
    @SerializedName("activityScores")
    var activityScores: ArrayList<Activity> = ArrayList()
)

data class Activity(
    @SerializedName("id")
    var id: String = "",

    @SerializedName("activityScore")
    var activityScore: Int = 0,

    @SerializedName("sensorDataCount")
    var sensorDataCount: Int = 0,

    @SerializedName("activeDeviceCount")
    var activeDeviceCount: Int = 0,

    @SerializedName("averageDistance")
    var averageDistance: Double = 0.0,

    @SerializedName("averageIntensity")
    var averageIntensity: Double = 0.0,

    @SerializedName("movementVariance")
    var movementVariance: Double = 0.0,

    @SerializedName("dataDistributionScore")
    var dataDistributionScore: Double = 0.0,

    @SerializedName("startTime")
    var startTime: String = "",

    @SerializedName("endTime")
    var endTime: String = "",

    @SerializedName("createdAt")
    var createdAt: String = "",

    @SerializedName("updatedAt")
    var updatedAt: String = "",

    @SerializedName("roomId")
    var roomId: String = ""
)