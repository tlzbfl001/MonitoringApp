package com.aitronbiz.arron.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Room(
    var id: Int = 0,
    var uid: Int = 0,
    var homeId: Int = 0,
    var subjectId: Int = 0,
    var serverId: String? = "",
    var name: String? = "",
    var createdAt: String? = ""
) : Parcelable