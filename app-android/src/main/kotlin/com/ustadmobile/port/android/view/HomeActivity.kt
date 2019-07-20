package com.ustadmobile.port.android.view

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.toughra.ustadmobile.R
import com.ustadmobile.core.controller.ContentEntryListFragmentPresenter
import com.ustadmobile.core.controller.ContentEntryListFragmentPresenter.Companion.ARG_CONTENT_ENTRY_UID
import com.ustadmobile.core.controller.ContentEntryListFragmentPresenter.Companion.ARG_DOWNLOADED_CONTENT
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.impl.AppConfig
import com.ustadmobile.core.impl.UMAndroidUtil
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.view.AboutView
import com.ustadmobile.core.view.ContentEditorView.Companion.CONTENT_ENTRY_UID
import com.ustadmobile.core.view.ContentEntryEditView
import com.ustadmobile.core.view.ContentEntryEditView.Companion.CONTENT_ENTRY_LEAF
import com.ustadmobile.core.view.ContentEntryEditView.Companion.CONTENT_TYPE
import com.ustadmobile.core.view.ContentEntryListView.Companion.CONTENT_CREATE_FOLDER
import com.ustadmobile.core.view.HomeView
import com.ustadmobile.sharedse.network.NetworkManagerBle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.dimorinny.floatingtextbutton.FloatingTextButton

class HomeActivity : UstadBaseWithContentOptionsActivity(), HomeView {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val toolbar = findViewById<Toolbar>(R.id.entry_toolbar)
        coordinatorLayout = findViewById(R.id.coordinationLayout)
        setSupportActionBar(toolbar)
        supportActionBar!!.setTitle(R.string.app_name)

        val viewPager = findViewById<ViewPager>(R.id.library_viewpager)
        viewPager.adapter = LibraryPagerAdapter(supportFragmentManager, this)
        val tabLayout = findViewById<TabLayout>(R.id.tabs)
        tabLayout.setupWithViewPager(viewPager)

        findViewById<FloatingTextButton>(R.id.download_all).setOnClickListener {
            val args = HashMap<String, String>()
            args["contentEntryUid"] = MASTER_SERVER_ROOT_ENTRY_UID.toString()
            UstadMobileSystemImpl.instance.go("DownloadDialog", args, this@HomeActivity)
        }


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val showControls = UstadMobileSystemImpl.instance.getAppConfigString(
                AppConfig.KEY_SHOW_CONTENT_EDITOR_CONTROLS, null, this)!!.toBoolean()
        menuInflater.inflate(R.menu.menu_home_activity, menu)
        menu.findItem(R.id.create_new_content).isVisible = showControls
        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_open_about -> UstadMobileSystemImpl.instance.go(AboutView.VIEW_NAME, this)
            R.id.action_clear_history -> {
                GlobalScope.launch {
                    val database = UmAppDatabase.getInstance(this)
                    database.networkNodeDao.deleteAllAsync()
                    database.entryStatusResponseDao.deleteAllAsync()
                    database.downloadJobItemHistoryDao.deleteAllAsync()
                    database.downloadJobDao.deleteAllAsync()
                    database.downloadJobItemDao.deleteAllAsync()
                    database.contentEntryStatusDao.deleteAllAsync()
                }
                networkManagerBle?.clearHistories()
            }
            R.id.create_new_content -> {
                val args = HashMap<String,String?>()
                args.putAll(UMAndroidUtil.bundleToMap(intent.extras))
                args[CONTENT_TYPE] = CONTENT_CREATE_FOLDER.toString()
                args[CONTENT_ENTRY_LEAF] = false.toString()
                args[CONTENT_ENTRY_UID] = 0.toString()
                args[ARG_CONTENT_ENTRY_UID] = MASTER_SERVER_ROOT_ENTRY_UID.toString()
                UstadMobileSystemImpl.instance.go(ContentEntryEditView.VIEW_NAME, args,
                        this)
            }
            R.id.action_send_feedback -> hearShake()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBleNetworkServiceBound(networkManagerBle: NetworkManagerBle) {
        super.onBleNetworkServiceBound(networkManagerBle)
        val impl = UstadMobileSystemImpl.instance
        runAfterGrantingPermission(Manifest.permission.ACCESS_COARSE_LOCATION,
                Runnable { networkManagerBle.checkP2PBleServices() },
                impl.getString(MessageID.location_permission_title, this),
                impl.getString(MessageID.location_permission_message, this))
    }

    class LibraryPagerAdapter internal constructor(fragmentManager: FragmentManager, private val context: Context) : FragmentPagerAdapter(fragmentManager) {
        private val impl: UstadMobileSystemImpl = UstadMobileSystemImpl.instance

        // Returns total number of pages
        override fun getCount(): Int {
            return NUM_ITEMS
        }

        // Returns the fragment to display for that page
        override fun getItem(position: Int): Fragment? {
            val bundle = Bundle()

            return when (position) {
                0 // Fragment # 0 - This will show FirstFragment
                -> {
                    bundle.putString(ARG_CONTENT_ENTRY_UID, MASTER_SERVER_ROOT_ENTRY_UID.toString())
                    ContentEntryListFragment.newInstance(bundle)
                }
                1 // Fragment # 0 - This will show FirstFragment different title
                -> {
                    bundle.putString(ARG_DOWNLOADED_CONTENT, "")
                    ContentEntryListFragment.newInstance(bundle)
                }
                else -> null
            }
        }

        // Returns the page title for the top indicator
        override fun getPageTitle(position: Int): CharSequence? {

            when (position) {
                0 -> return impl.getString(MessageID.libraries, context)
                1 -> return impl.getString(MessageID.downloaded, context)
            }
            return null

        }

        companion object {
            private const val NUM_ITEMS = 2
        }

    }

    companion object {
        const val MASTER_SERVER_ROOT_ENTRY_UID = 12347120167L
    }
}
