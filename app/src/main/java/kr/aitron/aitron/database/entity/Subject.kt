package kr.aitron.aitron.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subject")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uid: Int = 0,
    val image: String? = "",
    val name: String? = "",
    val birthdate: String? = "",
    val bloodType: String? = "",
    val address: String? = "",
    val contact: String? = "",
    val createdAt: String? = "",
    val updatedAt: String? = ""
)