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
import android.net.wifi.WifiManager
import android.os.IBinder
import android.text.format.Formatter
import android.view.Surface
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import android.util.Log
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.CopyOnWriteArrayList

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

    private var clientThread: Thread? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        // Inicializa el MediaProjectionManager
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
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

        // Inicia el servicio en segundo plano
        startForegroundService(isServer)

        // Verifica en que modo se debe ejecutar el servicio
        if (isServer) {
            // Servidor

            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_OK)
            val data = intent.getParcelableExtra<Intent>(EXTRA_DATA) ?: return START_NOT_STICKY

            // Obtiene el puerto del servidor
            serverPort = intent.getIntExtra(EXTRA_SERVER_PORT, 0)

            mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

            // Crea el servidor
            Server.createServer(this, serverPort)

            // Inicia la captura de pantalla
            startScreenCapture()
        } else {
            // Cliente

            // Obtiene los datos del cliente
            clientIp = intent.getStringExtra(EXTRA_CLIENT_IP)
            clientPort = intent.getIntExtra(EXTRA_CLIENT_PORT, 0)
            surface = intent.getParcelableExtra(EXTRA_SURFACE) ?: return START_NOT_STICKY

            // Conecta al servidor si los datos son validos
            if (clientIp != null && clientPort != 0) {
                clientThread = Thread {
                    Client.connectToServer(clientIp!!, clientPort, surface)
                }.apply { start() }
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
    private fun startForegroundService(isServer: Boolean) {
        // Asigna el título de la notificacion según el modo
        val tittleNotification = if (isServer) {
            "El servidor se esta ejecutando."
        } else {
            "El cliente se esta ejecutando."
        }

        // Asigna el texto de la notificacion según el modo
        val textNotification = if (isServer) {
            "Transmitiendo pantalla."
        } else {
            "Recibiendo pantalla."
        }

        val notification = Notification.buildNotification(this, tittleNotification, textNotification)
        startForeground(NOTIFICATION_ID, notification)
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
    *   sendBitmapToServer().
    *
    *   Comprime el bitmap en un arreglo de bytes que se puede enviar al servidor.
    *   Envia el arreglo de bytes al servidor.
    */
    private fun sendBitmapToServer(bitmap: Bitmap) {
        // Comprime el bitmap en un arreglo de bytes
        val byteArray = bitmapToByteArray(bitmap)
        Thread {

            synchronized(clients) {
                // Obtiene la lista de clientes conectados
                val iterator = clients.iterator()
                // Itera sobre la lista de clientes y envia el arreglo de bytes al cliente
                while (iterator.hasNext()) {
                    val clientSocket = iterator.next()
                    try {
                        // Verifica si el socket esta abierto
                        if (!clientSocket.isClosed) {
                            // Envia el arreglo de bytes al cliente
                            val outputStream = clientSocket.getOutputStream()
                            val dataOutputStream = DataOutputStream(outputStream)
                            Log.d("Server", "Sending data of size: ${byteArray.size}")

                            synchronized(dataOutputStream) {
                                // Envia el tamaño del arreglo de bytes
                                dataOutputStream.writeInt(byteArray.size)
                                // Envia el arreglo de bytes
                                dataOutputStream.write(byteArray)
                                dataOutputStream.flush()
                                Log.d("Server", "Data sent")
                            }
                        } else {
                            // Si el socket esta cerrado, lo elimina de la lista
                            Log.e("Server", "Socket is closed, removing from clients list")
                            iterator.remove()
                        }
                    } catch (e: IOException) {
                        // Si hay un error al enviar el arreglo de bytes, lo elimina de la lista
                        e.printStackTrace()
                        Log.e("Server", "Error sending data to client: ${e.message}")
                        iterator.remove() // Remove client if there's an error
                    }
                }
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
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    override fun onDestroy() {
        super.onDestroy()
        clientThread?.interrupt()
        surface.release()
        socket.close()
    }

    companion object {
        val clients = CopyOnWriteArrayList<Socket>()

        const val EXTRA_RESULT_CODE = "RESULT_CODE"
        const val EXTRA_DATA = "DATA"
        const val EXTRA_SURFACE = "SURFACE"
        const val EXTRA_CLIENT_IP = "CLIENT_IP"
        const val EXTRA_CLIENT_PORT = "CLIENT_PORT"
        const val EXTRA_SERVER_PORT = "SERVER_PORT"

        // Constantes para el intent que verifica si es el Server.
        const val EXTRA_IS_SERVER = "IS_SERVER"

        // Constantes para el canal de notificacion
        private const val NOTIFICATION_ID = 1
    }
}