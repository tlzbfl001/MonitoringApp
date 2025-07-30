package com.aitronbiz.arron.database

import com.aitronbiz.arron.entity.Activity
import androidx.core.database.sqlite.transaction

class RealTimeDao(private val dbHelper: DBHelper) {
    private val buffer = mutableListOf<Activity>()
    private val BATCH_SIZE = 20
    private val lock = Any()

    fun insert(activity: Activity) {
        synchronized(lock) {
            buffer.add(activity)
            if (buffer.size >= BATCH_SIZE) {
                insertBatch()
            }
        }
    }

    private fun insertBatch() {
        val db = dbHelper.writableDatabase
        synchronized(lock) {
            db.transaction() {
//                try {
//                    for (activity in buffer) {
//                        val values = ContentValues().apply {
//                            put("uid", activity.uid)
//                            put("subjectId", activity.subjectId)
//                            put("data", activity.activity)
//                            put("createdAt", activity.createdAt)
//                        }
//                        insert(TEST, null, values)
//                    }
//                    buffer.clear()
//                } catch (e: Exception) {
//                    Log.e(TAG, "Insert batch failed", e)
//                } finally {
//                }
            }
        }
    }

    fun flush() {
        if (buffer.isNotEmpty()) {
            insertBatch()
        }
    }
}