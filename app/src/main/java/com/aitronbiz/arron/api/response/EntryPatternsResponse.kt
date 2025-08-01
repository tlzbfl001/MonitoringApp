package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class EntryPatternsResponse(
    @SerializedName("patterns")
    val patterns: List<Pattern>,

    @SerializedName("analysisInfo")
    val analysisInfo: AnalysisInfo
)

data class Pattern(
    @SerializedName("id")
    val id: String,

    @SerializedName("patternType")
    val patternType: String,

    @SerializedName("timeSlot")
    val timeSlot: Int,

    @SerializedName("entryCount")
    val entryCount: Int,

    @SerializedName("exitCount")
    val exitCount: Int,

    @SerializedName("averageEntryTime")
    val averageEntryTime: Double,

    @SerializedName("averageExitTime")
    val averageExitTime: Double,

    @SerializedName("entryProbability")
    val entryProbability: Double,

    @SerializedName("exitProbability")
    val exitProbability: Double,

    @SerializedName("confidenceScore")
    val confidenceScore: Double,

    @SerializedName("analysisStartTime")
    val analysisStartTime: String,

    @SerializedName("analysisEndTime")
    val analysisEndTime: String,

    @SerializedName("dataPointsCount")
    val dataPointsCount: Int,

    @SerializedName("metadata")
    val metadata: PatternMetadata,

    @SerializedName("roomId")
    val roomId: String
)

data class PatternMetadata(
    @SerializedName("dayName")
    val dayName: String,

    @SerializedName("isWeekend")
    val isWeekend: Boolean,

    @SerializedName("totalWeeks")
    val totalWeeks: Int
)

data class AnalysisInfo(
    @SerializedName("startDate")
    val startDate: String,

    @SerializedName("endDate")
    val endDate: String,

    @SerializedName("totalWeeks")
    val totalWeeks: Int
)