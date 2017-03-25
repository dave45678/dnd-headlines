package com.davenotdavid.dndnews;

import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

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

    // Adapter for the list of Articles.
    private ArticleAdapter mArticleAdapter;

    // One loader ID at most for fetching news source data?
    // TODO: Consider using more than one loader for a refresh option for the latest articles.
    private static int articleLoaderID = 1;

    // String constant that represents the News API endpoint URL that later appends query
    // parameters.
    private static final String NEWS_ENDPOINT_URL = "https://newsapi.org/v1/articles";

    // Field used for displaying a progress bar while fetching data from the HTTP server.
    private ProgressBar mProgressBar;

    // TextView that is displayed when the list is empty.
    private TextView mEmptyStateTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initializes the progress bar.
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        // Initializes a ListView for the list of Articles.
        mArticleListView = (ListView) findViewById(R.id.article_list);

        // Initializes and then sets the empty state TextView to the ListView for when it should be
        // empty.
        mEmptyStateTextView = (TextView) findViewById(R.id.empty_view);
        mArticleListView.setEmptyView(mEmptyStateTextView);

        // Instantiates the following adapter that takes an empty array list as initial input.
        mArticleAdapter = new ArticleAdapter(this, new ArrayList<Article>());

        // Sets the adapter on the list view so the list can be populated in the UI.
        mArticleListView.setAdapter(mArticleAdapter);

        // Retrieves a reference to the ConnectivityManager to check state of network connectivity.
        ConnectivityManager mConnManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Retrieves details on the currently active default data network.
        NetworkInfo networkInfo = mConnManager.getActiveNetworkInfo();

        // Initializes and runs loaders should there be network connection. Otherwise, displays UI
        // related to an empty state.
        if (networkInfo != null && networkInfo.isConnected()) {

            // Retrieves a reference to the LoaderManager in order to interact with loaders.
            LoaderManager loaderManager = getLoaderManager();

            // Passes in the single loader.
            loaderManager.initLoader(articleLoaderID, null, this);
        } else {

            // Updates empty state with a no-connection-error message.
            mEmptyStateTextView.setText(R.string.no_internet_connection);
        }
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

        // Sets the empty state text to display the following message.
        mEmptyStateTextView.setText(R.string.no_results_found);

        // Clears the adapter of previous article data.
        mArticleAdapter.clear();

        // Adds the list of Articles to the adapter's dataset should it not be null nor empty.
        if (articles != null && !articles.isEmpty()) {
            mArticleAdapter.addAll(articles);
        }

        // Hides the progress bar after the data-fetching process is complete.
        mProgressBar.setVisibility(View.GONE);

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

        // Clears out the existing data since the loader resetted.
        mArticleAdapter.clear();
    }
}
