package com.ustadmobile.core.controller

import androidx.paging.DataSource
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.db.dao.SaleProductDao
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.db.dao.SaleProductPictureDao
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.view.PersonPictureDialogView
import com.ustadmobile.core.view.SaleProductShowcaseView
import com.ustadmobile.core.view.SaleProductShowcaseView.Companion.ARG_SALE_PRODUCT_SHOWCASE_SALE_PRODUCT_UID
import com.ustadmobile.lib.db.entities.SaleProductPicture
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch

/**
 *  Presenter for SaleProductShowcasePresenter view
 **/
class SaleProductShowcasePresenter(context: Any,
                                   arguments: Map<String, String>?,
                                   view: SaleProductShowcaseView,
                                   val impl: UstadMobileSystemImpl = UstadMobileSystemImpl.instance,
                                   private val repository: UmAppDatabase =
                                           UmAccountManager.getRepositoryForActiveAccount(context))
    : UstadBaseController<SaleProductShowcaseView>(context, arguments!!, view) {

    private var productPictureDao: SaleProductPictureDao = repository.saleProductPictureDao
    private var productDao : SaleProductDao = repository.saleProductDao

    private lateinit var factory: DataSource.Factory<Int, SaleProductPicture>
    var productUid : Long = 0

    override fun onCreate(savedState: Map<String, String?>?) {
        super.onCreate(savedState)

        if (arguments.containsKey(ARG_SALE_PRODUCT_SHOWCASE_SALE_PRODUCT_UID)) {
            productUid = arguments[ARG_SALE_PRODUCT_SHOWCASE_SALE_PRODUCT_UID]!!.toLong()
        }

        GlobalScope.launch {

            val product = productDao.findByUidAsync(productUid)
            view.runOnUiThread(Runnable {
                view.updateSaleProductOnView(product!!)
            })
        }
        getAndSetProvider()
    }


    private fun getAndSetProvider() {
        factory = productPictureDao.findAllByProductByIndex(productUid)
        view.setListProvider(factory)
    }

    fun openPictureDialog(imagePath: String){
        val args = HashMap<String, String>()
        args.put(PersonPictureDialogView.ARG_PERSON_IMAGE_PATH, imagePath)
        impl.go(PersonPictureDialogView.VIEW_NAME, args, context)
    }

}
