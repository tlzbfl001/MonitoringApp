package kr.aitron.aitron.database.entity

data class Token(
    var id: String? = "",
    var uid: String? = "",
    var accessToken: String? = "",
    var refreshToken: String? = "",
    var accessCreatedAt: String? = "",
    var refreshCreatedAt: String? = ""
)