package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class ErrorResponse(
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String
)

data class StatusResponse(
    @SerializedName("status") val code: String
)

data class SuccessResponse(
    @SerializedName("success") val success: Boolean
)