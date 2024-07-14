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
    private lateinit var clientIp: EditText
    private lateinit var clientPort: EditText
    private lateinit var clientButton: Button
    private lateinit var clientDisconnectButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_client)

        clientIp = findViewById(R.id.clientIpEditText)
        clientPort = findViewById(R.id.clientPortEditText)
        clientButton = findViewById(R.id.clientButton)
        clientDisconnectButton = findViewById(R.id.clientDisconnectButton)

        clientButton.setOnClickListener {
            val ip = clientIp.text.toString()
            val port = clientPort.text.toString().toInt()

            val intent = Intent(this, FullScreenActivity::class.java).apply {
                putExtra(FullScreenActivity.EXTRA_CLIENT_IP, ip)
                putExtra(FullScreenActivity.EXTRA_CLIENT_PORT, port)
            }

            startActivity(intent)
        }

        clientDisconnectButton.setOnClickListener {
            val intent = Intent(this, CapturadoraPantallaService::class.java)
            stopService(intent)
        }
    }
}