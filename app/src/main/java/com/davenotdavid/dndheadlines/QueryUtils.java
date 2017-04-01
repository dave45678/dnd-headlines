package com.davenotdavid.dndheadlines;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Static helper methods used for requesting and retrieving article data from the News API on a
 * certain source.
 */
public class QueryUtils {

    // Log tag constant.
    private static final String LOG_TAG = QueryUtils.class.getSimpleName();

    /**
     * Queries the articles' dataset and returns a list of {@link Article} objects.
     */
    public static List<Article> fetchArticleData(String requestUrl) {

        // Creates URL object.
        URL url = createUrl(requestUrl);

        // Performs HTTP request to the URL and receives a JSON response back.
        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException e) {
            //Log.e(LOG_TAG, "Problem making the HTTP request.", e);
        }

        // Extracts relevant fields from the JSON response and creates a list of Articles to return.
        return extractFeatureFromJson(jsonResponse);
    }

    /**
     * Returns new URL object from the given string URL.
     */
    private static URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            //Log.e(LOG_TAG, "Problem building the URL ", e);
        }
        return url;
    }

    /**
     * Makes an HTTP request to the given URL and returns a String as the response.
     */
    private static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";

        // Returns immediately with an empty string should the passed-in URL be null.
        if (url == null) {
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Reads the input stream and parses the response should the request be successful with
            // a response code of 200, or the HTTP_OK constant.
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                //Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
            }
        } catch (IOException e) {
            //Log.e(LOG_TAG, "Problem retrieving the article JSON results.", e);
        } finally {

            // Disconnects/closes the following to extract resources.
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    /**
     * Converts the {@link InputStream} into a String which contains the whole JSON response from
     * the server.
     */
    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader =
                    new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    /**
     * Return a list of {@link Article} objects that has been built up from parsing the given JSON
     * response.
     */
    private static List<Article> extractFeatureFromJson(String articleJSON) {

        // Returns immediately should the JSON string be empty or null.
        if (articleJSON.isEmpty()) return null;

        // Creates an empty ArrayList to add Articles to.
        List<Article> articles = new ArrayList<>();

        // Tries to parse the JSON response string.
        try {

            // Creates a JSONObject from the JSON response string.
            JSONObject baseJsonResponse = new JSONObject(articleJSON);

            // Extracts the JSONArray associated with the key called "articles", which represents a
            // list of articles.
            JSONArray articleArray = baseJsonResponse.getJSONArray("articles");

            // Creates a Article for each article in the array.
            for (int i = 0; i < articleArray.length(); i++) {

                // Retrieves a single article within each iteration.
                JSONObject currentArticle = articleArray.getJSONObject(i);

                // Extracts values for the following keys. Only the top 10 (at most) articles are
                // retrieved from the server, so it's essential that each of these gets added to
                // the list despite some of its values being "null" - yes, a String value of null
                // is really returned from the News API's JSON response.
                String title;
                if (currentArticle.has("title")) { // Checks anyways should News API change how they return empty key-values
                    title = currentArticle.getString("title");
                } else {
                    title = "null"; // Otherwise, returns a String value of null to match News API's standards
                }

                String url;
                if (currentArticle.has("url")) {
                    url = currentArticle.getString("url");
                } else {
                    url = "null";
                }

                String urlToImage;
                if (currentArticle.has("urlToImage")) {
                    urlToImage = currentArticle.getString("urlToImage");
                } else {
                    urlToImage = "null";
                }

                String publishedAt;
                if (currentArticle.has("publishedAt")) {
                    publishedAt = currentArticle.getString("publishedAt");
                } else {
                    publishedAt = "null";
                }

                // Creates a new Article with the extracted values from the JSON response.
                Article article = new Article(title, url, urlToImage, publishedAt);

                // Adds each Article object to the list of articles.
                articles.add(article);
            }

        } catch (JSONException e) {
            //Log.e(LOG_TAG, "Problem parsing the article JSON results", e);
        }

        // Return the list of articles.
        return articles;
    }
}
