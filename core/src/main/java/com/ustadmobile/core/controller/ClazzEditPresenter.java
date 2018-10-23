package com.ustadmobile.core.controller;

import java.util.Hashtable;

import com.ustadmobile.core.db.UmAppDatabase;
import com.ustadmobile.core.db.UmProvider;
import com.ustadmobile.core.db.dao.ClazzDao;
import com.ustadmobile.core.impl.UmCallback;
import com.ustadmobile.core.view.AddScheduleDialogView;
import com.ustadmobile.core.view.ClazzEditView;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;
import com.ustadmobile.lib.db.entities.Clazz;
import com.ustadmobile.core.db.UmLiveData;
import com.ustadmobile.lib.db.entities.Schedule;

import static com.ustadmobile.core.controller.ClazzListPresenter.ARG_CLAZZ_UID;


/**
 * The ClazzEdit Presenter.
 */
public class ClazzEditPresenter
        extends CommonHandlerPresenter<ClazzEditView> {
        //extends UstadBaseController<ClazzEditView> {


    //Any arguments stored as variables here
    private long currentClazzUid = -1;
    private Clazz mOriginalClazz;
    private Clazz mUpdatedClazz;

    private UmLiveData<Clazz> clazzLiveData;
    private UmProvider<Schedule> clazzScheduleLiveData;

    private ClazzDao clazzDao = UmAppDatabase.getInstance(context).getClazzDao();


    public ClazzEditPresenter(Object context, Hashtable arguments, ClazzEditView view) {
        super(context, arguments, view);

        //Get arguments and set them.
        if(arguments.containsKey(ARG_CLAZZ_UID)){
            currentClazzUid = (long) arguments.get(ARG_CLAZZ_UID);
        }

    }

    @Override
    public void onCreate(Hashtable savedState) {
        super.onCreate(savedState);

        //Handle Clazz info changed:
        //Get person live data and observe
        clazzLiveData = clazzDao.findByUidLive(currentClazzUid);
        //Observe the live data
        clazzLiveData.observe(ClazzEditPresenter.this,
                ClazzEditPresenter.this::handleClazzValueChanged);

        clazzDao.findByUidAsync(currentClazzUid, new UmCallback<Clazz>() {
            @Override
            public void onSuccess(Clazz result) {
                mUpdatedClazz = result;
                view.updateClazzEditView(result);
            }

            @Override
            public void onFailure(Throwable exception) {

            }
        });

        //Set Schedule live data:
        clazzScheduleLiveData = UmAppDatabase.getInstance(context).getScheduleDao()
                .findAllSchedulesByClazzUid(currentClazzUid);
        view.setClazzScheduleProvider(clazzScheduleLiveData);


    }

    public void updateName(String newName){
        mUpdatedClazz.setClazzName(newName);
    }

    public void updateDesc(String newDesc){
        mUpdatedClazz.setClazzDesc(newDesc);
    }

    public void handleClazzValueChanged(Clazz clazz){
        //TODO: check this

        //set the og person value
        if(mOriginalClazz == null)
            mOriginalClazz = clazz;

        if(mUpdatedClazz == null || !mUpdatedClazz.equals(clazz)) {

            //updateClazzViews
            view.updateClazzEditView(mUpdatedClazz);

            mUpdatedClazz = clazz;
        }
    }

    public void handleClickAddSchedule(){
        UstadMobileSystemImpl impl = UstadMobileSystemImpl.getInstance();
        Hashtable args = new Hashtable();
        args.put(ARG_CLAZZ_UID, currentClazzUid);
        impl.go(AddScheduleDialogView.VIEW_NAME, args, getContext());
    }

    public void handleClickDone() {
        mUpdatedClazz.setClazzActive(true);

        clazzDao.updateAsync(mUpdatedClazz, new UmCallback<Integer>(){

            @Override
            public void onSuccess(Integer result) {

                //Close the activity.
                view.finish();

            }

            @Override
            public void onFailure(Throwable exception) {
                exception.printStackTrace();
            }
        });
    }

    @Override
    public void setUIStrings() {

    }

    @Override
    public void handleCommonPressed(Object arg) {
        // No primary option
    }

    @Override
    public void handleSecondaryPressed(Object arg) {
        //No secondary option
    }
}
