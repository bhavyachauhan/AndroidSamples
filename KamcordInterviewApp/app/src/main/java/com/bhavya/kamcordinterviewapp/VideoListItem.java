package com.bhavya.kamcordinterviewapp;

import android.graphics.Bitmap;
import android.widget.ImageView;

public class VideoListItem {
    private final String videoId;
    private final String videoUrl;
    private final String thumbnailUrl;
    private final String title;
    private Bitmap bitmap;

    public VideoListItem(final String videoId, final String videoUrl, final String thumbnailUrl, final String title) {
        this.videoId = videoId;
        this.thumbnailUrl = thumbnailUrl;
        this.title = title;
        this.videoUrl = videoUrl;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public String getVideoId() {
        return videoId;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getTitle() {
        return title;
    }
}
