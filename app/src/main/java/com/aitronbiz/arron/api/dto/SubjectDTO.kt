package com.aitronbiz.arron.api.dto

import com.google.gson.annotations.SerializedName

data class SubjectDTO (
    @SerializedName("name")
    var name: String = ""
)