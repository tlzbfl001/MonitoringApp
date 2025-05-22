package com.aitronbiz.arron.database

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.util.Log
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.database.DBHelper.Companion.DEVICE
import com.aitronbiz.arron.database.DBHelper.Companion.SUBJECT
import com.aitronbiz.arron.database.DBHelper.Companion.USER
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.Subject
import com.aitronbiz.arron.entity.User
import com.aitronbiz.arron.util.CustomUtil.TAG

class DataManager(private var context: Context?) {
   private var dbHelper: DBHelper? = null

   @Throws(SQLException::class)
   fun open(): DataManager {
      dbHelper = DBHelper(context)
      return this
   }

   fun getUser() : User {
      val db = dbHelper!!.readableDatabase
      val value = User()
      val sql = "SELECT * FROM $USER WHERE id = ${AppController.prefs.getUserPrefs()}"
      val cursor = db!!.rawQuery(sql, null)
      while(cursor.moveToNext()) {
         value.id = cursor.getInt(0)
         value.username = cursor.getString(1)
         value.email = cursor.getString(2)
         value.role = cursor.getString(3)
         value.contact = cursor.getString(4)
         value.emergencyContact = cursor.getString(5)
         value.createdAt = cursor.getString(6)
      }
      cursor.close()
      db.close()
      return value
   }

   fun getUserId(type: String, email: String) : Int {
      val db = dbHelper!!.readableDatabase
      var value = 0
      val sql = "select id from $USER where type = '$type' and email = '$email'"
      val cursor = db!!.rawQuery(sql, null)
      while(cursor.moveToNext()) {
         value = cursor.getInt(0)
      }
      cursor.close()
      db.close()
      return value
   }

   fun getSubject(id: Int) : Subject {
      val db = dbHelper!!.readableDatabase
      val value = Subject()
      val sql = "SELECT * FROM $SUBJECT WHERE id = $id"
      val cursor = db!!.rawQuery(sql, null)
      while(cursor.moveToNext()) {
         value.id = cursor.getInt(0)
         value.uid = cursor.getInt(1)
         value.image = cursor.getString(2)
         value.name = cursor.getString(3)
         value.birthdate = cursor.getString(4)
         value.bloodType = cursor.getString(5)
         value.address = cursor.getString(6)
         value.contact = cursor.getString(7)
         value.createdAt = cursor.getString(8)
      }
      cursor.close()
      db.close()
      return value
   }

   fun getSubjects(uid: Int) : ArrayList<Subject> {
      val db = dbHelper!!.readableDatabase
      val list = ArrayList<Subject>()
      val sql = "SELECT * FROM $SUBJECT WHERE uid = $uid"
      val cursor = db!!.rawQuery(sql, null)
      while(cursor.moveToNext()) {
         val value = Subject()
         value.id = cursor.getInt(0)
         value.uid = cursor.getInt(1)
         value.image = cursor.getString(2)
         value.name = cursor.getString(3)
         value.birthdate = cursor.getString(4)
         value.bloodType = cursor.getString(5)
         value.address = cursor.getString(6)
         value.contact = cursor.getString(7)
         value.createdAt = cursor.getString(8)
         list.add(value)
      }
      cursor.close()
      db.close()
      return list
   }

   fun getDevices(subjectId: Int) : ArrayList<Device> {
      val db = dbHelper!!.readableDatabase
      val list = ArrayList<Device>()
      val sql = "SELECT * FROM $DEVICE WHERE subjectId = $subjectId"
      val cursor = db!!.rawQuery(sql, null)
      while(cursor.moveToNext()) {
         val value = Device()
         value.id = cursor.getInt(0)
         value.uid = cursor.getInt(1)
         value.subjectId = cursor.getInt(2)
         value.name = cursor.getString(3)
         value.productNumber = cursor.getString(4)
         value.serialNumber = cursor.getString(5)
         value.activityTime = cursor.getInt(6)
         value.status = cursor.getString(7)
         value.sign = cursor.getString(8)
         value.createdAt = cursor.getString(9)
         value.updatedAt = cursor.getString(10)
         list.add(value)
      }
      cursor.close()
      db.close()
      return list
   }

   fun insertUser(data: User): Boolean {
      val db = dbHelper!!.writableDatabase
      val values = ContentValues()
      values.put("type", data.type)
      values.put("idToken", data.idToken)
      values.put("accessToken", data.accessToken)
      values.put("username", data.username)
      values.put("email", data.email)
      values.put("role", data.role)
      values.put("createdAt", data.createdAt)

      val result = db!!.insert(USER, null, values)
      db.close()
      return result != -1L
   }

   fun insertSubject(data: Subject): Boolean {
      val db = dbHelper!!.writableDatabase
      val values = ContentValues()
      values.put("uid", data.uid)
      values.put("image", data.image)
      values.put("name", data.name)
      values.put("birthdate", data.birthdate)
      values.put("bloodType", data.bloodType)
      values.put("address", data.address)
      values.put("contact", data.contact)
      values.put("createdAt", data.createdAt)

      val result = db!!.insert(SUBJECT, null, values)
      db.close()
      return result != -1L
   }

   fun insertDevice(data: Device): Boolean {
      val db = dbHelper!!.writableDatabase
      val values = ContentValues()
      values.put("uid", data.uid)
      values.put("subjectId", data.subjectId)
      values.put("name", data.name)
      values.put("productNumber", data.productNumber)
      values.put("serialNumber", data.serialNumber)
      values.put("activityTime", data.activityTime)
      values.put("status", data.status)
      values.put("sign", data.sign)
      values.put("createdAt", data.createdAt)

      val result = db!!.insert(DEVICE, null, values)
      db.close()
      return result != -1L
   }

   fun updateUser(data: User){
      val db = dbHelper!!.writableDatabase
      val sql = "update $USER set idToken='${data.idToken}', accessToken='${data.accessToken}', username='${data.username}', " +
         "role='${data.role}' where type='${data.type}' and email='${data.email}'"
      db.execSQL(sql)
      db.close()
   }
}
