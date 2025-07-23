package com.aitronbiz.arron.database

import android.content.ContentValues
import android.content.Context
import com.aitronbiz.arron.database.DBHelper.Companion.ACTIVITY
import com.aitronbiz.arron.database.DBHelper.Companion.DEVICE
import com.aitronbiz.arron.database.DBHelper.Companion.HOME
import com.aitronbiz.arron.database.DBHelper.Companion.ROOM
import com.aitronbiz.arron.database.DBHelper.Companion.USER
import com.aitronbiz.arron.entity.Activity
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.entity.Room
import com.aitronbiz.arron.entity.User

class DataManager(private var context: Context?) {
   private val dbHelper: DBHelper = DBHelper(context!!.applicationContext)

   companion object {
      @Volatile private var instance: DataManager? = null

      fun getInstance(context: Context): DataManager {
         return instance ?: synchronized(this) {
            instance ?: DataManager(context).also { instance = it }
         }
      }
   }

   fun getUser(id: Int) : User {
      val db = dbHelper.readableDatabase
      val value = User()
      val sql = "SELECT * FROM $USER WHERE id = $id"
      val cursor = db.rawQuery(sql, null)
      while(cursor.moveToNext()) {
         value.id = cursor.getInt(0)
         value.type = cursor.getString(1)
         value.idToken = cursor.getString(2)
         value.accessToken = cursor.getString(3)
         value.sessionToken = cursor.getString(4)
         value.username = cursor.getString(5)
         value.email = cursor.getString(6)
         value.contact = cursor.getString(7)
         value.emergencyContact = cursor.getString(8)
         value.notificationStatus = cursor.getString(9)
         value.createdAt = cursor.getString(10)
      }
      cursor.close()
      return value
   }

   fun getUserId(type: String, email: String) : Int {
      val db = dbHelper.readableDatabase
      var value = 0
      val sql = "select id from $USER where type = '$type' and email = '$email'"
      val cursor = db!!.rawQuery(sql, null)
      while(cursor.moveToNext()) {
         value = cursor.getInt(0)
      }
      cursor.close()
      return value
   }

   fun insertUser(data: User): Boolean {
      val db = dbHelper.writableDatabase
      val values = ContentValues()
      values.put("type", data.type)
      values.put("idToken", data.idToken)
      values.put("accessToken", data.accessToken)
      values.put("sessionToken", data.sessionToken)
      values.put("username", data.username)
      values.put("email", data.email)
      values.put("createdAt", data.createdAt)

      val result = db!!.insert(USER, null, values)
      return result != -1L
   }

   fun updateSocialLoginUser(data: User){
      val db = dbHelper.writableDatabase
      val sql = "update $USER set idToken='${data.idToken}', accessToken='${data.accessToken}', username='${data.username}' " +
         "where type='${data.type}' and email='${data.email}'"
      db.execSQL(sql)
   }

   fun updateData(table: String, column: String, data: String, id: Int): Boolean {
      val db = dbHelper.writableDatabase
      val values = ContentValues().apply {
         put(column, data)
      }
      val rowsAffected = db.update(table, values, "id = ?", arrayOf(id.toString()))
      return rowsAffected > 0
   }
}