package com.aitronbiz.arron.api.response

import com.google.gson.annotations.SerializedName

data class SubjectResponse(
    @SerializedName("subject")
    var subject: Subject = Subject()
)

data class Subject(
    @SerializedName("id")
    var id: String = "",

    @SerializedName("name")
    var name: String = "",

    @SerializedName("createdAt")
    var createdAt: String = "",

    @SerializedName("updatedAt")
    var updatedAt: String = "",

    @SerializedName("userId")
    var userId: String = ""
)