package com.ustadmobile.core.controller;

import com.ustadmobile.core.catalog.ContentTypeManager;
import com.ustadmobile.core.db.DbManager;
import com.ustadmobile.core.db.UmLiveData;
import com.ustadmobile.core.db.UmObserver;
import com.ustadmobile.core.generated.locale.MessageID;
import com.ustadmobile.core.impl.BaseUmCallback;
import com.ustadmobile.core.impl.UMLog;
import com.ustadmobile.core.impl.UmCallback;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;
import com.ustadmobile.core.networkmanager.AcquisitionListener;
import com.ustadmobile.core.networkmanager.AcquisitionTaskStatus;
import com.ustadmobile.core.networkmanager.AvailabilityMonitorRequest;
import com.ustadmobile.core.networkmanager.NetworkManagerCore;
import com.ustadmobile.core.networkmanager.NetworkManagerListener;
import com.ustadmobile.lib.db.entities.DownloadJobItem;
import com.ustadmobile.lib.db.entities.NetworkNode;
import com.ustadmobile.core.networkmanager.NetworkTask;
import com.ustadmobile.core.opds.UstadJSOPDSEntry;
import com.ustadmobile.core.opds.UstadJSOPDSFeed;
import com.ustadmobile.core.opds.UstadJSOPDSItem;
import com.ustadmobile.core.opds.entities.UmOpdsLink;
import com.ustadmobile.core.util.UMFileUtil;
import com.ustadmobile.lib.db.entities.ContainerFile;
import com.ustadmobile.lib.db.entities.ContainerFileEntry;
import com.ustadmobile.lib.db.entities.OpdsEntryWithRelations;
import com.ustadmobile.lib.db.entities.OpdsLink;
import com.ustadmobile.lib.util.UMUtil;
import com.ustadmobile.core.view.CatalogEntryView;
import com.ustadmobile.core.view.DialogResultListener;
import com.ustadmobile.core.view.DismissableDialog;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import static com.ustadmobile.lib.db.entities.OpdsEntry.ENTRY_PROTOCOL;

/* $if umplatform != 2 $ */
/* $endif */

/**
 * Created by mike on 4/17/17.
 */

