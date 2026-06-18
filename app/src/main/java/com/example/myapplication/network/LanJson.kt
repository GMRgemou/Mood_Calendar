package com.example.myapplication.network

import com.example.myapplication.data.Tweet
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

internal object LanJson {
    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val broadcastPacketAdapter = moshi.adapter(BroadcastPacket::class.java)
    val deviceInfoAdapter = moshi.adapter(DeviceInfo::class.java)

    private val tweetListType = Types.newParameterizedType(List::class.java, Tweet::class.java)
    val tweetListAdapter = moshi.adapter<List<Tweet>>(tweetListType)
}
