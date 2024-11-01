package com.example.moviltaller3.logica

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.moviltaller3.R
import com.example.moviltaller3.databinding.ActivityRegistroBinding
import com.example.moviltaller3.datos.Data
import com.example.moviltaller3.model.UserPOJO
import com.example.moviltaller3.utility.AppUtilityHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

class Registro : AppCompatActivity() {
    private lateinit var binding: ActivityRegistroBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var database: FirebaseDatabase

    lateinit var photoUri: Uri
    private var imagenCargada = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.registerButton.setOnClickListener {
            registerUser()
        }

        binding.imageButton.setOnClickListener {
            requestPermissions()
        }
    }

    private fun registerUser() {
        val nombre = binding.nombreEditText.text.toString().trim()
        val apellido = binding.apellidoEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (validateForm(nombre, apellido, email, password)) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) { // Update user info
                            val upcrb = UserProfileChangeRequest.Builder()
                            upcrb.setDisplayName(nombre + " " + apellido)
                            user.updateProfile(upcrb.build())
                            val photoURL = runBlocking { uploadImageToFirebaseStorage(user.uid) }
                            saveUserToDatabase(user.uid, photoURL)
                            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                            Intent(this, PantallaPrincipal::class.java).also {
                                startActivity(it)
                            }
                        }
                    } else {
                        Log.w("Registro", "createUserWithEmail:failure", task.exception)
                    }
                }
        }
    }

    private suspend fun uploadImageToFirebaseStorage(userId: String): String {
        var photoURL = ""
        if (photoUri != null) {
            val storageRef = storage.reference.child("${Data.MY_PERMISSIONS_REQUEST_STORAGE}$userId.jpg")
            try {
                storageRef.putFile(photoUri).await()
                Log.d("Registro", "Successfully uploaded image")
                photoURL = storageRef.downloadUrl.await().toString()
            } catch (e: Exception) {
                Log.e("Registro", "Failed to upload image: ${e.message}")
            }
            AppUtilityHelper.deleteTempFiles(this)
        }
        return photoURL
    }

    private fun saveUserToDatabase(userId: String, photoURL: String) {
        val user = UserPOJO()
        user.uid = userId
        user.name = binding.nombreEditText.text.toString().trim()
        user.surname = binding.apellidoEditText.text.toString().trim()
        user.email = binding.emailEditText.text.toString().trim()
        user.photoUrl = photoURL
        database.reference.child(Data.DATABASE_USERS_PATH).child(userId).setValue(user)
            .addOnSuccessListener {
                Log.d("Registro", "User information saved successfully")
            }
            .addOnFailureListener {
                Log.e("Registro", "Failed to save user information: ${it.message}")
            }
    }

    private fun requestPermissions(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), Data.MY_PERMISSIONS_REQUEST_CAMERA)
        }
        else{
            // Permiso de camara concedido, pedir el de galeria
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), Data.MY_PERMISSIONS_REQUEST_STORAGE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            Data.MY_PERMISSIONS_REQUEST_CAMERA -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permiso concedido
                } else {
                    denegarFuncionalidad()
                }
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), Data.MY_PERMISSIONS_REQUEST_STORAGE)
            }
            Data.MY_PERMISSIONS_REQUEST_STORAGE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permiso concedido
                    showImageChooser()
                } else {
                    denegarFuncionalidad()
                }
            }
        }
    }

    private fun denegarFuncionalidad() {
        Toast.makeText(this, "Por favor acepte los permisos para cambiar la foto", Toast.LENGTH_SHORT).show()
    }

    private fun showImageChooser() {
        val chooserIntent = Intent.createChooser(Intent(), "Selecciona una opción")
        val intentArray = mutableListOf<Intent>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val tmpPictureFile = AppUtilityHelper.createTempPictureFile(this)
            photoUri = FileProvider.getUriForFile(this, "com.example.moviltaller3.fileprovider", tmpPictureFile)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            intentArray.add(takePictureIntent)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intentArray.add(pickPhotoIntent)
        }

        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray.toTypedArray())

        // Lanzar el chooser
        imageChooserLauncher.launch(chooserIntent)
    }

    // Registrar el ActivityResultLauncher para manejar el resultado
    private val imageChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data

            // Verificar si viene de la cámara o la galería
            if (data != null && data.data != null) {
                // Imagen desde galería
                val selectedImageUri = data.data
                binding.imageButton
                binding.imageButton.setImageURI(selectedImageUri)
                photoUri = selectedImageUri!!
            } else {
                // Imagen desde la cámara
                binding.imageButton.setImageURI(photoUri)
            }
            imagenCargada = true
        }
    }

    private fun validateForm(nombre: String, apellido: String, email: String, password: String): Boolean {
        var valid = true

        if (TextUtils.isEmpty(nombre)) {
            binding.nombreEditText.error = "Required."
            valid = false
        } else {
            binding.nombreEditText.error = null
        }

        if (TextUtils.isEmpty(apellido)) {
            binding.apellidoEditText.error = "Required."
            valid = false
        } else {
            binding.apellidoEditText.error = null
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.error = "Valid email required."
            valid = false
        } else {
            binding.emailEditText.error = null
        }

        if (TextUtils.isEmpty(password) || password.length < 6) {
            binding.passwordEditText.error = "Password must be at least 6 characters."
            valid = false
        } else {
            binding.passwordEditText.error = null
        }

        if (!imagenCargada) {
            Toast.makeText(this, "Imagen de perfil requerida", Toast.LENGTH_SHORT).show()
            valid = false
        }

        return valid
    }
}