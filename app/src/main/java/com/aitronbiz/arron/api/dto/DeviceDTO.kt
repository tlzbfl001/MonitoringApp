package com.aitronbiz.arron.api.dto

import com.google.gson.annotations.SerializedName

data class DeviceDTO (
    @SerializedName("name")
    var name: String = "",

    @SerializedName("version")
    var version: String = "",

    @SerializedName("modelName")
    var modelName: String = "",

    @SerializedName("modelNumber")
    var modelNumber: String = "",

    @SerializedName("serialNumber")
    var serialNumber: String = ""
)