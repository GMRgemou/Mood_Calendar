package com.example.myapplication.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.DiaryDatabase
import com.example.myapplication.data.DiaryEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DiaryViewModel(application: Application) : AndroidViewModel(application) {
    private val diaryDao = DiaryDatabase.getDatabase(application).diaryDao()

    val entries: StateFlow<List<DiaryEntry>> = diaryDao.getAllEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getEntriesByDateRange(start: Long, end: Long): StateFlow<List<DiaryEntry>> {
        return diaryDao.getEntriesByDateRange(start, end)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
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
}
