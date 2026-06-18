package com.example.myapplication.network

import com.example.myapplication.data.Tweet
import fi.iki.elonen.NanoHTTPD

/**
 * 局域网内嵌 HTTP 服务器，暴露本机推文与设备信息�?
 */
class LanHttpServer(
    port: Int,
    private val deviceIdProvider: () -> String,
    private val tweetProvider: () -> List<Tweet>
) : NanoHTTPD(port) {

    private val deviceInfoAdapter = LanJson.deviceInfoAdapter
    private val tweetListAdapter = LanJson.tweetListAdapter

    override fun serve(session: IHTTPSession): Response {
        return when (session.uri) {
            "/api/info" -> {
                val info = DeviceInfo(deviceId = deviceIdProvider())
                val json = deviceInfoAdapter.toJson(info)
                newFixedLengthResponse(Response.Status.OK, "application/json", json)
            }
            "/api/tweets" -> {
                val tweets = tweetProvider()
                val json = tweetListAdapter.toJson(tweets)
                newFixedLengthResponse(Response.Status.OK, "application/json", json)
            }
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
        }
    }
}

data class DeviceInfo(val deviceId: String)
