package com.eterultimate.eteruee.tts.controller

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.eterultimate.eteruee.tts.model.PlaybackState
import com.eterultimate.eteruee.tts.model.PlaybackStatus
import com.eterultimate.eteruee.tts.model.TTSResponse
import com.eterultimate.eteruee.tts.provider.TTSManager
import com.eterultimate.eteruee.tts.provider.TTSProviderSetting
import java.util.UUID

private const val TAG = "TtsController"

/**
 * TTS 鎺у埗鍣紙閲嶆瀯鐗堬級
 * - 璐熻矗鏂囨湰鍒嗙墖銆侀鍙栧悎鎴愩€佹帓闃熸挱鏀句笌鐘舵€佷笂鎶?
 * - 瀵瑰 API 涓庡師鐗堝吋瀹?
 */
class TtsController(
    context: Context,
    private val ttsManager: TTSManager
) {
    // 鍗忕▼浣滅敤鍩?
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // 缁勪欢
    private val chunker = TextChunker(maxChunkLength = 160)
    private val synthesizer = TtsSynthesizer(ttsManager)
    private val audio = AudioPlayer(context)

    // Provider & 浣滀笟
    private var currentProvider: TTSProviderSetting? = null
    private var workerJob: Job? = null
    private var isPaused = false

    // 闃熷垪涓庣紦瀛橈紙鍩轰簬绋冲畾 ID锛?
    private val queue: java.util.concurrent.ConcurrentLinkedQueue<TtsChunk> = java.util.concurrent.ConcurrentLinkedQueue()
    private val allChunks: MutableList<TtsChunk> = mutableListOf()
    private val cache = java.util.concurrent.ConcurrentHashMap<UUID, kotlinx.coroutines.Deferred<TTSResponse>>()
    private var lastPrefetchedIndex: Int = -1

    // 琛屼负鍙傛暟
    private val chunkDelayMs = 120L
    private val prefetchCount = 4

    // 鐘舵€佹祦锛堜繚鐣欎笌鏃х増鍏煎鐨?StateFlow锛?
    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentChunk = MutableStateFlow(0)
    val currentChunk: StateFlow<Int> = _currentChunk.asStateFlow()

    private val _totalChunks = MutableStateFlow(0)
    val totalChunks: StateFlow<Int> = _totalChunks.asStateFlow()

    // 缁熶竴鎾斁鐘舵€侊紙铻嶅悎闊抽鎾斁 + 鍒嗙墖杩涘害锛?
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    init {
        // 鍚屾搴曞眰鎾斁鍣ㄧ姸鎬佸埌缁熶竴鐘舵€侊紝骞惰ˉ鍏呭垎鐗囦俊鎭?
        scope.launch {
            audio.playbackState.collectLatest { audioState ->
                _playbackState.update {
                    audioState.copy(
                        currentChunkIndex = _currentChunk.value,
                        totalChunks = _totalChunks.value,
                        status = if (!_isAvailable.value) PlaybackStatus.Idle else audioState.status
                    )
                }
            }
        }
    }

    /** 閫夋嫨/鍙栨秷閫夋嫨 Provider */
    fun setProvider(provider: TTSProviderSetting?) {
        currentProvider = provider
        _isAvailable.update { provider != null }
        if (provider == null) stop()
    }

    /**
     * 鏈楄鏂囨湰
     * - flush=true: 娓呯┖褰撳墠杩涘害骞堕噸鏂板紑濮?
     * - flush=false: 缁х画闃熷垪锛岃拷鍔犳湕璇?
     */
    fun speak(text: String, flush: Boolean = true) {
        if (text.isBlank()) return
        val provider = currentProvider
        if (provider == null) {
            _error.update { "No TTS provider selected" }
            return
        }

        val newChunks = chunker.split(text)
        if (newChunks.isEmpty()) return

        if (flush) {
            internalReset()
            allChunks.addAll(newChunks)
            queue.addAll(newChunks)
            _currentChunk.update { 0 }
        } else {
            // 杩藉姞鏃讹紝閲嶆槧灏?index 浠ヤ繚鎸佸叏灞€椤哄簭
            val startIndex = (allChunks.lastOrNull()?.index ?: -1) + 1
            val remapped = newChunks.mapIndexed { i, c -> c.copy(index = startIndex + i) }
            allChunks.addAll(remapped)
            queue.addAll(remapped)
        }
        _totalChunks.update { queue.size }
        _error.update { null }

        _playbackState.update {
            it.copy(
                currentChunkIndex = _currentChunk.value,
                totalChunks = _totalChunks.value,
                status = PlaybackStatus.Buffering
            )
        }

        if (workerJob?.isActive != true) startWorker()
        prefetchFrom((_currentChunk.value).coerceAtLeast(0))
    }

    private fun internalReset() {
        // Reset current session while keeping provider availability
        workerJob?.cancel()
        audio.stop()
        audio.clear()
        isPaused = false
        queue.clear()
        allChunks.clear()
        cache.values.forEach { it.cancel(CancellationException("Reset")) }
        cache.clear()
        lastPrefetchedIndex = -1
        _isSpeaking.update { false }
        _currentChunk.update { 0 }
        _totalChunks.update { 0 }
        _error.update { null }
        _playbackState.update { PlaybackState(status = PlaybackStatus.Idle) }
    }

    /** 鏆傚仠鎾斁锛堜繚鐣欒繘搴︼級 */
    fun pause() {
        isPaused = true
        audio.pause()
        _playbackState.update { it.copy(status = PlaybackStatus.Paused) }
    }

    /** 鎭㈠鎾斁 */
    fun resume() {
        isPaused = false
        audio.resume()
        _playbackState.update { it.copy(status = PlaybackStatus.Playing) }
    }

    /** 蹇繘褰撳墠闊抽 */
    fun fastForward(ms: Long = 5_000) {
        audio.seekBy(ms)
    }

    /** 璁剧疆鎾斁閫熷害 */
    fun setSpeed(speed: Float) {
        audio.setSpeed(speed)
    }

    /** 璺宠繃涓嬩竴娈碉紙涓嶆墦鏂綋鍓嶆鍦ㄦ挱鏀撅級 */
    fun skipNext() {
        if (queue.isNotEmpty()) {
            queue.poll()
            _totalChunks.update { queue.size }
        }
    }

    /** 鍋滄骞舵竻绌虹姸鎬?*/
    fun stop() {
        workerJob?.cancel()
        audio.stop()
        audio.clear()
        isPaused = false
        queue.clear()
        allChunks.clear()
        cache.values.forEach { it.cancel(CancellationException("Stopped")) }
        cache.clear()
        lastPrefetchedIndex = -1
        _isSpeaking.update { false }
        _currentChunk.update { 0 }
        _totalChunks.update { 0 }
        _playbackState.update { PlaybackState(status = PlaybackStatus.Idle) }
    }

    /** 閲婃斁璧勬簮 */
    fun dispose() {
        stop()
        scope.cancel()
        audio.release()
    }

    // region 鍐呴儴锛氭挱鏀捐皟搴?
    private fun startWorker() {
        val provider = currentProvider
        if (provider == null) {
            _error.update { "No TTS provider selected" }
            return
        }

        workerJob = scope.launch {
            _isSpeaking.update { true }
            var processedCount = _currentChunk.value
            try {
                while (isActive) {
                    if (isPaused) {
                        delay(80)
                        continue
                    }

                    val chunk = queue.poll() ?: break

                    // 鏇存柊鐘舵€侊紙1-based锛?
                    _currentChunk.update { processedCount + 1 }
                    _totalChunks.update { queue.size + 1 }
                    _playbackState.update {
                        it.copy(
                            currentChunkIndex = _currentChunk.value,
                            totalChunks = _totalChunks.value
                        )
                    }

                    // 棰勫彇涓嬩竴绐楀彛
                    prefetchFrom(chunk.index + 1)

                    val response = try {
                        awaitOrCreate(chunk, provider)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Log.e(TAG, "Synthesis error", e)
                        _error.update { e.message ?: "TTS synthesis error" }
                        processedCount++
                        continue
                    }

                    // 鎾斁
                    try {
                        audio.play(response)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Log.e(TAG, "Playback error", e)
                        _error.update { e.message ?: "Audio playback error" }
                    }

                    if (queue.isNotEmpty()) delay(chunkDelayMs)

                    processedCount++
                }
            } finally {
                _isSpeaking.update { false }
                if (queue.isEmpty()) {
                    _playbackState.update { it.copy(status = PlaybackStatus.Ended) }
                }
            }
        }
    }

    private fun prefetchFrom(startIndex: Int) {
        val provider = currentProvider ?: return
        val begin = startIndex.coerceAtLeast(lastPrefetchedIndex + 1)
        val endExclusive = (begin + prefetchCount).coerceAtMost(allChunks.size)
        if (begin >= endExclusive) return

        for (i in begin until endExclusive) {
            val chunk = allChunks.getOrNull(i) ?: continue
            cache.computeIfAbsent(chunk.id) {
                scope.async(Dispatchers.IO) { synthesizer.synthesize(provider, chunk) }
            }
        }
        lastPrefetchedIndex = endExclusive - 1
    }

    private suspend fun awaitOrCreate(chunk: TtsChunk, provider: TTSProviderSetting): TTSResponse {
        val deferred = cache.computeIfAbsent(chunk.id) {
            scope.async(Dispatchers.IO) { synthesizer.synthesize(provider, chunk) }
        }
        return try {
            deferred.await()
        } finally {
            // 鍙寜闇€淇濈暀缂撳瓨锛堟澶勪繚鐣欙紝渚夸簬閲嶆挱/閲嶈瘯锛?
        }
    }
    // endregion
}

