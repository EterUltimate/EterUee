package com.eterultimate.eteruee.ai.ui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class UIMessageAnnotation {
    @Serializable
    @SerialName("url_citation")
    data class UrlCitation(
        val title: String,
        val url: String
    ) : UIMessageAnnotation()
}
