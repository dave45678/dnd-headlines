package com.davenotdavid.dndheadlines;

import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
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

import com.androidquery.AQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads up (with Loaders) to fetch data via an HTTP request of the News API that eventually
 * displays the top news/headlines based on a certain news source (either default or user's stored
 * preference).
 */
public class ArticleActivity extends AppCompatActivity implements LoaderCallbacks<List<Article>> {

    // Log tag constant.
    private static final String LOG_TAG = ArticleActivity.class.getSimpleName();

    // ListView initialization for the article list.
    private ListView mArticleListView;

    // Adapter for the list of Articles.
    private ArticleAdapter mArticleAdapter;

    // ID constant for the loader.
    private static final int ARTICLE_LOADER_ID = 1;

    // String constant that represents the News API endpoint URL that later appends query
    // parameters.
    private static final String NEWS_ENDPOINT_URL = "https://newsapi.org/v1/articles";

    // Field used to retrieve the news-source parameter.
    private static String mNewsSource;

    // Field used for displaying a progress bar while fetching data from the HTTP server.
    private ProgressBar mProgressBar;

    // TextView that is displayed when the list is empty.
    private TextView mEmptyStateTextView;

    // Android Query (AQuery) field used for caching images from online.
    private AQuery mAQuery;

    // Boolean flag used for indicating whether the refresh button was pressed or not.
    private boolean mPageRefresh;

    // CoordinatorLayout field used for UI purposes such as displaying a Snackbar message.
    private CoordinatorLayout mCoordLayout;

    // Static boolean flag used for indicating whether to force a loader to fetch data or not.
    private static boolean mForceLoad;

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

        // Sets the flag to true to force a loader to run each time this Activity is created.
        mForceLoad = true;

        // CoordinatorLayout initialization of ArticleActivity's layout.
        mCoordLayout = (CoordinatorLayout) findViewById(R.id.activity_main);

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

