package me.rerere.search

import kotlinx.serialization.Serializable

interface SearchService {
    suspend fun search(
        query: String,
        options: SearchCommonOptions
    ): SearchResult
}

@Serializable
data class SearchCommonOptions(
    val resultSize: Int
)

@Serializable
data class SearchResult(
    val items: List<SearchResultItem>,
)

@Serializable
data class SearchResultItem(
    val title: String,
    val url: String,
    val text: String,
)