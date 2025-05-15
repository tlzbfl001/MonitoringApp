package kr.aitron.aitron.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update
import kr.aitron.aitron.database.entity.Device

@Dao
interface DeviceDao {
    @Insert
    suspend fun insertDevice(device: Device): Long

    @Update
    suspend fun updateDevice(device: Device)

    @Delete
    suspend fun deleteDevice(device: Device): Int
}