package me.rerere.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

interface SearchService<T : SearchServiceOptions> {
    val name: String

    suspend fun search(
        query: String,
        commonOptions: SearchCommonOptions,
        serviceOptions: T
    ): Result<SearchResult>

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T : SearchServiceOptions> getService(options: T): SearchService<T> {
            return when (options) {
                is SearchServiceOptions.TavilyOptions -> TavilySearchService
                is SearchServiceOptions.ExaOptions -> ExaSearchService
            } as SearchService<T>
        }

        internal val httpClient by lazy {
            OkHttpClient.Builder()
                .build()
        }

        internal val json by lazy {
            Json {
                ignoreUnknownKeys = true
            }
        }
    }
}

@Serializable
data class SearchCommonOptions(
    val resultSize: Int = 7
)

@Serializable
data class SearchResult(
    val items: List<SearchResultItem>,
) {
    @Serializable
    data class SearchResultItem(
        val title: String,
        val url: String,
        val text: String,
    )
}

@Serializable
sealed class SearchServiceOptions {
    companion object {
        val DEFAULT = TavilyOptions("")

        val TYPES = mapOf(
            TavilyOptions::class to "Tavily",
            ExaOptions::class to "Exa"
        )
    }

    @Serializable
    @SerialName("tavily")
    data class TavilyOptions(
        val apiKey: String = ""
    ) : SearchServiceOptions()

    @Serializable
    @SerialName("exa")
    data class ExaOptions(
        val apiKey: String = ""
    ) : SearchServiceOptions()
}