package com.aitronbiz.arron.api.dto

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class HomeDTO1 (
    @SerializedName("name")
    var name: String = ""
) : Parcelable

@Parcelize
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
) : Parcelable

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