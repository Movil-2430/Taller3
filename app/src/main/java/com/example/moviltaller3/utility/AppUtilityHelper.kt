package com.example.moviltaller3.utility

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.example.moviltaller3.datos.Data
import com.example.moviltaller3.model.Localizacion
import com.example.moviltaller3.model.UserPOJO
import com.example.moviltaller3.model.UserStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class AppUtilityHelper {
    companion object{
        fun deleteTempFiles(context: Context) {
            val storageDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Temp")
            storageDir?.listFiles()?.forEach { file ->
                if (file.isFile && file.name.startsWith("JPEG_")) {
                    file.delete()
                }
            }
        }

        fun createTempPictureFile(context: Context): File {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Temp")
            if (!storageDir.exists()) {
                storageDir.mkdirs() // Crear el directorio si no existe
            }
            return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        }

        fun loadJSONFromAsset(context: Context, fileName: String): String? {
            return try {
                val inputStream = context.assets.open(fileName)
                val size = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()
                String(buffer, Charsets.UTF_8)
            } catch (ex: IOException) {
                ex.printStackTrace()
                null
            }
        }

        fun parseLocations(jsonString: String): List<Localizacion> {
            val locations = mutableListOf<Localizacion>()
            val jsonObject = JSONObject(jsonString)
            val locationsArray = jsonObject.getJSONArray("locationsArray")
            for (i in 0 until locationsArray.length()) {
                val locacion = Localizacion()
                val locationObject = locationsArray.getJSONObject(i)
                locacion.latitude = locationObject.getDouble("latitude")
                locacion.longitude = locationObject.getDouble("longitude")
                locacion.name = locationObject.getString("name")
                locations.add(locacion)
            }
            return locations
        }

        fun distanceBetweenTwoPoints(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val latDistance = Math.toRadians(lat2 - lat1)
            val lngDistance = Math.toRadians(lon2 - lon1)
            val a = (Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2))
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            val result = 6371 * c
            return (result * 100.0).roundToInt() / 100.0
        }

        fun userDisponible(auth: FirebaseAuth, database: FirebaseDatabase, callback: (Boolean) -> Unit) {
            val myRef = database.getReference("${Data.DATABASE_USERS_PATH}${auth.currentUser?.uid}")
            myRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val user = dataSnapshot.getValue(UserPOJO::class.java)
                    val disponible = user?.status == UserStatus.AVAILABLE.status
                    callback(disponible)
                }
                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w("UserStatus", "error en la consulta", databaseError.toException())
                    callback(false)
                }
            })
        }

        fun getUserByUID(auth: FirebaseAuth, database: FirebaseDatabase, uid: String, callback: (UserPOJO?) -> Unit) {
            val myRef = database.getReference("${Data.DATABASE_USERS_PATH}$uid")
            myRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val user = dataSnapshot.getValue(UserPOJO::class.java)
                    callback(user)
                }
                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w("UserStatus", "error en la consulta", databaseError.toException())
                    callback(null)
                }
            })
        }
    }
}