package com.aitronbiz.arron.api.dto

import com.google.gson.annotations.SerializedName

data class HomeDTO (
    @SerializedName("name")
    var name: String = "",

    @SerializedName("province")
    var province: String = "",

    @SerializedName("city")
    var city: String = "",

    @SerializedName("street")
    var street: String = "",

    @SerializedName("detailAddress")
    var detailAddress: String = "",

    @SerializedName("postalCode")
    var postalCode: String = ""
)

data class HomeDTO2 (
    @SerializedName("name")
    var name: String = "",

    @SerializedName("province")
    var province: String = "",

    @SerializedName("city")
    var city: String = "",

    @SerializedName("street")
    var street: String = "",

    @SerializedName("detailAddress")
    var detailAddress: String = ""
)