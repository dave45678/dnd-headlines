package com.davenotdavid.dndheadlines

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request

import org.json.JSONException
import org.json.JSONObject

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

        // Tries to parse the JSON response string. Otherwise, catches a JSONException.
        try {

            // Creates a JSONObject from the JSON response string.
            val baseJsonResponse = JSONObject(jsonResponse)

            // Extracts the JSONArray associated with the key called "articles", which represents a
            // list of articles.
            val articleArray = baseJsonResponse.getJSONArray("articles")

            // Creates a Article for each article in the array.
            for (i in 0..articleArray.length() - 1) {

                // Retrieves a single article within each iteration.
                val currentArticle = articleArray.getJSONObject(i)

                // Extracts values for the following keys. Only the top 10 (at most) articles are
                // retrieved from the server, so it's essential that each of these gets added to
                // the list despite some of its values being "null" - yes, a String value of null
                // is really returned from the News API's JSON response.
                val title = if (currentArticle.has("title")) currentArticle.getString("title")
                else "null" // Otherwise, returns a String value of null to match News API's standards

                val url = if (currentArticle.has("url")) currentArticle.getString("url")
                else "null"

                val urlToImage =
                        if (currentArticle.has("urlToImage")) currentArticle.getString("urlToImage")
                else "null"

                val publishedAt =
                        if (currentArticle.has("publishedAt")) currentArticle.getString("publishedAt")
                else "null"

                // Adds an Article object with the extracted values to the list of articles.
                articles.add(Article(title, url, urlToImage, publishedAt))
            }

        } catch (e: JSONException) {
            Log.e(LOG_TAG, "Problem parsing the article JSON results", e)
        }

        // Returns the list of articles.
        return articles
    }
}
