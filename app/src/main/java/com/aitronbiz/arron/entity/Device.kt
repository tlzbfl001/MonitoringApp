package com.aitronbiz.arron.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Device(
  var id: Int = 0,
  var uid: Int = 0,
  var subjectId: Int = 0,
  var name: String? = "",
  var productNumber: String? = "",
  var serialNumber: String? = "",
  var activityTime: Int? = 0,
  var room: Int? = 0,
  var status: String? = EnumData.NORMAL.name,
  var createdAt: String? = "",
  var updatedAt: String? = ""
) : Parcelable