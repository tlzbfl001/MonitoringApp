package com.aitronbiz.arron.entity

data class Temperature(
  var id: Int = 0,
  var uid: Int = 0,
  var subjectId: Int = 0,
  var deviceId: Int = 0,
  var temperature: Int = 0,
  var createdAt: String? = ""
)