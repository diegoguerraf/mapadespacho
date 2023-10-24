package com.example.prueba2

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlin.math.*

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), OnMapReadyCallback{

    companion object {
        private const val REQUEST_CODE_AUTOCOMPLETE_FROM = 1
        private const val REQUEST_CODE_AUTOCOMPLETE_TO = 2
        private const val TAG = "MainActivity"
    }
    private lateinit var mMap: GoogleMap
    private var mMarkerFrom: Marker? = null
    private var mMarkerTo: Marker? = null
    private lateinit var mFromLatLng: LatLng
    private lateinit var mToLatLng: LatLng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupMap()


        // Inicializar Places
        Places.initialize(applicationContext, getString(R.string.key_api))

        val btnFrom = findViewById<Button>(R.id.btnFrom)
        val btnTo = findViewById<Button>(R.id.btnTo)

        btnFrom.setOnClickListener {
            startAutocomplete(REQUEST_CODE_AUTOCOMPLETE_FROM)
        }

        btnTo.setOnClickListener {
            startAutocomplete(REQUEST_CODE_AUTOCOMPLETE_TO)
        }
    }

    private  fun setupMap(){
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }



    private fun startAutocomplete(requestCode: Int) {
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)

        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
            .build(this)
        startActivityForResult(intent, requestCode)
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        val tvFrom = findViewById<TextView>(R.id.tvFrom)
        val tvtO = findViewById<TextView>(R.id.tvTo)

        if (requestCode == REQUEST_CODE_AUTOCOMPLETE_FROM) {
            processAutocompleteResult(resultCode, data) {place ->
                tvFrom.text= getString(R.string.label_from) + " " + place.address
                place.latLng?.let {
                    mFromLatLng= it
                }
                setMarkerFrom(mFromLatLng)

            }
            return
        }else if (requestCode == REQUEST_CODE_AUTOCOMPLETE_TO) {
            processAutocompleteResult(resultCode, data) {place ->
                tvtO.text= getString(R.string.label_from) + " " + place.address
                place.latLng?.let {
                    mToLatLng= it
                }
                setMarkerTo(mFromLatLng)
                addMarker(mToLatLng, getString(R.string.marker_title_to))
            }
            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun processAutocompleteResult(resultCode: Int, data: Intent?,
                                          callback: (Place)-> Unit) {
        findViewById<TextView>(R.id.tvFrom)
        findViewById<TextView>(R.id.tvTo)
        Log.d(TAG, "pprocessAutocompleteResult")

        when (resultCode) {
            Activity.RESULT_OK -> {
                data?.let {
                    val place = Autocomplete.getPlaceFromIntent(data)
                    Log.i(TAG, "Place: $place")
                    callback(place)



                }
            }
            AutocompleteActivity.RESULT_ERROR -> {
                data?.let {
                    val status = Autocomplete.getStatusFromIntent(data)
                    status.statusMessage?.let {
                            message -> Log.i(TAG, message) }
                }
            }
            Activity.RESULT_CANCELED -> {
                // El usuario canceló la selección
            }
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setMinZoomPreference(5f)
        mMap.setMaxZoomPreference(20f)
        mMap.uiSettings.isZoomControlsEnabled = true

        //val Santiago =LatLng (-33.45694, -70.64827)
        //addMarker(Santiago, "Santiago")

        //mMap.moveCamera(CameraUpdateFactory.newLatLng(Santiago))
    }

    private fun addMarker(latLng: LatLng, title: String): Marker? {
        val markerOptions = MarkerOptions()
            .position(latLng)
            .title(title)

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))

        return mMap.addMarker(markerOptions)


    }


    private fun setMarkerFrom(latLng: LatLng) {
        try {
            mMarkerFrom?.remove()
            mMarkerFrom = addMarker(latLng, getString(R.string.marker_title_from))
            calculateDistanceAndPrice()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setMarkerTo(latLng: LatLng) {
        try {
            mMarkerTo?.remove()
            mMarkerTo = addMarker(latLng, getString(R.string.marker_title_to))
            calculateDistanceAndPrice()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateDistanceAndPrice() {
        val distanciaKm = calculateDistance(mFromLatLng, mToLatLng)
        val valorCompra = 60000 // Ejemplo de valor de compra (en pesos)
        val precioDespacho = calculatePrecioDespacho(valorCompra, distanciaKm)

        val txtDistanceAndPrice = findViewById<TextView>(R.id.txtDistanceAndPrice)
        txtDistanceAndPrice.text = "Distancia: $distanciaKm km\nPrecio de despacho: $$precioDespacho"
    }

    private fun calculateDistance(startLatLng: LatLng, endLatLng: LatLng): Double {
        val earthRadius = 6371.0 // Radio de la Tierra en kilómetros

        val lat1 = Math.toRadians(startLatLng.latitude)
        val lon1 = Math.toRadians(startLatLng.longitude)
        val lat2 = Math.toRadians(endLatLng.latitude)
        val lon2 = Math.toRadians(endLatLng.longitude)

        val dlon = lon2 - lon1
        val dlat = lat2 - lat1

        val a = sin(dlat/2).pow(2) + cos(lat1) * cos(lat2) * sin(dlon/2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    private fun calculatePrecioDespacho(valorCompra: Int, distanciaEnKilometros: Double): Int {
        return when {
            valorCompra > 50000 && distanciaEnKilometros <= 20 -> 0 // Despacho gratuito
            valorCompra in 25000..49999 -> (distanciaEnKilometros * 150).toInt()
            else -> (distanciaEnKilometros * 300).toInt()
        }
    }

}
