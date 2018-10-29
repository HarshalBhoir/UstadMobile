package com.ustadmobile.port.android.view;

import android.arch.paging.PagedListAdapter;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.app.Fragment;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.toughra.ustadmobile.R;
import com.ustadmobile.core.controller.ClazzActivityListPresenter;
import com.ustadmobile.core.util.UMCalendarUtil;
import com.ustadmobile.lib.db.entities.ClazzActivity;

/**
 * The ClazzActivityList's recycler adapter.
 */
public class ClazzActivityListRecyclerAdapter extends
        PagedListAdapter<ClazzActivity, ClazzActivityListRecyclerAdapter.ClazzActivityViewHolder> {

    Context theContext;
    Fragment theFragment;
    ClazzActivityListPresenter thePresenter;
    Boolean showImage = false;

    protected class ClazzActivityViewHolder extends RecyclerView.ViewHolder{
        protected ClazzActivityViewHolder(View itemView){
            super(itemView);
        }
    }

    protected ClazzActivityListRecyclerAdapter(@NonNull DiffUtil.ItemCallback<ClazzActivity>
                                                  diffCallback, Context context, Fragment fragment,
                                               ClazzActivityListPresenter mPresenter){
        super(diffCallback);
        theContext = context;
        theFragment = fragment;
        thePresenter = mPresenter;
    }
    protected ClazzActivityListRecyclerAdapter(@NonNull DiffUtil.ItemCallback<ClazzActivity>
                                                  diffCallback, Context context, Fragment fragment,
                                               ClazzActivityListPresenter mPresenter,
                                          boolean imageShow){
        super(diffCallback);
        theContext = context;
        theFragment = fragment;
        thePresenter = mPresenter;
        showImage = imageShow;
    }

    @NonNull
    @Override
    public ClazzActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View clazzLogListItem =
                LayoutInflater.from(theContext).inflate(
                        R.layout.item_clazzlog_log, parent, false);
        return new ClazzActivityViewHolder(clazzLogListItem);

    }

    /**
     * This method sets the elements after it has been obtained for that item'th position.
     *
     * For every item part of the recycler adapter, this will be called and every item in it
     * will be set as per this function.
     *
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(@NonNull ClazzActivityViewHolder holder, int position){
        ClazzActivity clazzActivity = getItem(position);

        String prettyDate =
                UMCalendarUtil.getPrettyDateFromLong(clazzActivity.getClazzActivityLogDate());
        String prettyShortDay =
                UMCalendarUtil.getSimpleDayFromLongDate(clazzActivity.getClazzActivityLogDate());

        ImageView secondaryTextImageView =
                holder.itemView.findViewById(R.id.item_clazzlog_log_status_text_imageview);

        long clazzActivityChangeUid = clazzActivity.getClazzActivityClazzActivityChangeUid();


        String clazzActivityStatus = "Hello change uid: " + clazzActivityChangeUid;

        TextView statusTextView = holder.itemView
                .findViewById(R.id.item_clazzlog_log_status_text);

        ((TextView)holder.itemView
                .findViewById(R.id.item_clazzlog_log_date))
                .setText(prettyDate);
        ((TextView)holder.itemView
                .findViewById(R.id.item_clazzlog_log_day))
                .setText(prettyShortDay);
        statusTextView.setText(clazzActivityStatus);

        if(!showImage){
            secondaryTextImageView.setVisibility(View.INVISIBLE);

            //Change the constraint layout so that the hidden bits are not empty spaces.
            ConstraintLayout cl = holder.itemView.findViewById(R.id.item_clazzlog_log_cl);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(cl);

            constraintSet.connect(R.id.item_clazzlog_log_status_text,
                    ConstraintSet.START, R.id.item_clazzlog_log_calendar_image,
                    ConstraintSet.END, 16);

            constraintSet.applyTo(cl);


        }else{
            secondaryTextImageView.setVisibility(View.VISIBLE);
        }

        //TODO: Not part of Sprint 3. Change as new views are set up.
        //holder.itemView.setOnClickListener(
        //        v -> thePresenter.goToClazzActivityDetailActivity(clazzLog));
    }
}