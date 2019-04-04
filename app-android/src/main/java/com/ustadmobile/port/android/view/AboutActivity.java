package com.ustadmobile.port.android.view;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;

import com.toughra.ustadmobile.R;
import com.ustadmobile.core.controller.AboutController;
import com.ustadmobile.core.view.AboutView;
import com.ustadmobile.port.android.util.UMAndroidUtil;

import static com.ustadmobile.port.android.util.UMAndroidUtil.bundleToMap;

public class AboutActivity extends UstadBaseActivity implements AboutView {

    private AboutController mAboutController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setUMToolbar(R.id.um_toolbar);
        setTitle(R.string.about);
        mAboutController = new AboutController(this,bundleToMap(getIntent().getExtras()),
                this);
        mAboutController.onCreate(null);
    }

    @Override
    public void setVersionInfo(final String versionInfo) {
        runOnUiThread(() -> ((TextView)findViewById(R.id.about_version_text)).setText(versionInfo));
    }

    @Override
    public void setAboutHTML(final String aboutHTML) {
        runOnUiThread(() -> ((WebView)findViewById(R.id.about_html))
                .loadData(aboutHTML, "text/html", "UTF-8"));
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_finish) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}