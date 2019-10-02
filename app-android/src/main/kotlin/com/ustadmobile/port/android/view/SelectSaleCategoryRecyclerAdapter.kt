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

import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

import com.squareup.picasso.Picasso
import com.toughra.ustadmobile.R
import com.ustadmobile.core.controller.SaleProductCategoryListPresenter
import com.ustadmobile.lib.db.entities.SaleNameWithImage

import java.io.File

class SelectSaleCategoryRecyclerAdapter internal constructor(
        diffCallback: DiffUtil.ItemCallback<SaleNameWithImage>,
        internal var mPresenter: SaleProductCategoryListPresenter,
        private val theActivity: Activity?,
        private val showContextMenu: Boolean?,
        private val listCategory: Boolean?,
        private val theContext: Context)
    : PagedListAdapter<SaleNameWithImage, SelectSaleCategoryRecyclerAdapter.SelectSaleProductViewHolder>(diffCallback) {

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
                .resize(dpToPxImagePerson(), dpToPxImagePerson())
                .noFade()
                .into(theImage)
    }

    override fun onBindViewHolder(holder: SelectSaleProductViewHolder, position: Int) {

        val entity = getItem(position)
        val imageView = holder.itemView.findViewById<ImageView>(R.id.item_sale_product_blob_image)
        val name = holder.itemView.findViewById<TextView>(R.id.item_sale_product_blob_title)
        val dots = holder.itemView.findViewById<ImageView>(R.id.item_sale_product_blob_dots)

        assert(entity != null)
        val pictureUid = entity!!.pictureUid
        val imagePath = ""
        if (pictureUid != 0L) {
            //TODO: KMP
            //            imagePath = UmAppDatabase.Companion.getInstance(theContext).getSaleProductPictureDao()
            //                    .getAttachmentPath(pictureUid);
        }

        if (imagePath != null && !imagePath.isEmpty())
            setPictureOnView(imagePath, imageView)
        else
            imageView.setImageResource(R.drawable.ic_card_giftcard_black_24dp)

        name.text = entity.name

        //Options to Edit/Delete every schedule in the list
        if (showContextMenu!!) {
            dots.setOnClickListener { v: View ->
                if (theActivity != null) {
                    //creating a popup menu
                    val popup = PopupMenu(theActivity.applicationContext, v)
                    popup.setOnMenuItemClickListener { item ->
                        val i = item.itemId
                        if (i == R.id.edit) {
                            mPresenter.handleClickEditCategory(entity.productUid)
                            true
                        } else if (i == R.id.delete) {
                            mPresenter.handleDeleteCategory(entity.productUid)
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

        holder.itemView.setOnClickListener { v -> mPresenter.handleClickProduct(entity.productUid, listCategory!!) }

    }

    inner class SelectSaleProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    companion object {

        private val IMAGE_WITH = 100

        private fun dpToPxImagePerson(): Int {
            return (IMAGE_WITH * Resources.getSystem().displayMetrics.density).toInt()
        }
    }

}