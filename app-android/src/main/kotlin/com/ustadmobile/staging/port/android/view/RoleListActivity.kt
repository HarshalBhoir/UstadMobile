package com.ustadmobile.staging.port.android.view

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.toughra.ustadmobile.R
import com.ustadmobile.core.controller.RoleListPresenter
import com.ustadmobile.core.impl.UMAndroidUtil
import com.ustadmobile.core.view.RoleListView
import com.ustadmobile.lib.db.entities.Role
import com.ustadmobile.port.android.view.UstadBaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.dimorinny.floatingtextbutton.FloatingTextButton

class RoleListActivity : UstadBaseActivity(), RoleListView {

    private var toolbar: Toolbar? = null
    private var mPresenter: RoleListPresenter? = null
    private var mRecyclerView: RecyclerView? = null


    /**
     * This method catches menu buttons/options pressed in the toolbar. Here it is making sure
     * the activity goes back when the back button is pressed.
     *
     * @param item The item selected
     * @return true if accounted for
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Setting layout:
        setContentView(R.layout.activity_role_list)

        //Toolbar:
        toolbar = findViewById(R.id.activity_role_list_toolbar)
        toolbar!!.title = getText(R.string.roles)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        //RecyclerView
        mRecyclerView = findViewById(
                R.id.activity_role_list_recyclerview)
        val mRecyclerLayoutManager = LinearLayoutManager(applicationContext)
        mRecyclerView!!.layoutManager = mRecyclerLayoutManager

        //Call the Presenter
        mPresenter = RoleListPresenter(this,
                UMAndroidUtil.bundleToMap(intent.extras), this)
        mPresenter!!.onCreate(UMAndroidUtil.bundleToMap(savedInstanceState))

        //FAB and its listener
        val fab = findViewById<FloatingTextButton>(R.id.activity_role_list_fab)

        fab.setOnClickListener { v -> mPresenter!!.handleClickPrimaryActionButton() }


    }

    override fun setListProvider(factory: DataSource.Factory<Int, Role>) {
        val recyclerAdapter = RoleListRecyclerAdapter(DIFF_CALLBACK, mPresenter!!, this,
                applicationContext)

        // get the provider, set , observe, etc.
        // A warning is expected
        val data = LivePagedListBuilder(factory, 20).build()
        //Observe the data:
        val thisP = this
        GlobalScope.launch(Dispatchers.Main) {
            data.observe(thisP,
                    Observer<PagedList<Role>> { recyclerAdapter.submitList(it) })
        }

        //set the adapter
        mRecyclerView!!.setAdapter(recyclerAdapter)
    }

    companion object {

        /**
         * The DIFF CALLBACK
         */
        val DIFF_CALLBACK: DiffUtil.ItemCallback<Role> = object : DiffUtil.ItemCallback<Role>() {
            override fun areItemsTheSame(oldItem: Role,
                                         newItem: Role): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Role,
                                            newItem: Role): Boolean {
                return oldItem.roleUid == newItem.roleUid
            }
        }
    }
}
