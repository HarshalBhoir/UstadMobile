package com.ustadmobile.core.controller

import com.soywiz.klock.DateTime
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.util.UMCalendarUtil
import com.ustadmobile.core.view.SelectMultipleEntriesTreeDialogView
import com.ustadmobile.core.view.SelectMultipleEntriesTreeDialogView.Companion.ARG_CONTENT_ENTRY_SET
import com.ustadmobile.core.view.SelectMultipleLocationTreeDialogView
import com.ustadmobile.core.view.SelectMultipleLocationTreeDialogView.Companion.ARG_LOCATIONS_SET
import com.ustadmobile.core.view.XapiReportOptionsView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch

class XapiReportOptionsPresenter(context: Any, arguments: Map<String, String>?, view: XapiReportOptionsView)
    : UstadBaseController<XapiReportOptionsView>(context, arguments!!, view) {


    private lateinit var impl: UstadMobileSystemImpl
    private lateinit var db: UmAppDatabase

    var fromDateTime: DateTime = DateTime.now()

    var toDateTime: DateTime = DateTime.now()

    private var selectedLocations: List<Long> = mutableListOf()

    private var selectedEntries: List<Long> = mutableListOf()

    private var selectedObjects: List<Long> = mutableListOf()

    private var selectedYaxis: Int = 0

    private var selectedChartType: Int = 0

    private var selectedXAxis: Int = 0

    private var selectedSubGroup: Int = 0

    override fun onCreate(savedState: Map<String, String?>?) {
        super.onCreate(savedState)
        db = UmAccountManager.getRepositoryForActiveAccount(context)
        impl = UstadMobileSystemImpl.instance

        val translatedGraphList = listOfGraphs.map { impl.getString(it, context) }
        val translatedYAxisList = yAxisList.map { impl.getString(it, context) }
        val translatedXAxisList = xAxisList.map { impl.getString(it, context) }

        view.runOnUiThread(Runnable { view.fillVisualChartType(translatedGraphList) })

        view.runOnUiThread(Runnable { view.fillYAxisData(translatedYAxisList) })

        view.runOnUiThread(Runnable { view.fillXAxisAndSubGroupData(translatedXAxisList) })

    }

    fun handleFromCalendarSelected(year: Int, month: Int, dayOfMonth: Int) {
        fromDateTime = UMCalendarUtil.setDate(year, month, dayOfMonth)
        handleFromCalendarSelected()
    }

    fun handleFromCalendarSelected() {
        view.runOnUiThread(Runnable { view.updateFromDialogText(fromDateTime.format("dd/MM/YYYY")) })
    }


    fun handleToCalendarSelected(year: Int, month: Int, dayOfMonth: Int) {
        toDateTime = UMCalendarUtil.setDate(year, month, dayOfMonth)
        handleToCalendarSelected()
    }

    fun handleToCalendarSelected() {
        view.runOnUiThread(Runnable { view.updateToDialogText(toDateTime.format("dd/MM/YYYY")) })
    }

    fun handleDateRangeSelected() {
        view.runOnUiThread(Runnable {
            view.updateWhenRangeText(
                    fromDateTime.format("dd MMM yyyy") + " - " + toDateTime.format("dd MMM yyyy"))
        })
    }

    fun handleWhoDataTyped(name: String, uidList: List<Long>) {
        GlobalScope.launch {
            val personsNames = db.personDao.getAllPersons("%$name%", uidList)
            view.runOnUiThread(Runnable { view.updateWhoDataAdapter(personsNames) })
        }
    }

    fun handleDidDataTyped(verb: String, uidList: List<Long>) {
        GlobalScope.launch {
            val verbs = db.xLangMapEntryDao.getAllVerbs("%$verb%", uidList)
            view.runOnUiThread(Runnable { view.updateDidDataAdapter(verbs) })
        }
    }

    fun handleWhereClicked() {
        val args = mutableMapOf<String, String>()
        args[ARG_LOCATIONS_SET] = selectedLocations.joinToString { it.toString() }
        impl.go(SelectMultipleLocationTreeDialogView.VIEW_NAME, args, context)
    }


    fun handleWhatClicked() {
        val args = mutableMapOf<String, String>()
        args[ARG_CONTENT_ENTRY_SET] = selectedEntries.joinToString { it.toString() }
        impl.go(SelectMultipleEntriesTreeDialogView.VIEW_NAME, args, context)
    }

    fun handleViewReportPreview(didOptionsList: List<Long>, whoOptionsList: List<Long>) {
        var report = XapiReportOptions(
                listOfGraphs[selectedChartType],
                yAxisList[selectedYaxis],
                xAxisList[selectedXAxis],
                xAxisList[selectedSubGroup],
                whoOptionsList,
                didOptionsList,
                selectedObjects,
                selectedEntries,
                fromDateTime.unixMillisLong,
                toDateTime.unixMillisLong,
                selectedLocations)

        report.toSql()


    }

    fun handleLocationListSelected(locationList: List<Long>) {
        selectedLocations = locationList
    }

    fun handleEntriesListSelected(entriesList: List<Long>) {
        selectedEntries = entriesList
        GlobalScope.launch {
            selectedObjects = db.xObjectDao.findListOfObjectUidFromContentEntryUid(selectedEntries)
        }
    }

    fun handleSelectedYAxis(position: Int) {
        selectedYaxis = position
    }

    fun handleSelectedChartType(position: Int) {
        selectedChartType = position
    }

    fun handleSelectedXAxis(position: Int) {
        selectedXAxis = position
    }

    fun handleSelectedSubGroup(position: Int) {
        selectedSubGroup = position
    }


    companion object {

        const val BAR_CHART = MessageID.bar_chart

        const val LINE_GRAPH = MessageID.line_graph

        const val FREQ_GRAPH = MessageID.freq_graph

        val listOfGraphs = arrayOf(BAR_CHART, LINE_GRAPH, FREQ_GRAPH)

        const val SCORE = MessageID.score

        const val DURATION = MessageID.duration

        const val COUNT_ACTIVITIES = MessageID.count_activity

        val yAxisList = arrayOf(SCORE, DURATION, COUNT_ACTIVITIES)

        const val DAY = MessageID.xapi_day

        const val WEEK = MessageID.xapi_week

        const val MONTH = MessageID.xapi_month

        const val CONTENT_ENTRY = MessageID.xapi_content_entry

        //TODO to be put back when varuna merges his branch
        // private const val LOCATION = MessageID.xapi_location

        const val GENDER = MessageID.xapi_gender

        val xAxisList = arrayOf(DAY, WEEK, MONTH, CONTENT_ENTRY, /*LOCATION, */ GENDER)

    }

}