package com.ustadmobile.test.sharedse.network;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ServiceTestRule;

import com.ustadmobile.core.controller.CatalogPresenter;
import com.ustadmobile.core.db.DbManager;
import com.ustadmobile.core.db.UmLiveData;
import com.ustadmobile.core.db.UmObserver;
import com.ustadmobile.core.db.dao.CrawlJobWithTotals;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;
import com.ustadmobile.core.networkmanager.NetworkTask;
import com.ustadmobile.core.util.UMFileUtil;
import com.ustadmobile.lib.db.entities.CrawlJob;
import com.ustadmobile.lib.db.entities.DownloadJobItem;
import com.ustadmobile.lib.db.entities.DownloadSet;
import com.ustadmobile.port.android.netwokmanager.NetworkServiceAndroid;
import com.ustadmobile.port.sharedse.networkmanager.NetworkManager;
import com.ustadmobile.test.core.impl.ClassResourcesResponder;
import com.ustadmobile.test.core.impl.PlatformTestUtil;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import fi.iki.elonen.router.RouterNanoHTTPD;

/**
 * Created by mike on 3/6/18.
 */
@SmallTest
public class TestCrawlJob {

    public static final int CRAWL_JOB_TIMEOUT = 1000000;

    private static RouterNanoHTTPD resourcesHttpd;

    public static final ServiceTestRule mServiceRule = new ServiceTestRule();


    @BeforeClass
    public static void startHttpResourcesServer() throws IOException, InterruptedException {
        if(resourcesHttpd == null) {
            resourcesHttpd = new RouterNanoHTTPD(0);
            resourcesHttpd.addRoute("/res/(.*)", ClassResourcesResponder.class, "/res/");
            resourcesHttpd.start();
        }
    }

    @AfterClass
    public static void stopHttpResourcesServer() throws IOException {
        if(resourcesHttpd != null) {
            resourcesHttpd.stop();
            resourcesHttpd = null;
        }
    }

    private static NetworkServiceAndroid sService;

    public static ServiceConnection sServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            sService = ((NetworkServiceAndroid.LocalServiceBinder)iBinder)
                    .getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @BeforeClass
    public static void startNetworkService() throws Exception{
        Context context = InstrumentationRegistry.getTargetContext();
        UstadMobileSystemImpl.getInstance().init(context);
        Intent serviceIntent = new Intent(context, NetworkServiceAndroid.class);
        mServiceRule.bindService(serviceIntent, sServiceConnection, Service.BIND_AUTO_CREATE);
    }

    @AfterClass
    public static void stopNetworkService() throws Exception {
        mServiceRule.unbindService();
    }




    @Test
    public void testCrawl() {
        String opdsRootIndexUrl = UMFileUtil.joinPaths(
                "http://localhost:" + resourcesHttpd.getListeningPort(),
                "res/com/ustadmobile/test/sharedse/crawlme/index.opds");

        String storageDir = UstadMobileSystemImpl.getInstance().getStorageDirs(
                CatalogPresenter.SHARED_RESOURCE, PlatformTestUtil.getTargetContext())[0].getDirURI();
        NetworkManager networkManager = (NetworkManager)UstadMobileSystemImpl.getInstance().getNetworkManager();
        DbManager dbManager = DbManager.getInstance(PlatformTestUtil.getTargetContext());

        DownloadSet set = new DownloadSet();
        set.setDestinationDir(storageDir);
        CrawlJob crawlJob = new CrawlJob();
        crawlJob.setRootEntryUri(opdsRootIndexUrl);
        crawlJob = UstadMobileSystemImpl.getInstance().getNetworkManager().prepareDownload(set, crawlJob);

        UmLiveData<CrawlJobWithTotals> crawlJobLiveData = dbManager.getCrawlJobDao().findWithTotalsByIdLive(
                crawlJob.getCrawlJobId());
        Object crawlLock = new Object();
        UmObserver<CrawlJobWithTotals> crawlJobUmObserver = (crawlJobValue) -> {
            if(crawlJobValue.getStatus() == NetworkTask.STATUS_COMPLETE){
                synchronized (crawlLock){
                    try {crawlLock.notifyAll(); }
                    catch(Exception e){}
                }
            }
        };
        crawlJobLiveData.observeForever(crawlJobUmObserver);

        if(crawlJobLiveData.getValue() == null
                || crawlJobLiveData.getValue().getStatus() != NetworkTask.STATUS_COMPLETE){
            synchronized (crawlLock) {
                try {crawlLock.wait(CRAWL_JOB_TIMEOUT);}
                catch(InterruptedException e) {}
            }
        }

        Assert.assertEquals("Crawl job status complete", NetworkTask.STATUS_COMPLETE,
                crawlJobLiveData.getValue().getStatus());

        Assert.assertEquals("Crawl job has three items", 3,
                crawlJobLiveData.getValue().getNumItems());

        List<DownloadJobItem> downloadJobItems = dbManager.getDownloadJobItemDao().
                findAllByDownloadJobId(crawlJob.getContainersDownloadJobId());
        for(DownloadJobItem item : downloadJobItems) {
            Assert.assertTrue("Job Item #" + item.getDownloadJobItemId() + " has size found > 0",
                    item.getDownloadLength() > 0);
        }

        Assert.assertEquals("All three crawl job items are completed", 3,
                crawlJobLiveData.getValue().getNumItemsCompleted());
    }



}
