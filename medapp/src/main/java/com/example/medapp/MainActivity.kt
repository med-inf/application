package com.example.medapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.*
import android.telecom.ConnectionService
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.androidnetworking.AndroidNetworking
import java.sql.Connection


class MainActivity : AppCompatActivity() {
//    /** Messenger for communicating with the service.  */
//    private var mService: Messenger? = null
//
//    /** Flag indicating whether we have called bind on the service.  */
//    private var bound: Boolean = false
//
//    /**
//     * Class for interacting with the main interface of the service.
//     */
//    private val mConnection = object : ServiceConnection {
//
//        override fun onServiceConnected(className: ComponentName, service: IBinder) {
//            // This is called when the connection with the service has been
//            // established, giving us the object we can use to
//            // interact with the service.  We are communicating with the
//            // service using a Messenger, so here we get a client-side
//            // representation of that from the raw IBinder object.
//            mService = Messenger(service)
//            bound = true
//        }
//
//        override fun onServiceDisconnected(className: ComponentName) {
//            // This is called when the connection with the service has been
//            // unexpectedly disconnected -- that is, its process crashed.
//            mService = null
//            bound = false
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

        val intent = Intent(this, MyService::class.java)
        startService(intent)
    }

//    override fun onDestroy() {
//        if(bound){
//            val msg: Message = Message.obtain(null, MSG_LEAVE_SQUARE, 0, 0)
//            try {
//                mService?.send(msg)
//            } catch (e: RemoteException) {
//                e.printStackTrace()
//            }
//        }
//
//        super.onDestroy()
//    }

//    override fun onStart() {
//        super.onStart()
//        // Bind to the service
//        Intent(this, MyService::class.java).also { intent ->
//            bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
//        }
//    }
//
//    override fun onStop() {
//        super.onStop()
//        // Unbind from the service
//        if (bound) {
//            unbindService(mConnection)
//            bound = false
//        }
//    }
}
