package com.aitronbiz.arron.api.dto

import com.google.gson.annotations.SerializedName

data class DeviceDTO (
    @SerializedName("name")
    var name: String = "",

    @SerializedName("serialNumber")
    var serialNumber: String = "",

    @SerializedName("homeId")
    var homeId: String = "",

    @SerializedName("roomId")
    var roomId: String = ""
)

data class DeviceDTO2 (
    @SerializedName("name")
    var name: String = "",

    @SerializedName("homeId")
    var homeId: String = "",

    @SerializedName("roomId")
    var roomId: String = ""
)

data class UpdateDeviceDTO (
    @SerializedName("name")
    var name: String = "",

    @SerializedName("serialNumber")
    var serialNumber: String = ""
)