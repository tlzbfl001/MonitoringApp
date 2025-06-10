package com.aitronbiz.arron.database

import android.content.ContentValues
import android.content.Context
import com.aitronbiz.arron.database.DBHelper.Companion.ACTIVITY
import com.aitronbiz.arron.database.DBHelper.Companion.DAILY_DATA
import com.aitronbiz.arron.database.DBHelper.Companion.DEVICE
import com.aitronbiz.arron.database.DBHelper.Companion.LIGHT
import com.aitronbiz.arron.database.DBHelper.Companion.SUBJECT
import com.aitronbiz.arron.database.DBHelper.Companion.TEMPERATURE
import com.aitronbiz.arron.database.DBHelper.Companion.USER
import com.aitronbiz.arron.entity.Activity
import com.aitronbiz.arron.entity.DailyData
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.Light
import com.aitronbiz.arron.entity.Subject
import com.aitronbiz.arron.entity.Temperature
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
         value.createdAt = cursor.getString(9)
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

   fun getSubject(id: Int) : Subject {
      val db = dbHelper.readableDatabase
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
         value.status = cursor.getString(8)
         value.createdAt = cursor.getString(9)
      }
      cursor.close()
      return value
   }

   fun getRoomStatus(id: Int) : Int {
      val db = dbHelper.readableDatabase
      var value = 0
      val sql = "SELECT room FROM $DEVICE WHERE id = $id"
      val cursor = db!!.rawQuery(sql, null)
      while(cursor.moveToNext()) {
         value = cursor.getInt(0)
      }
      cursor.close()
      return value
   }

   fun getSubjects(uid: Int) : ArrayList<Subject> {
      val db = dbHelper.readableDatabase
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
         value.gender = cursor.getString(5)
         value.bloodType = cursor.getString(6)
         value.email = cursor.getString(7)
         value.address = cursor.getString(8)
         value.contact = cursor.getString(9)
         value.status = cursor.getString(10)
         value.createdAt = cursor.getString(11)
         list.add(value)
      }
      cursor.close()
      return list
   }

   fun getDevices(subjectId: Int) : ArrayList<Device> {
      val db = dbHelper.readableDatabase
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
         value.room = cursor.getInt(7)
         value.createdAt = cursor.getString(8)
         value.updatedAt = cursor.getString(9)
         list.add(value)
      }
      cursor.close()
      return list
   }

   fun getWeeklyActivity(deviceId: Int, createdAt: String) : Int {
      val db = dbHelper.readableDatabase
      var data = 0
      val sql = "SELECT activity FROM $ACTIVITY WHERE deviceId = $deviceId AND strftime('%Y-%m-%d', createdAt) = '$createdAt'"
      val cursor = db!!.rawQuery(sql, null)
      while(cursor.moveToNext()) {
         data += cursor.getInt(0)
      }
      cursor.close()
      return data
   }

   fun getDailyActivities(deviceId: Int, createdAt: String) : ArrayList<Activity> {
      val db = dbHelper.readableDatabase
      val list = ArrayList<Activity>()
      val sql = "SELECT activity, createdAt FROM $ACTIVITY WHERE deviceId = $deviceId AND strftime('%Y-%m-%d', createdAt) = '$createdAt'"
      val cursor = db!!.rawQuery(sql, null)
      while(cursor.moveToNext()) {
         val value = Activity()
         value.activity = cursor.getInt(0)
         value.createdAt = cursor.getString(1)
         list.add(value)
      }
      cursor.close()
      return list
   }

   fun getDailyTemperature(deviceId: Int, createdAt: String) : ArrayList<Temperature> {
      val db = dbHelper.readableDatabase
      val list = ArrayList<Temperature>()
      val sql = "SELECT temperature, createdAt FROM $TEMPERATURE WHERE deviceId = $deviceId AND strftime('%Y-%m-%d', createdAt) = '$createdAt'"
      val cursor = db!!.rawQuery(sql, null)
      while(cursor.moveToNext()) {
         val value = Temperature()
         value.temperature = cursor.getInt(0)
         value.createdAt = cursor.getString(1)
         list.add(value)
      }
      cursor.close()
      return list
   }

   fun getDailyLight(deviceId: Int, createdAt: String) : ArrayList<Light> {
      val db = dbHelper.readableDatabase
      val list = ArrayList<Light>()
      val sql = "SELECT light, createdAt FROM $LIGHT WHERE deviceId = $deviceId AND strftime('%Y-%m-%d', createdAt) = '$createdAt'"
      val cursor = db!!.rawQuery(sql, null)
      while(cursor.moveToNext()) {
         val value = Light()
         value.light = cursor.getInt(0)
         value.createdAt = cursor.getString(1)
         list.add(value)
      }
      cursor.close()
      return list
   }

   fun getActivityNowData(deviceId: Int) : String {
      val db = dbHelper.readableDatabase
      var value = ""
      val sql = "SELECT createdAt FROM $ACTIVITY WHERE deviceId = $deviceId AND strftime('%Y-%m-%dT%H', createdAt) = strftime('%Y-%m-%dT%H', datetime('now', 'localtime')) limit 1"
      val cursor = db!!.rawQuery(sql, null)
      while(cursor.moveToNext()) {
         value = cursor.getString(0)
      }
      cursor.close()
      return value
   }

   fun getDailyData(deviceId: Int, createdAt: String) : Int {
      val db = dbHelper.readableDatabase
      var value = 0
      val sql = "SELECT activityRate FROM $DAILY_DATA WHERE deviceId = $deviceId AND createdAt = '$createdAt'"
      val cursor = db!!.rawQuery(sql, null)
      while(cursor.moveToNext()) {
         value = cursor.getInt(0)
      }
      cursor.close()
      return value
   }

   fun getAllDailyData(deviceId: Int, startDate: String, endDate: String) : ArrayList<DailyData> {
      val db = dbHelper.readableDatabase
      val list = ArrayList<DailyData>()
      val sql = "SELECT activityRate, createdAt FROM $DAILY_DATA WHERE deviceId = $deviceId AND createdAt BETWEEN '$startDate' AND '$endDate'"
      val cursor = db!!.rawQuery(sql, null)
      while(cursor.moveToNext()) {
         val value = DailyData()
         value.activityRate = cursor.getInt(0)
         value.createdAt = cursor.getString(1)
         list.add(value)
      }
      cursor.close()
      return list
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

   fun insertSubject(data: Subject): Boolean {
      val db = dbHelper.writableDatabase
      val values = ContentValues()
      values.put("uid", data.uid)
      values.put("image", data.image)
      values.put("name", data.name)
      values.put("birthdate", data.birthdate)
      values.put("bloodType", data.bloodType)
      values.put("address", data.address)
      values.put("contact", data.contact)
      values.put("status", data.status)
      values.put("createdAt", data.createdAt)

      val result = db!!.insert(SUBJECT, null, values)
      return result != -1L
   }

   fun insertDevice(data: Device): Boolean {
      val db = dbHelper.writableDatabase
      val values = ContentValues()
      values.put("uid", data.uid)
      values.put("subjectId", data.subjectId)
      values.put("name", data.name)
      values.put("productNumber", data.productNumber)
      values.put("serialNumber", data.serialNumber)
      values.put("activityTime", data.activityTime)
      values.put("room", data.room)
      values.put("createdAt", data.createdAt)

      val result = db!!.insert(DEVICE, null, values)
      return result != -1L
   }

   fun insertActivity(data: Activity): Boolean {
      val db = dbHelper.writableDatabase
      val values = ContentValues()
      values.put("uid", data.uid)
      values.put("subjectId", data.subjectId)
      values.put("deviceId", data.deviceId)
      values.put("activity", data.activity)
      values.put("createdAt", data.createdAt)

      val result = db!!.insert(ACTIVITY, null, values)
      return result != -1L
   }

   fun insertTemperature(data: Temperature): Boolean {
      val db = dbHelper.writableDatabase
      val values = ContentValues()
      values.put("uid", data.uid)
      values.put("subjectId", data.subjectId)
      values.put("deviceId", data.deviceId)
      values.put("temperature", data.temperature)
      values.put("createdAt", data.createdAt)

      val result = db!!.insert(TEMPERATURE, null, values)
      return result != -1L
   }

   fun insertLight(data: Light): Boolean {
      val db = dbHelper.writableDatabase
      val values = ContentValues()
      values.put("uid", data.uid)
      values.put("subjectId", data.subjectId)
      values.put("deviceId", data.deviceId)
      values.put("light", data.light)
      values.put("createdAt", data.createdAt)

      val result = db!!.insert(LIGHT, null, values)
      return result != -1L
   }

   fun insertDailyData(data: DailyData): Boolean {
      val db = dbHelper.writableDatabase
      val values = ContentValues()
      values.put("uid", data.uid)
      values.put("subjectId", data.subjectId)
      values.put("deviceId", data.deviceId)
      values.put("activityRate", data.activityRate)
      values.put("createdAt", data.createdAt)

      val result = db!!.insert(DAILY_DATA, null, values)
      return result != -1L
   }

   fun updateUser(data: User){
      val db = dbHelper.writableDatabase
      val sql = "update $USER set idToken='${data.idToken}', accessToken='${data.accessToken}', username='${data.username}' " +
         "where type='${data.type}' and email='${data.email}'"
      db.execSQL(sql)
   }

   fun updateDailyData(deviceId: Int, data: Int){
      val db = dbHelper.writableDatabase
      val sql = "update $DAILY_DATA set activityRate = $data where deviceId = $deviceId"
      db.execSQL(sql)
   }

   fun deleteData(table: String, deviceId: Int, createdAt: String): Int {
      val db = dbHelper.writableDatabase
      val result = db.delete(table, "deviceId = $deviceId AND strftime('%Y-%m-%d', createdAt) = '$createdAt'", null)
      return result
   }
}
