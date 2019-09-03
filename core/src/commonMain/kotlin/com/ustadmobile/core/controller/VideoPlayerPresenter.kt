package com.ustadmobile.core.controller

import com.ustadmobile.core.container.ContainerManager
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.db.dao.ContentEntryDao
import com.ustadmobile.core.impl.UstadMobileSystemCommon
import com.ustadmobile.core.impl.UstadMobileSystemCommon.Companion.ARG_REFERRER
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.util.UMFileUtil
import com.ustadmobile.core.view.ContentEntryDetailView
import com.ustadmobile.core.view.HomeView
import com.ustadmobile.core.view.VideoPlayerView
import com.ustadmobile.core.view.VideoPlayerView.Companion.ARG_CONTAINER_UID
import com.ustadmobile.core.view.VideoPlayerView.Companion.ARG_CONTENT_ENTRY_ID
import com.ustadmobile.lib.db.entities.Container
import com.ustadmobile.lib.db.entities.ContainerEntry
import com.ustadmobile.lib.db.entities.ContainerEntryWithContainerEntryFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.io.InputStream

class VideoPlayerPresenter(context: Any, arguments: Map<String, String>?, view: VideoPlayerView,
                           private val db: UmAppDatabase, private val repo: UmAppDatabase)
    : UstadBaseController<VideoPlayerView>(context, arguments!!, view) {

    data class VideoParams(val videoPath: String? = null,
                           val audioPath: ContainerEntryWithContainerEntryFile? = null,
                           val srtLangList: MutableList<String> = mutableListOf(),
                           val srtMap: MutableMap<String, String> = mutableMapOf())

    private var navigation: String? = null

    private lateinit var contentEntryDao: ContentEntryDao

    var audioEntry: ContainerEntryWithContainerEntryFile? = null
        private set

    var audioInput: InputStream? = null
        private set
        get() {
            val audioEntryVal = audioEntry
            return if(audioEntryVal != null) {
                containerManager.getInputStream(audioEntryVal)
            }else {
                null
            }
        }

    var videoPath: String? = null
        private set
    var srtMap = mutableMapOf<String, String>()
        private set
    var srtLangList = mutableListOf<String>()
        private set

    lateinit var container: Container

    lateinit var containerManager: ContainerManager

    var videoParams: VideoParams? = null
        private set

    override fun onCreate(savedState: Map<String, String?>?) {
        super.onCreate(savedState)
        val containerEntryDao = db.containerEntryDao
        val containerDao = db.containerDao
        contentEntryDao = db.contentEntryDao

        navigation = arguments[ARG_REFERRER] ?: ""
        val entryUuid = arguments.getValue(ARG_CONTENT_ENTRY_ID)!!.toLong()
        val containerUid = arguments.getValue(ARG_CONTAINER_UID)!!.toLong()


        GlobalScope.launch(Dispatchers.Main) {
            val contentEntry = contentEntryDao.getContentByUuidAsync(entryUuid)
            if (contentEntry != null)
                view.setVideoInfo(contentEntry)
        }

        GlobalScope.launch {
            val result = contentEntryDao.getContentByUuidAsync(entryUuid)
            view.setVideoInfo(result!!)
        }

        GlobalScope.launch {
            container = containerDao.findByUidAsync(containerUid)!!
            val result = containerEntryDao.findByContainerAsync(containerUid)
            containerManager = ContainerManager(container, db, repo)
            var defaultLangName = ""
            for (entry in result) {

                val fileInContainer = entry.cePath
                val containerEntryFile = entry.containerEntryFile

                if (fileInContainer != null && containerEntryFile != null) {
                    if (fileInContainer.endsWith(".mp4") || fileInContainer.endsWith(".webm")) {
                        videoPath = containerEntryFile.cefPath
                    } else if (fileInContainer == "audio.c2") {
                        //audioInput = containerManager.getInputStream(entry)
                        audioEntry = entry
                    } else if (fileInContainer == "subtitle.srt" || fileInContainer.toLowerCase() == "subtitle-english.srt") {
                        defaultLangName = if (fileInContainer.contains("-"))
                            fileInContainer.substring(fileInContainer.indexOf("-") + 1, fileInContainer.lastIndexOf("."))
                        else "English"
                        srtMap[defaultLangName] = fileInContainer
                    } else {
                        val name = fileInContainer.substring(fileInContainer.indexOf("-") + 1, fileInContainer.lastIndexOf("."))
                        srtMap[name] = fileInContainer
                        srtLangList.add(name)
                    }
                }
            }

            srtLangList.sortedWith(Comparator { a, b ->
                when {
                    a > b -> 1
                    a < b -> -1
                    else -> 0
                }
            })

            if(videoPath.isNullOrEmpty() && result.isNotEmpty()){
                videoPath = result[0].containerEntryFile?.cefPath
            }

            srtLangList.add(0, "No Subtitles")
            if(defaultLangName.isNotEmpty()) srtLangList.add(1, defaultLangName)

            view.runOnUiThread(Runnable {
                videoParams = VideoParams(videoPath, audioEntry, srtLangList, srtMap)
                view.setVideoParams(videoParams!!)
            })
        }

    }

    fun handleUpNavigation() {
        val impl = UstadMobileSystemImpl.instance
        val lastEntryListArgs = UMFileUtil.getLastReferrerArgsByViewname(ContentEntryDetailView.VIEW_NAME, navigation!!)
        if (lastEntryListArgs !=
                null) {
            impl.go(ContentEntryDetailView.VIEW_NAME,
                    UMFileUtil.parseURLQueryString(lastEntryListArgs), view.viewContext,
                    UstadMobileSystemCommon.GO_FLAG_CLEAR_TOP or UstadMobileSystemCommon.GO_FLAG_SINGLE_TOP)
        } else {
            impl.go(HomeView.VIEW_NAME, mapOf(), view.viewContext,
                    UstadMobileSystemCommon.GO_FLAG_CLEAR_TOP or UstadMobileSystemCommon.GO_FLAG_SINGLE_TOP)
        }

    }
}
