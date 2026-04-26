package com.example.safesafar.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.safesafar.R
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FollowMeActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvLocation: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var locationCallback: LocationCallback
    private var lastLocationUrl = ""
    private var lastLat = 0.0
    private var lastLng = 0.0

    private var googleMap: GoogleMap? = null
    private var currentMarker: Marker? = null
    private var isMapReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_follow_me)

        tvLocation     = findViewById(R.id.tvLiveLocation)
        tvAddress      = findViewById(R.id.tvAddress)
        tvSpeed        = findViewById(R.id.tvSpeed)
        tvLastUpdated  = findViewById(R.id.tvLastUpdated)
        val btnShare   = findViewById<View>(R.id.btnShareTracking)
        val btnOpenMap = findViewById<View>(R.id.btnOpenMap)
        val liveDot    = findViewById<View>(R.id.liveDot)

        // Start blinking animation on live dot
        val blinkAnim = AnimationUtils.loadAnimation(this, R.anim.blink)
        liveDot.startAnimation(blinkAnim)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnShare.setOnClickListener {
            if (lastLocationUrl.isNotEmpty()) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "🚨 Track my live location: $lastLocationUrl"
                    )
                }
                startActivity(Intent.createChooser(shareIntent, "Share via"))
            }
        }

        btnOpenMap.setOnClickListener {
            if (lastLocationUrl.isNotEmpty()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(lastLocationUrl)))
            }
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    lastLat = location.latitude
                    lastLng = location.longitude
                    lastLocationUrl =
                        "https://maps.google.com/?q=${location.latitude},${location.longitude}"

                    tvLocation.text =
                        "📍 Lat: ${String.format("%.5f", location.latitude)}   " +
                        "📍 Lng: ${String.format("%.5f", location.longitude)}   " +
                        "🎯 Accuracy: ${location.accuracy.toInt()} m"

                    val tvMiniCoords = findViewById<TextView>(R.id.tvMiniCoords)
                    val tvMiniAccuracy = findViewById<TextView>(R.id.tvMiniAccuracy)
                    tvMiniCoords.text = "${String.format("%.5f", location.latitude)} , ${String.format("%.5f", location.longitude)}"
                    tvMiniAccuracy.text = "Accuracy: ${location.accuracy.toInt()}m"

                    val speedKmH = (location.speed * 3.6).toInt()
                    tvSpeed.text =
                        if (speedKmH > 0) "Speed: $speedKmH km/h"
                        else "Speed: 0 km/h (stationary)"

                    tvLastUpdated.text =
                        "Last Updated: " +
                        SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())

                    if (isMapReady) {
                        updateMapLocation(location.latitude, location.longitude)
                    } else {
                        // Fallback: Load Google Static Map via Glide
                        loadStaticMap(location.latitude, location.longitude)
                    }

                    // Reverse geocoding
                    try {
                        val geocoder = Geocoder(this@FollowMeActivity, Locale.getDefault())
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                                runOnUiThread {
                                    if (addresses.isNotEmpty()) {
                                        tvAddress.text = "Address: ${addresses[0].getAddressLine(0)}"
                                    }
                                }
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            if (!addresses.isNullOrEmpty()) {
                                tvAddress.text = "Address: ${addresses[0].getAddressLine(0)}"
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapReady = true

        try {
            googleMap?.setMapStyle(com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.isZoomGesturesEnabled = true
        googleMap?.uiSettings?.isScrollGesturesEnabled = true

        // Hide placeholders when map is ready
        findViewById<View>(R.id.layoutMapLoading).visibility = View.GONE
        findViewById<View>(R.id.ivMapPlaceholder).visibility = View.GONE
        findViewById<View>(R.id.ivStaticMap).visibility = View.GONE
        findViewById<View>(R.id.layoutMiniInfo).visibility = View.VISIBLE

        if (lastLat != 0.0 && lastLng != 0.0) {
            updateMapLocation(lastLat, lastLng)
        }
    }

    private fun updateMapLocation(lat: Double, lng: Double) {
        val currentLatLng = LatLng(lat, lng)

        if (currentMarker == null) {
            currentMarker = googleMap?.addMarker(MarkerOptions().position(currentLatLng).title("You are here"))
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
        } else {
            currentMarker?.position = currentLatLng
            googleMap?.animateCamera(CameraUpdateFactory.newLatLng(currentLatLng))
        }
    }

    private fun loadStaticMap(lat: Double, lng: Double) {
        val ivStaticMap      = findViewById<ImageView>(R.id.ivStaticMap)
        val ivPlaceholder    = findViewById<ImageView>(R.id.ivMapPlaceholder)
        val layoutMapLoading = findViewById<LinearLayout>(R.id.layoutMapLoading)

        val mapUrl = "https://maps.googleapis.com/maps/api/staticmap" +
                "?center=$lat,$lng" +
                "&zoom=16" +
                "&size=600x300" +
                "&scale=2" +
                "&markers=color:red%7C$lat,$lng" +
                "&key=YOUR_API_KEY"

        Glide.with(this)
            .load(mapUrl)
            .placeholder(R.drawable.map_placeholder)
            .error(R.drawable.map_placeholder)
            .transition(DrawableTransitionOptions.withCrossFade(400))
            .into(ivStaticMap)

        ivStaticMap.visibility      = View.VISIBLE
        layoutMapLoading.visibility = View.GONE
        ivPlaceholder.visibility    = View.GONE
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 3000
        ).setMinUpdateIntervalMillis(1000).build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        } else {
            tvLocation.text = "❌ Location Permission Denied"
        }
    }
}