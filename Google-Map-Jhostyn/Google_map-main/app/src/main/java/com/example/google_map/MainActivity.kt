package com.example.google_map

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

data class DirectionsResponse(val routes: List<Route>)
data class Route(val legs: List<Leg>)
data class Leg(val steps: List<Step>)
data class Step(val polyline: Polyline)
data class Polyline(val points: String)

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentMapType = GoogleMap.MAP_TYPE_NORMAL

    // Lanzador de permisos usando Activity Result API
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getDeviceLocation()  // Permiso concedido
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar el FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val btnChangeMapType: Button = findViewById(R.id.btnChangeMapType)
        btnChangeMapType.setOnClickListener {
            changeMapType()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Habilitar "Mi ubicación" si se tiene permiso
        enableMyLocation()

        // Configurar el Listener para el click en el mapa
        mMap.setOnMapClickListener { latLng ->
            drawRouteTo(latLng)  // Trazar ruta al lugar seleccionado
        }
    }

    // Método para habilitar la ubicación del usuario
    private fun enableMyLocation() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                mMap.isMyLocationEnabled = true
                getDeviceLocation()  // Obtener la ubicación exacta
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    // Método para obtener la ubicación exacta del dispositivo
    private fun getDeviceLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val userLocation = LatLng(location.latitude, location.longitude)
                    mMap.addMarker(MarkerOptions().position(userLocation).title("You are here"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                } else {
                    Toast.makeText(this, "Unable to get your location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Cambiar el tipo de mapa (Normal, Satélite, Terreno, Híbrido)
    private fun changeMapType() {
        currentMapType = (currentMapType + 1) % 4
        when (currentMapType) {
            0 -> mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            1 -> mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            2 -> mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            3 -> mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        }
        Toast.makeText(this, "Map type changed", Toast.LENGTH_SHORT).show()
    }

    // Método para trazar la ruta al lugar seleccionado
    private fun drawRouteTo(destination: LatLng) {
        // Asegúrate de que el usuario está en una ubicación
        val origin = LatLng(mMap.myLocation.latitude, mMap.myLocation.longitude)

        // Asegúrate de que tienes la clave de API
        val apiKey = getString(R.string.google_maps_key)

        // Construye la URL de la API de Directions
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&key=$apiKey"

        // Realiza la solicitud a la API de Directions
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val directionsResponse = Gson().fromJson(responseBody, DirectionsResponse::class.java)

                    runOnUiThread {
                        // Dibuja las rutas obtenidas
                        directionsResponse.routes.forEach { route ->
                            route.legs.forEach { leg ->
                                leg.steps.forEach { step ->
                                    drawPolyline(step.polyline.points)
                                }
                            }
                        }
                    }
                } else {
                    Log.e("Directions API", "Error en la respuesta: ${response.message}")
                }
            }
        })
    }

    // Método para dibujar la polilínea en el mapa
    private fun drawPolyline(encoded: String) {
        val path = PolyUtil.decode(encoded)
        if (path.isNotEmpty()) {
            val polylineOptions = PolylineOptions()
                .addAll(path)
                .width(10f)
                .color(android.graphics.Color.BLUE)
            mMap.addPolyline(polylineOptions)
        } else {
            Log.d("Polyline", "No points found in polyline")
        }
    }
}
