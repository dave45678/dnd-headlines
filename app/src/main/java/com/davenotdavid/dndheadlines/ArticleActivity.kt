package com.davenotdavid.dndheadlines

import android.app.LoaderManager.LoaderCallbacks
import android.content.AsyncTaskLoader
import android.content.Context
import android.content.Intent
import android.content.Loader
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.support.v7.preference.PreferenceManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Uri
import android.support.design.widget.Snackbar
import android.support.v4.view.ViewCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View

import com.squareup.picasso.Picasso

import kotlinx.android.synthetic.main.activity_article.*

/**
 * Loads up (with Loaders) to fetch data via an HTTP request of the News API that eventually
 * displays the top news/headlines based on a certain news source (either default or user's stored
 * preference).
 */
class ArticleActivity : AppCompatActivity(), LoaderCallbacks<List<Article>>,
        OnSharedPreferenceChangeListener, ArticleAdapter.ListItemClickListener {

    // Log tag constant.
    private val LOG_TAG = ArticleActivity::class.java.simpleName

    // Loader ID constant.
    private val ARTICLE_LOADER_ID = 100

    // String constant that represents the News API endpoint URL that later appends query
    // parameters.
    private val NEWS_ENDPOINT_URL = "https://newsapi.org/v1/articles"

    // Field used to retrieve the news-source parameter.
    private var mNewsSource: String? = null

    // SharedPreferences object field that's used for user preference data throughout the app's
    // lifecycle.
    private var mSharedPrefs: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article)

        // Invokes the following to initialize/instantiate the Activity's UI.
        init()

        // Invokes the following to begin the data-fetching process via a Loader.
        runLoader(false)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Unregisters the SharedPreferences from OnSharedPreferenceChangeListener.
        mSharedPrefs!!.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        // Inflates the settings menu file.
        menuInflater.inflate(R.menu.settings, menu)

        // True to display.
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // Opens up SettingsActivity via an explicit intent should the following menu item be
        // selected.
        when (item.itemId) {
            R.id.action_settings -> {
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivity(settingsIntent)

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * RecyclerView's click method functionality.
     */
    override fun onListItemClick(article: Article) {

        // Initially retrieves the article's URL to display to the user via an explicit intent.
        // Otherwise, displays a Snackbar message informing the user that the URL doesn't exist.
        val articleUrl = article.url
        if (articleUrl != null) {

            // Instantiates an Intent object to pass data onto SourceViewActivity for
            // web-rendering purposes.
            val intent = Intent(this, SourceViewActivity::class.java)
            intent.putExtra("source_url", articleUrl)
            startActivity(intent)
        } else {
            Snackbar.make(activity_article, getString(R.string.snackbar_no_article_preview),
                    Snackbar.LENGTH_SHORT).setAction("Action", null).show()
        }
    }

    /**
     * Initialization/instantiation method for the Activity's UI.
     */
    private fun init() {

        // Initially sets the title as "Loading..." to display a loading UI.
        collapsing_toolbar.title = getString(R.string.toolbar_loading_title)

        // Displays the swipe-refresh UI for initial runtime.
        swipeRefreshLayout.isRefreshing = true

        // Sets a listener on the SwipeRefreshLayout to respond accordingly whenever the user runs
        // the swipe-refresh gesture.
        swipeRefreshLayout.setOnRefreshListener {

            // Runs a loader to provide a refresh UI for populating updated data.
            runLoader(true)
        }

        // Makes the RecyclerView scroll down linearally as well as have a fixed size.
        article_recycler_view.layoutManager = LinearLayoutManager(this)
        article_recycler_view.setHasFixedSize(true)

        // References the PreferenceManager to use throughout the app, and then registers it with
        // OnSharedPreferenceChangeListener.
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        mSharedPrefs!!.registerOnSharedPreferenceChangeListener(this)

        // Sets the news source preference during the app's initial runtime as well as during
        // configuration changes.
        setNewsSource()

        // Sets a custom Toolbar that gets displayed after its CollapsingToolbar Layout collapses.
        setSupportActionBar(toolbar)

        // Enables nested scrolling for the ListView which only works for Lollipop (21) and above.
        ViewCompat.setNestedScrollingEnabled(article_recycler_view, true)

        // Sets the custom News API attribution image clickable for branding guideline purposes.
        attribution_imgview.setOnClickListener {

            // Instantiates an Intent object to pass data onto SourceViewActivity for web-rendering
            // purposes.
            val intent = Intent(this, SourceViewActivity::class.java)
            intent.putExtra("source_url", getString(R.string.news_api_url))
            startActivity(intent)
        }

        // Sets the adapter on the RecyclerView so its list can be populated with UI.
        article_recycler_view.adapter = ArticleAdapter(this, mutableListOf<Article>())

        // MediaPlayer object used for sound UI.
        val buttonSound = MediaPlayer.create(this, R.raw.button_sound)

        // Sets the floating action button clickable with the following refresh functionality.
        // TODO: To get rid of or not?
        refresh_fab.setOnClickListener {

            // Plays a button sound when the refresh button is clicked.
            buttonSound.start()

            // Restarts a Loader.
            runLoader(true)
        }
    }

    /**
     * Setter for the news source preference.
     */
    private fun setNewsSource() {
        mNewsSource = mSharedPrefs!!.getString(
                getString(R.string.settings_news_sources_key),
                getString(R.string.settings_news_sources_default)
        )
    }

    /**
     * Runs a Loader to fetch data via an HTTP request only if the user's device has connection.
     *
     * @param restartLoader is a Boolean flag that indicates whether a Loader should be either
     *        initialized or restarted.
     */
    private fun runLoader(restartLoader: Boolean) {

        // Sets the toolbar's title to "Loading..." for a loading UI.
        collapsing_toolbar.title = getString(R.string.toolbar_loading_title)

        // Retrieves a reference to the LoaderManager in order to interact with loaders.
        val loaderManager = loaderManager

        // Initializes the ConnectivityManager to check state of network connectivity.
        val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Retrieves details on the currently active default data network.
        val networkInfo = connManager.activeNetworkInfo

        // Initializes and runs loaders should there be network connection. Otherwise, displays UI
        // related to an empty state.
        if (networkInfo != null && networkInfo.isConnected) {

            // Restarts a Loader should the flag param be true. Otherwise, initializes and runs a
            // Loader.
            if (restartLoader) {

                // Temporarily hides the TextView only if it's already visible.
                if (empty_text_view.visibility == View.VISIBLE) {
                    empty_text_view.visibility = View.INVISIBLE
                }

                loaderManager.restartLoader(ARTICLE_LOADER_ID, null, this)
            } else {
                loaderManager.initLoader(ARTICLE_LOADER_ID, null, this)
            }
        } else {

            // Disables the swipe-refresh UI.
            swipeRefreshLayout.isRefreshing = false

            // Sets the toolbar's title to "Error".
            collapsing_toolbar.title = getString(R.string.toolbar_error_title)

            // Updates the empty state view with a no-connection-error message while hiding the
            // RecyclerView.
            // TODO: Consider changing the following text message
            empty_text_view.setText(R.string.no_internet_connection)
            empty_text_view.visibility = View.VISIBLE
            article_recycler_view.visibility = View.INVISIBLE
        }
    }

    /**
     * Invoked every time a preference is changed.
     *
     * @param sharedPreferences is the SharedPreferences instance that's used throughout the app's
     *                          lifecycle.
     * @param key is the SharedPreferences key.
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == getString(R.string.settings_news_sources_key)) {

            // Updates the news source preference, accordingly.
            setNewsSource()

            // Restarts a Loader.
            runLoader(true)
        }
    }

    /**
     * Instantiates and returns a new Loader for the given ID.
     */
    override fun onCreateLoader(loaderId: Int, bundle: Bundle?): Loader<List<Article>> {

        // List instance used for storing raw JSON data from the background task.
        var articleJson: List<Article>? = null

        // Initializes the News API endpoint URL as a URI to build and eventually append upon.
        val baseUri = Uri.parse(NEWS_ENDPOINT_URL)
        val uriBuilder = baseUri.buildUpon()

        // Appends the following query parameters onto the URI. The "sortBy" parameter is defaulted
        // as "top news" for every source. Also, the user's news source preference is appended.
        uriBuilder.appendQueryParameter("source", mNewsSource)
        uriBuilder.appendQueryParameter("apiKey", getString(R.string.news_api_key))

        // Returns an anonymous class instance of AsyncTaskLoader to eventually return a list of
        // article data.
        return object : AsyncTaskLoader<List<Article>>(this) {

            /**
             * Initiates the load.
             */
            override fun onStartLoading() {

                // Forces a load should the article JSON data not exist as cached results.
                // Otherwise, delivers the existing JSON data.
                if (articleJson == null) {
                    forceLoad()
                } else {
                    deliverResult(articleJson)
                }
            }

            /**
             * Runs the load asynchronously. Performs the network request with a String value of
             * the endpoint URI, parses the JSON response, and extracts a list of article data to
             * return from the Utils object.
             */
            override fun loadInBackground(): List<Article>? =
                    QueryUtils.fetchArticleData(uriBuilder.toString())

            /**
             * Sends the result of the load to the registered listener.
             */
            override fun deliverResult(data: List<Article>?) {
                articleJson = data // Assigns the article data var

                super.deliverResult(data)
            }
        }
    }

    /**
     * Invoked right after the background thread finished its tasks. That said, this function
     * serves as the UI thread.
     */
    override fun onLoadFinished(loader: Loader<List<Article>>, articles: List<Article>?) {

        // Runs the following functionality and returns out of onLoadFinished() immediately should
        // the article data be either null or empty.
        if (articles == null || articles.isEmpty()) {

            // Disables the swipe-refresh UI.
            swipeRefreshLayout.isRefreshing = false

            // Sets the toolbar's title to "Error".
            collapsing_toolbar.title = getString(R.string.toolbar_error_title)

            // Updates the empty state view with a no-results-found message while hiding the
            // recycler view.
            empty_text_view.setText(R.string.no_results_found)
            empty_text_view.visibility = View.VISIBLE
            article_recycler_view.visibility = View.INVISIBLE

            return
        }

        // Sets the toolbar's title to the respective news source name.
        if (mNewsSource == getString(R.string.settings_news_sources_abc_news_au_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_abc_news_au_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_al_jazeera_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_al_jazeera_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_ars_technica_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_ars_technica_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_assoc_press_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_assoc_press_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_bbc_news_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_bbc_news_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_bloomberg_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_bloomberg_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_breitbart_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_breitbart_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_bus_insider_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_bus_insider_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_daily_mail_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_daily_mail_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_engadget_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_engadget_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_ent_weekly_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_ent_weekly_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_fin_times_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_fin_times_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_fortune_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_fortune_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_fourfourtwo_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_fourfourtwo_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_fox_sports_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_fox_sports_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_source_default_value)) {
            collapsing_toolbar.title = getString(R.string.google_news)
        } else if (mNewsSource == getString(R.string.settings_news_sources_ign_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_ign_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_mashable_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_mashable_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_metro_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_metro_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_mtv_news_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_mtv_news_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_nat_geographic_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_nat_geographic_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_ny_magazine_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_ny_magazine_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_nfl_news_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_nfl_news_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_reuters_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_reuters_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_talksport_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_talksport_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_techcrunch_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_techcrunch_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_techradar_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_techradar_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_guardian_uk_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_guardian_uk_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_the_hindu_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_the_hindu_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_the_lad_bible_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_the_lad_bible_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_the_nyt_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_the_nyt_label)
        } else if (mNewsSource == getString(R.string.settings_news_sources_wsj_value)) {
            collapsing_toolbar.title = getString(R.string.settings_news_sources_wsj_label)
        }

        // Renders newly loaded data into the following Adapter.
        article_recycler_view.adapter = ArticleAdapter(this, articles)

        // Renders the backdrop image accordingly should the news source not be National Geographic
        // (their images are too big to scale down). Otherwise, renders National Geographic's logo.
        if (mNewsSource != "national-geographic") {
            for (i in articles.indices) {
                val urlToImage = articles[i].urlToImage
                if (urlToImage!!.contains("http") || urlToImage!!.contains("https")) { // Custom way of validating News API's image URLs
                    Picasso.with(backdrop_image_view.context)
                            .load(urlToImage)
                            .into(backdrop_image_view)
                    break
                }
            }
        } else {
            backdrop_image_view.setImageResource(R.drawable.national_geo_logo)
        }

        // Hides the empty state view, and makes the RecyclerView visible should the data-fetching
        // process be successful.
        empty_text_view.visibility = View.INVISIBLE
        article_recycler_view.visibility = View.VISIBLE

        // Disables the swipe-refresh UI.
        swipeRefreshLayout.isRefreshing = false
    }

    /**
     * Invoked when the app closes.
     */
    override fun onLoaderReset(loader: Loader<List<Article>>) {

        // "Clears" out the existing data since the loader resetted.
        article_recycler_view.adapter = ArticleAdapter(this, mutableListOf<Article>())
    }

    /**
     * Invoked when the user presses the navigation key, back button.
     */
    override fun onBackPressed() {
        AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.dialog_exit_msg)
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes) { arg0, arg1 ->

                    // Implements the back button's function by exiting the app.
                    super.onBackPressed()
                }.create().show()
    }
}
