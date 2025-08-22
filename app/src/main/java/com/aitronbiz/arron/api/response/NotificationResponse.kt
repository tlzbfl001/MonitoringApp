package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class NotificationResponse(
    @SerializedName("notifications")
    var notifications: ArrayList<NotificationData> = ArrayList()
)

data class NotificationData(
    @SerializedName("id")
    var id: String? = "",

    @SerializedName("title")
    var title: String? = "",

    @SerializedName("body")
    var body: String? = "",

    @SerializedName("type")
    var type: String? = "",

    @SerializedName("status")
    var status: String? = "",

    @SerializedName("messageId")
    var messageId: String? = "",

    @SerializedName("errorMessage")
    var errorMessage: String? = "",

    @SerializedName("isRead")
    var isRead: Boolean? = false,

    @SerializedName("readAt")
    var readAt: String? = "",

    @SerializedName("sentAt")
    var sentAt: String? = "",

    @SerializedName("createdAt")
    var createdAt: String? = "",

    @SerializedName("updatedAt")
    var updatedAt: String? = "",

    @SerializedName("userId")
    var userId: String? = "",

    @SerializedName("fcmTokenId")
    var fcmTokenId: String? = ""
)

data class ReadNotificationResponse(
    @SerializedName("success")
    var success: Boolean? = false,

    @SerializedName("message")
    var message: String? = ""
)