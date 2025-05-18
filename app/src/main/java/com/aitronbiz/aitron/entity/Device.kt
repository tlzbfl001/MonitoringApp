package com.aitronbiz.aitron.entity

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
  var createdAt: String? = ""
) : Parcelable