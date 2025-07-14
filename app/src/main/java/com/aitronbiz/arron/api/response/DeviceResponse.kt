package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class DeviceResponse(
    @SerializedName("device")
    var device: Device = Device()
)

data class Device(
    @SerializedName("id")
    var id: String = "",

    @SerializedName("name")
    var name: String = "",

    @SerializedName("version")
    var version: String = "",

    @SerializedName("modelName")
    var modelName: String = "",

    @SerializedName("modelNumber")
    var modelNumber: String = "",

    @SerializedName("serialNumber")
    var serialNumber: String = "",

    @SerializedName("createdAt")
    var createdAt: String = "",

    @SerializedName("updatedAt")
    var updatedAt: String = "",

    @SerializedName("roomId")
    var roomId: String = "",

    @SerializedName("homeId")
    var userId: String = ""
)