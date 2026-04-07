package com.whisper.java

import android.content.res.AssetManager
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class WhisperLib(private val interpreter: Interpreter) {
    
    companion object {
        private const val TAG = "WhisperLib"

        fun init(assetManager: AssetManager, modelPath: String): WhisperLib? {
            return try {
                val options = Interpreter.Options()
                // 尝试使用 GPU 加速
                try {
                    options.addDelegate(GpuDelegate())
                } catch (e: Exception) {
                    Log.w(TAG, "GPU Delegate not available, using CPU")
                }
                options.setNumThreads(4)

                val modelBuffer = loadModelFile(assetManager, modelPath)
                val interpreter = Interpreter(modelBuffer, options)
                interpreter.allocateTensors()
                
                Log.d(TAG, "WhisperLib initialized successfully with $modelPath")
                WhisperLib(interpreter)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize WhisperLib", e)
                null
            }
        }

        private fun loadModelFile(assetManager: AssetManager, path: String): ByteBuffer {
            val fileDescriptor = assetManager.openFd(path)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
        }

        fun transcribe(whisperLib: WhisperLib, audioData: FloatArray): WhisperResult {
            return try {
                // 严格对齐模型要求的输入总数: 240160
                val targetSize = 240160
                val inputAudio = FloatArray(targetSize)
                val copySize = if (audioData.size > targetSize) targetSize else audioData.size
                System.arraycopy(audioData, 0, inputAudio, 0, copySize)

                // 构造 3D 输入: [1, 1, 240160]
                val input = Array(1) { Array(1) { inputAudio } }
                
                // 构造 2D 输出: [1, 200]
                val output = mutableMapOf<Int, Any>()
                val outputBuffer = Array(1) { IntArray(200) }
                output[0] = outputBuffer

                // 运行推理
                whisperLib.interpreter.runForMultipleInputsOutputs(arrayOf(input), output)

                val tokens = output[0] as Array<IntArray>
                // 此处建议在后续添加词表解析逻辑
                WhisperResult("（识别逻辑已准备就绪，待词表加载）")
            } catch (e: Exception) {
                Log.e(TAG, "Transcription failed", e)
                WhisperResult("识别失败: ${e.message}")
            }
        }
    }

    fun release() {
        interpreter.close()
    }
}

data class WhisperResult(val text: String)
