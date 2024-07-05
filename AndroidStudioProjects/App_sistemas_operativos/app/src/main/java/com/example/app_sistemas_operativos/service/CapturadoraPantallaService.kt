package com.example.app_sistemas_operativos.service


import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
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
import android.os.Build
import android.os.IBinder
import android.view.Surface
import androidx.core.app.NotificationCompat
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import android.util.Log
import com.example.app_sistemas_operativos.R
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
    private var clientIp: String? = "localhost"
    private var clientPort: Int = 0
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
        // Inicia el servicio en segundo plano
        startForegroundService()
    }

    /*
    *   onStartCommand().
    *
    *   Se ejecuta cuando se inicia el servicio.
    *   Utiliza el intent con los datos enviados en onActivityResult para iniciar la captura de pantalla.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Verifica si el intent es nulo
        if (intent == null) {
            return START_NOT_STICKY
        }
        // Verifica si es el servidor
        val isServer = intent.getBooleanExtra(EXTRA_IS_SERVER, false)

        // Verifica en que modo se debe ejecutar el servicio
        if (isServer) {
            // Servidor

            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_OK)
            val data = intent.getParcelableExtra<Intent>(EXTRA_DATA) ?: return START_NOT_STICKY
            surface = intent.getParcelableExtra(EXTRA_SURFACE) ?: return START_NOT_STICKY

            // Obtiene el puerto del servidor
            serverPort = intent.getIntExtra(EXTRA_SERVER_PORT, 0)

            mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

            // Crea el servidor
            createServer()

            // Inicia la captura de pantalla
            startScreenCapture()
        } else {
            // Cliente

            // Obtiene los datos del cliente
            clientIp = intent.getStringExtra(EXTRA_CLIENT_IP)
            clientPort = intent.getIntExtra(EXTRA_CLIENT_PORT, 0)

            // Conecta al servidor si los datos son validos
            if (clientIp != null && clientPort != 0) {
                Thread {
                    connectToServer()
                    clientHandler(socket)
                }.start()
            }
        }

        return START_STICKY
    }

    /*
    *   startForegroundService().
    *
    *   Ejecuta una notificacion al servicio para que el usuario sepa que el servicio esta corriendo.
    *   Ademas, es un requisito para que el servicio se ejecute en segundo plano.
    */
    private fun startForegroundService() {
        createNotificationChannel() // Crear el canal de notificación si es necesario

        // Crea una notificacion para el servicio
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Captura de Pantalla")
            .setContentText("Capturando pantalla...")
            .setSmallIcon(R.mipmap.share_screen_logo)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Prioridad de la notificación
            .build()

        // Inicia el servicio en segundo plano
        startForeground(NOTIFICATION_ID, notification)
    }

    /*
    *   createNotificationChannel().
    *
    *   Crea un canal de notificacion para el servicio.
    *   Este canal es necesario para que el servicio se ejecute en segundo plano para dispositivos con
    *   Android 8 o superior.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
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
        try {
            // Crea un socket para conectar al servidor
            socket = Socket(clientIp, clientPort)
            Log.d("CapturadoraPantallaSe", "Cliente conectado al servidor en $clientIp:$clientPort")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createServer(){

        Thread {
            // Crea el servidor de sockets
            serverSocket = ServerSocket(serverPort)
            Log.d("CapturadoraPantallaSe", "Servidor iniciado en el puerto 7584")

            while (true) {
                val clientSocket = serverSocket.accept()
                clients.add(clientSocket)
                Log.d("CapturadoraPantallaSe", "Cliente añadido a la lista")

                // Cuando se agrega un cliente, comienza la ejecucion del hilo para manejar la comunicacion con el cliente
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
            Log.d("Client", "Received byte array size: ${dataInputStream.available()}")


            while (true) {
                val byteArraySize = dataInputStream.readInt()
                Log.d("Client", "Received byte array size: $byteArraySize")

                val byteArray = ByteArray(byteArraySize)
                dataInputStream.readFully(byteArray)

                // Convertir el byteArray en un Bitmap y mostrarlo en la interfaz de usuario
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

                // Aquí puedes hacer lo que necesites con el bitmap recibido, como mostrarlo en un ImageView
                val outputSurface = surface.lockCanvas(null)
                outputSurface.drawBitmap(bitmap, 0f, 0f, null)
                surface.unlockCanvasAndPost(outputSurface)

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
        const val EXTRA_CLIENT_IP = "CLIENT_IP"
        const val EXTRA_CLIENT_PORT = "CLIENT_PORT"
        const val EXTRA_SERVER_PORT = "SERVER_PORT"

        // Constantes para el intent que verifica si es el Server.
        const val EXTRA_IS_SERVER = "IS_SERVER"

        // Constantes para el canal de notificacion
        private const val CHANNEL_ID = "SCREEN_CAPTURE_CHANNEL"
        private const val NOTIFICATION_ID = 1
    }
}