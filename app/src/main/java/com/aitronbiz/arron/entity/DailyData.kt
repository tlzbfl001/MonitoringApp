package com.aitronbiz.arron.entity

data class DailyData(
  var id: Int = 0,
  var uid: Int = 0,
  var subjectId: Int = 0,
  var deviceId: Int = 0,
  var activityRate: Int = 0,
  var createdAt: String? = ""
)