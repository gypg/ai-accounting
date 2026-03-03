package com.example.aiaccounting.data.service

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 图像处理服务 - 为普通语言模型提供图片理解能力
 * 优化版本：并行处理、精简数据、快速响应
 */
@Singleton
class ImageProcessingService @Inject constructor() {

    /**
     * 图片处理结果 - 精简版
     */
    data class ImageAnalysisResult(
        val text: String,           // OCR提取的文字（精简后）
        val labels: List<String>,   // 图像标签/描述
        val hasContent: Boolean     // 是否有识别到内容
    )

    /**
     * 分析单张图片 - 带超时保护
     */
    suspend fun analyzeImage(uri: Uri, context: Context, timeoutMs: Long = 5000): ImageAnalysisResult {
        return try {
            withTimeout(timeoutMs) {
                // 并行执行OCR和标签识别
                val textDeferred = async { extractTextFromImage(uri, context) }
                val labelsDeferred = async { extractLabelsFromImage(uri, context) }
                
                val text = textDeferred.await()
                val labels = labelsDeferred.await()
                
                // 精简文本内容（只保留前500字符，避免过长）
                val trimmedText = if (text.length > 500) {
                    text.substring(0, 500) + "..."
                } else {
                    text
                }
                
                ImageAnalysisResult(
                    text = trimmedText,
                    labels = labels.take(5), // 最多保留5个标签
                    hasContent = text.isNotBlank() || labels.isNotEmpty()
                )
            }
        } catch (e: TimeoutCancellationException) {
            ImageAnalysisResult(
                text = "",
                labels = emptyList(),
                hasContent = false
            )
        } catch (e: Exception) {
            ImageAnalysisResult(
                text = "",
                labels = emptyList(),
                hasContent = false
            )
        }
    }

    /**
     * 并行分析多张图片
     */
    suspend fun analyzeMultipleImages(
        uris: List<Uri>, 
        context: Context,
        timeoutMs: Long = 8000
    ): List<ImageAnalysisResult> = withContext(Dispatchers.IO) {
        try {
            withTimeout(timeoutMs) {
                // 并行处理所有图片
                val deferredResults = uris.map { uri ->
                    async { analyzeImage(uri, context, 5000) }
                }
                deferredResults.awaitAll()
            }
        } catch (e: TimeoutCancellationException) {
            // 超时返回空结果
            uris.map { 
                ImageAnalysisResult("", emptyList(), false)
            }
        } catch (e: Exception) {
            uris.map { 
                ImageAnalysisResult("", emptyList(), false)
            }
        }
    }

    /**
     * OCR文字识别 - 优化版
     */
    private suspend fun extractTextFromImage(uri: Uri, context: Context): String {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            
            val result = suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        // 快速提取文字，不过度处理
                        val text = visionText.text.trim()
                        recognizer.close()
                        continuation.resume(text)
                    }
                    .addOnFailureListener { e ->
                        recognizer.close()
                        continuation.resumeWithException(e)
                    }
            }
            result
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 图像标签识别 - 优化版
     */
    private suspend fun extractLabelsFromImage(uri: Uri, context: Context): List<String> {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val labeler = ImageLabeling.getClient(
                ImageLabelerOptions.Builder()
                    .setConfidenceThreshold(0.6f) // 提高阈值，减少标签数量
                    .build()
            )
            
            val result = suspendCancellableCoroutine<List<String>> { continuation ->
                labeler.process(image)
                    .addOnSuccessListener { labels ->
                        // 只取前3个最可能的标签
                        val labelList = labels
                            .sortedByDescending { it.confidence }
                            .take(3)
                            .map { it.text }
                        labeler.close()
                        continuation.resume(labelList)
                    }
                    .addOnFailureListener { e ->
                        labeler.close()
                        continuation.resumeWithException(e)
                    }
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 生成精简的AI提示词
     */
    fun generateCompactPrompt(
        results: List<ImageAnalysisResult>,
        userMessage: String
    ): String {
        return buildString {
            appendLine("你是小财娘，活泼可爱的管家婆AI助手！")
            appendLine()
            
            if (userMessage.isNotBlank()) {
                appendLine("用户说：$userMessage")
                appendLine()
            }
            
            appendLine("用户发了${results.size}张图片，内容如下：")
            appendLine()
            
            results.forEachIndexed { index, result ->
                appendLine("【图${index + 1}】")
                
                if (result.labels.isNotEmpty()) {
                    appendLine("类型：${result.labels.joinToString(", ")}")
                }
                
                if (result.text.isNotBlank()) {
                    // 进一步精简，只保留关键行
                    val keyLines = result.text.lines()
                        .filter { it.length > 2 } // 过滤太短的行
                        .take(10) // 最多10行
                        .joinToString(" | ")
                    
                    if (keyLines.isNotBlank()) {
                        appendLine("文字：$keyLines")
                    }
                }
                
                if (!result.hasContent) {
                    appendLine("（未识别到内容）")
                }
                
                appendLine()
            }
            
            appendLine("请分析以上内容，用活泼可爱的语气回复，如果是账单请提取金额、类型、类别、备注。")
        }
    }
}
