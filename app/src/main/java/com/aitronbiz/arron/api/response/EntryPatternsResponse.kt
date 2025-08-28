package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class EntryPatternsResponse(
    @SerializedName("homeId")
    val homeId: String? = "",

    @SerializedName("homeName")
    val homeName: String? = "",

    @SerializedName("totalRooms")
    val totalRooms: Int? = 0
)