package com.example.medapp

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.*
import android.util.Log
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject
import java.util.*

class MyService : Service() {

    private var mLocationManager: LocationManager? = null

    private var uniqueID: String? = null
    private val PREF_UNIQUE_ID = "PREF_UNIQUE_ID"

    private var locationPoint: Location? = null

    private inner class LocationListener(provider: String) : android.location.LocationListener {
        var mLastLocation: Location

        var center: Location? = null
        var edgeLen: Double = 0.0

        override fun onLocationChanged(location: Location) {
            Log.e(TAG, "onLocationChanged: $location")
            mLastLocation.set(location)
            if (center == null) {
                sendRequestToServer(location)
            } else {
                val isLocationChanged = checkIfLocationChanged(location);
                if (isLocationChanged) {
                    sendRequestToServer(location)
                }
            }
        }

        override fun onProviderDisabled(provider: String) {
            Log.e(TAG, "onProviderDisabled: $provider")
        }

        override fun onProviderEnabled(provider: String) {
            Log.e(TAG, "onProviderEnabled: $provider")
        }

        override fun onStatusChanged(
            provider: String,
            status: Int,
            extras: Bundle
        ) {
            Log.e(TAG, "onStatusChanged: $provider")
        }

        init {
            Log.e(TAG, "LocationListener $provider")
            mLastLocation = Location(provider)
        }

        fun checkIfLocationChanged(location: Location): Boolean {
            if (kotlin.math.abs(
                    location.latitude - (center?.latitude ?: 0.0)
                ) < edgeLen / 2 && kotlin.math.abs(
                    location.longitude - (center?.longitude ?: 0.0)
                ) < edgeLen / 2
            ) return false
            return true
        }

        fun sendRequestToServer(location: Location) {
            val deviceId = uniqueID
            println(location)
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
            AndroidNetworking.post(GET_SQUARE_ENDPOINT)
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
                    }

                    override fun onError(error: ANError) {
                        println(error)
                    }
                })


        }
    }

    private var mLocationListeners = arrayOf(
        LocationListener(LocationManager.GPS_PROVIDER),
        LocationListener(LocationManager.NETWORK_PROVIDER)
    )

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.e(TAG, "onStartCommand")
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onCreate() {
        Log.e(TAG, "onCreate")
        id(this)
        AndroidNetworking.initialize(getApplicationContext());
        initializeLocationManager()
        try {
            mLocationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                LOCATION_INTERVAL.toLong(),
                LOCATION_DISTANCE,
                mLocationListeners[1]
            )
        } catch (ex: SecurityException) {
            Log.i(
                TAG,
                "fail to request location update, ignore",
                ex
            )
        } catch (ex: IllegalArgumentException) {
            Log.d(
                TAG,
                "network provider does not exist, " + ex.message
            )
        }
        try {
            mLocationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_INTERVAL.toLong(),
                LOCATION_DISTANCE,
                mLocationListeners[0]
            )
        } catch (ex: SecurityException) {
            Log.i(
                TAG,
                "fail to request location update, ignore",
                ex
            )
        } catch (ex: IllegalArgumentException) {
            Log.d(TAG, "gps provider does not exist " + ex.message)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun leaveSquare() {
        Log.e(TAG, "leave square")
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

        AndroidNetworking.put(LEAVE_SQUARE_ENDPOINT)
            .addJSONObjectBody(jsonObj)
            .setTag(this)
            .setPriority(Priority.MEDIUM)
            .build().getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                }

                override fun onError(error: ANError) {
                    println(error)
                }
            });

    }

    override fun onDestroy() {
        Log.e(TAG, "onDestroy")

        this.leaveSquare()

        if (mLocationManager != null) {
            for (i in mLocationListeners.indices) {
                try {
                    mLocationManager?.removeUpdates(mLocationListeners[i])
                } catch (ex: Exception) {
                    Log.i(
                        TAG,
                        "fail to remove location listners, ignore",
                        ex
                    )
                }
            }
        }
        super.onDestroy()
    }

    private fun initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager")
        if (mLocationManager == null) {
            mLocationManager =
                applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }
    }

    companion object {
        private const val TAG = "TESTGPS"
        private const val LOCATION_INTERVAL = 0
        private const val LOCATION_DISTANCE = 0f
        private const val GET_SQUARE_ENDPOINT = "http://192.168.0.106:8080/getSquare"
        private const val LEAVE_SQUARE_ENDPOINT = "http://192.168.0.106:8080/leaveSquare"
    }


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
}