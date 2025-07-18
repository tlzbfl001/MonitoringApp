package com.aitronbiz.arron.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
   companion object {
      const val DATABASE_NAME = "local.db"
      const val DATABASE_VERSION = 1
      const val USER = "user"
      const val HOME = "home"
      const val SUBJECT = "subject"
      const val ROOM = "room"
      const val DEVICE = "device"
      const val ACTIVITY = "activity"
      const val RESPIRATION = "respiration"
   }

   override fun onCreate(db: SQLiteDatabase) {
      val user = "create table $USER(id integer primary key autoincrement, type text, idToken text, accessToken text, sessionToken, " +
         "username text, email text, contact text, emergencyContact text, notificationStatus text, createdAt text);"
      db.execSQL(user)

      val home = "create table $HOME(id integer primary key autoincrement, uid integer, serverId text, name text, province text, " +
         "city text, street text, detailAddress text, postalCode text, createdAt text);"
      db.execSQL(home)

      val subject = "create table $SUBJECT(id integer primary key autoincrement, uid integer, homeId integer, serverId text, " +
         "name text, createdAt text);"
      db.execSQL(subject)

      val room = "create table $ROOM(id integer primary key autoincrement, uid integer, homeId integer, serverId text, name text, createdAt text);"
      db.execSQL(room)

      val device = "create table $DEVICE(id integer primary key autoincrement, uid integer, homeId integer, roomId integer, " +
         "serverId text, name text, productNumber text, serialNumber text, createdAt text);"
      db.execSQL(device)

      val activity = "create table $ACTIVITY(id integer primary key autoincrement, uid integer, homeId integer, roomId integer, " +
         "activity integer, createdAt text);"
      db.execSQL(activity)

      val respiration = "create table $RESPIRATION(id integer primary key autoincrement, uid integer, homeId integer, roomId integer, " +
         "respiration integer, createdAt text);"
      db.execSQL(respiration)
   }

   override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
      db.execSQL("drop table if exists $USER")
      db.execSQL("drop table if exists $HOME")
      db.execSQL("drop table if exists $SUBJECT")
      db.execSQL("drop table if exists $ROOM")
      db.execSQL("drop table if exists $DEVICE")
      db.execSQL("drop table if exists $ACTIVITY")
      db.execSQL("drop table if exists $RESPIRATION")
      onCreate(db)
   }
}