package com.davenotdavid.dndheadlines

import android.app.LoaderManager.LoaderCallbacks
import android.content.AsyncTaskLoader
import android.content.Context
import android.content.Intent
import android.content.Loader
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.support.v7.preference.PreferenceManager
import android.net.ConnectivityManager
import android.net.Uri
import android.support.design.widget.Snackbar
import android.support.v4.view.ViewCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.ContextMenu
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
    private var newsSource: String? = null

    // SharedPreferences object field that's used for user preference data throughout the app's
    // lifecycle.
    private var sharedPrefs: SharedPreferences? = null

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
        sharedPrefs!!.unregisterOnSharedPreferenceChangeListener(this)
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
            R.id.actionSettings -> {
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivity(settingsIntent)

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Interface-inherited function from [ArticleAdapter] that listens to when one of its
     * RecyclerView's list items is clicked.
     */
    override fun onListItemClick(article: Article) {

        // Initially retrieves the article's URL to display to the user via an explicit Intent.
        // Otherwise, displays a Snackbar message informing the user that the URL doesn't exist.
        val articleUrl = article.url
        if (articleUrl != null) {

            // Instantiates an Intent object to pass data onto SourceViewActivity for
            // web-rendering purposes.
            val intent = Intent(this, SourceViewActivity::class.java)
            intent.putExtra("source_url", articleUrl)
            startActivity(intent)
        } else {
            Snackbar.make(activityArticle, getString(R.string.snackbar_no_article_preview),
                    Snackbar.LENGTH_SHORT).setAction("Action", null).show()
        }
    }

    // Var field that keeps track of an article's URL.
    private var articleUrl: String? = null

    /**
     * Interface-inherited function from [ArticleAdapter] that listens to when one of its
     * RecyclerView's list items is long-clicked.
     */
    override fun onListItemLongClick(article: Article) {

        // Retrieves the param article's URL and reassigns it to the var field.
        articleUrl = article.url

        // Opens the Context Menu (that was registered earlier) for the following View.
        openContextMenu(activityArticle)
    }

    /**
     * Invoked from openContextMenu() to create and display a Context Menu.
     */
    override fun onCreateContextMenu(menu: ContextMenu?, v: View?,
                                     menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)

        // Sets the article's URL as the Context Menu's header title.
        menu?.setHeaderTitle(articleUrl)

        // Adds a menu option as follows.
        menu?.add(0, v!!.id, 0, "Open in Browser")
    }

    /**
     * Callback method that listens to a MenuItem being selected from the Context Menu.
     */
    override fun onContextItemSelected(item: MenuItem?): Boolean {

        // Runs the following cases depending on which MenuItem was selected.
        when (item?.title) {
            "Open in Browser" -> {

                // Instantiates an implicit Intent in view mode with a URI-parsed article URL to
                // display the article in a browser.
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(articleUrl))
                startActivity(browserIntent)

                return true
            }
        }

        return super.onContextItemSelected(item)
    }

    /**
     * Initialization/instantiation method for the Activity's UI.
     */
    private fun init() {

        // Initially sets the title as "Loading..." to display a loading UI.
        collapsingToolbar.title = getString(R.string.toolbar_loading_title)

        // Displays the swipe-refresh UI for initial runtime.
        swipeRefreshLayout.isRefreshing = true

        // Sets a listener on the SwipeRefreshLayout to respond accordingly whenever the user runs
        // the swipe-refresh gesture.
        swipeRefreshLayout.setOnRefreshListener {

            // Runs a loader to provide a refresh UI for populating updated data.
            runLoader(true)
        }

        // Makes the RecyclerView scroll down linearally as well as have a fixed size.
        articleRecyclerView.layoutManager = LinearLayoutManager(this)
        articleRecyclerView.setHasFixedSize(true)

        // Registers a Context Menu to be shown for the following View.
        registerForContextMenu(activityArticle)

        // References the PreferenceManager to use throughout the app, and then registers it with
        // OnSharedPreferenceChangeListener.
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPrefs!!.registerOnSharedPreferenceChangeListener(this)

        // Sets the news source preference during the app's initial runtime as well as during
        // configuration changes.
        setNewsSource()

        // Sets a custom Toolbar that gets displayed after its CollapsingToolbar Layout collapses.
        setSupportActionBar(toolbar)

        // Enables nested scrolling for the ListView which only works for Lollipop (21) and above.
        ViewCompat.setNestedScrollingEnabled(articleRecyclerView, true)

        // Sets the custom News API attribution image clickable for branding guideline purposes.
        attributionImgView.setOnClickListener {

            // Instantiates an Intent object to pass data onto SourceViewActivity for web-rendering
            // purposes.
            val intent = Intent(this, SourceViewActivity::class.java)
            intent.putExtra("source_url", getString(R.string.news_api_url))
            startActivity(intent)
        }

        // Sets the adapter on the RecyclerView so its list can be populated with UI.
        articleRecyclerView.adapter = ArticleAdapter(this, mutableListOf<Article>())

        // Sets the floating action button clickable with the following refresh functionality.
        refreshFab.setOnClickListener {

            // Displays the swipe-refresh UI.
            swipeRefreshLayout.isRefreshing = true

            // Restarts a Loader.
            runLoader(true)
        }
    }

    /**
     * Setter for the news source preference.
     */
    private fun setNewsSource() {
        newsSource = sharedPrefs!!.getString(
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
        collapsingToolbar.title = getString(R.string.toolbar_loading_title)

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
                if (emptyTextView.visibility == View.VISIBLE) {
                    emptyTextView.visibility = View.INVISIBLE
                }

                loaderManager.restartLoader(ARTICLE_LOADER_ID, null, this)
            } else {
                loaderManager.initLoader(ARTICLE_LOADER_ID, null, this)
            }
        } else {

            // Disables the swipe-refresh UI.
            swipeRefreshLayout.isRefreshing = false

            // Sets the toolbar's title to "Error".
            collapsingToolbar.title = getString(R.string.toolbar_error_title)

            // Updates the empty state view with a no-connection-error message while hiding the
            // RecyclerView.
            emptyTextView.setText(R.string.no_internet_connection)
            emptyTextView.visibility = View.VISIBLE
            articleRecyclerView.visibility = View.INVISIBLE
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
        uriBuilder.appendQueryParameter("source", newsSource)
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
            collapsingToolbar.title = getString(R.string.toolbar_error_title)

            // Updates the empty state view with a no-results-found message while hiding the
            // recycler view.
            emptyTextView.setText(R.string.no_results_found)
            emptyTextView.visibility = View.VISIBLE
            articleRecyclerView.visibility = View.INVISIBLE

            return
        }

        // Sets the toolbar's title to the respective news source name.
        when (newsSource) {
            getString(R.string.settings_news_sources_abc_news_au_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_abc_news_au_label)
            } getString(R.string.settings_news_sources_al_jazeera_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_al_jazeera_label)
            } getString(R.string.settings_news_sources_ars_technica_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_ars_technica_label)
            } getString(R.string.settings_news_sources_assoc_press_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_assoc_press_label)
            } getString(R.string.settings_news_sources_bbc_news_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_bbc_news_label)
            } getString(R.string.settings_news_sources_bloomberg_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_bloomberg_label)
            } getString(R.string.settings_news_sources_breitbart_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_breitbart_label)
            } getString(R.string.settings_news_sources_bus_insider_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_bus_insider_label)
            } getString(R.string.settings_news_sources_daily_mail_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_daily_mail_label)
            } getString(R.string.settings_news_sources_engadget_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_engadget_label)
            } getString(R.string.settings_news_sources_ent_weekly_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_ent_weekly_label)
            } getString(R.string.settings_news_sources_fin_times_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_fin_times_label)
            } getString(R.string.settings_news_sources_fortune_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_fortune_label)
            } getString(R.string.settings_news_sources_fourfourtwo_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_fourfourtwo_label)
            } getString(R.string.settings_news_sources_fox_sports_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_fox_sports_label)
            } getString(R.string.settings_news_sources_source_default_value) -> {
                collapsingToolbar.title = getString(R.string.google_news)
            } getString(R.string.settings_news_sources_ign_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_ign_label)
            } getString(R.string.settings_news_sources_mashable_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_mashable_label)
            } getString(R.string.settings_news_sources_metro_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_metro_label)
            } getString(R.string.settings_news_sources_mtv_news_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_mtv_news_label)
            } getString(R.string.settings_news_sources_nat_geographic_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_nat_geographic_label)
            } getString(R.string.settings_news_sources_ny_magazine_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_ny_magazine_label)
            } getString(R.string.settings_news_sources_nfl_news_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_nfl_news_label)
            } getString(R.string.settings_news_sources_reuters_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_reuters_label)
            } getString(R.string.settings_news_sources_talksport_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_talksport_label)
            } getString(R.string.settings_news_sources_techcrunch_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_techcrunch_label)
            } getString(R.string.settings_news_sources_techradar_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_techradar_label)
            } getString(R.string.settings_news_sources_guardian_uk_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_guardian_uk_label)
            } getString(R.string.settings_news_sources_the_hindu_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_the_hindu_label)
            } getString(R.string.settings_news_sources_the_lad_bible_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_the_lad_bible_label)
            } getString(R.string.settings_news_sources_the_nyt_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_the_nyt_label)
            } getString(R.string.settings_news_sources_wsj_value) -> {
                collapsingToolbar.title = getString(R.string.settings_news_sources_wsj_label)
            }
        }

        // Renders newly loaded data into the following Adapter.
        articleRecyclerView.adapter = ArticleAdapter(this, articles)

        // Renders the backdrop image accordingly should the news source not be National Geographic
        // (their images are too big to scale down). Otherwise, renders National Geographic's logo.
        if (newsSource != "national-geographic") {
            for (i in articles.indices) {
                val urlToImage = articles[i].urlToImage
                if (urlToImage!!.contains("http") || urlToImage!!.contains("https")) { // Custom way of validating News API's image URLs
                    Picasso.with(backdropImageView.context)
                            .load(urlToImage)
                            .into(backdropImageView)
                    break
                }
            }
        } else {
            backdropImageView.setImageResource(R.drawable.national_geo_logo)
        }

        // Hides the empty state view, and makes the RecyclerView visible should the data-fetching
        // process be successful.
        emptyTextView.visibility = View.INVISIBLE
        articleRecyclerView.visibility = View.VISIBLE

        // Disables the swipe-refresh UI.
        swipeRefreshLayout.isRefreshing = false
    }

    /**
     * Invoked when the app closes.
     */
    override fun onLoaderReset(loader: Loader<List<Article>>) {

        // "Clears" out the existing data since the loader resetted.
        articleRecyclerView.adapter = ArticleAdapter(this, mutableListOf<Article>())
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
