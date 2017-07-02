package com.davenotdavid.dndheadlines

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_source_view.*

/**
 * This Activity is responsible for displaying a WebView of the respective source.
 */
class SourceViewActivity : AppCompatActivity() {

    // Log tag constant.
    private val LOG_TAG = SourceViewActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_source_view)

        // Sets the custom toolbar to act as the Activity's action bar.
        setSupportActionBar(toolbar)

        // Sets up a up/home button for the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Allows the WebView to be zoomable.
        web_view.settings.builtInZoomControls = true

        // Addresses things that may impact browser UI, particularly progress updates of the URL's
        // load time.
        web_view.setWebChromeClient(object : WebChromeClient() {
            override fun onProgressChanged(webView: WebView, progress: Int) {
                toolbar.setTitle(webView.title)
                toolbar.setSubtitle(webView.url)

                // Invoked once at most right when a new page is opened.
                if (webView.canGoBack()) {
                    progress_bar.visibility = View.VISIBLE
                }

                // Updates the progress bar determinantly.
                progress_bar.progress = progress

                // Runs the following functionality when the web page finishes loading.
                if (progress == 100) {

                    // Hides the progress bar.
                    progress_bar.visibility = View.GONE

                    // Scrolls the NestedScrollView (that consists of the WebView) to the top.
                    nested_scroll_view.scrollTo(0, 0)
                }
            }
        })

        // Addresses things that may impact the rendering of content, particularly web errors.
        web_view.setWebViewClient(object : WebViewClient() {
            override fun onReceivedError(webView: WebView, request: WebResourceRequest,
                                         error: WebResourceError) {
                super.onReceivedError(webView, request, error)

                // Logs the web error.
                Log.e(LOG_TAG, error.toString())
            }
        })

        // Finally loads the source's URL to the WebView.
        web_view.loadUrl(intent.getStringExtra("source_url"))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.webview_functionality, menu) // Inflates the WebView menu file
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // Dismisses the Activity should the action bar's up/home button be pressed.
        if (item.itemId == android.R.id.home) {
            web_view.clearCache(true) // Clears the app's cache

            finish()

        // Reloads the web page.
        } else if (item.itemId == R.id.action_reload) {
            web_view.reload()

        // The WebView goes back in history, but otherwise displays a Toast if it's not possible.
        } else if (item.itemId == R.id.action_back) {
            if (web_view.canGoBack()) {
                web_view.goBack()
            } else {
                Toast.makeText(this, getString(R.string.no_back_history_toast),
                        Toast.LENGTH_SHORT).show()
            }

        // The WebView goes forward in history, but otherwise displays a Toast if it's not possible.
        } else if (item.itemId == R.id.action_forward) {
            if (web_view.canGoForward()) {
                web_view.goForward()
            } else {
                Toast.makeText(this, getString(R.string.no_forward_history_toast),
                        Toast.LENGTH_SHORT).show()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {

        // The web page goes back in history only if there are stacks in the history. Otherwise,
        // dismisses the Activity.
        if (web_view.canGoBack()) {
            web_view.goBack()
        } else {
            web_view.clearCache(true) // Clears the app's cache
            super.onBackPressed()
        }
    }
}
