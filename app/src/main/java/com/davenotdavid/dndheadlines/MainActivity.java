package com.davenotdavid.dndheadlines;

import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads up (with Loaders) to fetch data via an HTTP request of the News API that eventually
 * displays the top news/headlines based on a certain news source (either default or user's stored
 * preference).
 */
public class MainActivity extends AppCompatActivity implements LoaderCallbacks<List<Article>> {

    // Log tag constant.
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    // ListView initialization for the article list.
    private ListView mArticleListView;

    // Adapter for the list of Articles.
    private ArticleAdapter mArticleAdapter;

    // Loader ID that later gets incremented for a refresh-page UI.
    private static int articleLoaderID = 1;

    // String constant that represents the News API endpoint URL that later appends query
    // parameters.
    private static final String NEWS_ENDPOINT_URL = "https://newsapi.org/v1/articles";

    // Field used to retrieve the news-source parameter.
    private static String newsSource;

    // Field used for displaying a progress bar while fetching data from the HTTP server.
    private ProgressBar mProgressBar;

    // TextView that is displayed when the list is empty.
    private TextView mEmptyStateTextView;

    // Android Query (AQuery) field used for caching images from online.
    private AQuery mAQuery;

    // Boolean flag used for indicating whether the refresh button was pressed or not.
    private boolean pageRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Invokes the following to initialize/instantiate UI.
        init();

