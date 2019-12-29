package com.ustadmobile.port.android.view

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.paging.DataSource
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.toughra.ustadmobile.R
import com.ustadmobile.core.controller.ContentEntryListPresenter
import com.ustadmobile.core.controller.ContentEntryListPresenter.Companion.ARG_DOWNLOADED_CONTENT
import com.ustadmobile.core.controller.ContentEntryListPresenter.Companion.ARG_LIBRARIES_CONTENT
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.impl.UMAndroidUtil.bundleToMap
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.networkmanager.AvailabilityMonitorRequest
import com.ustadmobile.core.networkmanager.LocalAvailabilityManager
import com.ustadmobile.core.view.ContentEntryListFragmentView
import com.ustadmobile.core.view.ContentEntryListFragmentView.Companion.EDIT_BUTTONS_ADD_CONTENT
import com.ustadmobile.core.view.ContentEntryListFragmentView.Companion.EDIT_BUTTONS_EDITOPTION
import com.ustadmobile.core.view.ContentEntryListFragmentView.Companion.EDIT_BUTTONS_NEWFOLDER
import com.ustadmobile.door.ext.asRepositoryLiveData
import com.ustadmobile.lib.db.entities.ContentEntry
import com.ustadmobile.lib.db.entities.ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer
import com.ustadmobile.lib.db.entities.DistinctCategorySchema
import com.ustadmobile.lib.db.entities.Language
import com.ustadmobile.port.android.view.ext.activeRange
import com.ustadmobile.port.android.view.ext.makeSnackbarIfRequired
import com.ustadmobile.sharedse.network.NetworkManagerBle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference


/**
 * A fragment representing a list of Items.
 *
 *
 * Activities containing this fragment MUST implement the []
 * interface.
 */
/**
 * Mandatory empty constructor for the fragment manager to instantiate the
 * fragment (e.g. upon screen orientation changes).
 */
