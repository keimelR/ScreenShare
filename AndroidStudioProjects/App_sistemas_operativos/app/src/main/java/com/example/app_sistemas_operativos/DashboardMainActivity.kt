package com.example.app_sistemas_operativos

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.app.Activity
import android.os.Build
import android.view.TextureView
import android.widget.Button
import com.example.app_sistemas_operativos.service.CapturadoraPantallaService
import android.widget.EditText


class firstActivity : AppCompatActivity() {
    private lateinit var buttonDashboardClient: Button
    private lateinit var buttonDashboardServer: Button
    private lateinit var buttonDashboardAcercaDe: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_main)

        buttonDashboardClient = findViewById(R.id.DashboardClient)
        buttonDashboardServer = findViewById(R.id.DashboardServer)
        buttonDashboardAcercaDe = findViewById(R.id.DashboardAcercaDe)

        buttonDashboardClient.setOnClickListener {
            val intent = Intent(this, DashboardClientActivity::class.java)
            startActivity(intent)
        }

        buttonDashboardServer.setOnClickListener {
            val intent = Intent(this, DashboardServerActivity::class.java)
            startActivity(intent)
        }
    }
}