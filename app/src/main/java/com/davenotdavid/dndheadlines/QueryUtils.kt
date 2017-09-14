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
            Log.e(LOG_TAG, "Problem making the HTTP request", e)
        }

        // Extracts relevant data from the JSON response and creates a list of Articles to return.
        return extractDataFromJson(jsonResponse)
    }

    /**
     * Returns a list of [Article] objects after parsing and extracting relevant data from the JSON
     * string response.
     */
    private fun extractDataFromJson(jsonResponse: String?): List<Article>? {

        // Returns immediately out of this method should the JSON string param be null.
        if (jsonResponse == null) {
            return null
        }

        // Initializes a mutable list to eventually return a list of project data.
        var articles = mutableListOf<Article>()

        // Utilizes the GSON library to parse the JSON string response into a list of article data
        // by Article as its collection type - each article's JSON key is referenced from each
        // Article var/val attribute names in order to retrieve the respective JSON value.
        val gson = Gson()
        val collectionType = object : TypeToken<Collection<Article>>(){}.type
        try {
            val jsonObject = gson.fromJson(jsonResponse, JsonObject::class.java)
            val jsonArray = jsonObject.getAsJsonArray("articles")
            val enums = gson.fromJson<Collection<Article>>(jsonArray, collectionType)
            articles = enums.toMutableList<Article>()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Problem parsing the article JSON results", e)

            return null
        }

        // Returns the list of articles.
        return articles
    }
}
