package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class HomeResponse(
    @SerializedName("home")
    var home: Home = Home()
)

data class HomesResponse(
    @SerializedName("homes")
    var homes: ArrayList<Home> = ArrayList()
)

data class Home(
    @SerializedName("id")
    var id: String = "",

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
    var postalCode: String = "",

    @SerializedName("createdAt")
    var createdAt: String = "",

    @SerializedName("updatedAt")
    var updatedAt: String = "",

    @SerializedName("userId")
    var userId: String = ""
)