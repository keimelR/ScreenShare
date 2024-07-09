package com.example.app_sistemas_operativos

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import android.widget.Button
import com.example.app_sistemas_operativos.service.CapturadoraPantallaService
import android.widget.EditText
import androidx.compose.material3.Surface


class firstActivity : AppCompatActivity() {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var textureView: TextureView
    private lateinit var clientIp: EditText
    private lateinit var clientPort: EditText
    private lateinit var serverPort: EditText
    private lateinit var serverButton: Button
    private lateinit var serverButtonDisconnect: Button
    private lateinit var clientButton: Button
    private var serviceIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)

        textureView = findViewById(R.id.textureView)
        serverPort = findViewById(R.id.serverPortEditText)
        serverButton = findViewById(R.id.serverButton)
        serverButtonDisconnect = findViewById(R.id.serverButtonDisconnect)
        clientIp = findViewById(R.id.clientIpEditText)
        clientPort = findViewById(R.id.clientPortEditText)
        clientButton = findViewById(R.id.clientButton)

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        serverButton.setOnClickListener {
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

    companion object {
        private const val REQUEST_CODE_SCREEN_CAPTURE = 1001
    }
}