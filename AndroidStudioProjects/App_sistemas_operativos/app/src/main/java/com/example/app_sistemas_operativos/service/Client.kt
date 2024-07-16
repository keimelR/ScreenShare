package com.example.app_sistemas_operativos.service

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.view.Surface
import java.io.DataInputStream
import java.io.EOFException
import java.io.IOException
import java.net.Socket

object Client {
    @Volatile
    private var isActive = true

    /*
    *   connectToServer()
    *
    *   Se encarga de establecer la conexión con el servidor
    *
    *   @param clientIp Dirección IP del servidor al que se quiere conectar.
    *   @param clientPort Puerto del servidor al que se quiere conectar.
    *   @param surface Superficie en la que se mostrarán los datos recibidos.
    */
    fun connectToServer(clientIp: String, clientPort: Int, surface: Surface) {
        // Crea un socket para conectar al servidor
        val socket: Socket
        try {
            Log.d("CapturadoraPantallaSe", "Intentando conectar un cliente al servidor en $clientIp:$clientPort")

            // Crea un socket para conectar al servidor
            socket = Socket(clientIp, clientPort)
            Log.d("CapturadoraPantallaSe", "Cliente conectado al servidor en ${socket.inetAddress}:${socket.port}")

            // Comienza a recibir datos del servidor
            isActive = true
            clientHandler(socket, surface)

        } catch (e: java.net.ConnectException) {
            // Excepción lanzada cuando no se puede establecer la conexión con el servidor
            Log.e("CapturadoraPantallaSe", "No se pudo conectar al servidor: ${e.message}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*
    *   clientHandler()
    *
    *   Se encarga de manejar la comunicación con el cliente y recibir datos del servidor
    *
    *   @param clientSocket Socket que representa la conexión con el servidor.
    *   @param surface Superficie en la que se mostrarán los datos recibidos (opcional).
    */
    fun clientHandler(clientSocket: Socket, surface: Surface? = null) {
        // Tamaño máximo del paquete
        val MAX_DATA_SIZE = 1024 * 1024 * 10 // 10 MB

        try {
            // Obtiene los streams de entrada y salida del socket
            val inputStream = clientSocket.getInputStream()
            val dataInputStream = DataInputStream(inputStream)

            while (isActive) {
                try {
                    Log.d("Client", "Waiting to read data size")

                    // Recibe el tamaño del paquete
                    val byteArraySize = dataInputStream.readInt()
                    Log.d("Client", "Data size received: $byteArraySize")

                    // Verifica el tamaño del paquete
                    if (byteArraySize <= 0 || byteArraySize > MAX_DATA_SIZE) {
                        Log.e("Client", "Invalid data size received: $byteArraySize")
                        continue // Ignora este paquete y espera el siguiente
                    }

                    // Recibe el paquete
                    val byteArray = ByteArray(byteArraySize)
                    dataInputStream.readFully(byteArray)
                    Log.d("Client", "Data received, converting to Bitmap")

                    // Convierte el arreglo de bytes en un bitmap
                    val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                    if (bitmap == null) {
                        Log.e("Client", "Failed to decode bitmap from received data")
                        continue // Ignora este paquete si la decodificación falla
                    }

                    // Ajusta el tamaño del bitmap al tamaño de la superficie del cliente
                    surface?.let { surface ->
                        synchronized(surface) {
                            // Bloquea la superficie del cliente
                            val outputSurface = surface.lockCanvas(null)
                            try {
                                if (outputSurface != null) {
                                    val destRect = Rect(0, 0, outputSurface.width, outputSurface.height)
                                    val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                                    // Crea un objeto Paint para dibujar el bitmap en la superficie del cliente
                                    val paint = Paint()

                                    // Dibuja el bitmap en la superficie del cliente
                                    outputSurface.drawBitmap(bitmap, srcRect, destRect, paint)
                                } else {
                                    Log.e("Client", "Output surface is null")
                                }
                            } finally {
                                // Desbloquea la superficie del cliente y la muestra en la pantalla
                                surface.unlockCanvasAndPost(outputSurface)
                            }
                        }
                    }
                    Log.d("Client", "Bitmap displayed on surface")
                } catch (e: EOFException) {
                    Log.e("Client", "Connection closed: ${e.message}")
                    break // Salir del bucle si se cierra la conexión
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("Client", "Error in client handler: ${e.message}")
                    break // Salir del bucle en caso de error
                }
            }
        } finally {
            try {
                clientSocket.close()
                Log.d("Client", "Socket closed")
            } catch (e: IOException) {
                Log.e("Client", "Error closing socket: ${e.message}")
            }
        }
    }

    /*
    *   Se encarga de detener la comunicación con el cliente
    */
    fun stopClient() {
        isActive = false
    }
}