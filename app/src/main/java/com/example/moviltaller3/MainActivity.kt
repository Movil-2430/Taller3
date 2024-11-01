package com.example.moviltaller3

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.moviltaller3.databinding.ActivityMainBinding
import com.example.moviltaller3.logica.PantallaPrincipal
import com.example.moviltaller3.logica.Registro
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth

        binding.loginButton.setOnClickListener {
            signInUser(binding.emailEditText.text.toString(), binding.passwordEditText.text.toString())
        }

        binding.signupButton.setOnClickListener {
            Intent(this, Registro::class.java).also {
                startActivity(it)
            }
        }
    }

    private fun signInUser(email: String, password: String){
        if(validateForm() && isEmailValid(email)){
            auth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
// Sign in success, update UI
                        Log.d(TAG, "signInWithEmail:success:")
                        val user = auth.currentUser
                        updateUI(auth.currentUser)
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(this, "Authentication failed.",
                            Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }
                }
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        val email = binding.emailEditText.text.toString()
        if (TextUtils.isEmpty(email)) {
            binding.emailEditText.error = "Required."
            valid = false
        } else {
            binding.emailEditText.error = null
        }
        val password = binding.passwordEditText.text.toString()
        if (TextUtils.isEmpty(password)) {
            binding.passwordEditText.error = "Required."
            valid = false
        } else {
            binding.passwordEditText.error = null
        }
        return valid
    }


    private fun isEmailValid(email: String): Boolean {
        if (!email.contains("@") ||
            !email.contains(".") ||
            email.length < 5)
            return false
        return true
    }



    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            val intent = Intent(this, PantallaPrincipal::class.java)
            intent.putExtra("user", currentUser.email)
            startActivity(intent)
        } else {
            binding.emailEditText.setText("")
            binding.passwordEditText.setText("")
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }
}