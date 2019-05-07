package com.ustadmobile.lib.db.entities

import com.ustadmobile.lib.database.annotation.UmEntity
import com.ustadmobile.lib.database.annotation.UmPrimaryKey

@UmEntity
class ScrapeQueueItem {

    @UmPrimaryKey(autoIncrement = true)
    var sqiUid: Int = 0

    var sqiContentEntryParentUid: Long = 0

    var destDir: String? = null

    var scrapeUrl: String? = null

    var status: Int = 0

    var runId: Int = 0

    var itemType: Int = 0

    var contentType: String? = null

    var timeAdded: Long = 0

    var timeStarted: Long = 0

    var timeFinished: Long = 0

    companion object {

        const val ITEM_TYPE_INDEX = 1

        const val ITEM_TYPE_SCRAPE = 2
    }
}
