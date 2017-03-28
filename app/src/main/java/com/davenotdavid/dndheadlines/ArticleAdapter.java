package com.davenotdavid.dndheadlines;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;

/**
 * Custom adapter, subclass of {@link ArrayAdapter}, that inflates views onto a ListView.
 */
public class ArticleAdapter extends ArrayAdapter<Article> {

    // Log tag constant.
    private static final String LOG_TAG = ArticleAdapter.class.getSimpleName();

    /**
     * Creates a {@link ArticleAdapter} object.
     *
     * @param context is the Activity context passed in.
     * @param articleList is the list of Articles passed in.
     */
    public ArticleAdapter(Context context, List<Article> articleList) {
        super(context, 0, articleList);
    }

    /**
     * Provides a view for an AdapterView (ListView, GridView, and etc.).
     *
     * @param position is the position in the list of data that should be displayed in the list
     *                 item view.
     * @param convertView is the recycled view to populate.
     * @param parent is the parent/root view group used for inflation.
     */
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        // Initializes the following to hold child views later on.
        ViewHolder holder;

        // Checks if the existing view is being reused, inflates the view otherwise.
        if (convertView == null) {
            convertView =
                    LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);

            // Initializes the following child views for the sake of not initializing them
            // repeatedly.
            holder = new ViewHolder();
            holder.articleTitle = (TextView) convertView.findViewById(R.id.article_title);
            holder.publishTimeAgo = (TextView) convertView.findViewById(R.id.publish_time_ago);

            // Associates the holder with the view for later lookup.
            convertView.setTag(holder);
        } else {

            // Otherwise, the view already exists so retrieve it.
            holder = (ViewHolder) convertView.getTag();
        }

        // Initializes the following Article being viewed at the moment.
        Article currentArticle = getItem(position);

        // Sets the article's title as text with ranks (top 10). Otherwise (which is unlikely, but
        // better safe than sorry!), "<VOID>" is set.
        String rankText = String.valueOf(position + 1) + ". ";
        StringBuilder titleSb = new StringBuilder();
        titleSb.append(rankText);
        if (!currentArticle.getTitle().isEmpty()) {
            titleSb.append(currentArticle.getTitle());
        } else {
            titleSb.append("<VOID>");
        }
        holder.articleTitle.setText(titleSb.toString());

        // Initializes the following reference variable to render a view of how long ago the
        // article was published as of now (user's locale time). Otherwise, hides the view.
        String articlePubDateTime = currentArticle.getPublishedAt();
        if (!articlePubDateTime.equals("null")) { // JSON apparently returns a null string

            // DateTimeFormatter initialized that's used to retrieve the UTC (4 hours faster than
            // EST) time in milliseconds of when the article was published.
            DateTimeFormatter formatter = DateTimeFormat
                    .forPattern("yyyy-MM-dd HH:mm:ss")
                    .withZoneUTC();
            long millisecondsSinceUnixEpoch =
                    formatter.parseMillis(formatUTCDateTime(articlePubDateTime));

            // Uses one of DateUtils' static methods to compare how long ago the article was
            // published (e.g. 37min ago, 5hours ago, and etc.).
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    millisecondsSinceUnixEpoch,
                    System.currentTimeMillis(),
                    MINUTE_IN_MILLIS); // Minimum time to be displayed (secs would constitute as "0min ago")

            holder.publishTimeAgo.setText(relativeTime.toString());
        } else {
            holder.publishTimeAgo.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }

    // ViewHolder class used to hold the set of child views.
    private static class ViewHolder {
        TextView articleTitle;
        TextView publishTimeAgo;
    }

    /**
     * Modifies the UTC date-time by removing the 'T' and 'Z' chars, and retrieving only the first
     * two chars of seconds (in case of partial seconds such as decimals) in order to satisfy
     * DateTimeFormat's format.
     *
     * @param utcDateTime is the UTC date-time according to ISO 8601 standards.
     */
    private String formatUTCDateTime(String utcDateTime) {
        StringBuilder dateTimeSb = new StringBuilder(utcDateTime);
        dateTimeSb.deleteCharAt(utcDateTime.length() - 1);
        String dateTime = dateTimeSb.toString();
        String formattedDateTime = dateTime.replace('T', ' ');
        String[] dateTimeParts = formattedDateTime.split(" ");
        String[] timeParts = dateTimeParts[1].split(":");
        String time = timeParts[0] + ":" +
                timeParts[1] + ":" +
                String.valueOf(timeParts[2].charAt(0)) +
                String.valueOf(timeParts[2].charAt(1));
        return dateTimeParts[0] + " " + time;
    }
}
