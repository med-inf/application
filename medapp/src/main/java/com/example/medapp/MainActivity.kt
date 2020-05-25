package com.example.medapp

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
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
import androidx.room.Room
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject
import java.lang.Math.abs
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    private var locationManager: LocationManager? = null
    private var locations = ArrayList<String>()
    private var adapter: MyRecyclerViewAdapter? = null
    private lateinit var wordViewModel: WordViewModel

    private var locationTimeBetweenMs: Long = 5000
    private var db: AppDatabase? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AndroidNetworking.initialize(getApplicationContext());

        id(this)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "location-database"
        ).build()

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

    private val locationListener: LocationListener = object : LocationListener {
        var prevSquareId: String = "c281af7f-830d-4c8b-8894-ba36d08d1aa7"
        var center: Location = Location("")
        var edgeLen: Double = 0.0

        override fun onLocationChanged(location: Location) {
            val isLocationChanged = checkIfLocationChanged(location);
            if (isLocationChanged) {
                val currentTime = Calendar.getInstance().time

                sendRequestToServer(location, currentTime)
            }
        }

        fun checkIfLocationChanged(location: Location): Boolean {
            if (kotlin.math.abs(location.latitude - center.latitude) < edgeLen / 2 && kotlin.math.abs(
                    location.longitude - center.longitude
                ) < edgeLen / 2
            ) return false
            return true
        }

        fun sendRequestToServer(location: Location, time: Date) {
            val deviceId = uniqueID
            println("id: $deviceId time: $time")
            val jsonString = """
                {
                    "userId": "$deviceId",
                    "time":"$time",
                    "position": {
                   	 "lat": ${location.latitude},
                   	 "lon": ${location.longitude}
                    },
                    "prevSquareId": "$prevSquareId"
                }
            """.trimIndent()
            val jsonObj = JSONObject(jsonString)
            AndroidNetworking.post("http://192.168.0.106:8080/getSquare")
                .addJSONObjectBody(jsonObj)
                .setTag(this)
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsJSONObject(object : JSONObjectRequestListener {
                    override fun onResponse(response: JSONObject) {
                        println(response)
                        prevSquareId =
                            response.getString("id")
                        center.latitude = response.getDouble("center.lat")
                        center.longitude = response.getDouble("center.lon")
                        edgeLen = response.getDouble("edgeLen")
                        val time = response.getDouble("time")
                        saveToDatabase(prevSquareId, time);
                        locations.add(prevSquareId)
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
