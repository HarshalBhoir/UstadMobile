package com.ustadmobile.test.port.android;

import android.arch.lifecycle.LiveData;
import android.arch.paging.DataSource;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;

import com.ustadmobile.core.db.DbManager;
import com.ustadmobile.core.db.UmLiveData;
import com.ustadmobile.core.db.UmProvider;
import com.ustadmobile.core.db.dao.OpdsFeedWithRelationsDao;
import com.ustadmobile.core.util.UMFileUtil;
import com.ustadmobile.lib.db.entities.OpdsEntryWithRelations;
import com.ustadmobile.lib.db.entities.OpdsFeedWithRelations;
import com.ustadmobile.test.core.ResourcesHttpdTestServer;
import com.ustadmobile.test.core.TestCaseCallbackHelper;
import com.ustadmobile.test.core.impl.PlatformTestUtil;

import junit.framework.TestCase;

import org.junit.Test;


/**
 * Created by mike on 1/15/18.
 */

public class TestOpdsFeedEntryProvider extends TestCase{

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ResourcesHttpdTestServer.startServer();
    }

    @Override
    protected void tearDown() throws Exception {
        ResourcesHttpdTestServer.stopServer();
        super.tearDown();
    }

    @Test
    public void testOpdsFeedEntryProvider() {
        OpdsFeedWithRelationsDao repository = DbManager.getInstance(PlatformTestUtil.getTargetContext())
                .getOpdsFeedWithRelationsRepository();
        String opdsUrl = UMFileUtil.joinPaths(new String[] {
                ResourcesHttpdTestServer.getHttpRoot(), "com/ustadmobile/test/core/acquire-multi.opds"});

        UmLiveData<OpdsFeedWithRelations> feed = repository.getFeedByUrl(opdsUrl);
        TestCaseCallbackHelper helper = new TestCaseCallbackHelper(this);

        OpdsFeedWithRelations[] returnedVal = new OpdsFeedWithRelations[1];
        helper.add(20000, () -> {
            feed.observeForever((t) -> {
                if(t != null) {
                    returnedVal[0] = t;
                    helper.onSuccess(t);
                }
            });
        }).add(20000, () -> {
            OpdsFeedWithRelations returnedFeed = (OpdsFeedWithRelations)helper.getResult();
            UmProvider<OpdsEntryWithRelations> entryProvider = DbManager.getInstance(PlatformTestUtil.getTargetContext())
                    .getOpdsEntryWithRelationsDao().findEntriesByFeed(returnedFeed.getId());
            DataSource.Factory<Integer, OpdsEntryWithRelations> factory =
                    (DataSource.Factory<Integer, OpdsEntryWithRelations>)entryProvider.getProvider();
            LiveData<PagedList<OpdsEntryWithRelations>> entryList = new LivePagedListBuilder(factory, 20).build();
            entryList.observeForever((t) -> {
                if(t != null && entryList.getValue().size() > 0) {
                    OpdsEntryWithRelations entry = entryList.getValue().get(0);
                    assertNotNull(entry);
                    helper.onSuccess("OK");
                }
            });
        });
        helper.start();
    }

}
