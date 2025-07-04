package com.aitronbiz.arron.entity

data class Room(
    var id: Int = 0,
    var uid: Int = 0,
    var homeId: Int = 0,
    var name: String? = "",
    var status: String? = "",
    var createdAt: String? = ""
)