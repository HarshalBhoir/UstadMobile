package com.ustadmobile.core.controller

import androidx.paging.DataSource
import com.soywiz.klock.DateTime
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.db.dao.SaleProductDao
import com.ustadmobile.core.db.dao.SaleProductParentJoinDao
import com.ustadmobile.core.db.dao.SaleProductPictureDao
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.view.SaleProductDetailView
import com.ustadmobile.core.view.SaleProductDetailView.Companion.ARG_ASSIGN_TO_CATEGORY_UID
import com.ustadmobile.core.view.SaleProductDetailView.Companion.ARG_NEW_CATEGORY
import com.ustadmobile.core.view.SaleProductDetailView.Companion.ARG_NEW_TITLE
import com.ustadmobile.core.view.SaleProductDetailView.Companion.ARG_SALE_PRODUCT_UID
import com.ustadmobile.door.DoorLiveData
import com.ustadmobile.lib.db.entities.SaleProduct
import com.ustadmobile.lib.db.entities.SaleProductParentJoin
import com.ustadmobile.lib.db.entities.SaleProductPicture
import com.ustadmobile.lib.db.entities.SaleProductSelected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch

/**
 * Presenter for SaleProductDetail view
 */
class SaleProductDetailPresenter(context: Any,
                                 arguments: Map<String, String>?,
                                 view: SaleProductDetailView)
    : UstadBaseController<SaleProductDetailView>(context, arguments!!, view) {

    internal var repository: UmAppDatabase
    private val saleProductDao: SaleProductDao
    private val productParentJoinDao: SaleProductParentJoinDao
    private val impl: UstadMobileSystemImpl
    var currentSaleProduct: SaleProduct? = null
        private set
    internal var categoriesProvider: DataSource.Factory<Int, SaleProductSelected>? = null
    private var isCategory: Boolean = false

    internal var pictureDao: SaleProductPictureDao

    private val selectedToCategoriesUid: HashMap<Long, Boolean>

    private lateinit var pictureLiveData: DoorLiveData<SaleProductPicture?>

    private var productUid : Long = 0L

    init {

        repository = UmAccountManager.getRepositoryForActiveAccount(context)

        //Get provider Dao
        saleProductDao = repository.saleProductDao
        productParentJoinDao = repository.saleProductParentJoinDao
        pictureDao = repository.saleProductPictureDao

        impl = UstadMobileSystemImpl.instance

        selectedToCategoriesUid = HashMap<Long, Boolean>()

    }

    override fun onCreate(savedState: Map<String, String?>?) {
        super.onCreate(savedState)

        //Update toolbar title
        var toolbarTitle = ""
        var categoryTitle = ""
        if (arguments.containsKey(ARG_NEW_TITLE)) {
            isCategory = false
            toolbarTitle = impl.getString(MessageID.create_new_item, context)
            categoryTitle = impl.getString(MessageID.category, context)
        } else if (arguments.containsKey(ARG_NEW_CATEGORY)) {
            toolbarTitle = impl.getString(MessageID.create_new_subcategory, context)
            categoryTitle = impl.getString(MessageID.subcategory, context)
            isCategory = true
        }
        view.updateToolbarTitle(toolbarTitle)
        view.updateCategoryTitle(categoryTitle)

        var thisP = this
        //Get SaleProductSelected and update the view
        GlobalScope.launch {
            if (arguments.containsKey(ARG_SALE_PRODUCT_UID)) {

                productUid = arguments[ARG_SALE_PRODUCT_UID]!!.toLong()

                val product = saleProductDao.findByUidLive(productUid)
                view.runOnUiThread(Runnable {
                    product.observe(thisP, thisP::updateView)
                })

            } else {
                currentSaleProduct = SaleProduct("", "", isCategory, false)

                currentSaleProduct!!.saleProductUid = saleProductDao.insertAsync(currentSaleProduct!!)

                val product = saleProductDao.findByUidLive(currentSaleProduct!!.saleProductUid)
                view.runOnUiThread(Runnable {
                    product.observe(thisP, thisP::updateView)
                })

            }
        }

    }

    fun updateView(saleProduct: SaleProduct?){
        currentSaleProduct = saleProduct
        updateView()
    }

    private fun updateView() {
        val itemUid: Long
        itemUid = currentSaleProduct!!.saleProductUid

        view.runOnUiThread(Runnable {
            view.initFromSaleProduct(currentSaleProduct!!)
        })

        //Assign
        if (arguments.containsKey(ARG_ASSIGN_TO_CATEGORY_UID)) {
            val defaultJoin = SaleProductParentJoin(itemUid,
                    (arguments[ARG_ASSIGN_TO_CATEGORY_UID]!!.toLong()), true)
            productParentJoinDao.insert(defaultJoin)
        }

        //Update provider:
        categoriesProvider = productParentJoinDao.findAllSelectedCategoriesForSaleProductProvider(
                itemUid)
        GlobalScope.launch(Dispatchers.Main) {
            view.runOnUiThread(Runnable {
                view.setListProvider(categoriesProvider!!)
            })
        }

        //Update image on view
        GlobalScope.launch {
            val productPicture =
                    pictureDao.findBySaleProductUidAsync(currentSaleProduct!!.saleProductUid)
            if (productPicture != null) {
                //TODO: Implement this for KMP
                //view.updateImageOnView(pictureDao.getAttachmentPath(productPicture.saleProductPictureUid))
            }
        }

        //Observe the picture
        pictureLiveData = pictureDao.findByProductUidLive(currentSaleProduct!!.saleProductUid)
        val thisP = this
        GlobalScope.launch(Dispatchers.Main) {
            pictureLiveData.observe(thisP, thisP::handleProductPictureChanged)
        }

    }

    private fun handleProductPictureChanged(productPicture: SaleProductPicture?) {
        if (productPicture != null) {
            view.runOnUiThread(Runnable{
                //TODO: Implement this for KMP
//                view.updateImageOnView(
//                        pictureDao.getAttachmentPath(productPicture.saleProductPictureUid))
            })
        }
    }

    fun handleClickSave() {

        val selectedIterator = selectedToCategoriesUid.keys.iterator()
        while (selectedIterator.hasNext()) {
            val productUid = selectedIterator.next()
            val selected = selectedToCategoriesUid.get(productUid)

            //Update assignment.
            GlobalScope.launch {
                productParentJoinDao.createJoin(currentSaleProduct!!.saleProductUid,
                        productUid, selected!!)
            }

        }
        currentSaleProduct!!.saleProductActive = true
        GlobalScope.launch {
            try {
                saleProductDao.updateAsync(currentSaleProduct!!)
                view.finish()
            }catch(e:Exception){
                print(e.message)
            }
        }
    }

    fun handleCheckboxChanged(state: Boolean, saleProductUid: Long) {
        selectedToCategoriesUid.put(saleProductUid, state)
    }

    fun updateTitleEng(title: String) {
        currentSaleProduct!!.saleProductName = title
    }

    fun updateTitleDari(title: String) {
        currentSaleProduct!!.saleProductNameDari = title
    }

    fun updateTitlePashto(title: String) {
        currentSaleProduct!!.saleProductNamePashto = title
    }

    fun updateDescEng(dec: String) {
        currentSaleProduct!!.saleProductDesc = dec
    }

    fun updateDescDari(desc: String) {
        currentSaleProduct!!.saleProductDescDari = desc
    }

    fun updateDescPashto(desc: String) {
        currentSaleProduct!!.saleProductDescPashto = desc
    }

    fun handleCompressedImage(imageFilePath: String) {

        //Create picture entry

        val productPicture = SaleProductPicture()
        productPicture.saleProductPictureSaleProductUid = currentSaleProduct!!.saleProductUid
        productPicture.saleProductPictureTimestamp = DateTime.nowUnixLong()

        GlobalScope.launch {
            val productPictureUid = pictureDao.insertAsync(productPicture)
            //TODO: Fix this for KMP
//            pictureDao.setAttachmentFromTmpFile(productPictureUid, imageFile)
        }

    }

    fun openPictureDialog(imagePath: String) {
        //TODO if needed.
        //open dialog
    }
}
