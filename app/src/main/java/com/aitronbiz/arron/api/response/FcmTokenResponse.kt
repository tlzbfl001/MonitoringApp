package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class FcmTokenResponse(
    @SerializedName("success")
    var success: Boolean = false,

    @SerializedName("fcmToken")
    var fcmToken: FcmToken = FcmToken()
)

data class FcmToken(
    @SerializedName("id")
    var id: String = "",

    @SerializedName("token")
    var token: String = "",

    @SerializedName("deviceId")
    var deviceId: String = "",

    @SerializedName("deviceType")
    var deviceType: String = "",

    @SerializedName("deviceName")
    var deviceName: String = "",

    @SerializedName("isActive")
    var isActive: String = "",

    @SerializedName("lastUsedAt")
    var lastUsedAt: String = "",

    @SerializedName("createdAt")
    var createdAt: String = "",

    @SerializedName("updatedAt")
    var updatedAt: String = "",

    @SerializedName("userId")
    var userId: String = ""
)

data class SendNotificationResponse(
    @SerializedName("success")
    var success: Boolean = false,

    @SerializedName("notification")
    var notification: Notification = Notification()
)

data class Notification(
    @SerializedName("id")
    var id: String = "",

    @SerializedName("token")
    var token: String = "",

    @SerializedName("title")
    var title: String = "",

    @SerializedName("body")
    var body: String = "",

    @SerializedName("status")
    var status: String = "",

    @SerializedName("createdAt")
    var createdAt: String = "",

    @SerializedName("updatedAt")
    var updatedAt: String = "",

    @SerializedName("fcmTokenId")
    var fcmTokenId: String = ""
)