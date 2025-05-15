package kr.aitron.aitron.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kr.aitron.aitron.database.entity.Subject

@Dao
interface SubjectDao {
    @Insert
    suspend fun insertSubject(subject: Subject): Long

    @Query("SELECT * FROM subject WHERE uid = :uid LIMIT 1")
    suspend fun getSubjectByUid(uid: Int): Subject?

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject): Int
}