package com.ustadmobile.core.controller;

import com.ustadmobile.core.impl.UmAccountManager;
import com.ustadmobile.core.db.UmAppDatabase;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;

import java.util.Hashtable;

import com.ustadmobile.core.view.ReportSalesPerformanceDetailView;


/**
 * Presenter for ReportSalesPerformanceDetail view
 **/
public class ReportSalesPerformanceDetailPresenter
        extends ReportDetailPresenter<ReportSalesPerformanceDetailView> {

    UmAppDatabase repository;


    public ReportSalesPerformanceDetailPresenter(Object context,
                                                 Hashtable arguments, ReportSalesPerformanceDetailView view) {
        super(context, arguments, view);

        repository = UmAccountManager.getRepositoryForActiveAccount(context);


    }

    @Override
    public void onCreate(Hashtable savedState) {
        super.onCreate(savedState);


    }


    @Override
    public void handleCommonPressed(Object arg, Object arg2) {

    }

    @Override
    public void handleSecondaryPressed(Object arg) {

    }

    @Override
    public void handleClickAddToDashboard() {

    }
}
