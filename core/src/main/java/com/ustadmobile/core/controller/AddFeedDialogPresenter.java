package com.ustadmobile.core.controller;

import com.ustadmobile.core.db.DbManager;
import com.ustadmobile.core.db.UmObserver;
import com.ustadmobile.core.impl.UMLog;
import com.ustadmobile.core.impl.UmCallback;
import com.ustadmobile.core.db.UmLiveData;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;
import com.ustadmobile.core.opds.OpdsEndpoint;
import com.ustadmobile.core.opds.UstadJSOPDSEntry;
import com.ustadmobile.core.opds.UstadJSOPDSFeed;
import com.ustadmobile.core.opds.UstadJSOPDSItem;
import com.ustadmobile.core.opds.entities.UmOpdsLink;
import com.ustadmobile.core.util.UMFileUtil;
import com.ustadmobile.core.util.UMIOUtils;
import com.ustadmobile.core.view.AddFeedDialogView;
import com.ustadmobile.lib.db.entities.OpdsEntry;
import com.ustadmobile.lib.db.entities.OpdsEntryParentToChildJoin;
import com.ustadmobile.lib.db.entities.OpdsEntryWithRelations;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Handles adding a feed to the user's feed list. Those feeds are stored as OPDS text in the
 * preferences keys, using OpdsEndpoint.
 */

public class AddFeedDialogPresenter extends UstadBaseController implements UstadJSOPDSItem.OpdsItemLoadCallback {

    private AddFeedDialogView addFeedDialogView;

    @Deprecated
    private UstadJSOPDSFeed presetFeeds;

    private List<OpdsEntryWithRelations> presetFeedsList;

    private int dropDownlSelectedIndex = 0;

    private String prefkey;

    public static final String ARG_PREFKEY = "pk";

    public static final String ARG_UUID = "uuid";

    private UstadJSOPDSFeed loadingFeed;

    private String opdsUrlError = null;

    private UmLiveData<OpdsEntryWithRelations> entry;

    private UmLiveData<List<OpdsEntryWithRelations>> presetListLiveData;

    private UmObserver<List<OpdsEntryWithRelations>> presetListObserver;

    String loadedUuid;

    private String uuidToAddTo;

    public AddFeedDialogPresenter(Object context, AddFeedDialogView addFeedDialogView) {
        super(context);
        this.addFeedDialogView = addFeedDialogView;
    }

    public void onCreate(Hashtable args, Hashtable savedState) {
        final UstadMobileSystemImpl impl = UstadMobileSystemImpl.getInstance();
        uuidToAddTo = (String)args.get(ARG_UUID);
        presetFeedsList = new ArrayList<>();
        entry = DbManager.getInstance(getContext()).getOpdsEntryWithRelationsRepository().getEntryByUrl(
                "asset:///com/ustadmobile/core/libraries.opds", "preset_libraries_opds");
        entry.observe(this, this::handlePresetParentFeedUpdated);
    }

    private void handlePresetParentFeedUpdated(OpdsEntryWithRelations presetParent) {
        if(loadedUuid == null && presetParent != null) {
            loadedUuid = presetParent.getId();
            presetListLiveData = DbManager.getInstance(getContext()).getOpdsEntryWithRelationsDao()
                    .getEntriesByParentAsList(loadedUuid);
            presetListObserver = this::handlePresetFeedListUpdated;
            presetListLiveData.observe(this, presetListObserver);
        }
    }

    private void handlePresetFeedListUpdated(List<OpdsEntryWithRelations> presetFeedsList) {
        if(presetFeedsList != null) {
            final String[] presetNames = new String[presetFeedsList.size() + 2];
            presetNames[0] = "Select a feed";
            presetNames[1] = "Add by URL";
            for(int i = 0; i < presetFeedsList.size(); i++) {
                presetNames[i + 2] = presetFeedsList.get(i).getTitle();
            }
            this.presetFeedsList = presetFeedsList;

            addFeedDialogView.runOnUiThread( () -> addFeedDialogView.setDropdownPresets(presetNames));
        }
    }

    public void handlePresetSelected(int index) {
        addFeedDialogView.setUrlFieldVisible(index == 1);
        dropDownlSelectedIndex = index;
    }

