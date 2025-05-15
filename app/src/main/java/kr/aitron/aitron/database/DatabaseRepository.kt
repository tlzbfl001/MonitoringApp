package kr.aitron.aitron.database

import android.content.Context
import androidx.room.Room
import kr.aitron.aitron.database.entity.Device
import kr.aitron.aitron.database.entity.Subject

class DatabaseRepository(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java, "local.db"
    ).fallbackToDestructiveMigration().build()

    private val subjectDao = db.subjectDao()
    private val deviceDao = db.deviceDao()

    suspend fun insertSubject(subject: Subject): Long {
        return subjectDao.insertSubject(subject)
    }

    suspend fun getSubjectByUid(uid: Int): Subject? {
        return subjectDao.getSubjectByUid(uid)
    }

    suspend fun insertDevice(device: Device): Long {
        return deviceDao.insertDevice(device)
    }
}