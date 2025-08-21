package com.aitronbiz.arron.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HomeForm(
    val homeName: String = "",
    val province: String = "",
    val city: String = "",
    val street: String = "",
    val fullAddress: String = "",
    val postalCode: String = ""
) : Parcelable
