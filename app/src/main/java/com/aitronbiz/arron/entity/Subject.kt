package com.aitronbiz.arron.entity

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Subject(
    var id: Int = 0,
    var uid: Int = 0,
    var homeId: Int = 0,
    var serverId: String? = "",
    var bitmap: Bitmap? = null,
    var image: String? = "",
    var name: String? = "",
    var birthdate: String? = "",
    var gender: String? = "",
    var bloodType: String? = "",
    var email: String? = "",
    var address: String? = "",
    var contact: String? = "",
    var status: String? = EnumData.NORMAL.name,
    var createdAt: String? = ""
) : Parcelable