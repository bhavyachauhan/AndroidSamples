package com.bhavya.kamcordinterviewapp;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

public class VideoDisplayDialogFragment extends DialogFragment {

    public static final String VIDEO_URL = "video_url";
    public static final String TAG = "VideoDisplayDialog";
    private VideoView videoView;
    private String url;
    private ProgressDialog progressDialog;
    private boolean closeClicked = false;

    public VideoDisplayDialogFragment() {
    }

    //Full screen dialog fragment
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(android.support.v4.app.DialogFragment.STYLE_NORMAL, R.style.FullScreenDialog);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.video_display_dialog, container);
        final Context context = getActivity();
        if (getDialog() != null) {
            getDialog().setCancelable(false);
            getDialog().setCanceledOnTouchOutside(false);
            Window window = getDialog().getWindow();
            window.requestFeature(Window.FEATURE_NO_TITLE);
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
        Bundle bundle = getArguments();
        if (bundle != null) {
            url = bundle.getString(VideoDisplayDialogFragment.VIDEO_URL);
        }

        videoView = (VideoView) view.findViewById(R.id.videoview);
        view.findViewById(R.id.close_button).setOnClickListener(closeOnClickListener);
        //Show progress dialog while video is loading
        progressDialog = new ProgressDialog(context);
        progressDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        progressDialog.setMessage("Loading Video...");
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
        progressDialog.show();
        progressDialog.getWindow().getDecorView().setSystemUiVisibility(
                getActivity().getWindow().getDecorView().getSystemUiVisibility());

        try {
            MediaController mediacontroller = new MediaController(context);
            mediacontroller.setAnchorView(videoView);
            Uri video = Uri.parse(url);
            videoView.setMediaController(mediacontroller);
            videoView.setVideoURI(video);

        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }

        videoView.requestFocus();
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                //Start video after its ready if user hasn't already clicked close
                if(!closeClicked) {
                    progressDialog.dismiss();
                    videoView.start();
                }
            }
        });

        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Toast.makeText(getActivity(), "Error loading video!", Toast.LENGTH_SHORT).show();
                closeDialog();
                return true;
            }
        });
        return view;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        videoView.stopPlayback();
        videoView.suspend();
        super.onDismiss(dialog);
    }

    public View.OnClickListener closeOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            closeClicked = true;
            closeDialog();
        }
    };

    public void closeDialog(){
        progressDialog.dismiss();
        if(videoView.isPlaying()) {
            videoView.stopPlayback();
        }
        MainActivity.dismissDialog(getActivity().getSupportFragmentManager(), TAG);
    }

}
