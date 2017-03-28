package com.davenotdavid.dndheadlines;

/**
 * Custom class that represents an article from a news source.
 */
public class Article {

    // String member fields of the class.
    private String mTitle, mUrl, mPublishedAt;

    /**
     * Creates an {@link Article} object.
     *
     * @param title is the title of the article.
     * @param url is the URL of the article.
     * @param publishedAt is the date and time the article was published.
     */
    public Article(String title, String url, String publishedAt) {
        mTitle = title;
        mUrl = url;
        mPublishedAt = publishedAt;
    }

    /**
     * Getter method for the article's title.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Getter method for the article's URL.
     */
    public String getUrl() {
        return mUrl;
    }

    /**
     * Getter method for the article's publish date/time.
     */
    public String getPublishedAt() {
        return mPublishedAt;
    }

    /**
     * Creates a String representation of {@link Article} to return.
     */
    @Override
    public String toString() {
        return "Article{" +
                "mTitle='" + mTitle + '\'' +
                ", mUrl='" + mUrl + '\'' +
                ", mPublishedAt='" + mPublishedAt + '\'' +
                '}';
    }

    /**
     * Compares two objects to see if they're both an instance of {@link Article} or not.
     *
     * @param o is the other object being compared.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Article article = (Article) o;

        if (mTitle != null ? !mTitle.equals(article.mTitle) : article.mTitle != null) return false;
        if (mUrl != null ? !mUrl.equals(article.mUrl) : article.mUrl != null) return false;
        return mPublishedAt != null ? mPublishedAt.equals(article.mPublishedAt) :
                article.mPublishedAt == null;

    }
}
