package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class AddressResponse(
    @SerializedName("addresses")
    var addresses: ArrayList<Address> = ArrayList()
)

data class Address(
    @SerializedName("province")
    var province: String = "",

    @SerializedName("city")
    var city: String = "",

    @SerializedName("street")
    var street: String = "",

    @SerializedName("postalCode")
    var postalCode: String = "",

    @SerializedName("latitude")
    var latitude: Double = 0.0,

    @SerializedName("longitude")
    var longitude: Double = 0.0
)