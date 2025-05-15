package kr.aitron.aitron.database.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "device")
data class Device(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uid: Int,
    var name: String?,
    val subjectId: Int,
    val productNumber: String?,
    val serialNumber: String?,
    val createdAt: String?
) : Parcelable