package com.ustadmobile.port.android.view

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import com.toughra.ustadmobile.R
import com.ustadmobile.core.controller.SaleProductCategoryListPresenter
import com.ustadmobile.core.impl.UMAndroidUtil
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.view.SaleProductCategoryListView
import com.ustadmobile.door.ext.asRepositoryLiveData
import com.ustadmobile.lib.db.entities.SaleProduct
import java.security.AccessController.getContext

class SaleProductCategoryListActivity : UstadBaseActivity(), SaleProductCategoryListView {

    private var toolbar: Toolbar? = null
    private var mPresenter: SaleProductCategoryListPresenter? = null
    private var mRecyclerView: RecyclerView? = null
    private var cRecyclerView: RecyclerView? = null

    private var itemActionButton: FloatingActionButton? = null
    private var subCategoryActionButton: FloatingActionButton? = null
    private var floatingActionMenu: FloatingActionMenu? = null

    private var menu: Menu? = null
    private var hideEdit = false

    private var sortSpinner: Spinner? = null
    internal lateinit var sortSpinnerPresets: Array<String?>

    /**
     * Creates the options on the toolbar - specifically the Done tick menu item
     * @param menu  The menu options
     * @return  true. always.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_search_edit, menu)

        menu.findItem(R.id.action_search).isVisible = true

        menu.findItem(R.id.action_edit).isVisible = !hideEdit
        return true
    }

    /**
     * This method catches menu buttons/options pressed in the toolbar. Here it is making sure
     * the activity goes back when the back button is pressed.
     *
     * @param item The item selected
     * @return true if accounted for
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val i = item.itemId
        if (i == android.R.id.home) {
            onBackPressed()
            return true

        } else if (i == R.id.action_search) {
            //TODO: Handle search
            return true
        } else if (i == R.id.action_edit) {
            mPresenter!!.handleClickEditThisCategory()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Setting layout:
        setContentView(R.layout.activity_sale_product_category_list)

        //Toolbar:
        toolbar = findViewById(R.id.activity_sale_product_category_list_toolbar)
        toolbar!!.title = getText(R.string.category)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        floatingActionMenu = findViewById(R.id.activity_sale_product_category_list_fab_menu)
        itemActionButton = findViewById(R.id.activity_sale_product_category_list_fab_item)
        subCategoryActionButton = findViewById(R.id.activity_sale_product_category_list_fab_subcategory)

        //RecyclerView
        mRecyclerView = findViewById(R.id.activity_sale_product_category_list_items_recyclerview)
        val mRecyclerLayoutManager = LinearLayoutManager(applicationContext)
        mRecyclerView!!.layoutManager = mRecyclerLayoutManager

        //categories RecyclerView
        cRecyclerView = findViewById(R.id.activity_sale_product_category_list_categories_recyclerview)
        val cRecyclerLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        cRecyclerView!!.layoutManager = cRecyclerLayoutManager

        sortSpinner = findViewById(R.id.activity_sale_product_category_list_sort_by_spinner)

        //Call the Presenter
        mPresenter = SaleProductCategoryListPresenter(this,
                UMAndroidUtil.bundleToMap(intent.extras), this)
        mPresenter!!.onCreate(UMAndroidUtil.bundleToMap(savedInstanceState))

        //Listeners
        itemActionButton!!.setOnClickListener { v ->
            floatingActionMenu!!.close(true)
            mPresenter!!.handleClickAddItem()
        }

        subCategoryActionButton!!.setOnClickListener { v ->
            floatingActionMenu!!.close(true)
            mPresenter!!.handleClickAddSubCategory()
        }

        //Sort handler
        sortSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                mPresenter!!.handleChangeSortOrder(id)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    override fun setListProvider(factory: DataSource.Factory<Int, SaleProduct>, allMode: Boolean) {

        val recyclerAdapter = SelectSaleProductWithDescRecyclerAdapter(DIFF_CALLBACK, mPresenter!!,
                this, false, applicationContext)

        var boundaryCallback: PagedList.BoundaryCallback<SaleProduct>? = null

        if(allMode){

            val data = factory.asRepositoryLiveData(UmAccountManager.getRepositoryForActiveAccount(applicationContext!!).saleProductDao)
            //Observe the data:
            data.observe(this,
                    Observer<PagedList<SaleProduct>> { recyclerAdapter.submitList(it) })

        }else{
            val data = factory.asRepositoryLiveData(UmAccountManager.getRepositoryForActiveAccount(applicationContext!!).saleProductParentJoinDao)
            //Observe the data:
            data.observe(this,
                    Observer<PagedList<SaleProduct>> { recyclerAdapter.submitList(it) })

        }

        //set the adapter
        mRecyclerView!!.adapter = recyclerAdapter
    }

    override fun setCategoriesListProvider(factory: DataSource.Factory<Int, SaleProduct>, allMode: Boolean) {

        val recyclerAdapter = SelectSaleCategoryRecyclerAdapter(DIFF_CALLBACK, mPresenter!!, this, false,
                true, applicationContext)

        var boundaryCallback: PagedList.BoundaryCallback<SaleProduct>? = null
        if(allMode){
            val data = factory.asRepositoryLiveData(UmAccountManager.getRepositoryForActiveAccount(applicationContext!!).saleProductDao)
            //Observe the data:
            data.observe(this,
                    Observer<PagedList<SaleProduct>> { recyclerAdapter.submitList(it) })

        }else{
            val data = factory.asRepositoryLiveData(UmAccountManager.getRepositoryForActiveAccount(applicationContext!!).saleProductParentJoinDao)
            //Observe the data:
            data.observe(this,
                    Observer<PagedList<SaleProduct>> { recyclerAdapter.submitList(it) })

        }

        //set the adapter
        cRecyclerView!!.adapter = recyclerAdapter
    }

    override fun setMessageOnView(messageId: Int) {
        val impl = UstadMobileSystemImpl.instance
        val message = impl.getString(messageId, getContext())

        runOnUiThread {
            Toast.makeText(
                    applicationContext,
                    message,
                    Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun updateToolbar(title: String?) {
        if(title != null) {
            toolbar!!.title = title
        }
    }

    override fun initFromSaleCategory(saleProductCategory: SaleProduct) {
        if (saleProductCategory != null) {
            runOnUiThread { updateToolbar(saleProductCategory.saleProductName) }
        }
    }

    override fun updateSortPresets(presets: Array<String?>) {
        this.sortSpinnerPresets = presets
        val adapter = ArrayAdapter(this,
                R.layout.item_simple_spinner_gray, sortSpinnerPresets)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner!!.adapter = adapter
    }

    override fun hideFAB(hide: Boolean) {
        runOnUiThread { floatingActionMenu!!.visibility = if (hide) View.GONE else View.VISIBLE }
    }

    override fun hideEditMenu(hide: Boolean) {
        hideEdit = hide
    }

    companion object {

        /**
         * The DIFF CALLBACK
         */
        val DIFF_CALLBACK: DiffUtil.ItemCallback<SaleProduct> = object : DiffUtil.ItemCallback<SaleProduct>() {
            override fun areItemsTheSame(oldItem: SaleProduct,
                                         newItem: SaleProduct): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: SaleProduct,
                                            newItem: SaleProduct): Boolean {
                return oldItem.saleProductUid == newItem.saleProductUid
            }
        }
    }
}