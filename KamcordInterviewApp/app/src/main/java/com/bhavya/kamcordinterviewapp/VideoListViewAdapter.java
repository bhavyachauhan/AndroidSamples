package com.bhavya.kamcordinterviewapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class VideoListViewAdapter extends BaseAdapter {

    final List<VideoListItem> videoList;
    final WeakReference<Activity> activityWeakReference;
    final ThumbnailOnClickListener mListener;
    ThumbnailDownloaderTask downloadThumbnailImageTask;
    boolean cancelled;

    public VideoListViewAdapter(final WeakReference<Activity> activityWeakReference,
                                final List<VideoListItem> videoList,
                                final ThumbnailOnClickListener mListener) {
        this.activityWeakReference = activityWeakReference;
        this.videoList = videoList;
        this.mListener = mListener;
    }

    @Override
    public int getCount() {
        return videoList.size();
    }

    @Override
    public VideoListItem getItem(int position) {
        return videoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (activityWeakReference != null) {
            final Activity activity = activityWeakReference.get();
            cancelled = false;

            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) activity
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_item, null);
                holder = new ViewHolder();
                holder.thumbnailImageView = (ImageView) convertView
                        .findViewById(R.id.thumbnail_imageview);
                holder.titleTextView = (TextView) convertView
                        .findViewById(R.id.video_title_textview);
                holder.progressBar = (ProgressBar) convertView.findViewById(R.id.wait_bar);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final VideoListItem video = videoList.get(position);
            holder.titleTextView.setText(video.getTitle()); //Set title

            if (holder.thumbnailImageView != null) {
                if(video.getBitmap() == null) {
                    holder.progressBar.setVisibility(View.VISIBLE);

                    //Cancel current and start new download for this imageview
                    if(cancelPotentialDownload(video.getThumbnailUrl(), holder.thumbnailImageView
                            , holder.progressBar)) {
                        downloadThumbnailImageTask = new ThumbnailDownloaderTask(
                                new WeakReference<>(holder.thumbnailImageView),
                                new WeakReference<>(holder.progressBar), position);
                        DownloadedThumbnail downloadedThumbnail =
                                new DownloadedThumbnail(downloadThumbnailImageTask);
                        holder.thumbnailImageView.setImageDrawable(downloadedThumbnail);
                        downloadThumbnailImageTask.execute();
                    }
                } else {
                    //If thumbnail was already downloaded earlier
                    holder.thumbnailImageView.setImageBitmap(video.getBitmap());
                    holder.progressBar.setVisibility(View.GONE);
                }
            }

            //Listener to open video for the clicked item in the list
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.OnThumbnailClicked(video.getVideoUrl());
                }
            });

        }
        return convertView;
    }

    //Cancel task if "Next Page is clicked in the middle of the downloader task"
    public void cancelThumbnailDownload(){
        if(downloadThumbnailImageTask != null){
            downloadThumbnailImageTask.cancel(true);
        }
        cancelled = true;
    }

    //Cancel current task if list is scrolled and need to download new thumbnail for the imageview
    private boolean cancelPotentialDownload(String url, ImageView imageView, ProgressBar progressBar) {
        ThumbnailDownloaderTask thumbnailDownloaderTask = getThumbnailDownloaderTask(imageView);

        if (thumbnailDownloaderTask != null) {
            String bitmapUrl = thumbnailDownloaderTask.url;
            if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
                thumbnailDownloaderTask.cancel(true);
                progressBar.setVisibility(View.GONE);
                cancelled = true;
            } else {
                //Don't cancel if need to download the same thumbnail
                return false;
            }
        }
        return true;
    }

    //Get ThumnailDownloaderTask associated with the ImageView
    private static ThumbnailDownloaderTask getThumbnailDownloaderTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedThumbnail) {
                DownloadedThumbnail downloadedDrawable = (DownloadedThumbnail)drawable;
                return downloadedDrawable.getThumbnailDownloaderTask();
            }
        }
        return null;
    }

    public interface ThumbnailOnClickListener {
        void OnThumbnailClicked(String url);
    }

    class DownloadedThumbnail extends ColorDrawable {
        private final WeakReference<ThumbnailDownloaderTask> bitmapDownloaderTaskReference;

        public DownloadedThumbnail(ThumbnailDownloaderTask downloadThumbnailImageTask) {
            super(Color.GRAY);
            bitmapDownloaderTaskReference =
                    new WeakReference<>(downloadThumbnailImageTask);
        }

        public ThumbnailDownloaderTask getThumbnailDownloaderTask() {
            return bitmapDownloaderTaskReference.get();
        }
    }

    private class ThumbnailDownloaderTask extends AsyncTask<Void, Void, Bitmap> {

        final WeakReference<ImageView> thumbnailImageViewReference;
        final WeakReference<ProgressBar> progressBarReference;
        final int position;
        final String url;

        public ThumbnailDownloaderTask(final WeakReference<ImageView> thumbnailImageViewReference,
                                       final WeakReference<ProgressBar> progressBarReference,
                                       final int position) {
            this.thumbnailImageViewReference = thumbnailImageViewReference;
            this.progressBarReference = progressBarReference;
            this.position = position;
            this.url = videoList.get(position).getThumbnailUrl();
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            return downloadThumbnailImage(url);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (thumbnailImageViewReference != null && !cancelled) {
                getItem(position).setBitmap(bitmap);
                progressBarReference.get().setVisibility(View.GONE);
                ImageView thumbnailImageView = thumbnailImageViewReference.get();
                ThumbnailDownloaderTask thumbnailDownloaderTask = getThumbnailDownloaderTask(thumbnailImageView);
                // Set thumbnail if this process is task is associated with that imageview
                if (this == thumbnailDownloaderTask) {
                    thumbnailImageView.setImageBitmap(bitmap);
                }
            }
        }

        private Bitmap downloadThumbnailImage(String url) {
            final URL thumbnailUrl;
            Bitmap thumbnailImage = null;
            try {
                thumbnailUrl = new URL(url);
                HttpURLConnection urlConnection = (HttpURLConnection) thumbnailUrl.openConnection();
                if (urlConnection.getResponseCode() == 200) {
                    thumbnailImage = BitmapFactory.decodeStream(urlConnection.getInputStream());
                }
                if(isCancelled()){
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return thumbnailImage;
        }

    }

    private class ViewHolder {
        ImageView thumbnailImageView;
        TextView titleTextView;
        ProgressBar progressBar;
    }
}
