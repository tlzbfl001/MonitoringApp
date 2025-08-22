package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class DeviceResponse(
    @SerializedName("device")
    var device: Device = Device()
)

data class DevicesResponse(
    @SerializedName("devices")
    var devices: ArrayList<Device> = ArrayList()
)

data class Device(
    @SerializedName("id")
    var id: String = "",

    @SerializedName("name")
    var name: String = "",

    @SerializedName("serialNumber")
    var serialNumber: String = "",

    @SerializedName("createdAt")
    var createdAt: String = "",

    @SerializedName("updatedAt")
    var updatedAt: String = "",

    @SerializedName("homeId")
    var homeId: String = "",

    @SerializedName("roomId")
    var roomId: String = "",

    @SerializedName("userId")
    var userId: String = ""
)