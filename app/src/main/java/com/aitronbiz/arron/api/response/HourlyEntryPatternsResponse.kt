package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class HourlyEntryPatternsResponse(
    @SerializedName("patterns")
    val patterns: List<HourlyPattern>
)

data class WeeklyEntryPatternsResponse(
    @SerializedName("patterns")
    val patterns: List<WeeklyPattern>
)

data class HourlyPattern(
    @SerializedName("id")
    val id: String? = "",

    @SerializedName("patternType")
    val patternType: String? = "",

    @SerializedName("timeSlot")
    val timeSlot: Int? = 0,

    @SerializedName("entryCount")
    val entryCount: Int? = 0,

    @SerializedName("exitCount")
    val exitCount: Int? = 0,

    @SerializedName("entryProbability")
    val entryProbability: Double? = 0.0,

    @SerializedName("exitProbability")
    val exitProbability: Double? = 0.0,

    @SerializedName("confidenceScore")
    val confidenceScore: Double? = 0.0,

    @SerializedName("analysisStartTime")
    val analysisStartTime: String? = "",

    @SerializedName("analysisEndTime")
    val analysisEndTime: String? = "",

    @SerializedName("dataPointsCount")
    val dataPointsCount: Int? = 0,

    @SerializedName("metadata")
    val metadata: HourlyMetadata? = null,
)

data class HourlyMetadata(
    @SerializedName("peakHour")
    val peakHour: Int? = 0,

    @SerializedName("totalDays")
    val totalDays: Int? = 0
)

data class WeeklyPattern(
    @SerializedName("id")
    val id: String? = "",

    @SerializedName("patternType")
    val patternType: String? = "",

    @SerializedName("timeSlot")
    val timeSlot: Int? = 0,

    @SerializedName("entryCount")
    val entryCount: Int? = 0,

    @SerializedName("exitCount")
    val exitCount: Int? = 0,

    @SerializedName("entryProbability")
    val entryProbability: Double? = 0.0,

    @SerializedName("exitProbability")
    val exitProbability: Double? = 0.0,

    @SerializedName("confidenceScore")
    val confidenceScore: Double? = 0.0,

    @SerializedName("analysisStartTime")
    val analysisStartTime: String? = "",

    @SerializedName("analysisEndTime")
    val analysisEndTime: String? = "",

    @SerializedName("dataPointsCount")
    val dataPointsCount: Int? = 0,

    @SerializedName("metadata")
    val metadata: WeeklyMetadata? = null,
)

data class WeeklyMetadata(
    @SerializedName("dayName")
    val dayName: String? = "",

    @SerializedName("isWeekend")
    val isWeekend: Boolean? = false,

    @SerializedName("totalWeeks")
    val totalWeeks: String? = ""
)