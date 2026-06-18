package com.gohan.newsfeed.model

/**
 * Data model untuk artikel berita.
 */
data class NewsArticle(
    val id: Int,
    val title: String,
    val category: Category,
    val content: String,
    val publishedAt: Long,
    val isRead: Boolean = false
)
