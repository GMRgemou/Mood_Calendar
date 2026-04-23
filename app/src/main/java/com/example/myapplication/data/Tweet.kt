package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tweets")
data class Tweet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val authorId: String,
    val timestamp: Long,
    val isLocal: Boolean = false
)
