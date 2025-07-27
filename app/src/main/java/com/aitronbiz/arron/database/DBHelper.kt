package com.aitronbiz.arron.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
   companion object {
      const val DATABASE_NAME = "local.db"
      const val DATABASE_VERSION = 1
      const val USER = "user"
   }

   override fun onCreate(db: SQLiteDatabase) {
      val user = "create table $USER(id integer primary key autoincrement, type text, idToken text, accessToken text, sessionToken, " +
         "email text, createdAt text);"
      db.execSQL(user)
   }

   override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
      db.execSQL("drop table if exists $USER")
      onCreate(db)
   }
}