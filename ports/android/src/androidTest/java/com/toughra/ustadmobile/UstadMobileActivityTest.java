package com.toughra.ustadmobile;

import android.app.Activity;
import android.os.Environment;
import android.test.ActivityInstrumentationTestCase2;

import com.ustadmobile.core.impl.*;
import com.ustadmobile.port.android.impl.UstadMobileSystemImplAndroid;

import com.ustadmobile.test.core.TestConstants;
import com.ustadmobile.test.core.TestUtils;

import java.io.File;
import java.util.Hashtable;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.toughra.ustadmobile.UstadMobileActivityTest \
 * com.toughra.ustadmobile.tests/android.test.InstrumentationTestRunner
 */
public class UstadMobileActivityTest extends ActivityInstrumentationTestCase2<UstadMobileActivity> implements UMProgressListener {

    private UMProgressEvent lastProgressEvent;

    public UstadMobileActivityTest() {
        super("com.toughra.ustadmobile", UstadMobileActivity.class);
    }



    public void testDownloadImpl() {
        Activity act = getActivity();
        File baseDir = Environment.getExternalStorageDirectory();
        File file3 = new File(baseDir, "phonepic-large.png");
        if(file3.exists()) {
            file3.delete();
        }

        String fileDownloadURL = TestUtils.getInstance().getHTTPRoot() + "phonepic-large.png";
        UMTransferJob job = UstadMobileSystemImpl.getInstance().downloadURLToFile(fileDownloadURL,
                file3.getAbsolutePath(), new Hashtable());
        job.addProgressListener(this);
        job.start();


        int timeout = 120000;
        int interval = 1500;
        int timeCount = 0;

        for(timeCount = 0; timeCount < timeout && !job.isFinished(); timeCount+= interval) {
            try {
                Thread.sleep(1500);
            }catch(InterruptedException e) {}
        }

        int totalSize = job.getTotalSize();
        int downloadedSize = job.getBytesDownloadedCount();
        assertTrue("Downloaded size is the same as total size: "
                + totalSize + " : " + downloadedSize,
                totalSize == downloadedSize);

    }


    public void testPrefs() {
        Activity act = getActivity();
        String currentUsername = "bobtheamazing";
        UstadMobileSystemImpl impl = UstadMobileSystemImpl.getInstance();
        impl.setActiveUser(currentUsername);
        assertEquals("Current username is as set", currentUsername, impl.getActiveUser());
        assertTrue("Current username in Shared Preferences", getActivity().getSharedPreferences(
                UstadMobileSystemImplAndroid.APP_PREFERENCES_NAME, 0).contains(
                UstadMobileSystemImplAndroid.KEY_CURRENTUSER));

        impl.setActiveUser(null);
        assertNull("User not logged in after setActiveUser null", impl.getActiveUser());

        impl.setAppPref("meaning", "forty-two");
        assertEquals("App preference set can be retrieved", "forty-two", impl.getAppPref("meaning"));

        impl.setActiveUser(currentUsername);

        String userPrefStr = "the answer is 42 for you too";
        String userPrefKey = "usermeaning";
        impl.setUserPref(userPrefKey, userPrefStr);
        assertEquals("Can retrieve set preference", userPrefStr, impl.getUserPref(userPrefKey));
        impl.setActiveUser("someoneelse");
        assertNull("After changing user preference is gone", impl.getUserPref(userPrefKey));
        impl.setActiveUser(null);
    }


    @Override
    public void progressUpdated(UMProgressEvent umProgressEvent) {
        lastProgressEvent = umProgressEvent;
    }
}
