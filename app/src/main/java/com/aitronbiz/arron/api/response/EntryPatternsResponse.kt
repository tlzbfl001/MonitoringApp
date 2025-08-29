package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class EntryPatternsResponse(
    @SerializedName("presences")
    var presences: ArrayList<Presence> = ArrayList()
)

data class Presence(
    @SerializedName("id")
    var id: String = "",

    @SerializedName("isPresent")
    var isPresent: Boolean = false,

    @SerializedName("startTime")
    var startTime: String = "",

    @SerializedName("endTime")
    var endTime: String = ""
)