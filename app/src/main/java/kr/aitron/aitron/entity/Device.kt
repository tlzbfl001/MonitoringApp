package kr.aitron.aitron.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Device(
    val id: Int = 0,
    val uid: Int,
    val subjectId: Int,
    var name: String?,
    val productNumber: String?,
    val serialNumber: String?,
    val createdAt: String?
) : Parcelable