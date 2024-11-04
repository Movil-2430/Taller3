package com.example.moviltaller3.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore

import android.util.Log

import com.google.firebase.database.*

class AvailabilityService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AvailabilityService", "Servicio iniciado")

        val db = FirebaseDatabase.getInstance()
        val usersRef = db.getReference("users")

        // Escuchar cambios en la colección "users" usando Realtime Database
        usersRef.addChildEventListener(object : ChildEventListener {
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val userName = snapshot.child("name").getValue(String::class.java)
                val status = snapshot.child("status").getValue(String::class.java) ?: "Desconectado"

                Log.d("AvailabilityService", "Cambio detectado en $userName: $status")

                // Mostrar el Toast en función del estado
                if (status == "Disponible") {
                    showToast("$userName está ahora disponible")
                } else if (status == "Desconectado") {
                    showToast("$userName se ha desconectado")
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

        return START_STICKY
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
}

