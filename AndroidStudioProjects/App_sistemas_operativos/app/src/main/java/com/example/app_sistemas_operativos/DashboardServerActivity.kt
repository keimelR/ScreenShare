package com.example.app_sistemas_operativos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.TextureView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.app_sistemas_operativos.service.CapturadoraPantallaService
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale

class DashboardServerActivity : AppCompatActivity(){
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var serverPort: EditText
    private lateinit var serverButton: Button
    private lateinit var serverButtonDisconnect: Button
    private lateinit var ipAddressTextView: TextView

    private var serviceIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_server)

        serverPort = findViewById(R.id.serverPortEditText)
        serverButton = findViewById(R.id.serverButton)
        serverButtonDisconnect = findViewById(R.id.serverButtonDisconnect)
        ipAddressTextView = findViewById(R.id.ipAddressTextView)

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        serverButton.setOnClickListener {
            val ipAddress = getIPAddress(true) // Obtener la dirección IP
            ipAddressTextView.text = "IP Address: $ipAddress" // Mostrar la dirección IP en el TextView

            val intent = Intent(this, CapturadoraPantallaService::class.java).apply {
                putExtra(CapturadoraPantallaService.EXTRA_IS_SERVER, true)
            }
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE)
        }

        serverButtonDisconnect.setOnClickListener {
            serviceIntent?.let {
                stopService(it)
                serverButton.isEnabled = true
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == Activity.RESULT_OK) {
            serviceIntent = Intent(this, CapturadoraPantallaService::class.java).apply {
                putExtra(CapturadoraPantallaService.EXTRA_RESULT_CODE, resultCode)
                putExtra(CapturadoraPantallaService.EXTRA_DATA, data)
                putExtra(CapturadoraPantallaService.EXTRA_SERVER_PORT, serverPort.text.toString().toInt())
                putExtra(CapturadoraPantallaService.EXTRA_IS_SERVER, true)
            }
            serverButton.isEnabled = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    fun getIPAddress(useIPv4: Boolean): String {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (intf in interfaces) {
            val addrs = Collections.list(intf.inetAddresses)
            for (addr in addrs) {
                if (!addr.isLoopbackAddress) {
                    val sAddr = addr.hostAddress ?: return ""
                    val isIPv4 = sAddr.indexOf(':') < 0
                    if (useIPv4) {
                        if (isIPv4) return sAddr
                    } else {
                        if (!isIPv4) {
                            val delim = sAddr.indexOf('%') // drop ip6 port suffix
                            return if (delim < 0) sAddr.uppercase(Locale.getDefault()) else sAddr.substring(0, delim).uppercase(Locale.getDefault())
                        }
                    }
                }
            }
        }
        return ""
    }

    companion object {
        private const val REQUEST_CODE_SCREEN_CAPTURE = 1001
    }
}