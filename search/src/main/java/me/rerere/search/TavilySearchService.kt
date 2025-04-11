package me.rerere.search

class TavilySearchService(
    private val token: String
) : SearchService {
    override suspend fun search(
        query: String,
        options: SearchCommonOptions
    ): SearchResult {
        TODO("Not yet implemented")
    }
}