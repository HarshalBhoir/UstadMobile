package com.ustadmobile.core.controller

import com.ustadmobile.core.db.JobStatus
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.db.UmLiveData
import com.ustadmobile.core.db.UmObserver
import com.ustadmobile.core.db.dao.ContainerDao
import com.ustadmobile.core.db.dao.NetworkNodeDao
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.impl.NoAppFoundException
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.impl.UmCallback
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.impl.UstadMobileSystemImpl.Companion.ARG_REFERRER
import com.ustadmobile.core.networkmanager.DownloadJobItemManager
import com.ustadmobile.core.networkmanager.DownloadJobItemStatusProvider
import com.ustadmobile.core.networkmanager.LocalAvailabilityMonitor
import com.ustadmobile.core.util.ContentEntryUtil
import com.ustadmobile.core.util.UMFileUtil
import com.ustadmobile.core.view.ContentEntryDetailView
import com.ustadmobile.core.view.ContentEntryListView
import com.ustadmobile.core.view.DummyView
import com.ustadmobile.lib.db.entities.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ContentEntryDetailPresenter(context: Any, arguments: Map<String, String>?,
                                  viewContract: ContentEntryDetailView,
                                  private val monitor: LocalAvailabilityMonitor,
                                  private val statusProvider: DownloadJobItemStatusProvider)
    : UstadBaseController<ContentEntryDetailView>(context, arguments!!, viewContract),
    DownloadJobItemManager.OnDownloadJobItemChangeListener{

    private var navigation: String? = null

    var entryUuid: Long = 0
        private set

    private var containerUid: Long? = 0L

    private var networkNodeDao: NetworkNodeDao? = null

    private var containerDao: ContainerDao? = null

    private val monitorStatus = AtomicBoolean(false)

    private val isListeningToDownloadStatus = AtomicBoolean(false)

    private var statusUmLiveData: UmLiveData<ContentEntryStatus>? = null

    private var statusUmObserver: UmObserver<ContentEntryStatus>? = null

    private val impl: UstadMobileSystemImpl = UstadMobileSystemImpl.instance

    override fun onCreate(map: Map<String, String>?) {
        super.onCreate(map)
        val repoAppDatabase = UmAccountManager.getRepositoryForActiveAccount(getContext())
        val appdb = UmAppDatabase.getInstance(getContext())
        val contentRelatedEntryDao = repoAppDatabase.contentEntryRelatedEntryJoinDao
        val contentEntryDao = repoAppDatabase.contentEntryDao
        val contentEntryStatusDao = appdb.contentEntryStatusDao
        containerDao = repoAppDatabase.containerDao
        networkNodeDao = appdb.networkNodeDao

        entryUuid = java.lang.Long.valueOf(arguments?.get(ARG_CONTENT_ENTRY_UID))
        navigation = arguments?.get(ARG_REFERRER)

        contentEntryDao.getContentByUuid(entryUuid!!, object : UmCallback<ContentEntry> {
            override fun onSuccess(result: ContentEntry) {
                val licenseType = getLicenseType(result)
                view?.runOnUiThread (Runnable{
                    view?.setContentEntryLicense(licenseType)
                    view?.setContentEntryAuthor(result.author)
                    view?.setContentEntryTitle(result.title)
                    view?.setContentEntryDesc(result.description)
                    if (result.thumbnailUrl != null && !result.thumbnailUrl.isEmpty()) {
                        view?.loadEntryDetailsThumbnail(result.thumbnailUrl)
                    }
                })
            }

            override fun onFailure(exception: Throwable) {

            }
        })

        containerDao!!.findFilesByContentEntryUid(entryUuid!!, object : UmCallback<List<Container>> {
            override fun onSuccess(result: List<Container>) {
                view?.runOnUiThread(Runnable {
                    view?.setDetailsButtonEnabled(!result.isEmpty())
                    if (!result.isEmpty()) {
                        val container = result[0]
                        view?.setDownloadSize(container.fileSize)
                    }
                })
            }

            override fun onFailure(exception: Throwable) {

            }
        })

        contentRelatedEntryDao.findAllTranslationsForContentEntry(entryUuid!!,
                object : UmCallback<List<ContentEntryRelatedEntryJoinWithLanguage>> {

                    override fun onSuccess(result: List<ContentEntryRelatedEntryJoinWithLanguage>) {
                        view?.runOnUiThread(Runnable {
                            view?.setTranslationLabelVisible(!result.isEmpty())
                            view?.setFlexBoxVisible(!result.isEmpty())
                            view?.setAvailableTranslations(result, entryUuid!!)
                        })
                    }

                    override fun onFailure(exception: Throwable) {

                    }
                })

        statusUmLiveData = contentEntryStatusDao.findContentEntryStatusByUid(entryUuid!!)
        statusUmObserver = UmObserver { this.onEntryStatusChanged(it) }
        statusUmLiveData!!.observe(this, statusUmObserver)
    }

    private fun getLicenseType(result: ContentEntry): String {
        when (result.licenseType) {
            ContentEntry.LICENSE_TYPE_CC_BY -> return "CC BY"
            ContentEntry.LICENSE_TYPE_CC_BY_SA -> return "CC BY SA"
            ContentEntry.LICENSE_TYPE_CC_BY_SA_NC -> return "CC BY SA NC"
            ContentEntry.LICENSE_TYPE_CC_BY_NC -> return "CC BY NC"
            ContentEntry.LICESNE_TYPE_CC_BY_NC_SA -> return "CC BY NC SA"
            ContentEntry.PUBLIC_DOMAIN -> return "Public Domain"
            ContentEntry.ALL_RIGHTS_RESERVED -> return "All Rights Reserved"
        }
        return ""
    }


    private fun onEntryStatusChanged(status: ContentEntryStatus?) {

        val isDownloadComplete = status != null && status.downloadStatus == JobStatus.COMPLETE

        val buttonLabel = impl.getString(if (status == null || !isDownloadComplete)
            MessageID.download
        else
            MessageID.open, getContext())

        val progressLabel = impl.getString(MessageID.downloading, getContext())

        val isDownloading = (status != null
                && status.downloadStatus >= JobStatus.RUNNING_MIN
                && status.downloadStatus <= JobStatus.RUNNING_MAX)

        if(isDownloading && isListeningToDownloadStatus.get()) {
            isListeningToDownloadStatus.set(true)
            statusProvider.addDownloadChangeListener(this)
            statusProvider.findDownloadJobItemStatusByContentEntryUid(entryUuid,
                    this::onDownloadJobItemChange)
            getView()!!.setDownloadButtonVisible(false)
            getView()!!.setDownloadProgressVisible(true)
        }else if(!isDownloading && isListeningToDownloadStatus.get()) {
            isListeningToDownloadStatus.set(false)
            statusProvider.removeDownloadChangeListener(this)
            getView()!!.setDownloadButtonVisible(true)
            getView()!!.setDownloadProgressVisible(false)
        }

        view?.runOnUiThread(Runnable {
            view?.setButtonTextLabel(buttonLabel)
            view?.setDownloadButtonVisible(!isDownloading)
            view?.setDownloadButtonClickableListener(isDownloadComplete)
            view?.setDownloadProgressVisible(isDownloading)
            view?.setDownloadProgressLabel(progressLabel)
            view?.setLocalAvailabilityStatusViewVisible(isDownloading)
        })


        if (!isDownloadComplete) {
            val currentTimeStamp = System.currentTimeMillis()
            val minLastSeen = currentTimeStamp - TimeUnit.MINUTES.toMillis(1)
            val maxFailureFromTimeStamp = currentTimeStamp - TimeUnit.MINUTES.toMillis(
                    TIME_INTERVAL_FROM_LAST_FAILURE.toLong())

            Thread {

                val container = containerDao!!.getMostRecentContainerForContentEntry(entryUuid!!)
                if (container != null) {
                    containerUid = container.containerUid
                    val localNetworkNode = networkNodeDao!!.findLocalActiveNodeByContainerUid(
                            containerUid!!, minLastSeen, BAD_NODE_FAILURE_THRESHOLD, maxFailureFromTimeStamp)

                    if (localNetworkNode == null && !monitorStatus.get()) {
                        monitorStatus.set(true)
                        monitor.startMonitoringAvailability(this,
                                listOf(containerUid!!))
                    }

                    val monitorSet : MutableSet<Long> = view?.allKnowAvailabilityStatus as MutableSet<Long>

                    monitorSet.add((if (localNetworkNode != null) containerUid else 0L)!!)

                    handleLocalAvailabilityStatus(monitorSet)
                }


            }.start()
        }


    }

    override fun onDownloadJobItemChange(status: DownloadJobItemStatus?) {
        if(status != null && status.contentEntryUid == entryUuid) {
            getView()!!.runOnUiThread(Runnable {
                getView()!!.updateDownloadProgress(
                        if(status.totalBytes > 0) (status.bytesSoFar.toFloat() / status.totalBytes.toFloat()) else 0F)
            })
        }
    }

    fun handleClickTranslatedEntry(uid: Long) {
        val args = HashMap<String, String>()
        args[ARG_CONTENT_ENTRY_UID] = uid.toString()
        impl.go(ContentEntryDetailView.VIEW_NAME, args, view?.context!!)
    }

    fun handleUpNavigation() {
        val lastEntryListArgs = UMFileUtil.getLastReferrerArgsByViewname(ContentEntryListView.VIEW_NAME, navigation!!)
        if (lastEntryListArgs != null) {
            impl.go(ContentEntryListView.VIEW_NAME,
                    UMFileUtil.parseURLQueryString(lastEntryListArgs) as Map<String, String>?, getContext(),
                    UstadMobileSystemImpl.GO_FLAG_CLEAR_TOP or UstadMobileSystemImpl.GO_FLAG_SINGLE_TOP)
        } else {
            impl.go(DummyView.VIEW_NAME, mutableMapOf(), getContext(),
                    UstadMobileSystemImpl.GO_FLAG_CLEAR_TOP or UstadMobileSystemImpl.GO_FLAG_SINGLE_TOP)
        }
    }

    fun handleDownloadButtonClick(isDownloadComplete: Boolean ?, entryUuid: Long?) {
        val repoAppDatabase = UmAccountManager.getRepositoryForActiveAccount(getContext())
        if (isDownloadComplete!!) {
            ContentEntryUtil.goToContentEntry(entryUuid!!, repoAppDatabase, impl, isDownloadComplete,
                    getContext()!!, object : UmCallback<Any> {
                override fun onSuccess(result: Any ?) {

                }

                override fun onFailure(exception: Throwable) {
                    val message = exception.message
                    if (exception is NoAppFoundException) {
                        view?.runOnUiThread (Runnable{
                            view?.showFileOpenError(impl.getString(MessageID.no_app_found, context!!),
                                    MessageID.get_app,
                                    exception.mimeType!!)
                        })
                    } else {
                        view?.runOnUiThread(Runnable { view?.showFileOpenError(message!!) })
                    }
                }
            })


        } else {
            val args = HashMap<String, String>()

            //hard coded strings because these are actually in sharedse
            args["contentEntryUid"] = this.entryUuid.toString()
            view?.runOnUiThread(Runnable { view!!.showDownloadOptionsDialog(args) })
        }

    }

    fun handleLocalAvailabilityStatus(locallyAvailableEntries: Set<Long>) {
        val icon = if (locallyAvailableEntries.contains(
                        containerUid))
            LOCALLY_AVAILABLE_ICON
        else
            LOCALLY_NOT_AVAILABLE_ICON

        val status = impl.getString(
                if (icon == LOCALLY_AVAILABLE_ICON)
                    MessageID.download_locally_availability
                else
                    MessageID.download_cloud_availability, getContext())

        view?.runOnUiThread (Runnable{ view?.updateLocalAvailabilityViews(icon, status) })
    }


    override fun onDestroy() {
        if (monitorStatus.get()) {
            monitorStatus.set(false)
            monitor.stopMonitoringAvailability(this)
        }
        statusUmLiveData!!.removeObserver(statusUmObserver)

        if(isListeningToDownloadStatus.getAndSet(false)) {
            statusProvider.removeDownloadChangeListener(this)
        }

        super.onDestroy()
    }

    companion object {

        const val ARG_CONTENT_ENTRY_UID = "entryid"

        const val LOCALLY_AVAILABLE_ICON = 1

        const val LOCALLY_NOT_AVAILABLE_ICON = 2

        private const val BAD_NODE_FAILURE_THRESHOLD = 3

        private const val TIME_INTERVAL_FROM_LAST_FAILURE = 5

        const val NO_ACTIVITY_FOR_FILE_FOUND = "No activity found for mimetype"
    }

}