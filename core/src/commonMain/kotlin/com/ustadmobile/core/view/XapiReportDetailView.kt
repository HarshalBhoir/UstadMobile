package com.ustadmobile.core.view

import com.ustadmobile.core.controller.XapiReportOptions
import com.ustadmobile.core.db.dao.StatementDao
import kotlin.js.JsName

interface XapiReportDetailView : UstadView {

    @JsName("setChartData")
    fun setChartData(chartData: List<StatementDao.ReportData>, options: XapiReportOptions, xAxisLabels: Map<String, String>, subgroupLabels: Map<String, String>)

    @JsName("setChartYAxisLabel")
    fun setChartYAxisLabel(yAxisLabel: String)

    @JsName("setReportListData")
    fun setReportListData(listResults: List<StatementDao.ReportListData>)

    companion object {

        const val VIEW_NAME = "ReportPreview"

        const val ARG_REPORT_OPTIONS = "options"

    }


}