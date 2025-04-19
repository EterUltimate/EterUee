package me.rerere.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface SearchService<T : SearchServiceOptions> {
    val name: String

    suspend fun search(
        query: String,
        commonOptions: SearchCommonOptions,
        serviceOptions: T
    ): SearchResult

    companion object {
        fun <T : SearchServiceOptions> getService(options: T): SearchService<out SearchServiceOptions> {
            return when (options) {
                is SearchServiceOptions.TavilyOptions -> TavilySearchService
                is SearchServiceOptions.ExaOptions -> ExaSearchService
            }
        }
    }
}

@Serializable
data class SearchCommonOptions(
    val resultSize: Int = 5
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