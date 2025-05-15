package kr.aitron.aitron.database

import androidx.room.Database
import androidx.room.RoomDatabase
import kr.aitron.aitron.database.dao.DeviceDao
import kr.aitron.aitron.database.dao.SubjectDao
import kr.aitron.aitron.database.entity.Subject
import kr.aitron.aitron.database.entity.Device

@Database(entities = [Subject::class, Device::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun deviceDao(): DeviceDao
}