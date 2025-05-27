package com.aitronbiz.arron.entity

data class Token(
    var id: Int? = 0,
    var uid: Int? = 0,
    var accessToken: String? = "",
    var refreshToken: String? = "",
    var accessCreatedAt: String? = "",
    var refreshCreatedAt: String? = ""
)