public class CatalogEntryPresenter extends BaseCatalogPresenter implements AcquisitionListener,
        NetworkManagerListener, DialogResultListener{

    private CatalogEntryView catalogEntryView;

    private Hashtable args;

    public static final String ARG_ENTRY_OPDS_STR = "opds_str";

    public static final String ARG_ENTRY_ID = "entry_id";

    /**
     * Where the dislpay mode is "normal" - e.g. DISPLAY_MODE_THUMBNAIL the title bar will collapse
     * and show the title header. This should normally come from the OPDS feed from which this item
     * was navigated to.
     */
    public static final String ARG_TITLEBAR_TEXT = "bar_title";

    public static final String APP_CONFIG_DISPLAY_MODE = "catalog_entry_display_mode";


    @Deprecated
    private UstadJSOPDSEntry entry;

    @Deprecated
    private UstadJSOPDSFeed entryFeed;

    private NetworkManagerCore manager;

    private long entryCheckTaskId = -1;

    private Vector alternativeTranslationLinks;

    private Vector[] sharedAcquiredEntries;

    private static final int CMD_REMOVE_PRESENTER_ENTRY = 60;

    private static final int CMD_SHARE_ENTRY = 63;

    protected AvailabilityMonitorRequest availabilityMonitorRequest;

    private long downloadTaskId;

    private RelatedItemLoader seeAlsoLoader = new RelatedItemLoader();

    private boolean openAfterLoginOrRegister = false;

    private boolean entryLoaded = false;

    private boolean seeAlsoVisible = false;

    private UmLiveData<OpdsEntryWithRelations> entryLiveData;

    private UmObserver<DownloadJobItem> entryDownloadJobItemObserver;

    private UmLiveData<DownloadJobItem> entryDownloadJobLiveData;

    private String baseHref;

    private String currentEntryId;

    /**
     * Represents a related item, as per the atom spec rel='related', used to provide see also
     * links for the user.
     */
    protected class RelatedItem {

        UstadJSOPDSItem opdsItem;

        String url;

        String[] link;

        /**
         *
         * @param opdsItem The OpdsItem (e.g. Entry or Feed) that represents the related item represents.
         *                 This may not have loaded yet.
         * @param link The link string array as per UstadJSOPDSItem containing the href and mime type.
         * @param baseHref The base href path from which links are resolveds
         */
        protected RelatedItem(UstadJSOPDSItem opdsItem, String[] link, String baseHref) {
            this.opdsItem = opdsItem;
            this.url = UMFileUtil.resolveLink(baseHref, link[UstadJSOPDSItem.ATTR_HREF]);
            this.link = link;
        }

        /**
         * Equivilent to calling RelatedItem(opdsItem, link, entry.getHref()) - resolves the link
         * href from the catalog entry presenter's main entry.
         *
         * @param opdsItem The OpdsItem (e.g. Entry or Feed) that represents the related item represents.
         *                 This may not have loaded yet.
         * @param link The link string array as per UstadJSOPDSItem containing the href and mime type.
         */
        protected RelatedItem(UstadJSOPDSItem opdsItem, String[] link) {
            this(opdsItem, link, CatalogEntryPresenter.this.entry.getHref());
        }
    }

    /**
     * Handles loading related items. In order to find the thumbnail for an item, we need to load
     * the entry xml itself, and then look in that to find the thumbnail url
     */
    protected class RelatedItemLoader implements UstadJSOPDSItem.OpdsItemLoadCallback{

        protected RelatedItem currentLoadingItem;

        private Vector itemsToLoad = new Vector();

        public void addItemToLoad(String[] link) {
            String linkUrl = UMFileUtil.resolveLink(entry.getHref(), link[UstadJSOPDSItem.ATTR_HREF]);
            UMFileUtil.TypeWithParamHeader typeWithParams = UMFileUtil.parseTypeWithParamHeader(
                    link[UstadJSOPDSItem.ATTR_MIMETYPE]);
            String catalogType = typeWithParams.getParam("type");
            UstadJSOPDSItem item;
            if(catalogType != null && catalogType.equals("entry")) {
                item = new UstadJSOPDSEntry(null);
            }else {
                item = new UstadJSOPDSFeed(linkUrl);
            }

            itemsToLoad.addElement(new RelatedItem(item, link));
            checkQueue();
        }

        private void checkQueue() {
            if(currentLoadingItem == null && itemsToLoad.size() > 0) {
                currentLoadingItem = (RelatedItem)itemsToLoad.remove(0);
                UstadMobileSystemImpl.l(UMLog.DEBUG, 679, currentLoadingItem.url);
                currentLoadingItem.opdsItem.loadFromUrlAsync(currentLoadingItem.url, null, getContext(),
                        this);
            }
        }

        @Override
        public void onEntryLoaded(UstadJSOPDSItem item, int position, UstadJSOPDSEntry entryLoaded) {

        }

        @Override
        public void onDone(UstadJSOPDSItem item) {
            if(currentLoadingItem != null && currentLoadingItem.opdsItem == item) {
                UstadMobileSystemImpl.l(UMLog.DEBUG, 680, currentLoadingItem.url);
                handleRelatedItemReady(currentLoadingItem);
                currentLoadingItem = null;
                checkQueue();
            }else {
                //something is wrong
            }
        }

        @Override
        public void onError(UstadJSOPDSItem item, Throwable cause) {
            if (currentLoadingItem != null && currentLoadingItem.opdsItem == item) {
                UstadMobileSystemImpl.l(UMLog.WARN, 681, currentLoadingItem.url);
                currentLoadingItem = null;
                checkQueue();
            } else {
                //something is wrong
            }
        }
    }


    public CatalogEntryPresenter(Object context) {
        super(context);
    }

    public CatalogEntryPresenter(Object context, CatalogEntryView view, Hashtable args) {
        super(context);
        this.catalogEntryView = view;
        this.args = args;
    }

    public void onCreate() {
        manager = UstadMobileSystemImpl.getInstance().getNetworkManager();

        if(args.containsKey(ARG_BASE_HREF)) {
            baseHref = (String)args.get(ARG_BASE_HREF);
        }

        String entryUri = (String)args.get(ARG_URL);
        if(entryUri.startsWith(ENTRY_PROTOCOL)) {
            String entryUuid = entryUri.substring(ENTRY_PROTOCOL.length());
            entryLiveData = DbManager.getInstance(getContext()).getOpdsEntryWithRelationsDao()
                    .getEntryByUuid(entryUuid);
            entryLiveData.observe(this, this::handleEntryUpdated);
        }

        if(this.args.containsKey(ARG_TITLEBAR_TEXT))
            catalogEntryView.setTitlebarText((String)this.args.get(ARG_TITLEBAR_TEXT));

        catalogEntryView.setLearnerProgressVisible(false);

        UstadMobileSystemImpl.getInstance().getNetworkManager().addAcquisitionTaskListener(this);
    }

    public void handleEntryUpdated(OpdsEntryWithRelations entry) {
        catalogEntryView.setEntryTitle(entry.getTitle());
        catalogEntryView.setDescription(entry.getContent(), entry.getContentType());
        OpdsLink acquisitionLink = entry.getAcquisitionLink(null, true);
        final UstadMobileSystemImpl impl = UstadMobileSystemImpl.getInstance();
        long containerSize = -1;
        if(acquisitionLink != null && acquisitionLink.getLength() > 0)
            containerSize = acquisitionLink.getLength();

        OpdsLink thumbnailLink = entry.getThumbnailLink(true);


        if(thumbnailLink != null)
            catalogEntryView.setThumbnail(UMFileUtil.resolveLink(baseHref, thumbnailLink.getHref()),
                    thumbnailLink.getMimeType());

        int containerFileId = -1;
        if(entry.getContainerFileEntries() != null && entry.getContainerFileEntries().size() > 0) {
            updateButtonsByStatus(CatalogPresenter.STATUS_ACQUIRED);
            containerFileId = entry.getContainerFileEntries().get(0).getContainerFileId();
        }else {
            updateButtonsByStatus(CatalogPresenter.STATUS_NOT_ACQUIRED);
        }

        if(currentEntryId == null || !currentEntryId.equals(entry.getEntryId())){
            if(entryDownloadJobItemObserver != null)
                entryDownloadJobLiveData.removeObserver(entryDownloadJobItemObserver);
        }

        String sizePrefix =  impl.getString(MessageID.size, getContext()) +  ": ";

        if(containerSize <= 0 && containerFileId != -1){
            DbManager.getInstance(getContext()).getContainerFileDao().findContainerFileLengthAsync(
                    containerFileId, new UmCallback<Long>() {
                        @Override
                        public void onSuccess(Long result) {
                            catalogEntryView.runOnUiThread(() -> catalogEntryView.setSize(sizePrefix +
                                    UMFileUtil.formatFileSize(result)));
                        }

                        @Override
                        public void onFailure(Throwable exception) {

                        }
                    }
            );
        }else if(containerSize > 0){
            catalogEntryView.setSize(sizePrefix + UMFileUtil.formatFileSize(containerSize));
        }else{
            catalogEntryView.setSize("");
        }

        entryDownloadJobItemObserver = this::handleDownloadJobItemUpdated;
        entryDownloadJobLiveData = DbManager.getInstance(getContext()).getDownloadJobItemDao()
                .findDownloadJobItemByEntryIdAndStatusRangeLive(entry.getEntryId(),
                        NetworkTask.STATUS_WAITING_MIN, NetworkTask.STATUS_RUNNING_MAX);
        entryDownloadJobLiveData.observe(this, entryDownloadJobItemObserver);

        currentEntryId = entry.getEntryId();
    }

    public void handleDownloadJobItemUpdated(DownloadJobItem jobItem) {
        if(jobItem != null) {
            catalogEntryView.setProgressVisible(true);
            float completed = (float) jobItem.getDownloadedSoFar() / (float) jobItem.getDownloadLength();
            catalogEntryView.setProgress(completed);
            catalogEntryView.setProgressStatusText(formatDownloadStatusText(jobItem));
        }else {
            catalogEntryView.setProgressVisible(false);
        }
    }


//    @Override
//    public void onEntryLoaded(UstadJSOPDSItem item, int position, UstadJSOPDSEntry entry) {
//
//    }

//    @Override
//    public void onDone(UstadJSOPDSItem item) {
//        catalogEntryView.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                handleEntryReady();
//            }
//        });
//    }

//    @Override
//    public void onError(UstadJSOPDSItem item, Throwable cause) {
//        UstadMobileSystemImpl.getInstance().getAppView(getContext()).showNotification(
//                "Error: ", AppView.LENGTH_LONG);
//    }

//    public void handleEntryReady() {
//        entryLoaded = true;
//        final UstadMobileSystemImpl impl = UstadMobileSystemImpl.getInstance();
//        catalogEntryView.setEntryTitle(entry.getTitle());
//
//        if(entry.getNumAuthors() > 0) {
//            catalogEntryView.setEntryAuthors(UMUtil.joinStrings(entry.getAuthors(), ", "));
//        }
//
//        CatalogEntryInfo entryInfo = CatalogPresenter.getEntryInfo(entry.getItemId(),
//                CatalogPresenter.ALL_RESOURCES, context);
//        catalogEntryView.setDescription(entry.getContent(), entry.getContentType());
//        UmOpdsLink firstAcquisitionLink = entry.getFirstAcquisitionLink(null);
//        if(firstAcquisitionLink != null
//                && firstAcquisitionLink.getLength() > 0) {
//            catalogEntryView.setSize(impl.getString(MessageID.size, getContext())
//                    + ": "
//                    + UMFileUtil.formatFileSize(firstAcquisitionLink.getLength()));
//        }
//
//        //set the available translated versions that can be found
//        alternativeTranslationLinks = entry.getAlternativeTranslationLinks();
//
//        String[] translatedLanguages = new String[alternativeTranslationLinks.size()];
//        String[] translatedLink;
//        for(int i = 0; i < translatedLanguages.length; i++) {
//            translatedLink = (String[])alternativeTranslationLinks.elementAt(i);
//            translatedLanguages[i] = translatedLink[UstadJSOPDSItem.ATTR_HREFLANG];
//            if(UstadMobileConstants.LANGUAGE_NAMES.containsKey(translatedLanguages[i]))
//                translatedLanguages[i] = UstadMobileConstants.LANGUAGE_NAMES
//                        .get(translatedLanguages[i]).toString();
//        }
//
//        catalogEntryView.setAlternativeTranslationLinks(translatedLanguages);
//
//        boolean isAcquired = entryInfo != null
//                ? entryInfo.acquisitionStatus == CatalogPresenter.STATUS_ACQUIRED
//                : false;
//
//        updateButtonsByStatus(isAcquired ? CatalogPresenter.STATUS_ACQUIRED :
//                CatalogPresenter.STATUS_NOT_ACQUIRED);
//
//
//        NetworkManagerCore networkManager = UstadMobileSystemImpl.getInstance().getNetworkManager();
//        boolean isDownloadInProgress = entryInfo != null
//                &&  networkManager.getTaskById(entryInfo.downloadID,
//                NetworkManagerCore.QUEUE_ENTRY_ACQUISITION) != null;
//
//        if(isDownloadInProgress) {
//            catalogEntryView.setProgressVisible(true);
//            downloadTaskId = entryInfo.downloadID;
//        }
//
//
//        //TODO: as this is bound to the activity - this might not be ready - lifecycle implication needs handled
//        NetworkManagerCore manager  = UstadMobileSystemImpl.getInstance().getNetworkManager();
//        /* $if umplatform != 2  $ */
//        List<EntryCheckResponse> fileResponse = manager.getEntryResponsesWithLocalFile(entry.getItemId());
//        if(fileResponse != null) {
//            catalogEntryView.setLocallyAvailableStatus(CatalogEntryView.LOCAL_STATUS_AVAILABLE);
//        }
//        manager.addNetworkManagerListener(this);
//        startMonitoringLocalAvailability();
//        /* $endif$ */
//
//        //set see also items
//        if(entry != null){
//            Vector relatedLinks = entry.getLinks(UstadJSOPDSItem.LINK_REL_RELATED, null);
//
//            String[] thumbnailLink = null;
//            String[] currentLink;
//            String thumbnailUrl = null;
//            UstadJSOPDSEntry relatedEntry;
//            for(int i = 0; i < relatedLinks.size(); i++) {
//                currentLink = (String[])relatedLinks.elementAt(i);
//                relatedEntry = null;
//
//                if(entryFeed != null) {
//                    Vector relatedEntryMatch = entryFeed.getEntriesByLinkParams(
//                            UstadJSOPDSFeed.LINK_REL_ALTERNATE, null,
//                            currentLink[UstadJSOPDSItem.ATTR_HREF], entry.getLanguage());
//                    if(relatedEntryMatch != null && relatedEntryMatch.size() > 0) {
//                        relatedEntry = (UstadJSOPDSEntry) relatedEntryMatch.elementAt(0);
//                    }
//                }
//
//                if(relatedEntry != null) {
//                    handleRelatedItemReady(new RelatedItem(relatedEntry, currentLink));
//                }else {
//                    seeAlsoLoader.addItemToLoad(currentLink);
//                }
//            }
////
////            if(relatedLinks.size() == 0)
////                catalogEntryView.setSeeAlsoVisible(false);
//        }
//
//        Vector coverImages = entry.getLinks(UstadJSOPDSItem.LINK_COVER_IMAGE, null);
//        if(coverImages != null && coverImages.size() > 0) {
//            String coverImageUrl = UMFileUtil.resolveLink(entry.getHref(),
//                    ((UmOpdsLink)coverImages.elementAt(0)).getHref());
//            catalogEntryView.setHeader(coverImageUrl);
//        }
//
//        Vector thumbnails = entry.getThumbnails();
//        if(thumbnails != null && thumbnails.size() > 0) {
//            String thumbnailUrl = UMFileUtil.resolveLink(entry.getHref(),
//                    ((UmOpdsLink) thumbnails.elementAt(0)).getHref());
//            catalogEntryView.setThumbnail(thumbnailUrl);
//        }else {
//            catalogEntryView.setThumbnail(null);
//        }
//
//        updateLearnerProgress();
//    }

    protected void updateLearnerProgress() {
//        CourseProgress progress = UstadMobileSystemImpl.getInstance().getCourseProgress(
//                new String[]{entry.getItemId()}, getContext());
//        if(progress == null || progress.getStatus() == CourseProgress.STATUS_NOT_STARTED) {
//            catalogEntryView.setLearnerProgressVisible(false);
//        }else {
//            catalogEntryView.setLearnerProgressVisible(true);
//            catalogEntryView.setLearnerProgress(progress);
//        }

    }


    /**
     * Handle adding a related item to the see also part of the view.
     *
     * @param item
     */
    protected void handleRelatedItemReady(final RelatedItem item) {
        UmOpdsLink thumbnailLink = item.opdsItem.getThumbnailLink(true);
        final String[] thumbnailUrl = new String[1];
        if(thumbnailLink != null) {
            thumbnailUrl[0] = UMFileUtil.resolveLink(entry.getHref(),
                    thumbnailLink.getHref());
        }

        catalogEntryView.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(!seeAlsoVisible) {
                    catalogEntryView.setSeeAlsoVisible(true);
                    seeAlsoVisible = true;
                }
                catalogEntryView.addSeeAlsoItem(item.link, thumbnailUrl[0]);
            }
        });
    }


    public void onStart() {
        if(entryLoaded)
            updateLearnerProgress();
    }

    public void onStop() {
        NetworkTask entryStatusTask = UstadMobileSystemImpl.getInstance().getNetworkManager().getTaskById(
                entryCheckTaskId, NetworkManagerCore.QUEUE_ENTRY_STATUS);
        if(entryStatusTask != null)
            entryStatusTask.stop(NetworkTask.STATUS_STOPPED);

        stopMonitoringLocalAvailability();
    }

    protected void startMonitoringLocalAvailability() {
        if(availabilityMonitorRequest == null) {
            HashSet<String> monitorIdSet = new HashSet<>();
            monitorIdSet.add(entry.getItemId());
            availabilityMonitorRequest = new AvailabilityMonitorRequest(monitorIdSet);
            UstadMobileSystemImpl.getInstance().getNetworkManager().startMonitoringAvailability(
                    availabilityMonitorRequest, true);
        }
    }

    protected void stopMonitoringLocalAvailability() {
        if(availabilityMonitorRequest != null) {
            UstadMobileSystemImpl.getInstance().getNetworkManager().stopMonitoringAvailability(
                    availabilityMonitorRequest);
            availabilityMonitorRequest = null;
        }
    }


    /**
     * Update which buttons are shown according to the acquisition status
     *
     * @param acquisitionStatus
     */
    protected void updateButtonsByStatus(int acquisitionStatus) {
        catalogEntryView.setButtonDisplayed(CatalogEntryView.BUTTON_DOWNLOAD,
                acquisitionStatus != CatalogPresenter.STATUS_ACQUIRED);
        catalogEntryView.setButtonDisplayed(CatalogEntryView.BUTTON_OPEN,
                acquisitionStatus == CatalogPresenter.STATUS_ACQUIRED);
        catalogEntryView.setButtonDisplayed(CatalogEntryView.BUTTON_MODIFY,
                acquisitionStatus == CatalogPresenter.STATUS_ACQUIRED);
        catalogEntryView.setShareButtonVisible(acquisitionStatus == CatalogPresenter.STATUS_ACQUIRED);
    }

    public void handleClickButton(int buttonId) {
        switch(buttonId) {
            case CatalogEntryView.BUTTON_DOWNLOAD:
                handleClickDownload(Arrays.asList(entryLiveData.getValue()));
//                Vector selectedEntries = new Vector();
//                selectedEntries.addElement(entry);
//                handleClickDownload(entryFeed, selectedEntries);
                break;

            case CatalogEntryView.BUTTON_MODIFY:
                handleClickRemove();
                break;
            case CatalogEntryView.BUTTON_OPEN:
                handleOpenEntry(entry);
                break;

        }
    }

    public void handleOpenEntry(UstadJSOPDSEntry entry) {
        final UstadMobileSystemImpl impl = UstadMobileSystemImpl.getInstance();

        //TODO: check if user login is required, and if so, is the user logged in?


        /**
         * Find if this entry is in a ContainerFile
         */
        List<ContainerFileEntry> containerFileEntries = entryLiveData.getValue().getContainerFileEntries();

        if(containerFileEntries != null && containerFileEntries.size() > 0) {
            int containerFileId = containerFileEntries.get(0).getContainerFileId();
            DbManager.getInstance(getContext()).getContainerFileDao()
                    .getContainerFileByIdAsync(containerFileId, new BaseUmCallback<ContainerFile>() {
                        @Override
                        public void onSuccess(ContainerFile result) {
                            Hashtable args = new Hashtable();
                            args.put(ContainerController.ARG_CONTAINERURI, result.getNormalizedPath());
                            args.put(ContainerController.ARG_MIMETYPE, result.getMimeType());
                            args.put(ContainerController.ARG_OPFINDEX, Integer.valueOf(0));
                            impl.go(ContentTypeManager.getViewNameForContentType(result.getMimeType()),
                                    args, getContext());
                        }
                    });
        }
    }

    public void handleClickShare() {
        Hashtable args = new Hashtable();
        args.put("title", entryLiveData.getValue().getTitle());
        args.put("entries", new String[]{entryLiveData.getValue().getUuid()});
        UstadMobileSystemImpl.getInstance().go("SendCourse", args, getContext());
    }

    protected void handleClickRemove() {
        handleClickRemove(new UstadJSOPDSEntry[]{entry});
    }

    public void handleClickAlternativeTranslationLink(int index) {
        String[] translatedEntryLinks = (String[])alternativeTranslationLinks.elementAt(index);
        UstadJSOPDSEntry translatedEntry = entryFeed != null
                ? entryFeed.findEntryByAlternateHref(translatedEntryLinks[UstadJSOPDSItem.ATTR_HREF])
                : null;
        if(translatedEntry != null) {
            handleOpenEntry(translatedEntry);
        }else {
            String entryUrl = UMFileUtil.resolveLink(entry.getHref(),
                    translatedEntryLinks[UstadJSOPDSItem.ATTR_HREF]);
            handleOpenEntryView(entryUrl);
        }
    }

    public void handleClickSeeAlsoItem(String[] link) {
        if(entryFeed != null) {
            Vector relatedEntryMatch = entryFeed.getEntriesByLinkParams(
                    UstadJSOPDSFeed.LINK_REL_ALTERNATE, null,
                    link[UstadJSOPDSItem.ATTR_HREF], entry.getLanguage());
            if(relatedEntryMatch.size() > 0) {
                handleOpenEntryView((UstadJSOPDSEntry)relatedEntryMatch.elementAt(0));
                return;
            }
        }

        String entryUrl = UMFileUtil.resolveLink(entry.getHref(), link[UstadJSOPDSItem.ATTR_HREF]);
        handleOpenEntryView(entryUrl);
    }

    public void handleClickStopDownload() {
        CatalogEntryInfo entryInfo = CatalogPresenter.getEntryInfo(entry.getItemId(),
                CatalogPresenter.SHARED_RESOURCE | CatalogPresenter.USER_RESOURCE, getContext());
        if(entryInfo != null
                && entryInfo.acquisitionStatus == CatalogPresenter.STATUS_ACQUISITION_IN_PROGRESS
                && entryInfo.downloadID > 0) {
            NetworkTask task = UstadMobileSystemImpl.getInstance().getNetworkManager().getTaskById(
                    entryInfo.downloadID, NetworkManagerCore.QUEUE_ENTRY_ACQUISITION);
            if(task != null)
                task.stop(NetworkTask.STATUS_STOPPED);
        }
    }

    
    public void appViewChoiceSelected(int commandId, int choice) {
        switch(commandId) {
            case CMD_REMOVE_PRESENTER_ENTRY:
//                UstadJSOPDSEntry entryToDelete = (UstadJSOPDSEntry)modifyAcquiredEntries[1].elementAt(choice);
//                handleClickRemove(new UstadJSOPDSEntry[]{entryToDelete});
                break;
        }

        super.appViewChoiceSelected(commandId, choice);
    }

    
    protected void onDownloadStarted() {
        catalogEntryView.setProgressVisible(true);
    }

    
    protected void onEntriesRemoved() {
        updateButtonsByStatus(CatalogPresenter.STATUS_NOT_ACQUIRED);
    }

    
    public void setUIStrings() {

    }

    
    public void acquisitionProgressUpdate(String entryId, final AcquisitionTaskStatus status) {
        if(entry != null && entryId.equals(entry.getItemId())) {
            catalogEntryView.runOnUiThread(new Runnable() {
                
                public void run() {
                    if(status.getTotalSize() == -1) {
                        catalogEntryView.setProgress(-1);
                    }else {
                        float completed = (float) status.getDownloadedSoFar() / (float) status.getTotalSize();
                        catalogEntryView.setProgress(completed);
                        catalogEntryView.setProgressStatusText(formatDownloadStatusText(status));
                    }
                }
            });
        }
    }

    
    public void acquisitionStatusChanged(String entryId, AcquisitionTaskStatus status) {
        if(entryId.equals(entry.getItemId())) {
            switch(status.getStatus()) {
                case UstadMobileSystemImpl.DLSTATUS_SUCCESSFUL:
                    catalogEntryView.runOnUiThread(new Runnable() {
                        
                        public void run() {
                            catalogEntryView.setProgressVisible(false);
                            updateButtonsByStatus(CatalogPresenter.STATUS_ACQUIRED);
                        }
                    });
                    break;

                case NetworkTask.STATUS_STOPPED:
                    catalogEntryView.runOnUiThread(new Runnable() {
                        public void run() {
                            catalogEntryView.setProgressVisible(false);
                        }
                    });
                    break;
                //TODO: handle show download failed
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        manager.removeNetworkManagerListener(this);
        manager.removeAcquisitionTaskListener(this);
    }

    public void fileStatusCheckInformationAvailable(String[] fileIds) {
        if(UMUtil.getIndexInArray(entry.getItemId(), fileIds) != -1) {
            final boolean available = manager.isEntryLocallyAvailable(entry.getItemId());
            updateViewLocallyAvailableStatus(available ?
                    CatalogEntryView.LOCAL_STATUS_AVAILABLE : CatalogEntryView.LOCAL_STATUS_NOT_AVAILABLE);
        }
    }

    
    public void networkTaskStatusChanged(NetworkTask task) {
        /* $if umplatform != 2  $ */
        if(task.getTaskId() == entryCheckTaskId && task.getStatus() == NetworkTask.STATUS_COMPLETE) {
            boolean available =
                UstadMobileSystemImpl.getInstance().getNetworkManager().getEntryResponsesWithLocalFile(entry.getItemId()) != null;
            updateViewLocallyAvailableStatus(available ?
                CatalogEntryView.LOCAL_STATUS_AVAILABLE : CatalogEntryView.LOCAL_STATUS_NOT_AVAILABLE);
            startMonitoringLocalAvailability();
        }
        /* $endif$ */
    }

    private void updateViewLocallyAvailableStatus(final int status) {
        catalogEntryView.runOnUiThread(new Runnable() {
            
            public void run() {
                catalogEntryView.setLocallyAvailableStatus(status);
            }
        });
    }

    /* $if umplatform != 2  $ */
    public void networkNodeDiscovered(NetworkNode node) {

    }

    
    public void networkNodeUpdated(NetworkNode node) {

    }

    /* $endif$ */

    
    public void fileAcquisitionInformationAvailable(String entryId, long downloadId, int downloadSource) {

    }

    
    public void wifiConnectionChanged(String ssid, boolean connected, boolean connectedOrConnecting) {

    }

    @Override
    public void onDialogResult(int commandId, DismissableDialog dialog, Hashtable args) {
        dialog.dismiss();

        if((commandId == LoginController.RESULT_LOGIN_SUCCESSFUL
            || commandId == RegistrationPresenter.RESULT_REGISTRATION_SUCCESS)
            && openAfterLoginOrRegister) {
            openAfterLoginOrRegister = false;
            handleOpenEntry(entry);
        }
    }
}
