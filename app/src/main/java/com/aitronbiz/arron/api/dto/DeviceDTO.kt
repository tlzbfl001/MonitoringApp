package com.aitronbiz.arron.api.dto

import com.google.gson.annotations.SerializedName

data class DeviceDTO (
    @SerializedName("name")
    var name: String = "",

    @SerializedName("roomId")
    var roomId: String = ""
)