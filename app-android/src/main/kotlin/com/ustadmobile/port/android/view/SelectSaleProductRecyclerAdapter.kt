package com.ustadmobile.port.android.view

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView

import androidx.fragment.app.Fragment
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

import com.squareup.picasso.Picasso
import com.toughra.ustadmobile.R
import com.ustadmobile.core.controller.SelectSaleProductPresenter
import com.ustadmobile.core.db.dao.SaleProductPictureDao
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.lib.db.entities.SaleDescWithSaleProductPicture
import kotlinx.coroutines.*

import java.io.File

class SelectSaleProductRecyclerAdapter
    : PagedListAdapter<SaleDescWithSaleProductPicture, SelectSaleProductRecyclerAdapter.SelectSaleProductViewHolder> {
    private var theContext: Context
    private var theActivity: Activity? = null
    private var theFragment: Fragment? = null
    internal var mPresenter: SelectSaleProductPresenter

    private var listCategory: Boolean = false
    private var isCatalog: Boolean = false

    private var productPictureDao : SaleProductPictureDao ? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectSaleProductViewHolder {


        val list = LayoutInflater.from(theContext).inflate(
                R.layout.item_sale_product_blob, parent, false)
        return SelectSaleProductViewHolder(list)

    }

    private fun setPictureOnView(imagePath: String, theImage: ImageView) {

        val imageUri = Uri.fromFile(File(imagePath))

        Picasso
                .get()
                .load(imageUri)
                .resize(0, dpToPxImagePerson())
                .noFade()
                .into(theImage)
    }

    override fun onBindViewHolder(holder: SelectSaleProductViewHolder, position: Int) {

        val entity = getItem(position)
        val imageView = holder.itemView.findViewById<ImageView>(R.id.item_sale_product_blob_image)
        val name = holder.itemView.findViewById<TextView>(R.id.item_sale_product_blob_title)
        val dots = holder.itemView.findViewById<ImageView>(R.id.item_sale_product_blob_dots)

        val pictureUid = entity!!.saleProductPictureUid
        var imagePath = ""

        holder.imageLoadJob?.cancel()

        holder.imageLoadJob = GlobalScope.async(Dispatchers.Main) {

            productPictureDao  = UmAccountManager.getRepositoryForActiveAccount(theContext).saleProductPictureDao

            val saleProductPicture = productPictureDao!!.findBySaleProductUidAsync2(entity.productUid)
            imagePath = productPictureDao!!.getAttachmentPath(saleProductPicture!!);

            if (!imagePath.isEmpty())
                setPictureOnView(imagePath, imageView)
            else
                imageView.setImageResource(R.drawable.ic_card_giftcard_black_24dp)
        }


        name.text = entity.name

        if (isCatalog) {
            dots.visibility = View.VISIBLE
            //Options to Edit/Delete every schedule in the list
            dots.setOnClickListener { v: View ->
                if (theActivity != null) {
                    //creating a popup menu
                    val popup = PopupMenu(theActivity!!.applicationContext, v)
                    popup.setOnMenuItemClickListener { item ->
                        val i = item.itemId
                        if (i == R.id.edit) {
                            mPresenter.handleClickProductMulti(entity.productUid, listCategory, true)
                            true
                        } else if (i == R.id.delete) {
                            mPresenter.handleDelteSaleProduct(entity.productUid, listCategory)
                            true
                        } else {
                            false
                        }
                    }

                    //inflating menu from xml resource
                    popup.inflate(R.menu.menu_edit_delete)
                    popup.menu.findItem(R.id.edit).isVisible = true
                    //displaying the popup
                    popup.show()
                } else if (theFragment != null) {
                    //creating a popup menu
                    val popup = PopupMenu(theFragment!!.context, v)
                    popup.setOnMenuItemClickListener { item ->
                        val i = item.itemId
                        if (i == R.id.edit) {
                            mPresenter.handleClickProductMulti(entity.productUid, listCategory, true)
                            true
                        } else if (i == R.id.delete) {
                            mPresenter.handleDelteSaleProduct(entity.productUid, listCategory)
                            true
                        } else {
                            false
                        }
                    }

                    //inflating menu from xml resource
                    popup.inflate(R.menu.menu_edit_delete)
                    popup.menu.findItem(R.id.edit).isVisible = true
                    //displaying the popup
                    popup.show()
                }

            }
        } else {
            dots.visibility = View.GONE
        }


        holder.itemView.setOnClickListener { mPresenter.handleClickProduct(entity.productUid, listCategory) }

    }

    inner class SelectSaleProductViewHolder(itemView: View, var imageLoadJob: Job? = null) : RecyclerView.ViewHolder(itemView)

    internal constructor(
            diffCallback: DiffUtil.ItemCallback<SaleDescWithSaleProductPicture>,
            thePresenter: SelectSaleProductPresenter,
            activity: Activity,
            isCategory: Boolean?,
            catalog: Boolean?,
            context: Context) : super(diffCallback) {
        mPresenter = thePresenter
        theActivity = activity
        listCategory = isCategory!!
        isCatalog = catalog!!
        theContext = context
    }

    internal constructor(
            diffCallback: DiffUtil.ItemCallback<SaleDescWithSaleProductPicture>,
            thePresenter: SelectSaleProductPresenter,
            fragment: Fragment,
            isCategory: Boolean?,
            catalog: Boolean?,
            context: Context) : super(diffCallback) {
        mPresenter = thePresenter
        theFragment = fragment
        listCategory = isCategory!!
        isCatalog = catalog!!
        theContext = context
    }

    companion object {

        private val IMAGE_WIDTH = 100

        private fun dpToPxImagePerson(): Int {
            return (IMAGE_WIDTH * Resources.getSystem().displayMetrics.density).toInt()
        }
    }


}
