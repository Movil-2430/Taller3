package com.example.moviltaller3.logica

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moviltaller3.R
import com.example.moviltaller3.adapters.ListUsersAdapter
import com.example.moviltaller3.databinding.ActivityListaUsuariosBinding
import com.example.moviltaller3.datos.Data
import com.example.moviltaller3.model.UserPOJO
import com.example.moviltaller3.model.UserStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import android.util.Log
import com.google.firebase.database.ValueEventListener


class ListaUsuarios : AppCompatActivity() {

    private lateinit var binding: ActivityListaUsuariosBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var usersAvailable: MutableList<UserPOJO>
    private lateinit var usersAdapter: ListUsersAdapter
    private lateinit var usersRef: DatabaseReference
    private lateinit var valueEventListener: ValueEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListaUsuariosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        usersRef = database.getReference(Data.DATABASE_USERS_PATH)

        // Configurar el RecyclerView
        usersAvailable = mutableListOf()
        usersAdapter = ListUsersAdapter(this, usersAvailable, this::onViewPositionClick)
        binding.rvListUsuarios.layoutManager = LinearLayoutManager(this)
        binding.rvListUsuarios.adapter = usersAdapter

        // Agregar el listener en tiempo real para la lista de usuarios
        setupRealtimeUserListener()
    }

    private fun setupRealtimeUserListener() {
        // Inicializar el ValueEventListener
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Limpiar la lista actual para evitar duplicados
                usersAvailable.clear()

                // Recorrer todos los usuarios en la base de datos
                for (snap in snapshot.children) {
                    val user = snap.getValue(UserPOJO::class.java)
                    // Agregar solo los usuarios disponibles y que no sean el usuario actual
                    if (user != null && user.status == UserStatus.AVAILABLE.status && user.uid != auth.currentUser?.uid) {
                        usersAvailable.add(user)
                    }
                }
                // Notificar al adaptador para actualizar la vista
                usersAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar el error si ocurre
            }
        }

        // Añadir el ValueEventListener al nodo de usuarios
        usersRef.addValueEventListener(valueEventListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Eliminar el ValueEventListener cuando la actividad se destruya para evitar fugas de memoria
        usersRef.removeEventListener(valueEventListener)
    }

    private fun onViewPositionClick(user: UserPOJO) {
        Intent(this, PosicionUsuario::class.java).apply {
            putExtra("UserUID", user.uid)
            startActivity(this)
        }
    }
}