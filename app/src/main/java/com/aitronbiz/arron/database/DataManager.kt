package com.aitronbiz.arron.database

import android.content.ContentValues
import android.content.Context
import com.aitronbiz.arron.database.DBHelper.Companion.ACTIVITY
import com.aitronbiz.arron.database.DBHelper.Companion.DEVICE
import com.aitronbiz.arron.database.DBHelper.Companion.HOME
import com.aitronbiz.arron.database.DBHelper.Companion.ROOM
import com.aitronbiz.arron.database.DBHelper.Companion.SUBJECT
import com.aitronbiz.arron.database.DBHelper.Companion.USER
import com.aitronbiz.arron.entity.Activity
import com.aitronbiz.arron.entity.DailyData
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.entity.Room
import com.aitronbiz.arron.entity.Subject
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

   fun getHomes(uid: Int) : ArrayList<Home> {
      val db = dbHelper.readableDatabase
      val list = ArrayList<Home>()
      val sql = "select * from $HOME where uid = $uid"
      val cursor = db!!.rawQuery(sql, null)
      while(cursor.moveToNext()) {
         val value = Home()
         value.id = cursor.getInt(0)
         value.uid = cursor.getInt(1)
         value.serverId = cursor.getString(2)
         value.name = cursor.getString(3)
         value.province = cursor.getString(4)
         value.city = cursor.getString(5)
         value.street = cursor.getString(6)
         value.detailAddress = cursor.getString(7)
         value.postalCode = cursor.getString(8)
         value.createdAt = cursor.getString(9)
         list.add(value)
      }
      cursor.close()
      return list
   }

   fun getSubjects(uid: Int, homeId: Int) : ArrayList<Subject> {
      val db = dbHelper.readableDatabase
      val list = ArrayList<Subject>()
      val sql = "select * from $SUBJECT where uid = $uid AND homeId = $homeId"
      val cursor = db!!.rawQuery(sql, null)
      while(cursor.moveToNext()) {
         val value = Subject()
         value.id = cursor.getInt(0)
         value.uid = cursor.getInt(1)
         value.homeId = cursor.getInt(2)
         value.serverId = cursor.getString(3)
         value.name = cursor.getString(4)
         value.createdAt = cursor.getString(5)
         list.add(value)
      }
      cursor.close()
      return list
   }

   fun getRooms(uid: Int, homeId: Int) : ArrayList<Room> {
      val db = dbHelper.readableDatabase
      val list = ArrayList<Room>()
      val sql = "select * from $ROOM where uid = $uid and homeId = $homeId"
      val cursor = db!!.rawQuery(sql, null)
      while(cursor.moveToNext()) {
         val value = Room()
         value.id = cursor.getInt(0)
         value.uid = cursor.getInt(1)
         value.homeId = cursor.getInt(2)
         value.subjectId = cursor.getInt(3)
         value.serverId = cursor.getString(4)
         value.name = cursor.getString(5)
         value.createdAt = cursor.getString(6)
         list.add(value)
      }
      cursor.close()
      return list
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

   fun getDevices(homeId: Int, roomId: Int) : ArrayList<Device> {
      val db = dbHelper.readableDatabase
      val list = ArrayList<Device>()
      val sql = "SELECT * FROM $DEVICE WHERE homeId = $homeId AND roomId = $roomId"
      val cursor = db!!.rawQuery(sql, null)
      while(cursor.moveToNext()) {
         val value = Device()
         value.id = cursor.getInt(0)
         value.uid = cursor.getInt(1)
         value.homeId = cursor.getInt(2)
         value.subjectId = cursor.getInt(3)
         value.roomId = cursor.getInt(4)
         value.serverId = cursor.getString(5)
         value.name = cursor.getString(6)
         value.productNumber = cursor.getString(7)
         value.serialNumber = cursor.getString(8)
         value.activityTime = cursor.getInt(9)
         value.createdAt = cursor.getString(10)
         list.add(value)
      }
      cursor.close()
      return list
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

   fun insertHome(data: Home): Int {
      val db = dbHelper.writableDatabase
      val values = ContentValues()
      values.put("uid", data.uid)
      values.put("name", data.name)
      values.put("province", data.province)
      values.put("city", data.city)
      values.put("street", data.street)
      values.put("detailAddress", data.detailAddress)
      values.put("postalCode", data.postalCode)
      values.put("createdAt", data.createdAt)

      val result = db.insert(HOME, null, values)
      return result.toInt()
   }

   fun insertSubject(data: Subject): Int {
      val db = dbHelper.writableDatabase
      val values = ContentValues()
      values.put("uid", data.uid)
      values.put("homeId", data.homeId)
      values.put("name", data.name)
      values.put("createdAt", data.createdAt)

      val result = db.insert(SUBJECT, null, values)
      return result.toInt()
   }

   fun insertRoom(data: Room): Int {
      val db = dbHelper.writableDatabase
      val values = ContentValues()
      values.put("uid", data.uid)
      values.put("homeId", data.homeId)
      values.put("name", data.name)
      values.put("createdAt", data.createdAt)

      val result = db.insert(ROOM, null, values)
      return result.toInt()
   }

   fun insertDevice(data: Device): Int {
      val db = dbHelper.writableDatabase
      val values = ContentValues()
      values.put("uid", data.uid)
      values.put("homeId", data.homeId)
      values.put("subjectId", data.subjectId)
      values.put("roomId", data.roomId)
      values.put("name", data.name)
      values.put("productNumber", data.productNumber)
      values.put("serialNumber", data.serialNumber)
      values.put("activityTime", data.activityTime)
      values.put("createdAt", data.createdAt)

      val result = db.insert(DEVICE, null, values)
      return result.toInt()
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

   fun updateHome(data: Home): Int {
      val db = dbHelper.writableDatabase
      val values = ContentValues().apply {
         put("name", data.name)
         put("province", data.province)
         put("city", data.city)
         put("street", data.street)
         put("detailAddress", data.detailAddress)
         put("postalCode", data.postalCode)
      }

      return db.update(HOME, values, "id=?", arrayOf(data.id.toString()))
   }

   fun updateSubject(data: Subject): Int {
      val db = dbHelper.writableDatabase
      val values = ContentValues().apply {
         put("name", data.name)
      }

      return db.update(SUBJECT, values, "id=?", arrayOf(data.id.toString()))
   }

   fun updateRoom(data: Room): Int {
      val db = dbHelper.writableDatabase
      val values = ContentValues().apply {
         put("name", data.name)
      }

      return db.update(ROOM, values, "id=?", arrayOf(data.id.toString()))
   }

   fun updateDevice(data: Device): Int {
      val db = dbHelper.writableDatabase
      val values = ContentValues().apply {
         put("name", data.name)
      }

      return db.update(DEVICE, values, "id=?", arrayOf(data.id.toString()))
   }

   fun deleteData(table: String, id: Int): Int {
      val db = dbHelper.writableDatabase
      val result = db.delete(table, "id = $id", null)
      return result
   }
}