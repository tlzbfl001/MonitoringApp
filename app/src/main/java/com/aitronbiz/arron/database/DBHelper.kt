package com.aitronbiz.arron.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
   companion object {
      const val DATABASE_NAME = "local.db"
      const val DATABASE_VERSION = 1
      const val USER = "user"
      const val DEVICE = "device"
      const val HOME = "home"
      const val ROOM = "room"
      const val ACTIVITY = "activity"
      const val TEMPERATURE = "temperature"
      const val LIGHT = "light"
      const val DAILY_DATA = "dailyData"
   }

   override fun onCreate(db: SQLiteDatabase) {
      val user = "create table $USER(id integer primary key autoincrement, type text, idToken text, accessToken text, sessionToken, " +
         "username text, email text, contact text, emergencyContact text, notificationStatus text, transmissionPeriod text, createdAt text);"
      db.execSQL(user)

      val home = "create table $HOME(id integer primary key autoincrement, uid integer, name text, createdAt text);"
      db.execSQL(home)

      val room = "create table $ROOM(id integer primary key autoincrement, uid integer, homeId integer, name text, status text, createdAt text);"
      db.execSQL(room)

      val device = "create table $DEVICE(id integer primary key autoincrement, uid integer, homeId integer, roomId integer, name text, productNumber text, " +
         "serialNumber text, activityTime integer, room integer, createdAt text, updatedAt text);"
      db.execSQL(device)

      val activity = "create table $ACTIVITY(id integer primary key autoincrement, uid integer, subjectId integer, deviceId integer, " +
         "activity integer, createdAt text);"
      db.execSQL(activity)

      val temperature = "create table $TEMPERATURE(id integer primary key autoincrement, uid integer, subjectId integer, deviceId integer, " +
         "temperature integer, createdAt text);"
      db.execSQL(temperature)

      val light = "create table $LIGHT(id integer primary key autoincrement, uid integer, subjectId integer, deviceId integer, " +
         "light integer, createdAt text);"
      db.execSQL(light)

      val dailyData = "create table $DAILY_DATA(id integer primary key autoincrement, uid integer, subjectId integer, deviceId integer, " +
         "activityRate integer, createdAt text);"
      db.execSQL(dailyData)
   }

   override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
      db.execSQL("drop table if exists $USER")
      db.execSQL("drop table if exists $HOME")
      db.execSQL("drop table if exists $ROOM")
      db.execSQL("drop table if exists $DEVICE")
      db.execSQL("drop table if exists $ACTIVITY")
      db.execSQL("drop table if exists $TEMPERATURE")
      db.execSQL("drop table if exists $LIGHT")
      db.execSQL("drop table if exists $DAILY_DATA")
      onCreate(db)
   }
}