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
        webView.settings.builtInZoomControls = true

        // Addresses things that may impact browser UI, particularly progress updates of the URL's
        // load time.
        webView.setWebChromeClient(object : WebChromeClient() {
            override fun onProgressChanged(webView: WebView, progress: Int) {
                toolbar.setTitle(webView.title)
                toolbar.setSubtitle(webView.url)

                // Invoked once at most right when a new page is opened.
                if (webView.canGoBack()) {
                    progressBar.visibility = View.VISIBLE
                }

                // Updates the progress bar determinantly.
                progressBar.progress = progress

                // Runs the following functionality when the web page finishes loading.
                if (progress == 100) {

                    // Hides the progress bar.
                    progressBar.visibility = View.GONE

                    // Scrolls the NestedScrollView (that consists of the WebView) to the top.
                    nestedScrollView.scrollTo(0, 0)
                }
            }
        })

        // Addresses things that may impact the rendering of content, particularly web errors.
        webView.setWebViewClient(object : WebViewClient() {
            override fun onReceivedError(webView: WebView, request: WebResourceRequest,
                                         error: WebResourceError) {
                super.onReceivedError(webView, request, error)

                // Logs the web error.
                Log.e(LOG_TAG, error.toString())
            }
        })

        // Finally loads the source's URL to the WebView.
        webView.loadUrl(intent.getStringExtra("source_url"))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.webview_functionality, menu) // Inflates the WebView menu file
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // Dismisses the Activity should the action bar's up/home button be pressed.
        if (item.itemId == android.R.id.home) {
            webView.clearCache(true) // Clears the app's cache

            finish()

            return true

        // Reloads the web page.
        } else if (item.itemId == R.id.actionReload) {
            webView.reload()

            return true

        // The WebView goes back in history, but otherwise displays a Toast if it's not possible.
        } else if (item.itemId == R.id.actionBack) {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                Toast.makeText(this, getString(R.string.no_back_history_toast),
                        Toast.LENGTH_SHORT).show()
            }

            return true

        // The WebView goes forward in history, but otherwise displays a Toast if it's not possible.
        } else if (item.itemId == R.id.actionForward) {
            if (webView.canGoForward()) {
                webView.goForward()
            } else {
                Toast.makeText(this, getString(R.string.no_forward_history_toast),
                        Toast.LENGTH_SHORT).show()
            }

            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {

        // The web page goes back in history only if there are stacks in the history. Otherwise,
        // dismisses the Activity.
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            webView.clearCache(true) // Clears the app's cache
            super.onBackPressed()
        }
    }
}
