package com.aitronbiz.arron.entity

data class Activity(
  var id: Int = 0,
  var uid: Int = 0,
  var subjectId: Int = 0,
  var deviceId: Int = 0,
  var activity: Int = 0,
  var createdAt: String? = ""
)