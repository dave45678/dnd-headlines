package com.davenotdavid.dndnews;

import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: Fill this part out when ready.
 */
public class MainActivity extends AppCompatActivity implements LoaderCallbacks<List<Article>> {

    // Log tag constant.
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    // ListView initialization for the article list.
    private ListView mArticleListView;

    // One loader ID at most for fetching news source data?
    // TODO: Consider using more than one loader for a refresh option for the latest articles.
    private static int articleLoaderID = 1;

    // String constant that represents the News API endpoint URL that later appends query
    // parameters.
    private static final String NEWS_ENDPOINT_URL = "https://newsapi.org/v1/articles";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mArticleListView = (ListView) findViewById(R.id.article_list);

        // Retrieves a reference to the LoaderManager in order to interact with loaders.
        LoaderManager loaderManager = getLoaderManager();

        // Passes in the single loader.
        loaderManager.initLoader(articleLoaderID, null, this);
    }

    @Override
    public Loader<List<Article>> onCreateLoader(int i, Bundle bundle) {
        Log.d(LOG_TAG, "onCreateLoader()");

        // Initializes the News API endpoint URL as a URI to build and eventually append upon.
        Uri baseUri = Uri.parse(NEWS_ENDPOINT_URL);
        Uri.Builder uriBuilder = baseUri.buildUpon();

        // Appends the following query parameters onto the URI. The "sortBy" parameter is defaulted
        // as "top news" for every source.
        // TODO: Include a Preference option for the news source.
        uriBuilder.appendQueryParameter("source", "google-news");
        uriBuilder.appendQueryParameter("apiKey", getString(R.string.news_api_key));

        return new ArticleLoader(this, uriBuilder.toString());
    }

    @Override
    public void onLoadFinished(Loader<List<Article>> loader, List<Article> articles) {

        // TODO: Temporary; reconfigure the following code soon.
        if (articles != null && !articles.isEmpty()) {
            List<String> articleList = new ArrayList<>();
            for (int i = 0; i < articles.size(); i++) {
                articleList.add(articles.get(i).getTitle());
            }

            ArrayAdapter<String> arrayAdapter =
                    new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, articleList);

            mArticleListView.setAdapter(arrayAdapter);
        }

        Log.d(LOG_TAG, "onLoadFinished");
    }

    /**
     * Invoked when the app closes.
     *
     * @param loader is the passed-in loader that could be addressed.
     */
    @Override
    public void onLoaderReset(Loader<List<Article>> loader) {
        Log.d(LOG_TAG, "onLoaderReset()");

        // TODO: Clear out the adapter's data clear.
    }
}
