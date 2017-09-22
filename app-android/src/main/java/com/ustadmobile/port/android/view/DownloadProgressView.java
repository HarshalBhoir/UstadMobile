package com.ustadmobile.port.android.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.toughra.ustadmobile.R;

/**
 * Created by mike on 9/22/17.
 */
public class DownloadProgressView extends LinearLayout {

    private ProgressBar progressBar;

    private TextView downloadPercentageTextView;

    private TextView downloadStatusTextView;

    public DownloadProgressView(Context context) {
        super(context);
        init();
    }

    public DownloadProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DownloadProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        inflate(getContext(), R.layout.view_download_progress, this);
        progressBar = findViewById(R.id.view_download_progress_progressbar);
        downloadPercentageTextView = findViewById(R.id.view_download_progress_status_percentage_text);
        downloadStatusTextView = findViewById(R.id.view_download_progress_status_text);
    }

    public float getProgress() {
        return (float)progressBar.getProgress() / 100f;
    }

    public void setProgress(float progress) {
        int progressPercentage = Math.round(progress * 100);
        progressBar.setProgress(progressPercentage);
        downloadPercentageTextView.setText(progressPercentage + "%");
    }

    public void setStatusText(String statusText) {
        downloadStatusTextView.setText(statusText);
    }

    public String getStatusText() {
        return downloadStatusTextView.getText().toString();
    }

}
