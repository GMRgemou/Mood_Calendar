package com.example.myapplication.network

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.withPermit
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * 局域网 UDP 广播发现管理器。
 * 定期广播本机信息，同时监听其他设备的广播。
 */
class LanDiscoveryManager(
    private val deviceId: String,
    private val serverPort: Int,
    private val scope: CoroutineScope
) {
    companion object {
        const val DISCOVERY_PORT = 8766
        const val BROADCAST_INTERVAL_MS = 3000L
        const val TAG = "LanDiscovery"
    }

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(BroadcastPacket::class.java)

    private val _discoveredDevices = MutableStateFlow<Map<String, DiscoveredDevice>>(emptyMap())
    val discoveredDevices: StateFlow<Map<String, DiscoveredDevice>> = _discoveredDevices

    private var udpSocket: DatagramSocket? = null
    private var broadcastJob: Job? = null
    private var listenJob: Job? = null

    fun startDiscovery() {
        stopDiscovery()
        try {
            udpSocket = DatagramSocket(DISCOVERY_PORT).apply {
                broadcast = true
                reuseAddress = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create UDP socket", e)
            return
        }

        listenJob = scope.launch(Dispatchers.IO) {
            runCatching { listenLoop() }
        }

        broadcastJob = scope.launch(Dispatchers.IO) {
            runCatching { broadcastLoop() }
        }
    }

    fun stopDiscovery() {
        broadcastJob?.cancel()
        listenJob?.cancel()
        udpSocket?.close()
        udpSocket = null
        broadcastJob = null
        listenJob = null
    }

    private suspend fun broadcastLoop() {
        val socket = udpSocket ?: return
        val packetJson = adapter.toJson(BroadcastPacket(deviceId, serverPort))
        val data = packetJson.toByteArray(Charsets.UTF_8)

        while (currentCoroutineContext().isActive) {
            try {
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(data, data.size, broadcastAddr, DISCOVERY_PORT)
                socket.send(packet)
            } catch (e: Exception) {
                Log.e(TAG, "Broadcast send failed", e)
            }
            delay(BROADCAST_INTERVAL_MS)
        }
    }

    private suspend fun listenLoop() {
        val socket = udpSocket ?: return
        val buffer = ByteArray(1024)

        while (currentCoroutineContext().isActive) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                val json = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val broadcast = adapter.fromJson(json) ?: continue

                // 忽略自己
                if (broadcast.deviceId == deviceId) continue

                val current = _discoveredDevices.value.toMutableMap()
                current[broadcast.deviceId] = DiscoveredDevice(
                    address = packet.address,
                    port = broadcast.port
                )
                _discoveredDevices.value = current
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) Log.e(TAG, "Listen receive failed", e)
            }
        }
    }

    /**
     * 主动扫描当前局域网网段，探测指定端口上的设备。
     */
    suspend fun scanLanForDevice(targetDeviceId: String): DiscoveredDevice? = withContext(Dispatchers.IO) {
        val localIp = getLocalIpAddress() ?: return@withContext null
        val prefix = localIp.substringBeforeLast(".")
        val semaphore = kotlinx.coroutines.sync.Semaphore(20)
        val jobs = (1..254).map { lastOctet ->
            async {
                semaphore.withPermit {
                    val testIp = "$prefix.$lastOctet"
                    val info = runCatching {
                        TweetApiClient().fetchDeviceInfo(testIp, serverPort)
                    }.getOrNull()
                    if (info?.deviceId == targetDeviceId) {
                        DiscoveredDevice(address = InetAddress.getByName(testIp), port = serverPort)
                    } else null
                }
            }
        }
        jobs.awaitAll().filterNotNull().firstOrNull()
    }

    private fun getLocalIpAddress(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains(":") == false }
                ?.hostAddress
        } catch (e: Exception) {
            null
        }
    }
}

data class BroadcastPacket(val deviceId: String, val port: Int)

data class DiscoveredDevice(val address: InetAddress, val port: Int)
