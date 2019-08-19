package com.ustadmobile.port.android.view
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

import com.toughra.ustadmobile.R
import com.ustadmobile.core.controller.CommonHandlerPresenter
import com.ustadmobile.lib.db.entities.Person

/**
 * A Simple recycler adapter for a dead simple list of Students.
 */

class SimplePeopleListRecyclerAdapter : PagedListAdapter<Person,
        SimplePeopleListRecyclerAdapter.ClazzStudentViewHolder> {

    internal var theContext: Context
    internal lateinit var theFragment: Fragment
    internal lateinit var mPresenter: CommonHandlerPresenter<*>


    class ClazzStudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    protected constructor(diffCallback: DiffUtil.ItemCallback<Person>,
                          context: Context, fragment: Fragment) : super(diffCallback) {
        theContext = context
        theFragment = fragment
    }

    constructor(diffCallback: DiffUtil.ItemCallback<Person>,
                          context: Context, fragment: Fragment,
                          presenter: CommonHandlerPresenter<*>) : super(diffCallback) {
        theContext = context
        theFragment = fragment
        mPresenter = presenter
    }

    protected constructor(diffCallback: DiffUtil.ItemCallback<Person>,
                          context: Context) : super(diffCallback) {
        theContext = context
    }

    protected constructor(diffCallback: DiffUtil.ItemCallback<Person>,
                          context: Context, presenter: CommonHandlerPresenter<*>) : super(diffCallback) {
        theContext = context
        mPresenter = presenter
    }

    /**
     * This method inflates the card layout (to parent view given) and returns it.
     * @param parent View given.
     * @param viewType View Type not used here.
     * @return New ViewHolder for the ClazzStudent type
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimplePeopleListRecyclerAdapter.ClazzStudentViewHolder {

        val clazzStudentListItem = LayoutInflater.from(theContext).inflate(
                R.layout.item_peoplelist, parent, false)
        return SimplePeopleListRecyclerAdapter.ClazzStudentViewHolder(clazzStudentListItem)
    }

    /**
     * This method sets the elements after it has been obtained for that item'th position.
     * @param holder    The holder
     * @param position  The position in the recycler view.
     */
    override fun onBindViewHolder(
            holder: SimplePeopleListRecyclerAdapter.ClazzStudentViewHolder, position: Int) {


        val thisPerson = getItem(position)
        val studentName: String
        if (thisPerson == null) {
            studentName = "Student"
        } else {
            studentName = thisPerson!!.firstNames + " " +
                    thisPerson!!.lastName
        }

        val studentEntry = holder.itemView
                .findViewById(R.id.item_peoplelist_name) as TextView
        studentEntry.text = studentName


        studentEntry.setOnClickListener { v -> mPresenter.handleCommonPressed(thisPerson!!.personUid) }

    }
}
