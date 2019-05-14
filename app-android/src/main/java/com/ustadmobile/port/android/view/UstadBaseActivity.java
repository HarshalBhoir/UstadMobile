package com.ustadmobile.port.android.view;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.squareup.seismic.ShakeDetector;
import com.toughra.ustadmobile.R;
import com.ustadmobile.core.controller.UstadBaseController;
import com.ustadmobile.core.impl.AppConfig;
import com.ustadmobile.core.impl.UMLog;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;
import com.ustadmobile.core.view.UstadViewWithNotifications;
import com.ustadmobile.core.view.ViewWithErrorNotifier;
import com.ustadmobile.port.android.impl.UserFeedbackException;
import com.ustadmobile.port.android.netwokmanager.NetworkManagerBleAndroidService;
import com.ustadmobile.port.android.netwokmanager.UmAppDatabaseSyncService;
import com.ustadmobile.port.sharedse.networkmanager.NetworkManagerBle;
import com.ustadmobile.port.sharedse.util.RunnableQueue;

import org.acra.ACRA;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.ustadmobile.core.impl.UstadMobileSystemImpl.*;
import static com.ustadmobile.core.impl.UstadMobileSystemImpl.getInstance;

/**
 * Base activity to handle interacting with UstadMobileSystemImpl
 * <p>
 * Created by mike on 10/15/15.
 */