    public void handleClickAdd() {
        if(dropDownlSelectedIndex > 1) {
            //take it from the libraries.opds preset that was selected
            OpdsEntry addedEntry = presetFeedsList.get(dropDownlSelectedIndex-2);
            OpdsEntryParentToChildJoin join = new OpdsEntryParentToChildJoin(uuidToAddTo,
                addedEntry.getId(), 0);
            new Thread(() -> {
                DbManager.getInstance(getContext()).getOpdsEntryParentToChildJoinDao().insert(join);
                addFeedDialogView.runOnUiThread(() -> addFeedDialogView.dismiss());
            }).start();
        }else if(dropDownlSelectedIndex == 1) {
            addFeedDialogView.setUiEnabled(false);
            addFeedDialogView.setProgressVisible(true);
            String feedUrl = addFeedDialogView.getOpdsUrl();
            loadingFeed = new UstadJSOPDSFeed(feedUrl);
            loadingFeed.loadFromUrlAsync(feedUrl, null, getContext(), this);
        }
    }

    public void handleClickCancel() {

    }

    public void handleOpdsUrlChanged(String opdsUrl) {
        if(opdsUrlError != null) {
            opdsUrlError = null;
            addFeedDialogView.setError(null);
        }
    }

    @Override
    public void onEntryLoaded(UstadJSOPDSItem item, int position, UstadJSOPDSEntry entryLoaded) {

    }

    @Override
    public void onDone(UstadJSOPDSItem item) {
        if(item == loadingFeed) {
            UmOpdsLink link = (UmOpdsLink)UstadMobileSystemImpl.getInstance().getOpdsDbManager().makeNew(UmOpdsLink.class);
            link.setRel(UstadJSOPDSItem.LINK_REL_SUBSECTION);
            link.setMimeType(UstadJSOPDSItem.TYPE_NAVIGATIONFEED);
            link.setHref(item.getHref());
            addFeed(item, link);
        }
    }

    @Override
    public void onError(UstadJSOPDSItem item, final Throwable cause) {
        if(item == loadingFeed) {
            addFeedDialogView.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addFeedDialogView.setProgressVisible(false);
                    addFeedDialogView.setUiEnabled(true);
                    opdsUrlError = "Error: " + cause != null ? cause.getMessage() : "";
                    addFeedDialogView.setError(opdsUrlError);
                }
            });
        }
    }

    public void addFeed(UstadJSOPDSItem item, UmOpdsLink link) {
        final boolean[] completedOk = new boolean[1];
        try {
            UstadJSOPDSFeed userFeedList = (UstadJSOPDSFeed)OpdsEndpoint.getInstance().loadItem(
                    UMFileUtil.joinPaths(new String[]{OpdsEndpoint.OPDS_PROTO_PREFKEY_FEEDS, prefkey}),
                    null, getContext(), null);
            UstadJSOPDSEntry feedEntry = new UstadJSOPDSEntry(userFeedList);
            feedEntry.setTitle(item.getTitle());
            feedEntry.setItemId(item.getItemId());
            feedEntry.addLink(link);
            UmOpdsLink thumbnailLinks = item.getThumbnailLink(false);
            if(thumbnailLinks != null) {
                UmOpdsLink thumbnailLinksMod = (UmOpdsLink)UstadMobileSystemImpl.getInstance().getOpdsDbManager().makeNew(UmOpdsLink.class);
//                String[] thumbnailLinksMod = new String[thumbnailLinks.length];
//                System.arraycopy(thumbnailLinks, 0, thumbnailLinksMod, 0, thumbnailLinksMod.length);
                thumbnailLinksMod.setHref(UMFileUtil.resolveLink(item.getHref(), thumbnailLinks.getHref()));
//                thumbnailLinksMod[UstadJSOPDSItem.ATTR_HREF] = UMFileUtil.resolveLink(item.getHref(),
//                        thumbnailLinksMod[UstadJSOPDSItem.ATTR_HREF]);
                thumbnailLinksMod.setLength(thumbnailLinks.getLength());
                thumbnailLinksMod.setMimeType(thumbnailLinks.getMimeType());
                thumbnailLinksMod.setRel(thumbnailLinks.getRel());
                feedEntry.addLink(thumbnailLinksMod);
            }
            userFeedList.addEntry(feedEntry);
            OpdsEndpoint.getInstance().saveFeedToPreferences(userFeedList, prefkey, getContext());
            completedOk[0] = true;
        }catch(IOException e) {
            UstadMobileSystemImpl.l(UMLog.ERROR, 683, prefkey, e);
        }

        addFeedDialogView.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(completedOk[0]) {
                    addFeedDialogView.dismiss();
                }else {
                    addFeedDialogView.setProgressVisible(false);
                    addFeedDialogView.setUiEnabled(true);
                }
            }
        });

    }

    @Override
    public void setUIStrings() {

    }


}
