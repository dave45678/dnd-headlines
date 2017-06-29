package com.davenotdavid.dndheadlines

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar

class ArticleViewActivity : AppCompatActivity() {

    // WebView field used for displaying a respective article source as well as its web
    // functionality.
    private var mWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article_view)

        val articleUrl = intent.getStringExtra("article_url")
        val articleTitle = intent.getStringExtra("article_title")
        supportActionBar!!.setTitle(articleTitle)
        supportActionBar!!.setSubtitle(articleUrl)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        mWebView = findViewById(R.id.webview) as WebView

        mWebView!!.settings.builtInZoomControls = true
        val progressBar = findViewById(R.id.progress_bar) as ProgressBar
        mWebView!!.setWebChromeClient(object : WebChromeClient() {
            override fun onProgressChanged(webView: WebView, progress: Int) {
                //Log.d(LOG_TAG, String.valueOf(progress));

                // Invoked once at most right when a new page is opened.
                if (webView.canGoBack()) {
                    progressBar.visibility = View.VISIBLE
                }

                // Updates the progress bar determinantly.
                progressBar.progress = progress

                // Hides the progress bar when it finishes loading.
                if (progress == 100) {
                    progressBar.visibility = View.GONE
                }
            }
        })
        mWebView!!.setWebViewClient(object : WebViewClient() {
            override fun onReceivedError(view: WebView, request: WebResourceRequest,
                                         error: WebResourceError) {
                super.onReceivedError(view, request, error)

                Log.e(LOG_TAG, error.toString())
            }
        })

        mWebView!!.loadUrl(articleUrl)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // Dismisses the Activity should the action bar's up/home button be pressed.
        if (item.itemId == android.R.id.home) {
            mWebView!!.clearCache(true) // Clears the app's cache

            finish()

            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {

        // The web page goes back in history only if there are stacks in the history. Otherwise,
        // dismisses the Activity.
        if (mWebView!!.canGoBack()) {
            mWebView!!.goBack()
        } else {
            mWebView!!.clearCache(true) // Clears the app's cache
            super.onBackPressed()
        }
    }

    companion object {

        // Log tag constant.
        private val LOG_TAG = ArticleViewActivity::class.java!!.getSimpleName()
    }
}
