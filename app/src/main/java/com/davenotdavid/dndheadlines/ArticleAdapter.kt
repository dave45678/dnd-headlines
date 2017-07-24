package com.davenotdavid.dndheadlines

import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import org.joda.time.format.DateTimeFormat

import android.text.format.DateUtils.MINUTE_IN_MILLIS

/**
 * Adapter subclass of [RecyclerView] that renders a list of Article data that's clickable via a
 * custom interface.
 */
class ArticleAdapter(private val mListItemClickListener: ListItemClickListener,
                     private val mArticleData: List<Article>) :
        RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder>() {

    // Log tag constant.
    private val LOG_TAG = ArticleAdapter::class.java.simpleName

    // Interface for the list items that are clickable.
    interface ListItemClickListener {
        fun onListItemClick(article: Article)
    }

    override fun getItemCount(): Int {
        return mArticleData.size
    }

    /**
     * Inflates a layout for the list items.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ArticleViewHolder(inflater.inflate(R.layout.list_item, parent, false))
    }

    /**
     * Binds each view from the ViewHolder onto the list item layout, and ultimately the
     * RecyclerView.
     */
    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {

        // Initializes the following Article being viewed at the moment.
        val currentArticle = mArticleData[position]

        // Sets the article's title as text with ranks (top 10 at most). Otherwise (which is
        // unlikely, but better safe than sorry!), "<VOID>" is set.
        val rankText = (position + 1).toString() + ". "
        val titleSb = StringBuilder()
        titleSb.append(rankText)
        if (currentArticle.title != null) {
            titleSb.append(currentArticle.title)
        } else {
            titleSb.append("<VOID>")
        }
        holder.mArticleTitleTV.text = titleSb.toString()

        // Initializes the following reference variable to render a view of how long ago the
        // article was published as of now (user's locale time). Otherwise, hides the view.
        val articlePubDateTime = currentArticle.publishedAt
        if (articlePubDateTime != null) {

            // Tries to format the publish time properly prior to setting it as text. Otherwise,
            // catches an exception and sets the text invisible.
            try {

                // DateTimeFormatter initialized that's used to retrieve the UTC (e.g. 4 hours
                // faster than EST) time in milliseconds of when the article was published.
                val formatter = DateTimeFormat
                        .forPattern("yyyy-MM-dd HH:mm:ss")
                        .withZoneUTC()
                val millisecondsSinceUnixEpoch =
                        formatter.parseMillis(formatUTCDateTime(articlePubDateTime))

                // Uses one of DateUtils' static methods to compare how long ago the article was
                // published (e.g. 37 minutes ago, 5 hours ago, and etc.).
                val relativeTime = DateUtils.getRelativeTimeSpanString(
                        millisecondsSinceUnixEpoch,
                        System.currentTimeMillis(),
                        MINUTE_IN_MILLIS) // Minimum time to be displayed (secs would constitute as "0min ago")

                // Initially converts relativeTime to a String to possibly set the following
                // TextView as "just now!". Or, the TextView will be hidden if the String is
                // something like "In 5 min" which sometimes appears due to a back-end bug.
                // Otherwise, sets the publish time as is.
                val relativeTimeString = relativeTime.toString()
                if (relativeTimeString == "0 minutes ago") {
                    holder.mPublishTimeTV.setText(R.string.just_now_text)
                } else if (relativeTimeString[0] == 'I' && relativeTimeString[1] == 'n') {
                    holder.mPublishTimeTV.visibility = View.INVISIBLE
                } else {
                    holder.mPublishTimeTV.text = relativeTimeString
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e(LOG_TAG, "Problem formatting time string", e)

                holder.mPublishTimeTV.visibility = View.INVISIBLE
            }
        } else {
            holder.mPublishTimeTV.visibility = View.INVISIBLE
        }
    }

    /**
     * RecyclerView's ViewHolder class.
     */
    inner class ArticleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnClickListener {
        val mArticleTitleTV: TextView = itemView.findViewById(R.id.article_title) as TextView
        val mPublishTimeTV: TextView = itemView.findViewById(R.id.publish_time_ago) as TextView

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            mListItemClickListener.onListItemClick(mArticleData[adapterPosition])
        }
    }

    /**
     * Modifies the UTC date-time by removing the 'T' and 'Z' chars, and retrieving only the first
     * two chars of seconds (in case of partial seconds such as decimals) in order to satisfy
     * DateTimeFormat's format.
     *
     * @param utcDateTime is the UTC date-time according to ISO 8601 standards.
     *
     * @throws [IndexOutOfBoundsException] since some custom indexing will be out of bounds of the
     *                                     # of time-attribute parts.
     */
    @Throws(IndexOutOfBoundsException::class)
    private fun formatUTCDateTime(utcDateTime: String?): String {
        val dateTimeSb = StringBuilder(utcDateTime)
        dateTimeSb.deleteCharAt(utcDateTime!!.length - 1)
        val dateTime = dateTimeSb.toString()
        val formattedDateTime = dateTime.replace('T', ' ')
        val dateTimeParts = formattedDateTime.split(" ".toRegex()).dropLastWhile {
            it.isEmpty()
        }.toTypedArray()
        val timeParts = dateTimeParts[1].split(":".toRegex()).dropLastWhile {
            it.isEmpty()
        }.toTypedArray()
        val time = timeParts[0] + ":" +
                timeParts[1] + ":" +
                timeParts[2][0].toString() +
                timeParts[2][1].toString()

        return dateTimeParts[0] + " " + time
    }
}