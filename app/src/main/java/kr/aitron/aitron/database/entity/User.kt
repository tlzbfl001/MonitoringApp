package kr.aitron.aitron.database.entity

data class User(
    var id: String? = "",
    var username: String? = "",
    var email: String? = "",
    var role: String? = "",
    var contact: String? = "",
    var emergencyContact: String? = "",
    var patientName: String? = "",
    var patientBirthdate: String? = "",
    var patientGender: String? = "",
    var patientBloodType: String? = "",
    var patientEmail: String? = "",
    var patientAddress: String? = "",
    var patientContact: String? = "",
    var createdAt: String? = "",
    var updatedAt: String? = ""
)