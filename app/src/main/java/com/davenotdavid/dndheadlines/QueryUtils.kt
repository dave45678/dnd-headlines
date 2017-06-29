package com.davenotdavid.dndheadlines

import android.util.Log

import org.json.JSONException
import org.json.JSONObject

import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.Scanner

/**
 * Object/singleton class that consists of helper methods used for requesting and retrieving
 * article data from the News API on a certain source.
 */
object QueryUtils {

    // Log tag constant.
    private val LOG_TAG = QueryUtils::class.java.simpleName

    /**
     * Queries the articles' data set and returns a list of [Article] objects.
     */
    fun fetchArticleData(requestUrl: String): List<Article>? {

        // Creates a URL object.
        val url = createUrl(requestUrl)

        // Performs an HTTP request to the URL and receives a JSON response back.
        var jsonResponse: String? = null
        try {
            jsonResponse = getResponseFromHttpUrl(url)
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Problem making the HTTP request.", e)
        }

        // Extracts relevant fields from the JSON response and creates a list of Articles to return.
        return extractFeatureFromJson(jsonResponse)
    }

    /**
     * Returns a new URL object from the given string URL.
     */
    private fun createUrl(stringUrl: String): URL? {
        var url: URL? = null
        try {
            url = URL(stringUrl)
        } catch (e: MalformedURLException) {
            Log.e(LOG_TAG, "Problem building the URL ", e)
        }

        return url
    }

    /**
     * Makes an HTTP request to the given URL and returns the entire JSON response.
     *
     * @param url is the URL to fetch the HTTP response from.
     */
    @Throws(IOException::class)
    fun getResponseFromHttpUrl(url: URL?): String? {
        val urlConnection = url?.openConnection() as HttpURLConnection

        try {

            // Retrieves the input stream and parses the response should the request be successful
            // with a response code of 200, or the HTTP_OK constant.
            if (urlConnection.responseCode == HttpURLConnection.HTTP_OK) {

                // A hack used to initially convert the input stream to a readable string, and then
                // tokenizes the entire stream.
                val inputStream = urlConnection.inputStream
                val scanner = Scanner(inputStream)
                scanner.useDelimiter("\\A") // Regex char that implies the first token

                // Returns the string of data should it exist.
                if (scanner.hasNext()) {
                    return scanner.next()
                }
            } else {
                Log.e(LOG_TAG, "Error response code: " + urlConnection.responseCode)
            }
        } finally {

            // Disconnects/closes the following to extract resources.
            urlConnection?.disconnect()
        }

        return null
    }

    /**
     * Return a list of [Article] objects that has been built up from parsing the given JSON
     * response.
     */
    private fun extractFeatureFromJson(jsonResponse: String?): List<Article>? {

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
