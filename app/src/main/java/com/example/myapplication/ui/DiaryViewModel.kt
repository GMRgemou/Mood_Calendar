package com.example.myapplication.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.DiaryDatabase
import com.example.myapplication.data.DiaryEntry
import com.example.myapplication.data.Tweet
import com.example.myapplication.network.DiscoveredDevice
import com.example.myapplication.network.LanDiscoveryManager
import com.example.myapplication.network.LanHttpServer
import com.example.myapplication.network.TweetApiClient
import com.example.myapplication.util.DeviceIdManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DiaryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DiaryDatabase.getDatabase(application)
    private val diaryDao = database.diaryDao()
    private val tweetDao = database.tweetDao()
    private val tweetApiClient = TweetApiClient()
    private val deviceId = DeviceIdManager.getDeviceId(application)

    val entries: StateFlow<List<DiaryEntry>> = diaryDao.getAllEntries().stateInViewModel()

    fun getEntriesByDateRange(start: Long, end: Long): StateFlow<List<DiaryEntry>> {
        return diaryDao.getEntriesByDateRange(start, end).stateInViewModel()
    }

    suspend fun getEntryById(id: Long): DiaryEntry? {
        return diaryDao.getEntryById(id)
    }

    fun saveEntry(
        id: Long,
        title: String,
        content: String,
        date: Long,
        imageUris: String,
        attachmentUris: String = "",
        audioUris: String = "",
        audioTranscriptions: String = "",
        audioNames: String = "",
        audioTranscriptionsVisibility: String = "",
        mood: String = "",
        location: String = ""
    ) {
        viewModelScope.launch {
            val entry = DiaryEntry(
                id = if (id == -1L) 0 else id,
                title = title,
                content = content,
                date = date,
                imageUris = imageUris,
                attachmentUris = attachmentUris,
                audioUris = audioUris,
                audioTranscriptions = audioTranscriptions,
                audioNames = audioNames,
                audioTranscriptionsVisibility = audioTranscriptionsVisibility,
                mood = mood,
                location = location
            )
            if (id == -1L) {
                diaryDao.insertEntry(entry)
            } else {
                diaryDao.updateEntry(entry)
            }
        }
    }

    fun deleteEntry(entry: DiaryEntry) {
        viewModelScope.launch {
            diaryDao.deleteEntry(entry)
        }
    }

    fun togglePin(entry: DiaryEntry) {
        viewModelScope.launch {
            diaryDao.updateEntry(entry.copy(isPinned = !entry.isPinned))
        }
    }

    // ---------- Tweet & LAN ----------

    val myTweets: StateFlow<List<Tweet>> = tweetDao.getMyTweets().stateInViewModel()

    private val _peerLoading = MutableStateFlow(false)
    val peerLoading: StateFlow<Boolean> = _peerLoading

    private val _peerError = MutableStateFlow<String?>(null)
    val peerError: StateFlow<String?> = _peerError

    private val httpServer: LanHttpServer
    private val discoveryManager: LanDiscoveryManager
    private val _discoveredDevices = MutableStateFlow<Map<String, DiscoveredDevice>>(emptyMap())
    val discoveredDevices: StateFlow<Map<String, DiscoveredDevice>> = _discoveredDevices

    init {
        val serverPort = findAvailablePort(8765)
        httpServer = LanHttpServer(
            port = serverPort,
            deviceIdProvider = { deviceId },
            tweetProvider = { myTweets.value }
        )
        try {
            httpServer.start()
        } catch (_: Exception) {
            // port taken after check, ignore
        }

        discoveryManager = LanDiscoveryManager(
            deviceId = deviceId,
            serverPort = serverPort,
            scope = viewModelScope
        )
        discoveryManager.startDiscovery()
        viewModelScope.launch {
            discoveryManager.discoveredDevices.collect {
                _discoveredDevices.value = it
            }
        }
    }

    fun publishTweet(content: String) {
        viewModelScope.launch {
            val tweet = Tweet(
                content = content,
                authorId = deviceId,
                timestamp = System.currentTimeMillis(),
                isLocal = true
            )
            tweetDao.insertTweet(tweet)
        }
    }

    fun deleteTweet(tweet: Tweet) {
        viewModelScope.launch {
            tweetDao.deleteTweet(tweet)
        }
    }

    fun fetchPeerTweets(ip: String, port: Int, authorId: String) {
        viewModelScope.launch {
            _peerLoading.value = true
            _peerError.value = null
            val tweets = tweetApiClient.fetchTweets(ip, port)
            if (tweets != null) {
                tweetDao.clearPeerTweets(authorId)
                tweets.map {
                    it.copy(id = 0, authorId = authorId, isLocal = false)
                }.forEach { tweetDao.insertTweet(it) }
            } else {
                _peerError.value = "获取推文失败，请检查对方是否在线"
            }
            _peerLoading.value = false
        }
    }

    private val peerTweetFlows = mutableMapOf<String, StateFlow<List<Tweet>>>()

    fun getPeerTweetsFlow(authorId: String): StateFlow<List<Tweet>> {
        return peerTweetFlows.getOrPut(authorId) {
            tweetDao.getPeerTweets(authorId).stateInViewModel()
        }
    }

    suspend fun scanLanForDevice(targetDeviceId: String): DiscoveredDevice? {
        return discoveryManager.scanLanForDevice(targetDeviceId)
    }

    override fun onCleared() {
        super.onCleared()
        discoveryManager.stopDiscovery()
        httpServer.stop()
    }

    private fun findAvailablePort(start: Int): Int {
        return (start until start + 100).firstOrNull { port ->
            runCatching { java.net.ServerSocket(port).close() }.isSuccess
        } ?: 0
    }

    private fun <T> Flow<List<T>>.stateInViewModel(): StateFlow<List<T>> {
        return stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }
}