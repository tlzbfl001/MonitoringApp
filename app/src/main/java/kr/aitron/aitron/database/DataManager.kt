package kr.aitron.aitron.database

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import kr.aitron.aitron.AppController
import kr.aitron.aitron.database.DBHelper.Companion.DEVICE
import kr.aitron.aitron.database.DBHelper.Companion.SUBJECT
import kr.aitron.aitron.database.DBHelper.Companion.USER
import kr.aitron.aitron.entity.Device
import kr.aitron.aitron.entity.Subject
import kr.aitron.aitron.entity.User

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
         value.updatedAt = cursor.getString(7)
      }
      cursor.close()
      db.close()
      return value
   }

   fun getSubject(uid: Int) : Subject {
      val db = dbHelper!!.readableDatabase
      val value = Subject()
      val sql = "SELECT * FROM $SUBJECT WHERE uid = $uid LIMIT 1"
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
         value.updatedAt = cursor.getString(9)
      }
      cursor.close()
      db.close()
      return value
   }

   fun insertDevice(data: User): Boolean {
      val db = dbHelper!!.writableDatabase
      val values = ContentValues()
      values.put("username", data.username)
      values.put("email", data.email)
      values.put("role", data.role)
      values.put("contact", data.contact)
      values.put("emergencyContact", data.emergencyContact)
      values.put("createdAt", data.createdAt)
      values.put("updatedAt", data.updatedAt)

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
      values.put("updatedAt", data.updatedAt)

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
      values.put("createdAt", data.createdAt)

      val result = db!!.insert(DEVICE, null, values)
      db.close()
      return result != -1L
   }
}
