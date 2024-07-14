package com.example.app_sistemas_operativos.service

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket

object Server {
    // Servidor de sockets
    private var serverSocket: ServerSocket? = null

    /*
    *   createServer()
    *
    *   Crea un servidor de sockets en el puerto especificado.
    */
    fun createServer(context: Context, serverPort: Int) {
        // Obtener la dirección IP del dispositivo
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAddress = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
        Log.d("CapturadoraPantallaSe", "IP del servidor: $ipAddress")

        // Inicia el servidor de sockets en un hilo separado
        Thread {
            try {
                // Crea el servidor de sockets
                serverSocket = ServerSocket(serverPort)

                // Obtiene la dirección del servidor
                val serverAddress: InetAddress = serverSocket!!.inetAddress
                val serverHostAddress: String = serverAddress.hostAddress
                Log.d("CapturadoraPantallaSe", "Servidor iniciado en la dirección $serverHostAddress y en el puerto ${serverSocket!!.localPort}")

                while (true) {
                    // Acepta una conexión
                    val clientSocket = serverSocket!!.accept()

                    // Agrega el cliente a la lista de clientes
                    CapturadoraPantallaService.clients.add(clientSocket)
                    Log.d("CapturadoraPantallaSe", "Cliente añadido a la lista")

                    // Cuando se agrega un cliente, comienza la ejecución del hilo para manejar la comunicación con el cliente
                    Thread {
                        Client.clientHandler(clientSocket)
                    }.start()
                }
            } catch (e: IOException) {
                Log.e("CapturadoraPantallaSe", "Error en el servidor de sockets", e)
            } finally {
                serverSocket?.close()
            }
        }.start()
    }

    fun stopServer() {
        serverSocket?.close()
    }
}