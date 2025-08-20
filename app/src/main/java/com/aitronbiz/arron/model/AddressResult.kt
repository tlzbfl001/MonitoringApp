package com.aitronbiz.arron.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AddressResult(
    val postalCode: String = "",
    val fullAddress: String = "",
    val province: String = "",
    val city: String = "",
    val street: String = ""
) : Parcelable