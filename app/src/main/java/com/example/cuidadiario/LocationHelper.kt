package com.example.cuidadiario

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

class LocationHelper(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun captureCurrentLocation(onResult: (latitude: Double, longitude: Double, address: String) -> Unit) {
        if (!hasLocationPermission()) {
            onResult(0.0, 0.0, "Ubicación sin permiso")
            return
        }

        val cts = CancellationTokenSource()

        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lng = location.longitude
                    val addressText = getAddressFromCoordinates(lat, lng)
                    onResult(lat, lng, addressText)
                } else {
                    onResult(0.0, 0.0, "Ubicación no disponible")
                }
            }
            .addOnFailureListener {
                onResult(0.0, 0.0, "Error al obtener GPS")
            }
    }

    private fun getAddressFromCoordinates(lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.Builder().setLanguage("es").setRegion("GT").build())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val dir = addresses[0]
                    val municipio = dir.locality ?: dir.subAdminArea ?: "Ciudad de Guatemala"
                    val departamento = dir.adminArea ?: "Guatemala"
                    "$municipio, $departamento"
                } else {
                    "Ciudad de Guatemala, Guatemala"
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val dir = addresses[0]
                    val municipio = dir.locality ?: dir.subAdminArea ?: "Ciudad de Guatemala"
                    val departamento = dir.adminArea ?: "Guatemala"
                    "$municipio, $departamento"
                } else {
                    "Ciudad de Guatemala, Guatemala"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Ciudad de Guatemala, Guatemala"
        }
    }
}