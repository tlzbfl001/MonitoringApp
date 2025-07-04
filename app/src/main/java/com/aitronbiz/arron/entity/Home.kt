package com.aitronbiz.arron.entity

data class Home(
    var id: Int = 0,
    var uid: Int = 0,
    var name: String? = "",
    var province: String? = "",
    var city: String? = "",
    var street: String? = "",
    var detailAddress: String? = "",
    var postalCode: String? = "",
    var createdAt: String? = ""
)