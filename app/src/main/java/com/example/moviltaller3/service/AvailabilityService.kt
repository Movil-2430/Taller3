package com.example.moviltaller3.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import com.google.firebase.database.*
import android.util.Log
import com.example.moviltaller3.datos.Data
import com.example.moviltaller3.model.UserStatus

class AvailabilityService : Service() {

    private val userStatusMap = mutableMapOf<String, String>()
    private lateinit var objectiveUserChangesListener: ChildEventListener

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AvailabilityService", "Servicio iniciado")

        val db = FirebaseDatabase.getInstance()
        val usersRef = db.getReference(Data.DATABASE_USERS_PATH)
        initStatus(usersRef)

        // Escuchar cambios en la colección "users" usando Realtime Database
        objectiveUserChangesListener = (object : ChildEventListener {
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val userId = snapshot.key ?: return
                val userName = snapshot.child("name").getValue(String::class.java)
                val newStatus = snapshot.child("status").getValue(String::class.java) ?: UserStatus.DISCONNECTED.status

                val previousStatus = userStatusMap[userId]
                if (previousStatus != newStatus) {
                    userStatusMap[userId] = newStatus
                    Log.d("AvailabilityService", "Cambio detectado en $userName: $newStatus")

                    // Mostrar el Toast en función del estado
                    if (newStatus == UserStatus.AVAILABLE.status) {
                        showToast("$userName está ahora disponible")
                    } else if (newStatus == UserStatus.DISCONNECTED.status) {
                        showToast("$userName se ha desconectado")
                    }
                }
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // No es necesario manejar este evento en este caso
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Opcional: puedes manejar la eliminación de usuarios si es necesario
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {
                Log.e("AvailabilityService", "Error al escuchar cambios", error.toException())
            }
        })
        usersRef.addChildEventListener(objectiveUserChangesListener)

        return START_STICKY
    }

    private fun initStatus(usersRef: DatabaseReference) {
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    val userStatus = userSnapshot.child("status").getValue(String::class.java) ?: UserStatus.DISCONNECTED.status
                    userStatusMap[userId] = userStatus
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AvailabilityService", "Error al obtener el estado inicial", error.toException())
            }
        })
    }

    private fun showToast(message: String) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        val db = FirebaseDatabase.getInstance()
        val usersRef = db.getReference(Data.DATABASE_USERS_PATH)
        usersRef.removeEventListener(objectiveUserChangesListener)
        Log.d("AvailabilityService", "Servicio destruido y listener removido")
    }
}