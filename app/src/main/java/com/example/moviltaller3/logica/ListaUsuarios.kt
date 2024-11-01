package com.example.moviltaller3.logica

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

class ListaUsuarios : AppCompatActivity() {

    private lateinit var binding: ActivityListaUsuariosBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var usersAvailable: MutableList<UserPOJO>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_usuarios)

        binding = ActivityListaUsuariosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        getAvailableUsers { users ->
            usersAvailable = users.toMutableList()
            binding.rvListUsuarios.layoutManager = LinearLayoutManager(this)
            binding.rvListUsuarios.adapter = ListUsersAdapter(this, usersAvailable, this::onViewPositionClick)
        }
    }

    private fun getAvailableUsers(callback: (List<UserPOJO>) -> Unit) {
        val users = mutableListOf<UserPOJO>()
        val ref = database.getReference(Data.DATABASE_USERS_PATH)
        ref.get().addOnSuccessListener {
            for (snap in it.children) {
                val user = snap.getValue(UserPOJO::class.java)
                if (user != null && user.status == UserStatus.AVAILABLE.status && user.uid != auth.currentUser?.uid) {
                    users.add(user)
                }
            }
            callback(users)
        }
    }


    private fun onViewPositionClick(user: UserPOJO){
        Intent(this, PosicionUsuario::class.java).apply {
            putExtra("UserUID", user.uid)
            startActivity(this)
        }
    }
}