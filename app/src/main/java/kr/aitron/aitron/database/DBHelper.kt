package kr.aitron.aitron.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
   companion object {
      const val DATABASE_NAME = "local.db"
      const val DATABASE_VERSION = 1
      const val USER = "user"
      const val DEVICE = "device"
      const val SUBJECT = "subject"
   }

   override fun onCreate(db: SQLiteDatabase) {
      val user = "create table $USER(id integer primary key autoincrement, type text, idToken text, accessToken text, " +
         "username text, email text, role text, contact text, emergencyContact text, createdAt text, updatedAt text);"
      db.execSQL(user)

      val device = "create table $DEVICE(id integer primary key autoincrement, uid integer, subjectId integer, name text, " +
         "productNumber text, serialNumber text, createdAt text);"
      db.execSQL(device)

      val subject = "create table $SUBJECT(id integer primary key autoincrement, uid integer, image text, name text, birthdate text," +
      "gender text, bloodType text, email text, address text, contact text, createdAt text, updatedAt text);"
      db.execSQL(subject)
   }

   override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
      db.execSQL("drop table if exists $USER")
      db.execSQL("drop table if exists $DEVICE")
      db.execSQL("drop table if exists $SUBJECT")
      onCreate(db)
   }
}