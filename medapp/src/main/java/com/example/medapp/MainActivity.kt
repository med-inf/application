package com.example.medapp

import android.Manifest
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


class MainActivity : AppCompatActivity() {
    private var locationManager: LocationManager? = null
    private var locations = ArrayList<String>()
    private var adapter: MyRecyclerViewAdapter? = null

    private var locationTimeBetweenMs : Long = 5000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AndroidNetworking.initialize(getApplicationContext());

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
        override fun onLocationChanged(location: Location) {
            locations.add("" + location.longitude + ":" + location.latitude)
            adapter?.notifyDataSetChanged()

            sendRequestToServer()
        }

        fun sendRequestToServer(){
                        val jsonString = """
                {
                    "userId": "c281af7f-830d-4c8b-8894-ba36d08d1aa7",
                    "time":"2020-05-06T19:03:34",
                    "position": {
                   	 "lat": 10.0,
                   	 "lon": 15.0
                    },
                    "prevSquareId": "c281af7f-830d-4c8b-8894-ba36d08d1aa7"
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
                            System.out.println(response)
                            val squareId: String =
                                    response.getString("id")
                        }

                        override fun onError(error: ANError) {
                            System.out.println(error)
                        }
                    })
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
}
