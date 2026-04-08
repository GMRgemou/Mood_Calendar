package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,
    val title: String,
    val content: String,
    val isPinned: Boolean = false,
    val imageUris: String = "", // 逗号分隔的图片 URI 字符串
    val attachmentUris: String = "", // 逗号分隔的附件 URI 字符串
    val audioUris: String = "", // 逗号分隔的录音 URI 字符串
    val audioTranscriptions: String = "", // 逗号分隔的录音转文字字符串
    val audioNames: String = "", // 逗号分隔的录音自定义名称字符串
    val audioTranscriptionsVisibility: String = "", // 逗号分隔的录音转文字可见性 (0: 隐藏, 1: 显示)
    val mood: String = "", // E.g., "😊", "😢", etc.
    val location: String = "" // E.g., "New York", "San Francisco"
)
