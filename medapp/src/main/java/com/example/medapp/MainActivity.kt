package com.example.medapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    private var locationManager: LocationManager? = null
    private var locations = ArrayList<String>()
    private var adapter: MyRecyclerViewAdapter? = null

    private var locationTimeBetweenMs: Long = 5000
    private var locationPoint: Location? = null

    private var uniqueID: String? = null
    private val PREF_UNIQUE_ID = "PREF_UNIQUE_ID"

    @Synchronized
    fun id(context: Context): String? {
        if (uniqueID == null) {
            val sharedPrefs: SharedPreferences = context.getSharedPreferences(
                PREF_UNIQUE_ID, Context.MODE_PRIVATE
            )
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null)
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString()
                val editor: SharedPreferences.Editor = sharedPrefs.edit()
                editor.putString(PREF_UNIQUE_ID, uniqueID)
                editor.commit()
            }
        }
        return uniqueID
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        AndroidNetworking.initialize(getApplicationContext());

        id(this)

        // check location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
//                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ), 1
            )
        }

        // request location updates
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                locationTimeBetweenMs,
                0f,
                locationListener
            )
        } catch (ex: SecurityException) {
            Log.d("myTag", "Security Exception, no location available")
        }


        // set up the RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.locationsList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MyRecyclerViewAdapter(this, locations)
        recyclerView.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        val jsonString = """
                {
                    "userId": "$uniqueID",
                    "time":"1",
                    "squareCenter": {
                   	 "lat": ${locationPoint?.latitude},
                   	 "lon": ${locationPoint?.longitude}
                    }
                }
            """.trimIndent()

        val jsonObj = JSONObject(jsonString)
        AndroidNetworking.put("http://192.168.0.106:8080/leaveSquare")
            .addJSONObjectBody(jsonObj)
            .setTag(this)
            .setPriority(Priority.MEDIUM)
            .build();
    }

    private val locationListener: LocationListener = object : LocationListener {
        var center: Location? = null
        var edgeLen: Double = 0.0

        override fun onLocationChanged(location: Location) {
            if (center == null) {
                sendRequestToServer(location)
            } else {
                val isLocationChanged = checkIfLocationChanged(location);
                if (isLocationChanged) {
                    sendRequestToServer(location)
                }
            }
        }

        fun checkIfLocationChanged(location: Location): Boolean {
            if (kotlin.math.abs(location.latitude - center?.latitude!!) < edgeLen / 2 && kotlin.math.abs(
                    location.longitude - center?.longitude!!
                ) < edgeLen / 2
            ) return false
            return true
        }

        fun sendRequestToServer(location: Location) {
            val deviceId = uniqueID

            var jsonString = ""
            if (center == null) {
                jsonString = """
                {
                    "userId": "$deviceId",
                    "time":${location.time},
                    "position": {
                   	 "lat": ${location.latitude},
                   	 "lon": ${location.longitude}
                    },
                    "prevSquareCenter": null
                }
            """.trimIndent()
            } else {
                jsonString = """
                {
                    "userId": "$deviceId",
                    "time":${location.time},
                    "position": {
                   	 "lat": ${location.latitude},
                   	 "lon": ${location.longitude}
                    },
                    "prevSquareCenter": {
                   	 "lat": ${center?.latitude},
                   	 "lon": ${center?.longitude}
                    }
                }
            """.trimIndent()
            }
            val jsonObj = JSONObject(jsonString)
            AndroidNetworking.post("http://192.168.0.106:8080/getSquare")
                .addJSONObjectBody(jsonObj)
                .setTag(this)
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsJSONObject(object : JSONObjectRequestListener {
                    override fun onResponse(response: JSONObject) {
                        println("${location.latitude};${location.longitude} -- center: ${center?.latitude};${center?.longitude} -- edgeLen: $edgeLen")
                        center = Location("")
                        center?.latitude = response.getJSONObject("center").getDouble("lat")
                        center?.longitude = response.getJSONObject("center").getDouble("lon")
                        locationPoint = center
                        edgeLen = response.getDouble("edgeLen")
                        locations.add("${location.latitude};${location.longitude}")
                        adapter?.notifyDataSetChanged()
                    }

                    override fun onError(error: ANError) {
                        println(error)
                    }
                })
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
}
