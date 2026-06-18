package com.example.myapplication.network

import com.example.myapplication.data.Tweet
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 用于向局域网内其他设备请求推文数据的 HTTP 客户端。
 */
class TweetApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val deviceInfoAdapter = LanJson.deviceInfoAdapter
    private val tweetListAdapter = LanJson.tweetListAdapter

    suspend fun fetchDeviceInfo(ip: String, port: Int): DeviceInfo? {
        return getJson("http://$ip:$port/api/info", deviceInfoAdapter)
    }

    suspend fun fetchTweets(ip: String, port: Int): List<Tweet>? {
        return getJson("http://$ip:$port/api/tweets", tweetListAdapter)
    }

    private suspend fun <T> getJson(url: String, adapter: JsonAdapter<T>): T? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).get().build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let(adapter::fromJson)
                } else null
            }
        }.getOrNull()
    }
}