public abstract class UstadBaseActivity extends AppCompatActivity implements ServiceConnection,
        UstadViewWithNotifications, ViewWithErrorNotifier, ShakeDetector.Listener {

    private UstadBaseController baseController;

    protected Toolbar umToolbar;

    /**
     * Currently running instance of NetworkManagerBle
     */
    protected NetworkManagerBle networkManagerBle;

    private List<WeakReference<Fragment>> fragmentList;

    private boolean localeChanged = false;

    private String localeOnCreate = null;

    private boolean isStarted = false;

    private static final int RUN_TIME_REQUEST_CODE = 111;

    private boolean permissionRequestRationalesShown = false;

    private Runnable afterPermissionMethodRunner;

    private RunnableQueue runWhenServiceConnectedQueue = new RunnableQueue();

    private String permissionDialogTitle;

    private String permissionDialogMessage;

    private String permission;


    private ServiceConnection mSyncServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mSyncServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mSyncServiceBound = false;
        }
    };

    /**
     * Ble service connection
     */
    private ServiceConnection bleServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            networkManagerBle = ((NetworkManagerBleAndroidService.LocalServiceBinder) service)
                    .getService().getNetworkManagerBle();
            bleServiceBound = true;
            onBleNetworkServiceBound(networkManagerBle);
            runWhenServiceConnectedQueue.setReady(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bleServiceBound = false;
            onBleNetworkServiceUnbound();
        }
    };

    private boolean mSyncServiceBound = false;

    private volatile boolean bleServiceBound = false;
    private ShakeDetector shakeDetector;
    private SensorManager sensorManager;
    boolean feedbackDialogVisible = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //bind to the LRS forwarding service
        getInstance().handleActivityCreate(this, savedInstanceState);
        fragmentList = new ArrayList<>();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_LOCALE_CHANGE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocaleChangeBroadcastReceiver,
                intentFilter);
        super.onCreate(savedInstanceState);
        localeOnCreate = getInstance().getDisplayedLocale(this);


        Intent syncServiceIntent = new Intent(this, UmAppDatabaseSyncService.class);
        bindService(syncServiceIntent, mSyncServiceConnection,
                Context.BIND_AUTO_CREATE | Context.BIND_ADJUST_WITH_ACTIVITY);

        //bind ble service
        Intent bleServiceIntent = new Intent(this, NetworkManagerBleAndroidService.class);
        bindService(bleServiceIntent, bleServiceConnection,
                Context.BIND_AUTO_CREATE | Context.BIND_ADJUST_WITH_ACTIVITY);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        shakeDetector = new ShakeDetector(this);

    }

    @Override
    public void hearShake() {

        if (feedbackDialogVisible) {
            return;
        }

        feedbackDialogVisible = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.send_feedback);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.view_feedback_layout, null);
        EditText editText = dialogView.findViewById(R.id.feedback_edit_comment);
        builder.setView(dialogView);
        builder.setPositiveButton(R.string.send, (dialogInterface, whichButton) -> {
            ACRA.getErrorReporter().handleSilentException(new UserFeedbackException(editText.getText().toString()));
            Toast.makeText((Context) getContext(), R.string.feedback_thanks, Toast.LENGTH_LONG).show();
            dialogInterface.cancel();
        });
        builder.setNegativeButton(R.string.cancel, ((dialogInterface, i) -> dialogInterface.cancel()));
        builder.setOnDismissListener(dialogInterface -> feedbackDialogVisible = false);
        builder.setOnCancelListener(dialogInterface -> feedbackDialogVisible = false);
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    /**
     * Display the snackbar at the bottom of the page
     *
     * @param errorMessage    message for the snackbar
     * @param actionMessageId id of action name
     * @param action          action listener
     */
    public void showErrorNotification(String errorMessage, Runnable action, int actionMessageId) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), errorMessage, Snackbar.LENGTH_LONG);
        if (action != null) {
            UstadMobileSystemImpl impl = getInstance();
            snackbar.setAction(impl.getString(actionMessageId, getContext()), view -> action.run());
            snackbar.setActionTextColor(ContextCompat.getColor((Context) getContext(), R.color.accent));
        }
        snackbar.show();
    }

    /**
     * All activities descending from UstadBaseActivity bind to the network manager. This method
     * can be overriden when presenters need to use a reference to the networkmanager.
     *
     * @param networkManagerBle
     */
    protected void onBleNetworkServiceBound(NetworkManagerBle networkManagerBle) {
    }

    protected void onBleNetworkServiceUnbound() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (localeChanged) {
            if (getInstance().hasDisplayedLocaleChanged(localeOnCreate, this)) {
                new Handler().postDelayed(this::recreate, 200);
            }
        }
        if (shakeDetector != null && sensorManager != null) {
            shakeDetector.start(sensorManager);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (shakeDetector != null) {
            shakeDetector.stop();
        }
    }

    /**
     * Handles internal locale changes. When the user changes the locale using the system settings
     * Android will take care of destroying and recreating the activity.
     */
    private BroadcastReceiver mLocaleChangeBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UstadMobileSystemImpl.ACTION_LOCALE_CHANGE:
                    localeChanged = true;
                    break;
            }
        }
    };

    /**
     * UstadMobileSystemImpl will bind certain services to each activity (e.g. HTTP, P2P services)
     * If needed the child activity can override this method to listen for when the service is ready
     *
     * @param name
     * @param iBinder
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder iBinder) {

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    protected void setUMToolbar(int toolbarID) {
        umToolbar = (Toolbar) findViewById(toolbarID);
        setSupportActionBar(umToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    /**
     * Get the toolbar that's used for the support action bar
     *
     * @return
     */
    protected Toolbar getUMToolbar() {
        return umToolbar;
    }


    protected void setBaseController(UstadBaseController baseController) {
        this.baseController = baseController;
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onStart() {
        isStarted = true;
        super.onStart();
    }

    public void onStop() {
        isStarted = false;
        super.onStop();
    }

    /**
     * Can be used to check if the activity has been started.
     *
     * @return true if the activity is started. false if it has not been started yet, or it was started, but has since stopped
     */
    public boolean isStarted() {
        return isStarted;
    }

    @Override
    public void onDestroy() {
        if (bleServiceBound) {
            unbindService(bleServiceConnection);
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocaleChangeBroadcastReceiver);
        getInstance().handleActivityDestroy(this);
        if (mSyncServiceBound) {
            unbindService(mSyncServiceConnection);
        }
        shakeDetector = null;
        sensorManager = null;
        super.onDestroy();
    }

    public Object getContext() {
        return this;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                UstadMobileSystemImpl impl = getInstance();
                impl.go(impl.getAppConfigString(AppConfig.INSTANCE.getKEY_FIRST_DEST(), null,
                        this), this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        fragmentList.add(new WeakReference<>(fragment));
    }


    /**
     * Handle our own delegation of back button presses.  This allows UstadBaseFragment child classes
     * to handle back button presses if they want to.
     */
    @Override
    public void onBackPressed() {
        for (WeakReference<Fragment> fragmentReference : fragmentList) {
            if (fragmentReference.get() == null)
                continue;

            if (!fragmentReference.get().isVisible())
                continue;

            if (fragmentReference.get() instanceof UstadBaseFragment && ((UstadBaseFragment) fragmentReference.get()).canGoBack()) {
                ((UstadBaseFragment) fragmentReference.get()).goBack();
                return;
            }
        }

        super.onBackPressed();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        final Resources res = newBase.getResources();
        final Configuration config = res.getConfiguration();
        String languageSetting = getInstance().getLocale(newBase);

        if (Build.VERSION.SDK_INT >= 17) {
            Locale locale = languageSetting.equals(UstadMobileSystemImpl.LOCALE_USE_SYSTEM)
                    ? Locale.getDefault() : new Locale(languageSetting);
            config.setLocale(locale);
            super.attachBaseContext(newBase.createConfigurationContext(config));
        } else {
            super.attachBaseContext(newBase);
        }
    }


    @Override
    public void showNotification(String notification, int length) {
        runOnUiThread(() -> Toast.makeText(this, notification, length).show());
    }

    /**
     * Responsible for running task after checking permissions
     *
     * @param permission    Permission to be checked
     * @param runnable      Future task to be executed
     * @param dialogTitle   Permission dialog title
     * @param dialogMessage Permission dialog message
     */
    protected void runAfterGrantingPermission(String permission, Runnable runnable,
                                              String dialogTitle, String dialogMessage) {
        this.afterPermissionMethodRunner = runnable;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            afterPermissionMethodRunner.run();
            afterPermissionMethodRunner = null;
            return;
        }


        this.permissionDialogMessage = dialogMessage;
        this.permissionDialogTitle = dialogTitle;
        this.permission = permission;

        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
            if (!permissionRequestRationalesShown) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(permissionDialogTitle)
                        .setMessage(permissionDialogMessage)
                        .setNegativeButton(getString(android.R.string.cancel),
                                (dialog, which) -> dialog.dismiss())
                        .setPositiveButton(getString(android.R.string.ok), (dialog, which) ->
                                runAfterGrantingPermission(permission, afterPermissionMethodRunner,
                                        permissionDialogTitle, permissionDialogMessage));
                AlertDialog dialog = builder.create();
                dialog.show();
                permissionRequestRationalesShown = true;
            } else {
                permissionRequestRationalesShown = false;
                ActivityCompat.requestPermissions(this, new String[]{permission}, RUN_TIME_REQUEST_CODE);
            }
        } else {
            afterPermissionMethodRunner.run();
            afterPermissionMethodRunner = null;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case RUN_TIME_REQUEST_CODE:
                boolean allPermissionGranted = grantResults.length == permissions.length;
                for (int result : grantResults) {
                    allPermissionGranted &= result == PackageManager.PERMISSION_GRANTED;
                }

                if (!allPermissionGranted &&
                        permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    afterPermissionMethodRunner.run();
                    afterPermissionMethodRunner = null;
                }

                if (allPermissionGranted) {
                    afterPermissionMethodRunner.run();
                    afterPermissionMethodRunner = null;
                    return;
                }
                break;

        }
    }

    /**
     * Make sure NetworkManagerBle is not null when running a certain logic
     *
     * @param runnable Future task to be executed
     */
    public void runAfterServiceConnection(Runnable runnable) {
        runWhenServiceConnectedQueue.runWhenReady(runnable);
    }

    /**
     * @return Active NetworkManagerBle
     */
    public NetworkManagerBle getNetworkManagerBle() {
        return networkManagerBle;
    }
}
