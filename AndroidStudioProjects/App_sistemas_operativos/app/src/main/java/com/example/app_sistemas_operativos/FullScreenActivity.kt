package com.example.app_sistemas_operativos

import android.content.Intent
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.app_sistemas_operativos.service.CapturadoraPantallaService
import com.example.app_sistemas_operativos.service.Client

class FullScreenActivity : AppCompatActivity() {
    private lateinit var textureView: TextureView
    private var surface: Surface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen)

        textureView = findViewById(R.id.fullScreenTextureView)

        val clientIp = intent.getStringExtra(EXTRA_CLIENT_IP)
        val clientPort = intent.getIntExtra(EXTRA_CLIENT_PORT, 0)

        val backButton: Button = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            stopClientSessionAndNavigateBack()
        }

        if (textureView.isAvailable) {
            textureView.surfaceTexture?.let { startClientService(clientIp, clientPort, it) }
        } else {
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                    startClientService(clientIp, clientPort, surfaceTexture)
                }

                override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {}

                override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                    surface?.release() // Libera la superficie
                    surface = null
                    return true
                }

                override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
            }
        }
    }

    private fun startClientService(ip: String?, port: Int, surfaceTexture: SurfaceTexture) {
        surface = Surface(surfaceTexture)
        val serviceIntent = Intent(this, CapturadoraPantallaService::class.java).apply {
            putExtra(CapturadoraPantallaService.EXTRA_IS_SERVER, false)
            putExtra(CapturadoraPantallaService.EXTRA_CLIENT_IP, ip)
            putExtra(CapturadoraPantallaService.EXTRA_CLIENT_PORT, port)
            putExtra(CapturadoraPantallaService.EXTRA_SURFACE, surface)
        }

        // Verificar la versión de Android para iniciar el servicio en el modo correcto
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopClientSessionAndNavigateBack() {
        Client.stopClient() // Detener el cliente
        stopClientService()
        releaseSurface()
        val intent = Intent(this, DashboardClientActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish() // Finaliza la actividad actual
    }

    override fun onBackPressed() {
        super.onBackPressed()
        stopClientSessionAndNavigateBack()
    }

    private fun stopClientService() {
        val serviceIntent = Intent(this, CapturadoraPantallaService::class.java)
        stopService(serviceIntent)
    }

    private fun releaseSurface() {
        surface?.release()
        surface = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Client.stopClient() // Asegurarse de detener el cliente al destruir la actividad
        releaseSurface() // Libera la superficie al destruir la actividad
    }

    companion object {
        const val EXTRA_CLIENT_IP = "CLIENT_IP"
        const val EXTRA_CLIENT_PORT = "CLIENT_PORT"
    }
}