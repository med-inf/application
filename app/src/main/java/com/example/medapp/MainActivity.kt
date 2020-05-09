package com.example.medapp

import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.example.medapp.ui.main.SectionsPagerAdapter
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_main.*
import org.json.JSONObject


class MainActivity : AppCompatActivity() {
    private var locationManager: LocationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)

        AndroidNetworking.initialize(getApplicationContext());

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?

        try {
            // Request location updates
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                0L,
                0f,
                locationListener
            )
        } catch (ex: SecurityException) {
            Log.d("myTag", "Security Exception, no location available")
        }

    }

    //define the listener
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            thetext.text = ("" + location.longitude + ":" + location.latitude)

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
                        val squareId : String =
                            response.getString("id")
                        thetext2.text = (squareId)
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