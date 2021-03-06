package com.ustadmobile.port.android.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.toughra.ustadmobile.R
import com.toughra.ustadmobile.databinding.ItemPersonWithAssignmentMetricsBinding
import com.ustadmobile.core.controller.ClazzAssignmentDetailProgressPresenter
import com.ustadmobile.core.util.UMCalendarUtil
import com.ustadmobile.lib.db.entities.PersonWithAssignmentMetrics

class PersonWithAssignmentMetricsRecyclerAdapter(
        diffCallback: DiffUtil.ItemCallback<PersonWithAssignmentMetrics>,
        internal var mPresenter: ClazzAssignmentDetailProgressPresenter?)
    : PagedListAdapter<PersonWithAssignmentMetrics,
        PersonWithAssignmentMetricsRecyclerAdapter.PersonWithAssignmentMetricsViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonWithAssignmentMetricsViewHolder {
        val clazzAssignmentListBinding = ItemPersonWithAssignmentMetricsBinding.inflate(
                LayoutInflater.from(parent.context), parent, false)
        clazzAssignmentListBinding.presenter = mPresenter

        return PersonWithAssignmentMetricsViewHolder(clazzAssignmentListBinding)
    }

    override fun onBindViewHolder(holder: PersonWithAssignmentMetricsViewHolder, position: Int) {

        val entity = getItem(position)
        holder.binding.personwithassignmentmetrics = entity

        if(entity?.startedDate?:0 > 0L) {
            val descText = holder.itemView.context.getText(R.string.started).toString() + " " +
                    UMCalendarUtil.getPrettyDateSimpleFromLong(entity?.startedDate?:0L, null) + " - " +
                    entity?.percentageCompleted + "% " + holder.itemView.context.getText(R.string.completed)
            holder.binding.itemPersonWithAssignmentMetricsDescription.text = descText
        }
    }

    inner class PersonWithAssignmentMetricsViewHolder
    internal constructor(val binding: ItemPersonWithAssignmentMetricsBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        mPresenter = null
    }
}
