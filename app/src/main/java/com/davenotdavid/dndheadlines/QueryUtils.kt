package com.davenotdavid.dndheadlines

import android.util.Log

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken

import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Object/singleton class that consists of helper methods used for requesting and retrieving
 * article data from the News API on a certain news source.
 */
object QueryUtils {

    // Log tag constant.
    private val LOG_TAG = QueryUtils::class.java.simpleName

    /**
     * Queries the articles' data set and returns a list of [Article] objects.
     */
    fun fetchArticleData(requestUrl: String): List<Article>? {

        // Performs an HTTP request via the OkHttp client library of the request URL and retrieves
        // a JSON (string) response back.
        var jsonResponse: String? = null
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                    .url(requestUrl)
                    .build()
            val response = client.newCall(request).execute()

            // Reassigns the value of the (string) response.
            jsonResponse = response.body()?.string()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Problem making the HTTP request.", e)
        }

        // Extracts relevant data from the JSON response and creates a list of Articles to return.
        return extractDataFromJson(jsonResponse)
    }

    /**
     * Returns a list of [Article] objects after parsing and extracting relevant data from the JSON
     * (string) response.
     */
    private fun extractDataFromJson(jsonResponse: String?): List<Article>? {

        // Returns immediately should the JSON string be empty or null.
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            return null
        }

        // Creates an empty ArrayList to add Articles to.
        val articles = mutableListOf<Article>()

        // Uses GSON to parse the JSON string as an object, and then into a JSON array of data.
        val gson = Gson()
        val collectionType = object : TypeToken<Collection<Article>>(){}.type
        var articleResult = emptyArray<Article>()
        try {
            val jsonObject = gson.fromJson(jsonResponse, JsonObject::class.java)
            val jsonArray = jsonObject.getAsJsonArray("articles")
            val enums = gson.fromJson<Collection<Article>>(jsonArray, collectionType)

            // Reassigns the array to an iterable list of JSON data.
            articleResult = enums.toTypedArray<Article>()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Problem parsing the article JSON results", e)

            return null
        }

        // Iterates through the array to extract each data element.
        for (i in 0..articleResult.size - 1) {

            // Retrieves the current Article.
            val article = articleResult[i]

            // Assigns values accordingly (null should the data element not exist).
            val title = if (article.title != null) article.title else null
            val url = if (article.url != null) article.url else null
            val urlToImage = if (article.urlToImage != null) article.urlToImage else null
            val publishedAt = if (article.publishedAt != null) article.publishedAt else null

            // Adds each Article to the list.
            articles.add(Article(title, url, urlToImage, publishedAt))
        }

        // Returns the list of articles.
        return articles
    }
}
