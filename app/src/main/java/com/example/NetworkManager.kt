package com.example

import java.net.ServerSocket
import java.net.Socket
import java.io.PrintWriter
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import java.net.NetworkInterface

enum class NetworkRole { NONE, HOST, CLIENT }

object NetworkManager {
    private val _betAmount = MutableStateFlow(0)
    val betAmount: StateFlow<Int> = _betAmount.asStateFlow()

    fun setBetAmount(amount: Int) {
        _betAmount.value = amount
    }

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var out: PrintWriter? = null
    private var inReader: BufferedReader? = null

    private val _connectionStatus = MutableStateFlow("Hujaunganishwa (Not Connected)")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _opponentName = MutableStateFlow<String?>(null)
    val opponentName: StateFlow<String?> = _opponentName.asStateFlow()

    private val _role = MutableStateFlow(NetworkRole.NONE)
    val role: StateFlow<NetworkRole> = _role.asStateFlow()

    private var moveCallback: ((Move) -> Unit)? = null
    private var resignCallback: (() -> Unit)? = null

    fun setMoveCallback(callback: (Move) -> Unit) {
        moveCallback = callback
    }

    fun setResignCallback(callback: () -> Unit) {
        resignCallback = callback
    }

    fun startHost(myName: String, port: Int = 8080) {
        disconnect()
        _role.value = NetworkRole.HOST
        _connectionStatus.value = "Nambari yako (IP): ${getLocalIpAddress()}\nMpe mwenzako namba hii aingize."
        CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(port)
                clientSocket = serverSocket?.accept() // Blocks until someone connects
                _isConnected.value = true
                _connectionStatus.value = "Imeunganishwa na mchezaji mwingine!"
                setupStreams(clientSocket!!)
                sendName(myName)
                listenForMessages()
            } catch (e: Exception) {
                if (_role.value == NetworkRole.HOST) {
                    _isConnected.value = false
                    _connectionStatus.value = "Imeshindwa kusubiri: ${e.message}"
                }
            }
        }
    }

    fun connectToHost(myName: String, ip: String, port: Int = 8080) {
        disconnect()
        if (ip.isBlank()) {
            _connectionStatus.value = "Tafadhali ingiza IP kwanza."
            return
        }
        _role.value = NetworkRole.CLIENT
        _connectionStatus.value = "Inaunganisha na $ip..."
        CoroutineScope(Dispatchers.IO).launch {
            try {
                clientSocket = Socket(ip, port)
                _isConnected.value = true
                _connectionStatus.value = "Imeunganishwa!"
                setupStreams(clientSocket!!)
                sendName(myName)
                listenForMessages()
            } catch (e: Exception) {
                _isConnected.value = false
                _connectionStatus.value = "Imeshindwa kuunganisha: ${e.message}"
            }
        }
    }

    private fun setupStreams(socket: Socket) {
        out = PrintWriter(socket.getOutputStream(), true)
        inReader = BufferedReader(InputStreamReader(socket.getInputStream()))
    }

    private fun sendName(name: String) {
        CoroutineScope(Dispatchers.IO).launch {
            out?.println("NAME:$name")
        }
    }

    private suspend fun listenForMessages() {
        withContext(Dispatchers.IO) {
            try {
                // If there's a reader and socket is alive
                while (clientSocket?.isConnected == true) {
                    val message = inReader?.readLine() ?: break // break if stream closed
                    if (message.startsWith("NAME:")) {
                        val name = message.removePrefix("NAME:")
                        _opponentName.value = name
                    } else if (message.startsWith("MOVE:")) {
                        val parts = message.removePrefix("MOVE:").split(",")
                        if(parts.size >= 4) {
                            val fromX = parts[0].toInt()
                            val fromY = parts[1].toInt()
                            val toX = parts[2].toInt()
                            val toY = parts[3].toInt()
                            val captured = mutableListOf<Pos>()
                            if (parts.size > 4 && parts[4].isNotEmpty()) {
                                val capParts = parts[4].split(";")
                                for (c in capParts) {
                                    val cc = c.split("-")
                                    if(cc.size == 2) {
                                        captured.add(Pos(cc[0].toInt(), cc[1].toInt()))
                                    }
                                }
                            }
                            withContext(Dispatchers.Main) {
                                moveCallback?.invoke(Move(Pos(fromX, fromY), Pos(toX, toY), captured))
                            }
                        }
                    } else if (message == "RESIGN") {
                        withContext(Dispatchers.Main) {
                            resignCallback?.invoke()
                        }
                    } else if (message == "RESTART") {
                        // Restart game requested
                    }
                }
            } catch (e: Exception) {
                // Connection lost
            }
        }
        if (_role.value != NetworkRole.NONE) {
            _isConnected.value = false
            _connectionStatus.value = "Muunganisho umekatika."
            disconnect()
        }
    }

    fun sendResign() {
        CoroutineScope(Dispatchers.IO).launch {
            out?.println("RESIGN")
        }
    }

    fun sendMove(move: Move) {
        CoroutineScope(Dispatchers.IO).launch {
            val capturedStr = move.captured.joinToString(";") { "${it.x}-${it.y}" }
            val msg = "MOVE:${move.from.x},${move.from.y},${move.to.x},${move.to.y},${capturedStr}"
            out?.println(msg)
        }
    }

    fun disconnect() {
        _role.value = NetworkRole.NONE
        _isConnected.value = false
        _opponentName.value = null
        _connectionStatus.value = "Hujaunganishwa"
        try {
            out?.close()
            inReader?.close()
            clientSocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {}
        clientSocket = null
        serverSocket = null
        out = null
        inReader = null
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.address.size == 4) {
                        return address.hostAddress ?: ""
                    }
                }
            }
        } catch (e: Exception) {
        }
        return "Haijulikani"
    }
}
