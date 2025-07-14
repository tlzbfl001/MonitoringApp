package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class RoomResponse(
    @SerializedName("room")
    var room: Room = Room()
)

data class Room(
    @SerializedName("id")
    var id: String = "",

    @SerializedName("name")
    var name: String = "",

    @SerializedName("createdAt")
    var createdAt: String = "",

    @SerializedName("updatedAt")
    var updatedAt: String = "",

    @SerializedName("homeId")
    var homeId: String = ""
)