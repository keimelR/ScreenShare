package com.example.app_sistemas_operativos

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.app.Activity
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.app_sistemas_operativos.service.CapturadoraPantallaService


class firstActivity : AppCompatActivity() {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)

        surfaceView = findViewById(R.id.surfaceView)
        surfaceHolder = surfaceView.holder

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == Activity.RESULT_OK) {
            val intent = Intent(this, CapturadoraPantallaService::class.java)
            // Agregar los datos necesarios al Intent
            intent.putExtra(CapturadoraPantallaService.EXTRA_RESULT_CODE, resultCode)
            intent.putExtra(CapturadoraPantallaService.EXTRA_DATA, data)

            // Agregar el SurfaceView al Intent
            intent.putExtra(CapturadoraPantallaService.EXTRA_SURFACE, surfaceView.holder.surface)

            // Configurar el Activity para que el usuario pueda ingresar la dirección IP y puerto del servidor
            intent.putExtra(CapturadoraPantallaService.EXTRA_SERVER_IP, "192.168.1.100")
            intent.putExtra(CapturadoraPantallaService.EXTRA_SERVER_PORT, 8080)  // Cambia esto al puerto del servidor

            // Iniciar el servicio de captura de pantalla
            startService(intent)
        }
    }

    companion object {
        private const val REQUEST_CODE_SCREEN_CAPTURE = 1001
    }

    /*      CODIGO FUNCIONAL 100%
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private val REQUEST_CODE_CAPTURE_PERM = 1
    private val REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 2

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_first)

            // Solicitar permisos de almacenamiento si no están concedidos
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_WRITE_EXTERNAL_STORAGE)
                } else {
                    startMediaProjection()
                }
            } else {
                startMediaProjection()
            }
        }

        private fun startMediaProjection() {
            mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val intent = mediaProjectionManager.createScreenCaptureIntent()
            startActivityForResult(intent, REQUEST_CODE_CAPTURE_PERM)
        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startMediaProjection()
                } else {
                    Toast.makeText(this, "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CAPTURE_PERM && resultCode == Activity.RESULT_OK) {
            val serviceIntent = Intent(this, CapturadoraPantallaService::class.java).apply {
                putExtra("result_code", resultCode)
                putExtra("data", data)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val serviceChannel = NotificationChannel(
                    "SCREEN_CAPTURE_CHANNEL",
                    "Screen Capture Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                val manager = getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(serviceChannel)
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }
    */
}