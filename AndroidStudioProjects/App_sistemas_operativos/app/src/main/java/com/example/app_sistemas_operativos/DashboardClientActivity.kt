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
import androidx.appcompat.app.AppCompatActivity
import com.example.app_sistemas_operativos.service.CapturadoraPantallaService

class DashboardClientActivity : AppCompatActivity() {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var textureView: TextureView
    private lateinit var clientIp: EditText
    private lateinit var clientPort: EditText
    private lateinit var clientButton: Button
    private lateinit var clientDisconnectButton: Button

    private var serviceIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_client)

        textureView = findViewById(R.id.textureView)
        clientIp = findViewById(R.id.clientIpEditText)
        clientPort = findViewById(R.id.clientPortEditText)
        clientButton = findViewById(R.id.clientButton)
        clientDisconnectButton = findViewById(R.id.clientDisconnectButton)

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        clientButton.setOnClickListener {
            val ip = clientIp.text.toString()
            val port = clientPort.text.toString().toInt()

            serviceIntent = Intent(this, CapturadoraPantallaService::class.java).apply {
                putExtra(CapturadoraPantallaService.EXTRA_IS_SERVER, false)
                putExtra(CapturadoraPantallaService.EXTRA_CLIENT_IP, ip)
                putExtra(CapturadoraPantallaService.EXTRA_CLIENT_PORT, port)
                putExtra(CapturadoraPantallaService.EXTRA_SURFACE, android.view.Surface(textureView.surfaceTexture))
            }

            // Verificar la versión de Android para iniciar el servicio en el modo correcto
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }

        clientDisconnectButton.setOnClickListener {
            serviceIntent?.let {
                stopService(it)
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == Activity.RESULT_OK) {
            serviceIntent = Intent(this, CapturadoraPantallaService::class.java).apply {
                putExtra(CapturadoraPantallaService.EXTRA_RESULT_CODE, resultCode)
                putExtra(CapturadoraPantallaService.EXTRA_DATA, data)
                putExtra(CapturadoraPantallaService.EXTRA_IS_SERVER, true)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_SCREEN_CAPTURE = 1001
    }
}