class ContentEntryListFragment : UstadBaseFragment(), ContentEntryListFragmentView,
        ContentEntryListRecyclerViewAdapter.AdapterViewListener{


    interface ContentEntryListHostActivity {

        fun setTitle(title: String)

        fun setFilterSpinner(idToValuesMap: Map<Long, List<DistinctCategorySchema>>)

        fun setLanguageFilterSpinner(result: List<Language>)

    }


    private var buttonVisibilityFlags: Int = 0

    override val viewContext: Any
        get() = context!!

    private var presenter: ContentEntryListPresenter? = null

    private lateinit var recyclerView: RecyclerView

    private var contentEntryListHostActivity: ContentEntryListHostActivity? = null

    private lateinit var ustadBaseActivity: UstadBaseActivity

    private lateinit var managerAndroidBle: NetworkManagerBle

    private var recyclerAdapter: ContentEntryListRecyclerViewAdapter? = null

    private var savedInstanceState: Bundle? = null

    private lateinit var rootContainer: View

    private lateinit var repoLoadingStatusView: RepoLoadingStatusView

    internal class LocalAvailabilityPagedListCallback(private val localAvailabilityManager: LocalAvailabilityManager,
                                                      var pagedList: PagedList<ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer>?,
                                                      private val onEntityAvailabilityChanged: (Map<Long, Boolean>) -> Unit) : PagedList.Callback() {

        private val availabilityMonitorRequest = AtomicReference<AvailabilityMonitorRequest?>()

        private val activeRange = AtomicReference(Pair(-1, -1))

        override fun onChanged(position: Int, count: Int) {
            handleActiveRangeChanged()
        }

        override fun onInserted(position: Int, count: Int) {
            handleActiveRangeChanged()
        }

        override fun onRemoved(position: Int, count: Int) {
            handleActiveRangeChanged()
        }

        private fun handleActiveRangeChanged() {
            val currentPagedList = pagedList
            val currentActiveRange = currentPagedList?.activeRange()
            if(currentPagedList != null && currentActiveRange != null
                    && !activeRange.compareAndSet(currentActiveRange, currentActiveRange)) {
                val containerUidsToMonitor = (currentActiveRange.first until currentActiveRange.second)
                        .fold(mutableListOf<Long>(), {uidList, index ->
                            val contentEntry = currentPagedList[index]
                            if(contentEntry != null && contentEntry.leaf) {
                                val mostRecentContainerUid = contentEntry.mostRecentContainer?.containerUid ?: 0L
                                if(mostRecentContainerUid != 0L) {
                                    uidList += mostRecentContainerUid
                                }
                            }
                            uidList
                        })
                val newRequest = if(containerUidsToMonitor.isNotEmpty()) {
                    AvailabilityMonitorRequest(containerUidsToMonitor, onEntityAvailabilityChanged)
                }else {
                    null
                }
                val oldRequest = availabilityMonitorRequest.getAndSet(newRequest)
                if(oldRequest != null) {
                    localAvailabilityManager.removeMonitoringRequest(oldRequest)
                }

                if(newRequest != null) {
                    localAvailabilityManager.addMonitoringRequest(newRequest)
                }
            }
        }

        fun onDestroy() {
            val currentRequest = availabilityMonitorRequest.getAndSet(null)
            if(currentRequest != null){
                localAvailabilityManager.removeMonitoringRequest(currentRequest)
            }
        }
    }


    private var listSnapShot: List<ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer?> = listOf()

    private var localAvailabilityPagedListCallback: LocalAvailabilityPagedListCallback? = null

    fun filterByLang(langUid: Long) {
        presenter?.handleClickFilterByLanguage(langUid)
    }

    fun filterBySchemaCategory(contentCategoryUid: Long) {
        presenter?.handleClickFilterByCategory(contentCategoryUid)
    }

    fun clickUpNavigation() {
        presenter?.handleUpNavigation()
    }

    override fun setCategorySchemaSpinner(spinnerData: Map<Long, List<DistinctCategorySchema>>) {
        runOnUiThread(Runnable {
            contentEntryListHostActivity?.setFilterSpinner(spinnerData)
        })
    }

    override fun setLanguageOptions(result: List<Language>) {
        runOnUiThread(Runnable{
            contentEntryListHostActivity?.setLanguageFilterSpinner(result)
        })
    }

    override fun setEditButtonsVisibility(buttonVisibilityFlags: Int) {
        this.buttonVisibilityFlags = buttonVisibilityFlags
        activity?.invalidateOptionsMenu()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        menu?.findItem(R.id.create_new_folder)?.isVisible = (buttonVisibilityFlags and EDIT_BUTTONS_NEWFOLDER) == EDIT_BUTTONS_NEWFOLDER
        menu?.findItem(R.id.edit_category_content)?.isVisible =  (buttonVisibilityFlags and EDIT_BUTTONS_EDITOPTION) == EDIT_BUTTONS_EDITOPTION
        menu?.findItem(R.id.create_new_content)?.isVisible = (buttonVisibilityFlags and EDIT_BUTTONS_ADD_CONTENT) == EDIT_BUTTONS_ADD_CONTENT
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.edit_category_content -> presenter?.handleClickEditButton()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        rootContainer = inflater.inflate(R.layout.fragment_contententry_list, container, false)
        this.savedInstanceState = savedInstanceState

        // Set the adapter
        val context = rootContainer.context
        recyclerView = rootContainer.findViewById(R.id.content_entry_list)
        repoLoadingStatusView = rootContainer.findViewById(R.id.statusView)
        recyclerView.layoutManager = LinearLayoutManager(context)


        val isLibrarySection = bundleToMap(arguments).containsKey(ARG_LIBRARIES_CONTENT)
        val isDownloadedSection = bundleToMap(arguments).containsKey(ARG_DOWNLOADED_CONTENT)
        val emptyMessage: Int
        val resource = when {
            isLibrarySection -> {
                emptyMessage = R.string.empty_state_libraries
                R.drawable.ic_file_download_black_24dp}
            isDownloadedSection -> {
                emptyMessage = R.string.empty_state_downloaded
                R.drawable.ic_folder_black_24dp
            }
            else -> {
                emptyMessage = R.string.empty_state_recycle
                R.drawable.ic_delete_black_24dp
            }
        }


        repoLoadingStatusView.emptyStatusImage = resource
        repoLoadingStatusView.emptyStatusText = emptyMessage

        val dividerItemDecoration = DividerItemDecoration(context,
                LinearLayoutManager.VERTICAL)
        recyclerView.addItemDecoration(dividerItemDecoration)

        GlobalScope.launch(Dispatchers.Main) {
            val networkManagerVal = ustadBaseActivity.networkManagerBle.await()
            managerAndroidBle = networkManagerVal
            showSnackbarPromptsIfRequired()


            //create entry adapter here to make sure bleManager is not null
            val thisFrag = this@ContentEntryListFragment
            recyclerAdapter = ContentEntryListRecyclerViewAdapter(ustadBaseActivity, thisFrag,
                    managerAndroidBle.containerDownloadManager)

            recyclerAdapter?.fistItemLoadedListener = repoLoadingStatusView

            localAvailabilityPagedListCallback = LocalAvailabilityPagedListCallback(
                    managerAndroidBle.localAvailabilityManager, null) { availabilityMap ->
                runOnUiThread(Runnable { recyclerAdapter?.updateLocalAvailability(availabilityMap) })
            }

            val umDb = UmAppDatabase.getInstance(ustadBaseActivity)
            val umRepoDb = UmAccountManager.getRepositoryForActiveAccount(ustadBaseActivity)
            presenter = ContentEntryListPresenter(context as Context,
                    bundleToMap(arguments), thisFrag, umDb.contentEntryDao,
                    umRepoDb.contentEntryDao, UmAccountManager.getActiveAccount(context), UstadMobileSystemImpl.instance, umRepoDb).also {
                it.onCreate(bundleToMap(savedInstanceState))
            }
        }


        return rootContainer
    }


    private fun showSnackbarPromptsIfRequired() {
        val currentContext = context
        if(currentContext != null && ::managerAndroidBle.isInitialized
                && ::rootContainer.isInitialized){
            managerAndroidBle.enablePromptsSnackbarManager.makeSnackbarIfRequired(rootContainer,
                    currentContext)
        }
    }

    override fun onAttach(context: Context?) {
        if (context is ContentEntryListHostActivity) {
            this.contentEntryListHostActivity = context
        }

        if (context is UstadBaseActivity) {
            this.ustadBaseActivity = context
        }

        super.onAttach(context)
    }

    override fun onResume() {
        super.onResume()
        if(::managerAndroidBle.isInitialized) {
            showSnackbarPromptsIfRequired()
        }
    }

    override fun onDetach() {
        super.onDetach()
        this.contentEntryListHostActivity = null
    }


    override fun setContentEntryProvider(entryProvider: DataSource.Factory<Int, ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer>) {
        val data = entryProvider.asRepositoryLiveData(
                UmAccountManager.getRepositoryForActiveAccount(ustadBaseActivity).contentEntryDao,
                repoLoadingStatusView)

        data.observe(this, Observer<PagedList<ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer>> {
            recyclerAdapter!!.submitList(it)
            localAvailabilityPagedListCallback!!.pagedList = it
            it.addWeakCallback(listSnapShot, localAvailabilityPagedListCallback!!)
        })

        recyclerView.adapter = recyclerAdapter
    }

    override fun setToolbarTitle(title: String) {
        runOnUiThread(Runnable {
            contentEntryListHostActivity?.setTitle(title)
        })
    }

    override fun showError() {
        Toast.makeText(context, R.string.content_entry_not_found, Toast.LENGTH_SHORT).show()
    }


    override fun contentEntryClicked(entry: ContentEntry?) {
        runOnUiThread(Runnable {
            if(entry != null) {
                presenter?.handleContentEntryClicked(entry)
            }
        })
    }

    override fun downloadStatusClicked(entry: ContentEntry) {
        val impl = UstadMobileSystemImpl.instance
        if(::ustadBaseActivity.isInitialized){
            ustadBaseActivity.runAfterGrantingPermission(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    Runnable { presenter?.handleDownloadStatusButtonClicked(entry) },
                    impl.getString(MessageID.download_storage_permission_title, context!!),
                    impl.getString(MessageID.download_storage_permission_message, context!!))
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        localAvailabilityPagedListCallback?.onDestroy()
    }

    fun handleBottomSheetClicked(contentType: Int) {
        presenter?.handleClickAddContent(contentType)
    }

    companion object {

        fun newInstance(args: Bundle): ContentEntryListFragment {
            val fragment = ContentEntryListFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
