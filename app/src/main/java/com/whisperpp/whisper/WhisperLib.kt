package com.whisperpp.whisper

import android.util.Log

class WhisperLib {
    companion object {
        var isLoaded = false
            private set
        var loadError: String? = null
            private set

        init {
            try {
                // Try to load dependencies if they exist
                val libs = listOf("c++_shared", "omp", "ggml-base", "ggml-cpu", "ggml", "whisper")
                for (lib in libs) {
                    try {
                        System.loadLibrary(lib)
                        Log.d("WhisperLib", "Native library '$lib' loaded successfully")
                    } catch (e: Throwable) {
                        if (lib == "whisper") {
                            throw e // Whisper is mandatory
                        }
                        Log.w("WhisperLib", "Optional native library '$lib' not found or failed to load: ${e.message}")
                    }
                }
                isLoaded = true
            } catch (e: Throwable) {
                val arch = System.getProperty("os.arch")
                val abi = android.os.Build.SUPPORTED_ABIS.joinToString(", ")
                loadError = "Arch: $arch, ABIs: $abi, Error: ${e.message}"
                Log.e("WhisperLib", "Could not load mandatory native library 'whisper'. $loadError", e)
                isLoaded = false
            }
        }
    }

    external fun initContext(modelPath: String): Long
    external fun freeContext(contextPtr: Long)
    external fun fullTranscribe(contextPtr: Long, audioData: FloatArray)
    external fun getTextSegmentCount(contextPtr: Long): Int
    external fun getTextSegment(contextPtr: Long, index: Int): String
}

class WhisperContext(private val modelPath: String) {
    private var contextPtr: Long = 0
    private val whisperLib = WhisperLib()
    private var lastInitError: String? = null

    fun load() {
        if (!WhisperLib.isLoaded) {
            lastInitError = "Native library not loaded"
            return
        }
        if (contextPtr == 0L) {
            try {
                val file = java.io.File(modelPath)
                if (!file.exists()) {
                    lastInitError = "Model file not found at $modelPath"
                    return
                }
                if (file.length() < 1024) {
                    lastInitError = "Model file too small (${file.length()} bytes). Likely corrupt."
                    return
                }
                
                contextPtr = whisperLib.initContext(modelPath)
                if (contextPtr == 0L) {
                    lastInitError = "initContext returned null pointer (invalid model format or insufficient memory)"
                }
            } catch (e: Throwable) {
                lastInitError = e.message ?: e.toString()
                Log.e("WhisperContext", "Failed to init whisper context", e)
            }
        }
    }

    fun transcribe(audioData: FloatArray): String {
        if (!WhisperLib.isLoaded) return "Error: STT Native library not loaded (${WhisperLib.loadError ?: "Unknown error"})"
        if (contextPtr == 0L) return "Error: Whisper context not initialized (Reason: ${lastInitError ?: "Unknown error"})"
        return try {
            whisperLib.fullTranscribe(contextPtr, audioData)
            val count = whisperLib.getTextSegmentCount(contextPtr)
            if (count <= 0) return "Error: No transcription segments found"
            val sb = StringBuilder()
            for (i in 0 until count) {
                sb.append(whisperLib.getTextSegment(contextPtr, i))
            }
            sb.toString()
        } catch (e: Throwable) {
            Log.e("WhisperContext", "Transcription failed", e)
            "Error: ${e.message ?: "Unknown transcription error"}"
        }
    }

    fun release() {
        if (!WhisperLib.isLoaded || contextPtr == 0L) return
        if (contextPtr != 0L) {
            try {
                whisperLib.freeContext(contextPtr)
            } catch (e: Throwable) {
                Log.e("WhisperContext", "Failed to release whisper context", e)
            }
            contextPtr = 0L
        }
    }
}
