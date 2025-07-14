package com.aitronbiz.arron.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Device(
  var id: Int = 0,
  var uid: Int = 0,
  var homeId: Int = 0,
  var subjectId: Int = 0,
  var roomId: Int = 0,
  var serverId: String? = "",
  var name: String? = "",
  var productNumber: String? = "",
  var serialNumber: String? = "",
  var activityTime: Int? = 0,
  var createdAt: String? = ""
) : Parcelable