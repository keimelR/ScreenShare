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
    private lateinit var clientButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)

        textureView = findViewById(R.id.textureView)

        //Puerto del servidor ingresado por el dispositivo que inicia la transmision
        serverPort = findViewById(R.id.serverPortEditText)

        //Direccion ip ingresado por el dispositivo que ve la transmision
        clientIp = findViewById(R.id.clientIpEditText)
        //Puerto del servidor ingresado por el dispositivo que ve la transmision
        clientPort = findViewById(R.id.clientPortEditText)

        // Boton que inicia la transmision
        serverButton = findViewById(R.id.serverButton)


        // Escuchar el clic del botón de creacion del servidor y transmision
        serverButton.setOnClickListener {
            val intent = Intent(this, CapturadoraPantallaService::class.java).apply {
                putExtra(CapturadoraPantallaService.EXTRA_IS_SERVER, true)
                mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE)
            }
        }

        // Escuchar el clic del botón de creacion del servidor y transmision
        clientButton = findViewById(R.id.clientButton)

        clientButton.setOnClickListener {
            // Iniciar el servicio de captura de pantalla en modo cliente
            val intent = Intent(this, CapturadoraPantallaService::class.java).apply {
                // Notificar al servicio que se debe iniciar en modo cliente
                putExtra(CapturadoraPantallaService.EXTRA_IS_SERVER, false)

                // Enviar los datos necesarios del puerto del servidor y la dirección IP del cliente
                putExtra(CapturadoraPantallaService.EXTRA_CLIENT_IP, clientIp.text.toString())
                putExtra(CapturadoraPantallaService.EXTRA_CLIENT_PORT, clientPort.text.toString().toInt())
            }

            // Verificar la versión de Android para iniciar el servicio en el modo correcto
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == Activity.RESULT_OK) {
            val intent = Intent(this, CapturadoraPantallaService::class.java).apply {
                // Agregar los datos necesarios al Intent
                putExtra(CapturadoraPantallaService.EXTRA_RESULT_CODE, resultCode)
                putExtra(CapturadoraPantallaService.EXTRA_DATA, data)

                // Agregar el SurfaceView al Intent
                putExtra(CapturadoraPantallaService.EXTRA_SURFACE,
                    android.view.Surface(textureView.surfaceTexture)
                )

                // Configurar el Activity para que el usuario pueda ingresar la dirección IP y puerto del servidor
                putExtra(CapturadoraPantallaService.EXTRA_SERVER_PORT, serverPort.text.toString().toInt())

                // Indica al servicio que se debe iniciar en modo servidor
                putExtra(CapturadoraPantallaService.EXTRA_IS_SERVER, true)
            }
            serverButton.isEnabled = false

            // Iniciar el servicio de captura de pantalla en modo servidor
            startService(intent)
        }
    }

    companion object {
        private const val REQUEST_CODE_SCREEN_CAPTURE = 1001
    }
}