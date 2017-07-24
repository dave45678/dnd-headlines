package com.davenotdavid.dndheadlines

/**
 * Custom data class that represents an article (title, URL, URL of its thumbnail-image, and its
 * publish date) from a news source.
 */
data class Article (val title: String?, val url: String?, val urlToImage: String?,
                    val publishedAt: String?)
