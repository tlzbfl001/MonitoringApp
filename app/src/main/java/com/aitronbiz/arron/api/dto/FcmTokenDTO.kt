package com.aitronbiz.arron.api.dto

data class FcmTokenDTO(
    val token: String,
    val deviceId: String,
    val deviceType: String,
    val deviceName: String
)

data class SendNotificationDTO(
    val title: String,
    val body: String,
    val data: String,
    val type: String,
    val userId: String
)
