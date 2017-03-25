package com.davenotdavid.dndheadlines;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import java.util.List;

/**
 * A subclass of {@link AsyncTaskLoader} that loads a list of articles by using an AsyncTask to
 * perform the network request to the given URL.
 */
public class ArticleLoader extends AsyncTaskLoader<List<Article>> {

    // Log tag constant.
    private static final String LOG_TAG = ArticleLoader.class.getSimpleName();

    // URL used for querying in a server.
    private static String mRequestUrl;

    /**
     * Creates a new {@link ArticleLoader} object.
     *
     * @param context is the passed-in Activity's context.
     * @param requestUrl is the passed-in request URL.
     */
    public ArticleLoader(Context context, String requestUrl) {
        super(context);
        mRequestUrl = requestUrl;
    }

    @Override
    protected void onStartLoading() {
        Log.d(LOG_TAG, "onStartLoading()");

        forceLoad();
    }

    @Override
    public List<Article> loadInBackground() {
        Log.d(LOG_TAG, "loadInBackground()");

        if (mRequestUrl == null) return null;

        // Performs the network request, parses the response, and extracts a list of articles to
        // return.
        return QueryUtils.fetchArticleData(mRequestUrl);
    }
}
