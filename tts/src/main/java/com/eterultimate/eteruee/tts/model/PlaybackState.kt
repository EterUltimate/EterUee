package com.eterultimate.eteruee.tts.model

/**
 * 缁熶竴鐨勬挱鏀剧姸鎬侊紙瀵瑰鏆撮湶缁?app 渚т娇鐢級銆?
 */
enum class PlaybackStatus {
    Idle,
    Buffering,
    Playing,
    Paused,
    Ended,
    Error
}

data class PlaybackState(
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1.0f,
    val currentChunkIndex: Int = 0, // 1-based锛屼笌 currentChunk StateFlow 瀵归綈
    val totalChunks: Int = 0,
    val errorMessage: String? = null
)



