package kr.aitron.aitron.database.entity

data class Message(
    var id: String? = "",
    var text: String? = "",
    var date: String? = "",
    var isSentByMe: Boolean
)