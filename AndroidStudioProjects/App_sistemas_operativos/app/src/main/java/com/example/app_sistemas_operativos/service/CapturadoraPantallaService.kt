package com.example.app_sistemas_operativos.service

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.net.ServerSocket
import java.net.Socket
import android.util.Log
import java.io.DataInputStream

class CapturadoraPantallaService : Service() {
    // Variables para la captura de pantalla
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var imageReader: ImageReader
    private lateinit var surface: Surface

    // Variables para el servidor
    private lateinit var socket: Socket
    private var serverIp: String? = "localhost"
    private var serverPort: Int = 0
    private lateinit var serverSocket: ServerSocket
    private val clients = mutableListOf<Socket>()


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

        // Creacion del servidor
        createServer()
        //Creacion del cliente
        connectToServer()

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
                sendBitmapToServer(bitmap)


                //  Muestra el bitmap en el SurfaceView de mi activity. Fue usado para ejemplo, pero no es necesario
                val outputSurface = surface.lockCanvas(null)
                outputSurface.drawBitmap(bitmap, 0f, 0f, null)
                surface.unlockCanvasAndPost(outputSurface)


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
                Log.d("CapturadoraPantallaSe", "Cliente conectado al servidor en $serverIp:$serverPort")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun createServer(){

        Thread {
            // Crea el servidor de sockets
            serverSocket = ServerSocket(7584)
            Log.d("CapturadoraPantallaSe", "Servidor iniciado en el puerto 7584")

            while (true) {
                val clientSocket = serverSocket.accept()
                clients.add(clientSocket)
                Log.d("CapturadoraPantallaSe", "Cliente añadido a la lista")

                Thread {
                    clientHandler(clientSocket)
                }.start()
            }

        }.start()
    }

    private fun clientHandler(clientSocket: Socket) {
        try {
            // Lógica para manejar la comunicación con el cliente
            val inputStream = clientSocket.getInputStream()
            val dataInputStream = DataInputStream(inputStream)

            while (true) {
                val byteArraySize = dataInputStream.readInt()
                val byteArray = ByteArray(byteArraySize)
                dataInputStream.readFully(byteArray)

                // Convertir el byteArray en un Bitmap y mostrarlo en la interfaz de usuario
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                // Aquí puedes hacer lo que necesites con el bitmap recibido, como mostrarlo en un ImageView

                // Realizar cualquier otra lógica requerida con el bitmap recibido
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Cerrar el socket cuando finalice la comunicación con el cliente
            clientSocket.close()
        }
    }

    /*
    *   sendBitmapToServer().
    *
    *   Comprime el bitmap en un arreglo de bytes que se puede enviar al servidor.
    *   Envia el arreglo de bytes al servidor.
    */
    private fun sendBitmapToServer(bitmap: Bitmap) {
        // Convierte el bitmap a un arreglo de bytes
        val byteArray = bitmapToByteArray(bitmap)

        // Hilo para enviar el arreglo de bytes a todos los clientes
        Thread {
            try {
                // Recorre la lista de clientes y envía el arreglo de bytes a cada uno
                for (clientSocket in clients) {
                    val outputStream = clientSocket.getOutputStream()
                    val dataOutputStream = DataOutputStream(outputStream)
                    dataOutputStream.writeInt(byteArray.size)
                    dataOutputStream.write(byteArray)
                    dataOutputStream.flush()
                }
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