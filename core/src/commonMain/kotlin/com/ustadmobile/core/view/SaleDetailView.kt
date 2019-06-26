package com.ustadmobile.core.view

import com.ustadmobile.core.db.UmProvider
import com.ustadmobile.lib.db.entities.Sale
import com.ustadmobile.lib.db.entities.SaleItemListDetail
import com.ustadmobile.lib.db.entities.SalePayment


/**
 * Core View. Screen is for SaleDetail's View
 */
interface SaleDetailView : UstadView {

    /**
     * Method to finish the screen / view.
     */
    fun finish()


    /**
     * Sets the given provider to the view's provider adapter.
     *
     * @param listProvider The provider to set to the view
     */
    fun setListProvider(listProvider: UmProvider<SaleItemListDetail>)


    fun setLocationPresets(locationPresets: Array<String>, selectedPosition: Int)

    /**
     * Payment provider for this sale.
     *
     * @param paymentProvider   the provider to set to the view
     */
    fun setPaymentProvider(paymentProvider: UmProvider<SalePayment>)

    fun updateOrderTotal(orderTotal: Long)

    fun updateOrderTotalAfterDiscount(discount: Long)
    fun updateOrderTotalAfterDiscountTotalChanged(total: Long)


    fun updateSaleOnView(sale: Sale)

    //N/A:
    fun updatePaymentTotal(paymentTotal: Long)

    fun showSaveButton(show: Boolean)
    fun showCalculations(show: Boolean)
    fun showDelivered(show: Boolean)
    fun showNotes(show: Boolean)
    fun showSignature(show: Boolean)
    fun showPayments(show: Boolean)

    fun updateSaleVoiceNoteOnView(fileName: String)

    fun updateBalanceDue(balance: Long)

    companion object {


        // This defines the view name that is an argument value in the go() in impl.
        const val VIEW_NAME = "SaleDetail"

        //Any argument keys:
        const val ARG_SALE_UID = "ArgSaleUid"
        const val ARG_SALE_GEN_NAME = "ArgSaleGenName"
    }


}

