package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class LifePatternsResponse(
    @SerializedName("lifePatterns")
    var lifePatterns: ArrayList<LifePatterns> = ArrayList(),

    @SerializedName("total")
    var total: Int = 0,

    @SerializedName("page")
    var page: Int = 0,

    @SerializedName("limit")
    var limit: Int = 0,

    @SerializedName("totalPages")
    var totalPages: Int = 0
)

data class LifePatterns(
    @SerializedName("id")
    var id: String = "",

    @SerializedName("homeId")
    var homeId: String = "",

    @SerializedName("summaryDate")
    var summaryDate: String = "",

    @SerializedName("totalActiveMinutes")
    var totalActiveMinutes: Int = 0,

    @SerializedName("totalInactiveMinutes")
    var totalInactiveMinutes: Int = 0,

    @SerializedName("averageActivityScore")
    var averageActivityScore: Double = 0.0,

    @SerializedName("maxActivityScore")
    var maxActivityScore: Int = 0,

    @SerializedName("firstActivityTime")
    var firstActivityTime: String = "",

    @SerializedName("lastActivityTime")
    var lastActivityTime: String = "",

    @SerializedName("activitySessionCount")
    var activitySessionCount: Int = 0,

    @SerializedName("averageSessionDuration")
    var averageSessionDuration: Int = 0,

    @SerializedName("maxSessionDuration")
    var maxSessionDuration: Int = 0,

    @SerializedName("estimatedSleepMinutes")
    var estimatedSleepMinutes: Int = 0,

    @SerializedName("estimatedSleepStart")
    var estimatedSleepStart: String? = null,

    @SerializedName("estimatedSleepEnd")
    var estimatedSleepEnd: String? = null,

    @SerializedName("mostActiveHour")
    var mostActiveHour: Int = 0,

    @SerializedName("leastActiveHour")
    var leastActiveHour: Int = 0,

    @SerializedName("activityPatternType")
    var activityPatternType: String = "",

    @SerializedName("activityRegularityScore")
    var activityRegularityScore: Double = 0.0,

    @SerializedName("createdAt")
    var createdAt: String = "",

    @SerializedName("updatedAt")
    var updatedAt: String = ""
)