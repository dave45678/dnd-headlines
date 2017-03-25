package com.davenotdavid.dndheadlines;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Custom adapter, subclass of {@link ArrayAdapter}, that inflates views onto a ListView.
 */
public class ArticleAdapter extends ArrayAdapter<Article> {

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

            // Initializes the following child view for the sake of not initializing it repeatedly.
            holder = new ViewHolder();
            holder.articleTitle = (TextView) convertView.findViewById(R.id.article_title);

            // Associates the holder with the view for later lookup.
            convertView.setTag(holder);
        } else {

            // Otherwise, the view already exists so retrieve it.
            holder = (ViewHolder) convertView.getTag();
        }

        // Initializes the following Article being viewed at the moment.
        Article currentArticle = getItem(position);

        // Sets the article's title as text. Otherwise (which is unlikely, but better safe than
        // sorry!), "VOID" is setted.
        if (!currentArticle.getTitle().isEmpty()) {
            holder.articleTitle.setText(currentArticle.getTitle());
        } else {
            holder.articleTitle.setText(R.string.void_title);
        }

        return convertView;
    }

    // ViewHolder class used to hold the set of child views.
    private static class ViewHolder {
        TextView articleTitle;
    }
}
