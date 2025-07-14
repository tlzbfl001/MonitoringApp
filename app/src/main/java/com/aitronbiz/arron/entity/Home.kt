package com.aitronbiz.arron.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Home(
    var id: Int = 0,
    var uid: Int = 0,
    var serverId: String? = "",
    var name: String? = "",
    var province: String? = "",
    var city: String? = "",
    var street: String? = "",
    var detailAddress: String? = "",
    var postalCode: String? = "",
    var createdAt: String? = ""
) : Parcelable