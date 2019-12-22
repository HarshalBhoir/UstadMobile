package com.ustadmobile.port.android.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.toughra.ustadmobile.R
import com.ustadmobile.core.db.JobStatus
import com.ustadmobile.core.impl.UMAndroidUtil
import com.ustadmobile.core.networkmanager.downloadmanager.ContainerDownloadManager
import com.ustadmobile.door.DoorLiveData
import com.ustadmobile.lib.db.entities.ContentEntry
import com.ustadmobile.lib.db.entities.ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer
import com.ustadmobile.lib.db.entities.DownloadJobItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*


class ContentEntryListRecyclerViewAdapter internal constructor(private val activity: FragmentActivity,
                                                               private val listener: AdapterViewListener,
                                                               private val containerDownloadManager: ContainerDownloadManager)
    : RepoLoadingPageListAdapter<ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer, ContentEntryListRecyclerViewAdapter.ViewHolder>(DIFF_CALLBACK){

    private val boundViewHolders: MutableSet<ViewHolder> = HashSet()

    private var localAvailabilityMap: Map<Long, Boolean> = mapOf()

    @MainThread
    fun updateLocalAvailability(localAvailabilityMap: Map<Long, Boolean>) {
        boundViewHolders.forEach {
            if(localAvailabilityMap.containsKey(it.containerUid)) {
                it.updateLocallyAvailableStatus(localAvailabilityMap.get(it.containerUid) ?: false)
            }
        }
    }

    interface AdapterViewListener {
        fun contentEntryClicked(entry: ContentEntry?)

        fun downloadStatusClicked(entry: ContentEntry?)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        synchronized(boundViewHolders) {
            boundViewHolders.remove(holder)
        }

        super.onViewRecycled(holder)

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_content_entry, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val entry = getItem(position)

        synchronized(boundViewHolders) {
            boundViewHolders.add(holder)
        }

        holder.downloadJobItemLiveData?.removeObserver(holder)
        GlobalScope.launch(Dispatchers.Main.immediate) {
            holder.downloadJobItemLiveData = containerDownloadManager
                    .getDownloadJobItemByContentEntryUid(entry?.contentEntryUid ?: 0).also {
                        it.observe(activity, holder)
                    }
        }

        if (entry == null) {
            holder.containerUid = 0L
            holder.contentEntryUid = 0L
            holder.entryTitle.text = ""
            holder.entryDescription.text = ""
            holder.thumbnailView.setImageDrawable(null)
            holder.downloadView.progress = 0
            holder.downloadView.setImageResource(R.drawable.ic_file_download_black_24dp)
            holder.view.setOnClickListener(null)
            holder.downloadView.setOnClickListener(null)
            holder.availabilityStatus.text = ""
            holder.availabilityIcon.setImageDrawable(null)
        } else {
            holder.containerUid = entry.mostRecentContainer?.containerUid ?: 0L
            holder.contentEntryUid = entry.contentEntryUid

            holder.view.tag = entry.contentEntryUid
            holder.entryTitle.text = entry.title
            holder.entryDescription.text = entry.description
            if (entry.thumbnailUrl == null || entry.thumbnailUrl!!.isEmpty()) {
                holder.thumbnailView.setImageDrawable(null)
            } else {
                UMAndroidUtil.loadImage(entry.thumbnailUrl,R.drawable.img_placeholder,
                        holder.thumbnailView)
            }


            var contentDescription: String? = null
            var showLocallyAvailabilityViews = true

            val iconView = holder.iconView
            val iconFlag = entry.contentTypeFlag
            iconView.setImageResource(
                    if (CONTENT_TYPE_TO_ICON_RES_MAP.containsKey(entry.contentTypeFlag))
                        CONTENT_TYPE_TO_ICON_RES_MAP[entry.contentTypeFlag]!!
                    else
                        R.drawable.ic_book_black_24dp)

            if (iconFlag == ContentEntry.TYPE_UNDEFINED) {
                iconView.visibility = View.GONE
            } else {
                iconView.visibility = View.VISIBLE
            }

            val viewVisibility = if (showLocallyAvailabilityViews && entry.leaf)
                View.VISIBLE
            else
                View.GONE
            holder.availabilityIcon.visibility = viewVisibility
            holder.availabilityStatus.visibility = viewVisibility

            holder.downloadView.imageResource!!.contentDescription = contentDescription
            holder.view.setOnClickListener { listener.contentEntryClicked(entry) }
            holder.downloadView.setOnClickListener { listener.downloadStatusClicked(entry) }
            holder.downloadView.progress = 0
            holder.updateLocallyAvailableStatus(
                    localAvailabilityMap.get(entry.mostRecentContainer?.containerUid ?: 0L) ?: false)
        }
    }

    inner class ViewHolder internal constructor(val view: View) : RecyclerView.ViewHolder(view),
        Observer<DownloadJobItem?> {
        internal val entryTitle: TextView = view.findViewById(R.id.content_entry_item_title)
        internal val entryDescription: TextView = view.findViewById(R.id.content_entry_item_description)
        private val entrySize: TextView = view.findViewById(R.id.content_entry_item_library_size)
        internal val thumbnailView: ImageView = view.findViewById(R.id.content_entry_item_thumbnail)
        val availabilityIcon: ImageView = view.findViewById(R.id.content_entry_local_availability_icon)
        val availabilityStatus: TextView = view.findViewById(R.id.content_entry_local_availability_status)
        val downloadView: DownloadStatusButton = view.findViewById(R.id.content_entry_item_download)
        val iconView: ImageView = view.findViewById(R.id.content_entry_item_imageview)

        internal var containerUid: Long = 0

        var contentEntryUid: Long = 0

        private var currentDownloadStatus = -1

        internal var downloadJobItemLiveData: DoorLiveData<DownloadJobItem?>? = null

        internal fun updateLocallyAvailableStatus(available: Boolean) {
            val icon = if (available)
                R.drawable.ic_nearby_black_24px
            else
                R.drawable.ic_cloud_download_black_24dp
            val status = if (available)
                R.string.download_locally_availability
            else
                R.string.download_cloud_availability
            availabilityIcon.setImageResource(icon)
            availabilityStatus.text = view.context.getString(status)
        }

        override fun toString(): String {
            return super.toString() + " '" + entryDescription.text + "'"
        }


        override fun onChanged(t: DownloadJobItem?) {
            if(t?.djiStatus != currentDownloadStatus) {
                val context = view.context
                //val status = entry.contentEntryStatus
                val dlStatus = t?.djiStatus ?: 0

                var localAvailabilityVisible = true

                var contentDescription = if (dlStatus > 0 && dlStatus <= JobStatus.RUNNING_MAX) {
                    context.getString(R.string.downloading)
                } else {
                    context.getString(R.string.download_entry_state_queued)
                }

                if (dlStatus > 0 && dlStatus < JobStatus.WAITING_MAX) {
                    downloadView.setImageResource(R.drawable.ic_pause_black_24dp)
                    contentDescription = context.getString(R.string.download_entry_state_paused)
                } else if (dlStatus == JobStatus.COMPLETE) {
                    localAvailabilityVisible = false
                    downloadView.setImageResource(R.drawable.ic_offline_pin_black_24dp)
                    contentDescription = context.getString(R.string.downloaded)
                } else {
                    downloadView.setImageResource(R.drawable.ic_file_download_black_24dp)
                }

                downloadView.progressVisibility = if(dlStatus <= JobStatus.COMPLETE_MIN) {
                    View.VISIBLE
                }else {
                    View.INVISIBLE
                }

            }

            if(t != null && t.djiStatus >= JobStatus.WAITING_MIN
                    && t.djiStatus < JobStatus.COMPLETE_MIN) {
                downloadView.progress = if(t.downloadLength> 0)
                    (t.downloadedSoFar* 100 / t.downloadLength).toInt()
                else
                    0
            }else{
                downloadView.progress = 0
            }
        }
    }

    companion object {

        private val CONTENT_TYPE_TO_ICON_RES_MAP = HashMap<Int, Int>()

        init {
            CONTENT_TYPE_TO_ICON_RES_MAP[ContentEntry.TYPE_EBOOK] = R.drawable.ic_book_black_24dp
            CONTENT_TYPE_TO_ICON_RES_MAP[ContentEntry.TYPE_AUDIO] = R.drawable.ic_audiotrack_24px
            CONTENT_TYPE_TO_ICON_RES_MAP[ContentEntry.TYPE_VIDEO] = R.drawable.ic_video_library_24px
            CONTENT_TYPE_TO_ICON_RES_MAP[ContentEntry.TYPE_COLLECTION] = R.drawable.ic_collections_24px
            CONTENT_TYPE_TO_ICON_RES_MAP[ContentEntry.TYPE_DOCUMENT] = R.drawable.ic_file_24px
            CONTENT_TYPE_TO_ICON_RES_MAP[ContentEntry.TYPE_INTERACTIVE_EXERCISE] = R.drawable.ic_assignment_24px
            CONTENT_TYPE_TO_ICON_RES_MAP[ContentEntry.TYPE_ARTICLE] = R.drawable.ic_newspaper
        }

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer>() {
            override fun areItemsTheSame(oldItem: ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer,
                                         newItem: ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer): Boolean {
                return oldItem.contentEntryUid == newItem.contentEntryUid
            }

            override fun areContentsTheSame(oldItem: ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer,
                                            newItem: ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer): Boolean {
                if (if (oldItem.title != null) oldItem.title != newItem.title else newItem.title != null) {
                    return false
                }
                if (if (oldItem.description != null) oldItem.description != newItem.description else newItem.description != null)
                    return false
                if (if (oldItem.thumbnailUrl != null) oldItem.thumbnailUrl != newItem.thumbnailUrl else newItem.thumbnailUrl == null) {
                    return false
                }
            }
        }
    }
}
