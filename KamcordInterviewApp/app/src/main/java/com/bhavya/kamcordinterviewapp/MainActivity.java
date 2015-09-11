package com.bhavya.kamcordinterviewapp;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements VideoListViewAdapter.ThumbnailOnClickListener{

    public static final String FEED_URL = "https://app.kamcord.com/app/v3/feeds/featured_feed";
    public static final String TAG = "MyLOGTAG";
    List<VideoListItem> videoList = new ArrayList<>();
    ListView videoListView;
    VideoListViewAdapter mAdapter;
    TextView nextPageButton;
    Activity mActivity;
    NetworkTask networkTask;
    String nextPage = "";
    String currPage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setImmersiveMode();
        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    setImmersiveMode();
                }
            }
        });
        setContentView(R.layout.activity_main);
        mActivity = this;
        nextPageButton = (TextView) findViewById(R.id.next_page_button);
        nextPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nextPage != null) {
                    nextPageButton.setEnabled(false); //Disabling the button till next page is loaded

                    //Cancel loading video list if "Next Page" clicked in the middle of the task
                    if(networkTask != null){
                        networkTask.cancel(true);
                        if(mAdapter != null){
                            mAdapter.cancelThumbnailDownload();
                        }
                    }
                    loadPage(nextPage);
                }
            }
        });

        nextPageButton.setEnabled(false);
        videoListView = (ListView) this.findViewById(R.id.video_listview);
        mAdapter = new VideoListViewAdapter
                (new WeakReference<>(mActivity), videoList, this);
        videoListView.setAdapter(mAdapter);
        loadPage(currPage);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setImmersiveMode();
        //Refresh current page
        loadPage(currPage);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Stop Video and close the DialogFragment if open
        MainActivity.dismissDialog(getSupportFragmentManager(), VideoDisplayDialogFragment.TAG);
    }

    private void loadPage(String page) {
        videoList.clear();
        mAdapter.notifyDataSetChanged();
        StringBuilder url = new StringBuilder(MainActivity.FEED_URL);
        if (!page.equals("")) {
            url.append("?page=");
            url.append(page);
        }
        networkTask = new NetworkTask(new WeakReference<>(mActivity),
                url.toString(), mAdapter);
        WaitDialog.show(new WeakReference<Context>(this), getSupportFragmentManager());
        networkTask.execute();
    }

    public static void dismissDialog(FragmentManager fragmentManager, String tag){
        DialogFragment dialogFragment = (DialogFragment) fragmentManager.findFragmentByTag(tag);
        if(dialogFragment != null){
            dialogFragment.dismiss();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus){
            setImmersiveMode();
        }
    }

    @Override
    public void OnThumbnailClicked(String url) {
        VideoDisplayDialogFragment videoDisplayDialogFragment = new VideoDisplayDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(VideoDisplayDialogFragment.VIDEO_URL, url);
        videoDisplayDialogFragment.setArguments(bundle);
        videoDisplayDialogFragment.show(getSupportFragmentManager(), VideoDisplayDialogFragment.TAG);
    }

    private void setImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    //AsynTask to load the Video List
    public class NetworkTask extends AsyncTask<Void, Void, String> {

        final WeakReference<Activity> activityWeakReference;
        final String stringUrl;
        final VideoListViewAdapter mAdapter;

        public NetworkTask(final WeakReference<Activity> activityWeakReference,
                           final String stringUrl, final VideoListViewAdapter mAdapter) {
            this.activityWeakReference = activityWeakReference;
            this.stringUrl = stringUrl;
            this.mAdapter = mAdapter;
        }

        @Override
        protected String doInBackground(Void... params) {
            if (activityWeakReference != null) {
                return getResponse(stringUrl);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            try {
                JSONObject jsonResponse = new JSONObject(response);
                JSONObject jsonObject = jsonResponse.getJSONObject("response")
                        .getJSONObject("video_list");
                currPage = nextPage;
                nextPage = jsonObject.getString("next_page");
                //Enable "Next Page" button if there is a next page
                if (activityWeakReference != null && nextPageButton != null) {
                    activityWeakReference.get().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (nextPage == null) {
                                nextPageButton.setVisibility(View.INVISIBLE);
                                nextPageButton.setEnabled(false);
                            } else {
                                nextPageButton.setVisibility(View.VISIBLE);
                                nextPageButton.setEnabled(true);
                            }
                        }
                    });
                }
                loadListItems(jsonObject);
            } catch (JSONException e) {
                Log.e(TAG, "Error in parsing JSON: " + e);
            }
        }

        private String getResponse(String url) {
            HttpURLConnection urlConnection = null;
            StringBuilder response = new StringBuilder();
            try {
                URL feedUrl = new URL(url);
                urlConnection = (HttpURLConnection) feedUrl.openConnection();
                urlConnection.setRequestProperty("device-token", "ANYSTRING");
                if (urlConnection.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(urlConnection.getInputStream()));
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    Log.e(TAG, response.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return response.toString();
        }

        private void loadListItems(final JSONObject jsonObject) {
            if (activityWeakReference != null) {
                final Activity activity = activityWeakReference.get();
                try {
                    JSONArray videoListArray = jsonObject.getJSONArray("video_list");
                    if (videoListArray.length() == 0) {
                        WaitDialog.hide();
                    } else {
                        for (int i = 0; i < videoListArray.length(); i++) {
                            JSONObject videoListItem = videoListArray.getJSONObject(i);
                            final String videoId = videoListItem.getString("video_id");
                            final String title = videoListItem.getString("title");
                            final String thumbnailUrl = videoListItem.getJSONObject("thumbnails")
                                    .getString("regular");
                            final String videoUrl = videoListItem.getString("video_url");
                            VideoListItem object = new VideoListItem(videoId, videoUrl,
                                    thumbnailUrl, title);
                            //Exit if task is cancelled when "Next Page" is clicked while loading
                            // the video list for current page
                            if(isCancelled()){
                                break;
                            }
                            videoList.add(object);
                            //Update adapter as next item in list becomes available
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    WaitDialog.hide();
                                    mAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error in parsing JSON: " + e);
                }
            }
        }
    }
}
