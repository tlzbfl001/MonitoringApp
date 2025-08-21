package com.aitronbiz.arron.database

import android.content.ContentValues
import android.content.Context
import com.aitronbiz.arron.database.DBHelper.Companion.USER
import com.aitronbiz.arron.model.User

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
         value.email = cursor.getString(5)
         value.createdAt = cursor.getString(6)
      }
      cursor.close()
      return value
   }

   fun getUserId(type: String, email: String) : Int {
      val db = dbHelper.readableDatabase
      var value = 0
      val sql = "SELECT id FROM $USER WHERE type = '$type' AND email = '$email'"
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
      values.put("email", data.email)
      values.put("createdAt", data.createdAt)

      val result = db!!.insert(USER, null, values)
      return result != -1L
   }

   fun updateSocialLoginUser(data: User){
      val db = dbHelper.writableDatabase
      val sql = "UPDATE $USER SET idToken='${data.idToken}', accessToken='${data.accessToken}' " +
         "WHERE type='${data.type}' AND email='${data.email}'"
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