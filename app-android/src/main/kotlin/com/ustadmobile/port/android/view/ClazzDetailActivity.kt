package com.ustadmobile.port.android.view

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.toughra.ustadmobile.R
import com.ustadmobile.core.controller.ClazzDetailPresenter
import com.ustadmobile.core.impl.UMAndroidUtil
import com.ustadmobile.core.view.ClassDetailView
import com.ustadmobile.core.view.ClazzListView.Companion.ARG_CLAZZ_UID
import java.util.*

/**
 * The ClassDetail activity.
 *
 * This Activity extends UstadBaseActivity and implements ClassDetailView
 */
class ClazzDetailActivity : UstadBaseActivity(), ClassDetailView, TabLayout.OnTabSelectedListener {

    private lateinit var mPager: ViewPager
    private lateinit var mPagerAdapter: ClassDetailViewPagerAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var mTabLayout: TabLayout
    private lateinit var mPresenter: ClazzDetailPresenter
    internal var currentClazzUid: Long = 0
    private var attendanceVisibility: Boolean = false
    private var activityVisibility: Boolean = false
    private var selVisibility: Boolean = false
    private var settingsVisibility: Boolean = false
    internal var menu: Menu? = null

    private val fragPosMap = HashMap<Int, Class<*>>()

    /**
     * Separated out view pager setup for clarity.
     */
    override fun setupViewPager() {

        runOnUiThread {
            mPager = findViewById(R.id.class_detail_view_pager_container)
            mPagerAdapter = ClassDetailViewPagerAdapter(supportFragmentManager)
            var fragCount = 0
            mPagerAdapter.addFragments(fragCount, ClazzStudentListFragment.newInstance(currentClazzUid))
            fragPosMap[fragCount++] = ClazzStudentListFragment::class.java

            if (attendanceVisibility) {
                mPagerAdapter.addFragments(fragCount, ClazzLogListFragment.newInstance(currentClazzUid))
                fragPosMap[fragCount++] = ClazzLogListFragment::class.java
            }

            if (activityVisibility) {
                mPagerAdapter.addFragments(fragCount, ClazzActivityListFragment.newInstance(currentClazzUid))
                fragPosMap[fragCount++] = ClazzActivityListFragment::class.java
            }

            if (selVisibility) {
                mPagerAdapter.addFragments(fragCount, SELAnswerListFragment.newInstance(currentClazzUid))
                fragPosMap[fragCount++] = SELAnswerListFragment::class.java!!
            }

            mPager.setAdapter(mPagerAdapter)

            mTabLayout = findViewById(R.id.activity_class_detail_tablayout)
            mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL)
            mTabLayout.setupWithViewPager(mPager)

        }

    }

    /**
     * The ClazzDetailActivity's onCreate get the Clazz UID from arguments given to it
     * and sets up TabLayout.
     * @param savedInstanceState    Android bundle
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Setting layout:
        setContentView(R.layout.activity_clazz_detail)

        if (Objects.requireNonNull(intent.extras).get(ARG_CLAZZ_UID) is String) {
            currentClazzUid = java.lang.Long.valueOf(intent.getStringExtra(ARG_CLAZZ_UID))
        } else {
            currentClazzUid = intent.getLongExtra(ARG_CLAZZ_UID, 0L)
        }

        toolbar = findViewById(R.id.class_detail_toolbar)
        //Set title as Class name
        toolbar!!.setTitle("Class")
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        //Presenter
        mPresenter = ClazzDetailPresenter(this,
                UMAndroidUtil.bundleToMap(intent.extras), this)
        mPresenter!!.onCreate(UMAndroidUtil.bundleToMap(savedInstanceState))

    }

    public override fun onResume() {
        super.onResume()

        //Update title
        mPresenter!!.updateToolbarTitle()
    }

    /**
     * Creates the options on the toolbar - specifically the Done tick menu item
     * @param menu  The menu options
     * @return  true. always.
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_clazzdetail, menu)

        val settingsGearMenuItem = menu!!.findItem(R.id.menu_clazzdetail_gear)
        val gearIcon = AppCompatResources.getDrawable(applicationContext, R.drawable.ic_settings_white_24dp)
        gearIcon!!.setColorFilter(resources.getColor(R.color.icons), PorterDuff.Mode.SRC_IN)
        settingsGearMenuItem!!.setIcon(gearIcon)

        if (menu != null) {
            if (settingsGearMenuItem != null)
                settingsGearMenuItem.isVisible = settingsVisibility
        }

        return true
    }


    /**
     * Handles Action Bar menu button click.
     * @param item  The MenuItem clicked.
     * @return  Boolean if handled or not.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        val i = item.itemId
        //If this activity started from other activity
        if (i == R.id.menu_clazzdetail_gear) {
            mPresenter!!.handleClickClazzEdit()
            return super.onOptionsItemSelected(item)
        } else if (i == R.id.menu_clazzdetail_search) {
            mPresenter!!.handleClickSearch()
            return super.onOptionsItemSelected(item)
        } else if (i == android.R.id.home) {
            onBackPressed()
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    //Tab layout's on Tab selected
    override fun onTabSelected(tab: TabLayout.Tab) {
        mPagerAdapter!!.getItem(tab.getPosition()) //Loads first fragment
        mPagerAdapter!!.notifyDataSetChanged()
        mPager!!.setCurrentItem(tab.getPosition())
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {

    }

    override fun onTabReselected(tab: TabLayout.Tab) {

    }

    override fun setToolbarTitle(toolbarTitle: String) {
        runOnUiThread {
            toolbar!!.setTitle(toolbarTitle)
            setSupportActionBar(toolbar)
            Objects.requireNonNull(supportActionBar)!!.setDisplayHomeAsUpEnabled(true)
        }

    }

    override fun setAttendanceVisibility(visible: Boolean) {
        attendanceVisibility = visible
    }

    override fun setActivityVisibility(visible: Boolean) {
        activityVisibility = visible
    }

    override fun setSELVisibility(visible: Boolean) {
        selVisibility = visible
    }

    override fun setSettingsVisibility(visible: Boolean) {
        settingsVisibility = visible
    }

    /**
     * ClassDetailView's view pager adapter
     */
    private inner class ClassDetailViewPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getCount(): Int {
            return positionMap.size
        }

        //Map of position and fragment
        internal var positionMap: WeakHashMap<Int, UstadBaseFragment>


        init {
            positionMap = WeakHashMap()
        }

        fun addFragments(pos: Int, fragment: Fragment) {
            positionMap[pos] = fragment as UstadBaseFragment
        }

        /**
         * Generates fragment for that page/position
         *
         * @param position The position of the fragment to generate
         * @return void
         */
        override fun getItem(position: Int): Fragment? {
            val thisFragment = positionMap[position]
            if (thisFragment != null) {
                return thisFragment
            } else {
                val fragClass = fragPosMap[position]
                return if (fragClass == ClazzStudentListFragment::class.java) {
                    ClazzStudentListFragment.newInstance(currentClazzUid)
                } else if (fragClass == ClazzLogListFragment::class.java) {
                    ClazzLogListFragment.newInstance(currentClazzUid!!)
                } else if (fragClass == ClazzActivityListFragment::class.java) {
                    ClazzActivityListFragment.newInstance(currentClazzUid!!) as Fragment
                } else if (fragClass == SELAnswerListFragment::class.java) {
                    SELAnswerListFragment.newInstance(currentClazzUid) as Fragment
                } else {
                    null
                }
            }
        }

        /**
         * Gets the title of the tab position
         *
         * @param position the position of the tab
         * @return void
         */
        override fun getPageTitle(position: Int): CharSequence {
            val fragClass = fragPosMap[position]!!
            return if (fragClass == ClazzStudentListFragment::class.java) {
                (getText(R.string.students_literal) as String).toUpperCase()
            } else if (fragClass == ClazzLogListFragment::class.java) {
                (getText(R.string.attendance) as String).toUpperCase()
            } else if (fragClass == ClazzActivityListFragment::class.java) {
                (getText(R.string.activity) as String).toUpperCase()
            } else if (fragClass == SELAnswerListFragment::class.java) {
                (getText(R.string.sel) as String).toUpperCase()
            } else {
                ""
            }
        }
    }

}
