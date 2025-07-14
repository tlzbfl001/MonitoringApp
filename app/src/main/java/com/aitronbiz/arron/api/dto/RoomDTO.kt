package com.aitronbiz.arron.api.dto

import com.google.gson.annotations.SerializedName

data class RoomDTO (
    @SerializedName("name")
    var name: String = "",

    @SerializedName("homeId")
    var homeId: String = ""
)

data class UpdateRoomDTO (
    @SerializedName("name")
    var name: String = ""
)