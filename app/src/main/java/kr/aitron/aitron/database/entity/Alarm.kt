package kr.aitron.aitron.database.entity

data class Alarm(
    var id: String? = "",
    var uid: String? = "",
    var title: String? = "",
    var message: String? = "",
    var time: String? = "",
    var isOn: String? = "",
    var createdAt: String? = "",
    var updatedAt: String? = ""
)