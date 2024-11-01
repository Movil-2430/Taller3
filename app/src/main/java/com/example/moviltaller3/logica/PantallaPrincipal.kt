package com.example.moviltaller3.logica

import android.Manifest
import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.moviltaller3.BuildConfig
import com.example.moviltaller3.MainActivity
import com.example.moviltaller3.R
import com.example.moviltaller3.databinding.ActivityPantallaPrincipalBinding
import com.example.moviltaller3.datos.Data
import com.example.moviltaller3.model.UserPOJO
import com.example.moviltaller3.model.UserStatus
import com.example.moviltaller3.utility.AppUtilityHelper
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay

class PantallaPrincipal : AppCompatActivity() {

    private val reportedMeters: Int = 20
    private val normalZoom = 18.0
    private val extraZoom = 20.0
    private val locationUpdateInterval:Long = 10000
    private val minLocationUpdateInterval:Long = 5000
    private var userDisponible = false

    private lateinit var binding: ActivityPantallaPrincipalBinding
    private lateinit var map: MapView
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var currentLocationMarker: Marker
    private lateinit var mapController: IMapController
    private lateinit var currentLocation: Location

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private val getLocationSettings = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if(result.resultCode == RESULT_OK){
            startLocationUpdates()
        }else{
            Toast.makeText(this, "No se puede obtener la ubicación", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla_principal)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID)
        binding = ActivityPantallaPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        map = binding.osmMap
        mapController = map.controller

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = createLocationRequest()
        initLocationCallBack()
        requestLocationPermission(this)
    }

    private fun initUserLocation(){
        try {
            mFusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    currentLocation = it
                    currentLocationMarker = Marker(map)
                    currentLocationMarker.icon = ContextCompat.getDrawable(this, R.drawable.marker_user_icon)
                    updateMarker(it)
                    adjustMarkerSize(currentLocationMarker)
                    updateUserLocation()

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

        checkLocationSettings()
    }

    private fun initLocationCallBack(){
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null &&
                    currentLocation.distanceTo(location) > reportedMeters) {

                    updateMarker(location)
                    currentLocation = location
                    updateUserLocation()
                }
            }
        }
    }

    private fun updateUserLocation(){
        val myRef = database.getReference("${Data.DATABASE_USERS_PATH}${auth.currentUser?.uid}")
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val myUser = dataSnapshot.getValue(UserPOJO::class.java)
                myUser?.latitude = currentLocation.latitude
                myUser?.longitude = currentLocation.longitude
                myRef.setValue(myUser)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("UserStatus", "error en la consulta", databaseError.toException())
            }
        })
    }

    private fun updateMarker(location: Location){
        GeoPoint(location.latitude, location.longitude).let {
            mapController.setZoom(normalZoom)
            mapController.setCenter(it)
            currentLocationMarker.position = it
            currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            currentLocationMarker.title = "Ubicación actual"
            map.invalidate() // Refresh the map
            Log.d("Location", "Location updated: ${location.latitude}, ${location.longitude}")
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null)
        }
    }

    private fun checkLocationSettings(){
        val builder = LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            startLocationUpdates()
        }

        task.addOnFailureListener { e ->
            if ((e as ApiException).statusCode == CommonStatusCodes.RESOLUTION_REQUIRED){
                val resolvable = e as ResolvableApiException
                val isr = IntentSenderRequest.Builder(resolvable.resolution).build()
                getLocationSettings.launch(isr)
            }else{
                Toast.makeText(this, "No se puede obtener la ubicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createLocationRequest(): LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,locationUpdateInterval).apply {
            setMinUpdateIntervalMillis(minLocationUpdateInterval)
        }.build()

    private fun adjustMarkerSize(marker: Marker) {
        val zoomLevel = map.zoomLevelDouble
        val scaleFactor = zoomLevel / 20.0 // Adjust the divisor to control scaling
        val icon = marker.icon
        icon?.setBounds(0, 0, (icon.intrinsicWidth * scaleFactor).toInt(), (icon.intrinsicHeight * scaleFactor).toInt())
        marker.icon = icon
    }

    private fun initUI(){
        initMap()
        initLocations()
    }

    private fun initMap(){
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(false)
        map.overlays.add(currentLocationMarker)
        val uiManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        if(uiManager.nightMode == UiModeManager.MODE_NIGHT_YES)
            binding.osmMap.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
    }

    private fun initLocations(){
        AppUtilityHelper.loadJSONFromAsset(this, "locations.json")?.let { it ->
            val locations = AppUtilityHelper.parseLocations(it)
            locations.forEach {
                val marker = Marker(map)
                marker.icon = ContextCompat.getDrawable(this, R.drawable.marker_location_icon)
                marker.position = GeoPoint(it.latitude, it.longitude)
                marker.title = it.name
                map.overlays.add(marker)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        mapController = map.controller
        mapController.setZoom(normalZoom)
    }
    override fun onPause() {
        super.onPause()
        map.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        if (::mFusedLocationClient.isInitialized) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback)
        }
    }

    private fun requestLocationPermission(context: Activity){
        val permiso = android.Manifest.permission.ACCESS_FINE_LOCATION
        val idCode = Data.MY_PERMISSIONS_REQUEST_LOCATION
        if (ContextCompat.checkSelfPermission(context, permiso) != PackageManager.PERMISSION_GRANTED) {
            // Si el permiso no ha sido concedido, lo solicitamos
            ActivityCompat.requestPermissions(context, arrayOf(permiso), idCode)
        } else {
            initUserLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Data.MY_PERMISSIONS_REQUEST_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    initUserLocation()
                } else {
                    Toast.makeText(this, "Necesita la localización para terminar", Toast.LENGTH_SHORT).show()
                    denegarFuncionalidad()
                }
                return
            }
            else -> {

            }
        }
    }

    private fun denegarFuncionalidad() {

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
// Handle item selection
        return when (item.itemId) {
            R.id.menuLogOut -> {
                auth.signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                true
            }
            R.id.menuStatus -> {
                changeUserStatus()
                true
            }
            R.id.menuUsuarios -> {
                val intent = Intent(this, ListaUsuarios::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val menuItem = menu.findItem(R.id.menuStatus)
        AppUtilityHelper.userDisponible(auth, database, callback = { disponible ->
            userDisponible = disponible
        })
        if (userDisponible) {
            menuItem.title = "Desconectarse"
        } else {
            menuItem.title = "Conectarse"
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun changeUserStatus(){
        val myRef = database.getReference("${Data.DATABASE_USERS_PATH}${auth.currentUser?.uid}")
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val myUser = dataSnapshot.getValue(UserPOJO::class.java)
                myUser?.status = if (!userDisponible) UserStatus.AVAILABLE.status else UserStatus.DISCONNECTED.status
                myRef.setValue(myUser)
                userDisponible = !userDisponible
                invalidateOptionsMenu()
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("UserStatus", "error en la consulta", databaseError.toException())
            }
        })
    }
}