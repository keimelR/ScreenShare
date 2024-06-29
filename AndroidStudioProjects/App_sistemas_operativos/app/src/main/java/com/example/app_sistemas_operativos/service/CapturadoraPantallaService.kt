package com.example.app_sistemas_operativos.service

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.view.Surface
import androidx.core.app.NotificationCompat
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.Socket

class CapturadoraPantallaService : Service() {
    // Variables para la captura de pantalla
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var imageReader: ImageReader
    private lateinit var surface: Surface

    // Variables para el servidor
    private lateinit var socket: Socket
    private var serverIp: String? = null
    private var serverPort: Int = 0

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        // Inicializa el MediaProjectionManager
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startForegroundService()
    }
    /*
    *   onStartCommand().
    *
    *   Se ejecuta cuando se inicia el servicio.
    *   Utiliza el intent con los datos enviados en onActivityResult para iniciar la captura de pantalla.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Obtiene los datos enviados en el intent
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_OK) ?: return START_NOT_STICKY
        // Obtiene los datos enviados en el intent
        val data = intent.getParcelableExtra<Intent>(EXTRA_DATA) ?: return START_NOT_STICKY
        // Obtiene el SurfaceView del intent
        surface = intent.getParcelableExtra(EXTRA_SURFACE) ?: return START_NOT_STICKY
        // Obtiene los datos enviados en el intent
        serverIp = intent.getStringExtra(EXTRA_SERVER_IP)
        // Obtiene los datos enviados en el intent
        serverPort = intent.getIntExtra(EXTRA_SERVER_PORT, 0)

        // Inicia la captura de pantalla
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
        startScreenCapture()
        return START_STICKY
    }

    /*
    *   startForegroundService().
    *
    *   Ejecuta una notificacion al servicio para que el usuario sepa que el servicio esta corriendo.
    *   Ademas, es un requisito para que el servicio se ejecute en segundo plano.
    */
    private fun startForegroundService() {
        // Crea una notificacion para el servicio
        val notification = NotificationCompat.Builder(this, "SCREEN_CAPTURE_CHANNEL")
            .setContentTitle("Captura de Pantalla")
            .setContentText("Capturando pantalla...")
            .build()

        // Inicia el servicio en segundo plano
        startForeground(1, notification)
    }

    /*
    *   startScreenCapture().
    *
    *   Ejecuta la captura de pantalla utilizando mediaProjection y virtualDisplay.
    *   Crea un VirtualDisplay que permite capturar la pantalla en tiempo real y
    *   procesa las imagenes capturadas con el ImageReader.
    */
    private fun startScreenCapture() {
        // Obtiene las metricas de la pantalla
        val metrics = Resources.getSystem().displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val dpi = metrics.densityDpi

        // Configura el ImageReader para capturar la pantalla
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        // Crea un VirtualDisplay para capturar la pantalla en tiempo real
        virtualDisplay = mediaProjection!!.createVirtualDisplay(
            "ScreenCapture",
            width, height, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )

        // Conecta al servidor
//        connectToServer()

        // Escucha los eventos de captura de pantalla
        imageReader.setOnImageAvailableListener({ reader ->
            // Captura la imagen capturada
            val image = reader.acquireLatestImage()
            image?.let {
                // Procesamiento los planos de la imagen y el buffer de pixeles
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width

                // Crea un bitmap para la imagen capturada
                val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)

                // Envia el bitmap al servidor
//                sendBitmapToServer(bitmap)

                /*  Muestra el bitmap en el SurfaceView de mi activity. Fue usado para ejemplo, pero no es necesario
                    val outputSurface = surface.lockCanvas(null)
                    outputSurface.drawBitmap(bitmap, 0f, 0f, null)
                    surface.unlockCanvasAndPost(outputSurface)
                */

                // Cierra la imagen capturada
                image.close()
            }
        }, null)
    }


    /*
    *   connectToServer().
    *
    *   Conecta al servidor utilizando el socket.
    */
    private fun connectToServer() {
        // Hilo para conectar al servidor
        Thread {
            try {
                // Crea un socket para conectar al servidor
                socket = Socket(serverIp, serverPort)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    /*
    *   sendBitmapToServer().
    *
    *   Comprime el bitmap en un arreglo de bytes que se puede enviar al servidor.
    *   Envia el arreglo de bytes al servidor.
    */
    private fun sendBitmapToServer(bitmap: Bitmap) {
        // Hilo para enviar el bitmap al servidor
        Thread {
            try {
                // Comprime el bitmap en un arreglo de bytes
                val byteArray = bitmapToByteArray(bitmap)

                // Obtiene el flujo de salida del socket
                val outputStream = socket.getOutputStream()

                // Escribe el tamaño del arreglo de bytes en el flujo de salida
                val dataOutputStream = DataOutputStream(outputStream)

                // Escribe el arreglo de bytes en el flujo de salida
                dataOutputStream.writeInt(byteArray.size)
                dataOutputStream.write(byteArray)
                // Limpia el flujo de salida
                dataOutputStream.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    /*
    *   bitmapToByteArray().
    *
    *   Comprime el bitmap en un arreglo de bytes.
    */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        // Crea un flujo de salida en memoria para el bitmap
        val stream = ByteArrayOutputStream()
        // Comprime el bitmap en el flujo de salida
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    companion object {
        const val EXTRA_RESULT_CODE = "RESULT_CODE"
        const val EXTRA_DATA = "DATA"
        const val EXTRA_SURFACE = "SURFACE"
        const val EXTRA_SERVER_IP = "SERVER_IP"
        const val EXTRA_SERVER_PORT = "SERVER_PORT"
    }
}