        // Invokes the following to begin the data-fetching process via loaders.
        runLoaders();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu); // Inflates the settings icon
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Runs the following code for each menu item.
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Initialization/instantiation method for UI.
     */
    private void init() {

        // Instantiates the following to cache images with a URL.
        mAQuery = new AQuery(this);

        // Initializes a custom Toolbar that gets displayed as it collapses.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initializes the progress bar.
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        // Initializes a ListView for the list of Articles.
        mArticleListView = (ListView) findViewById(R.id.article_list);

        // Enables nested scrolling for the ListView which only works for Lollipop (21) and above.
        ViewCompat.setNestedScrollingEnabled(mArticleListView, true);

        // Sets the custom News API attribution image clickable for branding guideline purposes.
        findViewById(R.id.news_api_att_img).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri newsApiAttUrl = Uri.parse("https://newsapi.org/");
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, newsApiAttUrl);
                startActivity(browserIntent);
            }
        });

        // Initializes and then sets the empty state TextView to the ListView for when it should be
        // empty.
        mEmptyStateTextView = (TextView) findViewById(R.id.empty_view);
        mArticleListView.setEmptyView(mEmptyStateTextView);

        // Instantiates the following adapter that takes an empty array list as initial input.
        mArticleAdapter = new ArticleAdapter(this, new ArrayList<Article>());

        // Sets the adapter on the list view so the list can be populated in the UI.
        mArticleListView.setAdapter(mArticleAdapter);

        // Sets the ListView of Articles clickable with functionality.
        mArticleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                // Initially parses the article's URL to preview it to the user via an implicit
                // intent. Otherwise, displays a Toast message informing the user that the URL
                // doesn't exist.
                String articleUrl = mArticleAdapter.getItem(position).getUrl();
                if (!articleUrl.equals("null")) {
                    Uri articlePreviewUrl = Uri.parse(articleUrl);
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, articlePreviewUrl);
                    startActivity(browserIntent);
                } else {
                    Toast.makeText(getApplicationContext(), "Sorry! There's no preview for this" +
                            " article.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Sets the floating action button clickable with the following refresh functionality.
        findViewById(R.id.refresh_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Sets the flag to true to be addressed later on.
                pageRefresh = true;

                // Increments the loader ID so new data could be fetched for the current news
                // source.
                articleLoaderID++;
                Log.d(LOG_TAG, String.valueOf(articleLoaderID));

                // Reruns the loaders.
                runLoaders();
            }
        });
    }

    /**
     * Runs loaders to fetch data via an HTTP request URL only if the user's device has connection.
     */
    private void runLoaders() {
        Log.d(LOG_TAG, "runLoaders()");

        // Retrieves a reference to the LoaderManager in order to interact with loaders.
        LoaderManager loaderManager = getLoaderManager();

        // Initializes the ConnectivityManager to check state of network connectivity.
        ConnectivityManager connManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Retrieves details on the currently active default data network.
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

        // Initializes and runs loaders should there be network connection. Otherwise, displays UI
        // related to an empty state.
        if (networkInfo != null && networkInfo.isConnected()) {
            Log.d(LOG_TAG, "runLoaders(): Connected to network");

            // Displays the ProgressBar only if the user has network connection.
            mProgressBar.setVisibility(View.VISIBLE);

            // When the refresh button is pressed, the ListView and empty state TextView will
            // temporarily be hidden for a page-refreshing UI.
            if (pageRefresh) {
                mArticleListView.setVisibility(View.INVISIBLE);

                // Temporarily hides the TextView only if it's already visible. Apparently, the
                // visibility is set back when the text is set when encountering another empty
                // state.
                if (mEmptyStateTextView.getVisibility() == View.VISIBLE) {
                    mEmptyStateTextView.setVisibility(View.INVISIBLE);
                }
            }

            // Passes in a loader that could either be a single loader or multiple loaders for a
            // refresh-page UI.
            loaderManager.initLoader(articleLoaderID, null, this);
        } else {
            Log.d(LOG_TAG, "runLoaders(): Can't connect to network");

            // Displays the following Toast message, and clears the previous article data should
            // the user lose network connection mid-session.
            if (pageRefresh) {
                Toast.makeText(this, "No network connection", Toast.LENGTH_SHORT).show();
                mArticleAdapter.clear();
            }

            // Updates the empty state view with a no-connection-error message.
            mEmptyStateTextView.setText(R.string.no_network_connection);
        }
    }

    @Override
    public Loader<List<Article>> onCreateLoader(int i, Bundle bundle) {
        Log.d(LOG_TAG, "onCreateLoader()");

        // Preferences instantiation used to retrieve the user's stored data (news source
        // preference in this case).
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        newsSource = sharedPrefs.getString(
                getString(R.string.settings_news_sources_key),
                getString(R.string.settings_news_sources_default)
        );

        // Initializes the News API endpoint URL as a URI to build and eventually append upon.
        Uri baseUri = Uri.parse(NEWS_ENDPOINT_URL);
        Uri.Builder uriBuilder = baseUri.buildUpon();

        // Appends the following query parameters onto the URI. The "sortBy" parameter is defaulted
        // as "top news" for every source. Also, the user's news source preference is appended.
        uriBuilder.appendQueryParameter("source", newsSource);
        uriBuilder.appendQueryParameter("apiKey", getString(R.string.news_api_key));

        return new ArticleLoader(this, uriBuilder.toString());
    }

    @Override
    public void onLoadFinished(Loader<List<Article>> loader, List<Article> articles) {

        // Clears the adapter of previous article data.
        mArticleAdapter.clear();

        // Initializes the following toolbar layout to set up the respective news source as its
        // title.
        CollapsingToolbarLayout collapsingToolbar =
                (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        if (newsSource.equals(getString(R.string.settings_news_sources_source1_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_source1_label));
        } else if (newsSource.equals(getString(R.string.settings_news_sources_source2_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_source2_label));
        } else if (newsSource.equals(getString(R.string.settings_news_sources_source3_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_source3_label));
        } else if (newsSource.equals(getString(R.string.settings_news_sources_source4_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_source4_label));
        } else if (newsSource.equals(getString
                (R.string.settings_news_sources_source_default_value))) {
            collapsingToolbar.setTitle(getString(R.string.google_news));
        } else if (newsSource.equals(getString(R.string.settings_news_sources_source5_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_source5_label));
        } else if (newsSource.equals(getString(R.string.settings_news_sources_source6_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_source6_label));
        } else if (newsSource.equals(getString(R.string.settings_news_sources_source7_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_source7_label));
        } else if (newsSource.equals(getString(R.string.settings_news_sources_source8_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_source8_label));
        } else if (newsSource.equals(getString(R.string.settings_news_sources_source9_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_source9_label));
        } else if (newsSource.equals(getString(R.string.settings_news_sources_source10_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_source10_label));
        } else if (newsSource.equals(getString(R.string.settings_news_sources_source11_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_source11_label));
        } else if (newsSource.equals(getString(R.string.settings_news_sources_source12_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_source12_label));
        }

        // Adds the list of Articles to the adapter's dataset, and renders backdrop images
        // accordingly should it not be null nor empty.
        ImageView backdropImg = (ImageView) findViewById(R.id.backdrop);
        if (articles != null && !articles.isEmpty()) {
            Log.d(LOG_TAG, "onLoadFinished(): Adding articles and rendering backdrop images");

            mArticleAdapter.addAll(articles);

            // Renders the backdrop image accordingly should the news source not be National
            // Geographic (their images are too big to scale down). Otherwise, renders National
            // Geographic's logo.
            if (!newsSource.equals("national-geographic")) {
                for (int i = 0; i < articles.size(); i++) {
                    String urlToImage = articles.get(i).getUrlToImage();
                    if (urlToImage.contains("http") || urlToImage.contains("https")) { // Custom way of validating News API's image URLs
                        mAQuery.id(backdropImg).image(urlToImage);
                        break;
                    }
                }
            } else {
                backdropImg.setImageResource(R.drawable.national_geo_logo);
            }

            // Displays the following Toast message when the page is refreshed.
            if (pageRefresh) Toast.makeText(this, "Page refreshed", Toast.LENGTH_SHORT).show();
        }

        // Sets the ListView visible, particularly for page-refreshing purposes.
        mArticleListView.setVisibility(View.VISIBLE);

        // Sets the flag for page refreshing back to false.
        pageRefresh = false;

        // Updates the empty state view with a no-results-found message.
        mEmptyStateTextView.setText(R.string.no_results_found);

        // Hides the progress bar after the data-fetching process is complete.
        mProgressBar.setVisibility(View.INVISIBLE);

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
