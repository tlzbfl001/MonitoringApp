package com.aitronbiz.arron.entity

data class User(
    var id: Int = 0,
    var type: String = "",
    var idToken: String = "",
    var accessToken: String = "",
    var username: String? = "",
    var email: String = "",
    var contact: String? = "",
    var emergencyContact: String? = "",
    var createdAt: String? = ""
)