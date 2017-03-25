package com.davenotdavid.dndnews;

/**
 * Custom class that represents an article from a news source.
 */
public class Article {

    // String member fields of the class.
    private String mAuthor, mTitle, mDescription, mUrl, mUrlToImage, mPublishedAt;

    /**
     * Creates an {@link Article} object.
     *
     * @param author is the author of the article.
     * @param title is the title of the article.
     * @param description is the description of the article.
     * @param url is the URL of the article.
     * @param urlToImage is the image (thumbnail) URL of the article.
     * @param publishedAt is the date and time the article was published.
     */
    public Article(String author, String title, String description, String url, String urlToImage,
                   String publishedAt) {
        mAuthor = author;
        mTitle = title;
        mDescription = description;
        mUrl = url;
        mUrlToImage = urlToImage;
        mPublishedAt = publishedAt;
    }

    /**
     * Getter method for the article's author.
     */
    public String getAuthor() {
        return mAuthor;
    }

    /**
     * Getter method for the article's title.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Getter method for the article's description.
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * Getter method for the article's URL.
     */
    public String getUrl() {
        return mUrl;
    }

    /**
     * Getter method for the article's image (thumbnail) URL.
     */
    public String getUrlToImage() {
        return mUrlToImage;
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
                "mAuthor='" + mAuthor + '\'' +
                ", mTitle='" + mTitle + '\'' +
                ", mDescription='" + mDescription + '\'' +
                ", mUrl='" + mUrl + '\'' +
                ", mUrlToImage='" + mUrlToImage + '\'' +
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

        if (mAuthor != null ? !mAuthor.equals(article.mAuthor) : article.mAuthor != null)
            return false;
        if (mTitle != null ? !mTitle.equals(article.mTitle) : article.mTitle != null) return false;
        if (mDescription != null ? !mDescription.equals(article.mDescription) :
                article.mDescription != null)
            return false;
        if (mUrl != null ? !mUrl.equals(article.mUrl) : article.mUrl != null) return false;
        if (mUrlToImage != null ? !mUrlToImage.equals(article.mUrlToImage) :
                article.mUrlToImage != null)
            return false;
        return mPublishedAt != null ? mPublishedAt.equals(article.mPublishedAt) :
                article.mPublishedAt == null;

    }
}