                // Verifies that the user's device has the required "app" for running the implicit
                // intent. Otherwise, displays a Snackbar message that indicates an error.
                if (browserIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(browserIntent);
                } else {
                    Snackbar.make(mCoordLayout, getString(R.string.snackbar_preview_error),
                            Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                }
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
                // intent. Otherwise, displays a Snackbar message informing the user that the URL
                // doesn't exist.
                String articleUrl = mArticleAdapter.getItem(position).getUrl();
                if (!articleUrl.equals("null")) { // Yes, News API actually returns a String value of null
                    Uri articlePreviewUrl = Uri.parse(articleUrl);
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, articlePreviewUrl);

                    // Verifies that the user's device has the required "app" for running the
                    // implicit intent. Otherwise, displays a Snackbar message that indicates an
                    // error.
                    if (browserIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(browserIntent);
                    } else {
                        Snackbar.make(mCoordLayout, getString(R.string.snackbar_preview_error),
                                Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                    }
                } else {
                    Snackbar.make(mCoordLayout, getString(R.string.snackbar_no_article_preview),
                            Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                }
            }
        });

        // MediaPlayer object used for sound UI.
        final MediaPlayer buttonSound = MediaPlayer.create(this, R.raw.button_sound);

        // Sets the floating action button clickable with the following refresh functionality.
        findViewById(R.id.refresh_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Plays a button sound when the refresh button is clicked.
                buttonSound.start();

                // Sets the flag to true to be addressed later on.
                mPageRefresh = true;

                // Sets the flag to true to re-force a loader.
                mForceLoad = true;

                // Reruns the loaders.
                runLoaders();
            }
        });
    }

    /**
     * Runs loaders to fetch data via an HTTP request URL only if the user's device has connection.
     */
    private void runLoaders() {
        //Log.d(LOG_TAG, "runLoaders()");

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
            //Log.d(LOG_TAG, "runLoaders(): Connected to network");

            // Displays the ProgressBar only if the user has network connection.
            mProgressBar.setVisibility(View.VISIBLE);

            // When the refresh button is pressed, the ListView and empty state TextView will
            // temporarily be hidden, and the loader will restart for a page-refreshing UI.
            // Otherwise, initializes the loader.
            if (mPageRefresh) {
                mArticleListView.setVisibility(View.INVISIBLE);

                // Temporarily hides the TextView only if it's already visible. Apparently, the
                // visibility is set back when the text is set when encountering another empty
                // state.
                if (mEmptyStateTextView.getVisibility() == View.VISIBLE) {
                    mEmptyStateTextView.setVisibility(View.INVISIBLE);
                }

                loaderManager.restartLoader(ARTICLE_LOADER_ID, null, this);
            } else {
                loaderManager.initLoader(ARTICLE_LOADER_ID, null, this);
            }
        } else {
            //Log.d(LOG_TAG, "runLoaders(): Can't connect to network");

            // Displays the following Snackbar message.
            Snackbar.make(mCoordLayout, getString(R.string.snackbar_no_internet_connection),
                    Snackbar.LENGTH_SHORT).setAction("Action", null).show();

            // Clears the previous article data should the user lose network connection mid-session.
            if (mPageRefresh) mArticleAdapter.clear();

            // Updates the empty state view with a no-connection-error message.
            mEmptyStateTextView.setText(R.string.no_internet_connection);
        }
    }

    /**
     * Displays the following Snackbar message when the page is refreshed, and then sets the flag
     * back to false.
     */
    private void displayRefreshSnackbar() {
        if (mPageRefresh) {
            Snackbar.make(mCoordLayout, getString(R.string.snackbar_page_refreshed),
                    Snackbar.LENGTH_SHORT).setAction("Action", null).show();

            mPageRefresh = false;
        }
    }

    @Override
    public Loader<List<Article>> onCreateLoader(int loaderId, Bundle bundle) {
        //Log.d(LOG_TAG, "onCreateLoader()");

        // Preferences instantiation used to retrieve the user's stored data (news source
        // preference in this case).
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mNewsSource = sharedPrefs.getString(
                getString(R.string.settings_news_sources_key),
                getString(R.string.settings_news_sources_default)
        );

        // Initializes the News API endpoint URL as a URI to build and eventually append upon.
        Uri baseUri = Uri.parse(NEWS_ENDPOINT_URL);
        Uri.Builder uriBuilder = baseUri.buildUpon();

        // Appends the following query parameters onto the URI. The "sortBy" parameter is defaulted
        // as "top news" for every source. Also, the user's news source preference is appended.
        uriBuilder.appendQueryParameter("source", mNewsSource);
        uriBuilder.appendQueryParameter("apiKey", getString(R.string.news_api_key));

        // References the string value of the URI into the following variable.
        final String requestUrl = uriBuilder.toString();

        // Returns the following AsyncTaskLoader.
        return new AsyncTaskLoader<List<Article>>(this) {

            /**
             * Invoked whenever the user returns to the app after being minimized.
             */
            @Override
            protected void onStartLoading() {
                //Log.d(LOG_TAG, "onStartLoading()");

                // Only forces a load should the flag be true for the sake of not consistently
                // running loaders in the background (say, when the user reopens this app).
                if (mForceLoad) {
                    forceLoad();
                }
            }

            @Override
            public List<Article> loadInBackground() {
                //Log.d(LOG_TAG, "loadInBackground()");

                if (requestUrl == null) return null;

                // Performs the network request, parses the JSON response, and extracts a list of
                // articles to return.
                return QueryUtils.fetchArticleData(requestUrl);
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<List<Article>> loader, List<Article> articles) {
        //Log.d(LOG_TAG, "onLoadFinished");

        // Hides the progress bar after the data-fetching process is complete.
        mProgressBar.setVisibility(View.INVISIBLE);

        // Clears the adapter of previous article data.
        mArticleAdapter.clear();

        // Updates the empty state view with a no-results-found message.
        mEmptyStateTextView.setText(R.string.no_results_found);

        // Displays the refresh-Snackbar message and returns out of onLoadFinished() immediately
        // should the article data be null.
        if (articles == null || articles.isEmpty()) {
            displayRefreshSnackbar();

            return;
        }

        // Initializes the following toolbar layout to set up the respective news source as its
        // title.
        CollapsingToolbarLayout collapsingToolbar =
                (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        if (mNewsSource.equals(getString(R.string.settings_news_sources_abc_news_au_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_abc_news_au_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_al_jazeera_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_al_jazeera_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_ars_technica_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_ars_technica_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_assoc_press_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_assoc_press_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_bbc_news_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_bbc_news_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_bloomberg_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_bloomberg_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_breitbart_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_breitbart_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_bus_insider_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_bus_insider_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_daily_mail_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_daily_mail_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_engadget_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_engadget_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_ent_weekly_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_ent_weekly_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_fin_times_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_fin_times_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_fortune_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_fortune_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_fourfourtwo_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_fourfourtwo_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_fox_sports_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_fox_sports_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_source_default_value))) {
            collapsingToolbar.setTitle(getString(R.string.google_news));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_ign_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_ign_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_mashable_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_mashable_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_metro_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_metro_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_mtv_news_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_mtv_news_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_nat_geographic_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_nat_geographic_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_ny_magazine_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_ny_magazine_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_nfl_news_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_nfl_news_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_reuters_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_reuters_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_talksport_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_talksport_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_techcrunch_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_techcrunch_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_techradar_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_techradar_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_guardian_uk_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_guardian_uk_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_the_hindu_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_the_hindu_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_the_lad_bible_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_the_lad_bible_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_the_nyt_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_the_nyt_label));
        } else if (mNewsSource.equals(getString(R.string.settings_news_sources_wsj_value))) {
            collapsingToolbar.setTitle(getString(R.string.settings_news_sources_wsj_label));
        }

        // Adds the list of Articles to the adapter's dataset, and renders backdrop images
        // accordingly should it not be null nor empty.
        ImageView backdropImg = (ImageView) findViewById(R.id.backdrop);
        if (articles != null && !articles.isEmpty()) {
            //Log.d(LOG_TAG, "onLoadFinished(): Adding articles and rendering backdrop images");

            mArticleAdapter.addAll(articles);

            // Renders the backdrop image accordingly should the news source not be National
            // Geographic (their images are too big to scale down). Otherwise, renders National
            // Geographic's logo.
            if (!mNewsSource.equals("national-geographic")) {
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
        }

        // Sets the ListView visible, particularly for page-refreshing purposes.
        mArticleListView.setVisibility(View.VISIBLE);

        // Invokes the following to display a Snackbar message after successfully refreshing the
        // article page.
        displayRefreshSnackbar();

        // After the loader runs successfully up to this point, the following flag is set to false
        // so that the loader won't run anymore unnecessarily.
        mForceLoad = false;
    }

    /**
     * Invoked when the app closes.
     *
     * @param loader is the passed-in loader that could be addressed.
     */
    @Override
    public void onLoaderReset(Loader<List<Article>> loader) {
        //Log.d(LOG_TAG, "onLoaderReset()");

        // Clears out the existing data since the loader resetted.
        mArticleAdapter.clear();
    }

    /**
     * Invoked when the user presses the navigation key, back button.
     */
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.dialog_exit_msg)
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface arg0, int arg1) {

                        // Implements the back button's function by exiting the app.
                        ArticleActivity.super.onBackPressed();
                    }
                }).create().show();
    }
}
