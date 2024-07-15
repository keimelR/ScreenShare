package com.example.app_sistemas_operativos.service

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketException

object Server {
    private var serverSocket: ServerSocket? = null
    private var serverThread: Thread? = null

    /*
    *   createServer()
    *
    *   Crear el servidor en el puerto especificado
    */
    fun createServer(context: Context, serverPort: Int) {
        // Obtener la dirección IP del dispositivo
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAddress = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
        Log.d("CapturadoraPantallaSe", "IP del servidor: $ipAddress")

        // Crear el servidor
        serverThread = Thread {
            try {
                // Iniciar el servidor
                serverSocket = ServerSocket(serverPort)

                // Obtener la dirección del servidor
                val serverAddress: InetAddress = serverSocket!!.inetAddress
                val serverHostAddress: String = serverAddress.hostAddress
                Log.d("CapturadoraPantallaSe", "Servidor iniciado en la dirección $serverHostAddress y en el puerto ${serverSocket!!.localPort}")

                while (!Thread.currentThread().isInterrupted) {
                    try {
                        // Aceptar conexiones
                        val clientSocket = serverSocket!!.accept()

                        // Añadir el cliente a la lista
                        CapturadoraPantallaService.clients.add(clientSocket)
                        Log.d("CapturadoraPantallaSe", "Cliente añadido a la lista")

                        // Procesar la conexión
                        Thread {
                            Client.clientHandler(clientSocket)
                        }.start()
                    } catch (e: SocketException) {
                        // El servidor se ha cerrado
                        if (!serverSocket!!.isClosed) {
                            Log.e("CapturadoraPantallaSe", "Error en el servidor de sockets", e)
                        }
                        break
                    }
                }
            } catch (e: IOException) {
                Log.e("CapturadoraPantallaSe", "Error en el servidor de sockets", e)
            } finally {
                // Cerrar el servidor
                serverSocket?.close()
            }
        }
        // Iniciar el hilo del servidor
        serverThread?.start()
    }

    /*
    *   stopServer()
    *
    *   Detiene el servidor
    */
    fun stopServer() {
        serverThread?.interrupt()
        serverSocket?.close()
    }
}