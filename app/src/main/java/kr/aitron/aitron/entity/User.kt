package kr.aitron.aitron.entity

data class User(
    var id: Int? = 0,
    var type: String = "",
    var idToken: String = "",
    var accessToken: String = "",
    var username: String? = "",
    var email: String? = "",
    var role: String? = "",
    var contact: String? = "",
    var emergencyContact: String? = "",
    var createdAt: String? = "",
    var updatedAt: String? = ""
)