package com.example.myapplication.network

import com.example.myapplication.data.Tweet
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * 用于向局域网内其他设备请求推文数据的 HTTP 客户端。
 */
class TweetApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val deviceInfoAdapter = moshi.adapter(DeviceInfo::class.java)
    private val tweetListAdapter = moshi.adapter<List<Tweet>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, Tweet::class.java)
    )

    suspend fun fetchDeviceInfo(ip: String, port: Int): DeviceInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("http://$ip:$port/api/info")
            .get()
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let { deviceInfoAdapter.fromJson(it) }
                } else null
            }
        }.getOrNull()
    }

    suspend fun fetchTweets(ip: String, port: Int): List<Tweet>? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("http://$ip:$port/api/tweets")
            .get()
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let { tweetListAdapter.fromJson(it) }
                } else null
            }
        }.getOrNull()
    }
}
