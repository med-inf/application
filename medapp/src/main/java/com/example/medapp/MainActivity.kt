package com.example.medapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.StringRequestListener
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var mHandler: Handler

    private var uniqueID: String? = null


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
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ), 1
            )
        }
        id(this)
        println(uniqueID)

        val intent = Intent(this, MyService::class.java)
        startService(intent)

        mHandler = Handler()
        startRepeatingTask()
    }

    private fun changeStatusToDanger(){
        val img: ImageView = findViewById<ImageView>(R.id.status_image)
        img.setImageResource(android.R.drawable.ic_delete)
        val text: TextView = findViewById<TextView>(R.id.main_text)
        text.text = "You may be infected. Please contact a doctor."
    }

    private fun changeStatusToGood(){
        val img: ImageView = findViewById<ImageView>(R.id.status_image)
        img.setImageResource(android.R.drawable.ic_input_add)
        val text: TextView = findViewById<TextView>(R.id.main_text)
        text.text = ""
    }


    override fun onDestroy() {
        super.onDestroy()
        stopRepeatingTask()
    }

    private var mStatusChecker: Runnable = object : Runnable {
        override fun run() {
            AndroidNetworking.get(CHECK_INFECTED_ENDPOINT)
                .addQueryParameter("userId", uniqueID)
                .setTag(this)
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsString(object : StringRequestListener {
                    override fun onResponse(response: String?) {
                        if (response.equals("true")){
                            changeStatusToDanger()
                        } else {
                            changeStatusToGood()
                        }
                    }

                    override fun onError(error: ANError) {
                        println(error.errorBody)
                    }
                })

            mHandler.postDelayed(this, CHECK_HEALTH_INTERVAL.toLong())
        }
    }

    private fun startRepeatingTask() {
        mStatusChecker.run()
    }

    private fun stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker)
    }

    companion object {
        private const val CHECK_INFECTED_ENDPOINT = "http://192.168.0.106:8080/infected"
        private const val PREF_UNIQUE_ID = "PREF_UNIQUE_ID"
        private const val CHECK_HEALTH_INTERVAL = 5000
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
