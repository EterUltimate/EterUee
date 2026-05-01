package com.eterultimate.eteruee.ui.pages.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.eterultimate.eteruee.data.db.dao.ConversationDAO
import com.eterultimate.eteruee.data.db.dao.MessageNodeDAO
import com.eterultimate.eteruee.data.db.dao.getMessageCountPerDay
import com.eterultimate.eteruee.data.db.dao.getTokenStats
import com.eterultimate.eteruee.data.datastore.SettingsStore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class AppStats(
    val isLoading: Boolean = true,
    val totalConversations: Int = 0,
    val totalMessages: Int = 0,
    val totalPromptTokens: Long = 0L,
    val totalCompletionTokens: Long = 0L,
    val totalCachedTokens: Long = 0L,
    val conversationsPerDay: Map<LocalDate, Int> = emptyMap(),
    val launchCount: Int = 0,
)

class StatsVM(
    private val conversationDAO: ConversationDAO,
    private val messageNodeDAO: MessageNodeDAO,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _stats = MutableStateFlow(AppStats())
    val stats = _stats.asStateFlow()

    init {
        viewModelScope.launch { loadStats() }
    }

    private suspend fun loadStats() {
        delay(50)

        val today = LocalDate.now()

        // 鐑姏鍥捐捣濮嬫棩鏈燂紙52 鍛ㄥ墠鐨勫懆鏃ワ級锛屾牸寮?"yyyy-MM-dd" 鐩存帴涓?JSON 涓殑 LocalDateTime 鍓嶇紑姣旇緝
        val startDate = today
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            .minusWeeks(52)
            .toString()

        // 鍩轰簬鐢ㄦ埛娑堟伅鐨?createdAt 缁熻姣忔棩娲昏穬娑堟伅鏁帮紝SQLite 渚?GROUP BY锛岃繑鍥?鈮?71 琛?
        val conversationsPerDay = withContext(Dispatchers.IO) {
            messageNodeDAO
                .getMessageCountPerDay(startDate)
                .mapNotNull { entry ->
                    runCatching { LocalDate.parse(entry.day) to entry.count }.getOrNull()
                }
                .toMap()
        }

        val totalConversations = conversationDAO.countAll()

        // json_each() + json_extract() 鍦?SQLite 渚ц仛鍚堬紝涓嶅啀鍔犺浇瀹屾暣 JSON 鍒?Kotlin
        val tokenStats = messageNodeDAO.getTokenStats()

        val launchCount = settingsStore.settingsFlow.value.launchCount

        _stats.value = AppStats(
            isLoading = false,
            totalConversations = totalConversations,
            totalMessages = tokenStats.totalMessages,
            totalPromptTokens = tokenStats.promptTokens,
            totalCompletionTokens = tokenStats.completionTokens,
            totalCachedTokens = tokenStats.cachedTokens,
            conversationsPerDay = conversationsPerDay,
            launchCount = launchCount,
        )
    }
}

