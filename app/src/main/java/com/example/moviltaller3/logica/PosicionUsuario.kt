package com.example.moviltaller3.logica

import android.app.UiModeManager
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.moviltaller3.BuildConfig
import com.example.moviltaller3.R
import com.example.moviltaller3.databinding.ActivityPantallaPrincipalBinding
import com.example.moviltaller3.databinding.ActivityPosicionUsuarioBinding
import com.example.moviltaller3.datos.Data
import com.example.moviltaller3.model.UserPOJO
import com.example.moviltaller3.utility.AppUtilityHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PosicionUsuario : AppCompatActivity() {

    private val normalZoom = 18.0

    private lateinit var binding: ActivityPosicionUsuarioBinding
    private lateinit var map: MapView
    private lateinit var currentLocationMarker: Marker
    private lateinit var objectiveUserMarker: Marker
    private lateinit var mapController: IMapController
    private lateinit var currentLocation: Location

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference

    private lateinit var usuarioObjetivo: UserPOJO
    private lateinit var objectiveUserChangesListener: ValueEventListener
    private var isListenerActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_posicion_usuario)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID)
        binding = ActivityPosicionUsuarioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        map = binding.osmMap
        mapController = map.controller

        initUserLocation()
    }

    private fun initUserLocation(){
        try {
            AppUtilityHelper.getUserByUID(auth, database, auth.currentUser?.uid ?: "") { user ->
                if (user != null) {
                    currentLocation = Location("currentLocation").apply {
                        latitude = user.latitude
                        longitude = user.longitude
                    }
                    currentLocationMarker = Marker(map)
                    currentLocationMarker.icon = ContextCompat.getDrawable(this, R.drawable.marker_user_icon)
                    currentLocationMarker.position = GeoPoint(currentLocation.latitude, currentLocation.longitude)
                    currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    currentLocationMarker.title = "Ubicación actual"
                    map.overlays.add(currentLocationMarker)
                    adjustMarkerSize(currentLocationMarker)
                    initActivity()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "No se puede obtener la ubicación", Toast.LENGTH_SHORT).show()
            Log.e("Location", "Error: ${e.message}")
        }
    }

    private fun initActivity(){
        initUI()
        initObjectiveUser()
    }

    private fun updateMarker(location: Location){
        GeoPoint(location.latitude, location.longitude).let {
            mapController.setZoom(normalZoom)
            mapController.setCenter(it)
            objectiveUserMarker.position = it
            objectiveUserMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            objectiveUserMarker.title = "Ubicación ${usuarioObjetivo.name}"
            map.invalidate() // Refresh the map
            Log.d("Location", "Location updated: ${location.latitude}, ${location.longitude}")
        }
    }

    private fun adjustMarkerSize(marker: Marker) {
        val zoomLevel = map.zoomLevelDouble
        val scaleFactor = zoomLevel / 20.0 // Adjust the divisor to control scaling
        val icon = marker.icon
        icon?.setBounds(0, 0, (icon.intrinsicWidth * scaleFactor).toInt(), (icon.intrinsicHeight * scaleFactor).toInt())
        marker.icon = icon
    }

    private fun initUI(){
        initMap()
    }

    private fun initMap(){
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(false)
        map.overlays.add(currentLocationMarker)
        val uiManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        if(uiManager.nightMode == UiModeManager.MODE_NIGHT_YES)
            binding.osmMap.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
    }

    private fun initObjectiveUser(){
        val objectiveUID = intent.getStringExtra("UserUID")

        if (objectiveUID != null) {
            lifecycleScope.launch {
                loadObjectiveUser(objectiveUID)
                if (::usuarioObjetivo.isInitialized) {
                    myRef = database.getReference(Data.DATABASE_USERS_PATH + usuarioObjetivo.uid)
                    initObjectiveUserChangesListener()
                    myRef.addValueEventListener(objectiveUserChangesListener)
                    isListenerActive = true
                }
            }
        }
    }

    private suspend fun loadObjectiveUser(objectiveUID: String){
        usuarioObjetivo = suspendCoroutine { continuation ->
            AppUtilityHelper.getUserByUID(auth, database, objectiveUID) { user ->
                if (user != null) {
                    usuarioObjetivo = user
                    val userLocation = Location("userLocation").apply {
                        latitude = user.latitude
                        longitude = user.longitude
                    }
                    objectiveUserMarker = Marker(map)
                    objectiveUserMarker.icon = ContextCompat.getDrawable(this, R.drawable.marker_objective_icon)
                    updateMarker(userLocation)
                    map.overlays.add(objectiveUserMarker)
                    adjustMarkerSize(objectiveUserMarker)
                    continuation.resume(user)
                }
            }
        }
    }

    private fun initObjectiveUserChangesListener(){
        objectiveUserChangesListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(UserPOJO::class.java)
                if (user != null) {
                    usuarioObjetivo = user
                    val userLocation = Location("userLocation").apply {
                        latitude = user.latitude
                        longitude = user.longitude
                    }
                    updateMarker(userLocation)
                    val distancia = AppUtilityHelper.distanceBetweenTwoPoints(currentLocation.latitude, currentLocation.longitude, user.latitude, user.longitude)
                    Toast.makeText(this@PosicionUsuario, "Distancia: $distancia km", Toast.LENGTH_SHORT).show()
                }
                Log.d("UserStatus", "User location updated")
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("UserStatus", "error en la consulta", databaseError.toException())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        mapController = map.controller
        mapController.setZoom(normalZoom)

        if (!isListenerActive && ::myRef.isInitialized) {
            myRef.addValueEventListener(objectiveUserChangesListener)
            isListenerActive = true
        }
    }
    override fun onPause() {
        super.onPause()
        map.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        if (isListenerActive) {
            myRef.removeEventListener(objectiveUserChangesListener)
            isListenerActive = false
        }
    }

}