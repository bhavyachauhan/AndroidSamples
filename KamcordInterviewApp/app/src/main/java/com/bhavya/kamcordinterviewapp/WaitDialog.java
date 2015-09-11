package com.bhavya.kamcordinterviewapp;

import java.lang.ref.WeakReference;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

//Wait Dialog to disable user interaction while app is busy
public class WaitDialog extends DialogFragment {

    public static final String TAG = "WaitDialog";
    private static WaitDialog waitDialog;
    private static FragmentManager fragmentManager;
    private static Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialog);
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
        if(getDialog() != null){
            getDialog().setCancelable(false);
            getDialog().setCanceledOnTouchOutside(false);
            Window window = getDialog().getWindow();
            window.requestFeature(Window.FEATURE_NO_TITLE);
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }

        View view = inflater.inflate(R.layout.wait_dialog, container);
        return view;
    }

    private static WaitDialog getInstance() {
        if (waitDialog == null) {
            waitDialog = new WaitDialog();
        }
        return waitDialog;
    }

    public static void show(WeakReference<Context> context, FragmentManager fragmentManager) {
        if (context != null && fragmentManager.findFragmentByTag(TAG) == null) {
            WaitDialog.fragmentManager = fragmentManager;
            WaitDialog.context = context.get();
            DialogFragment waitDialog = WaitDialog.getInstance();
            waitDialog.show(fragmentManager, TAG);
        }
    }

    public static void hide() {
        if (context != null && fragmentManager.findFragmentByTag(TAG) != null) {
            if (fragmentManager.findFragmentByTag(TAG) != null) {
                DialogFragment waitDialog = WaitDialog.getInstance();
                waitDialog.dismiss();
            }
        }
    }
}
