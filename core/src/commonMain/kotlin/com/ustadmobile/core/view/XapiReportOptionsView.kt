package com.ustadmobile.core.view

import com.ustadmobile.core.db.dao.PersonDao
import com.ustadmobile.core.db.dao.XLangMapEntryDao

interface XapiReportOptionsView : UstadView {

    fun fillVisualChartType(translatedGraphList: List<String>)

    fun fillYAxisData(translatedYAxisList: List<String>)

    fun fillXAxisAndSubGroupData(translatedXAxisList: List<String>)

    fun fillDidData(didList: List<XLangMapEntryDao.Verb>)

    fun updateWhoDataAdapter(whoList: List<PersonDao.PersonNameAndUid>)

    fun updateDidDataAdapter(didList: List<XLangMapEntryDao.Verb>)

    fun updateFromDialogText(fromDate: String)

    fun updateToDialogText(toDate: String)

    fun updateWhenRangeText(rangeText: String)

    fun updateChartTypeSelected(indexChart: Int)

    fun updateYAxisTypeSelected(indexYAxis: Int)

    fun updateXAxisTypeSelected(indexXAxis: Int)

    fun updateSubgroupTypeSelected(indexSubgroup: Int)

    fun updateWhoListSelected()

    fun updateDidListSelected()

    fun updateDatesSelected()

    companion object {

        const val VIEW_NAME = "XapiReportOptionsView"



    }

}