package com.aitronbiz.arron.entity

import java.io.Serializable

data class User(
    var id: Int = 0,
    var type: String = "",
    var idToken: String = "",
    var accessToken: String = "",
    var sessionToken: String = "",
    var email: String = "",
    var createdAt: String? = ""
) : Serializable