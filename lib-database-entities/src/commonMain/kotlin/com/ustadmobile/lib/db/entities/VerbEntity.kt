package com.ustadmobile.lib.db.entities

import com.ustadmobile.lib.database.annotation.*
import com.ustadmobile.lib.db.entities.VerbEntity.Companion.TABLE_ID

@UmEntity(tableId = TABLE_ID)
class VerbEntity {

    @UmPrimaryKey(autoGenerateSyncable = true)
    var verbUid: Long = 0

    var urlId: String? = null

    @UmSyncMasterChangeSeqNum
    var verbMasterChangeSeqNum: Long = 0

    @UmSyncLocalChangeSeqNum
    var verbLocalChangeSeqNum: Long = 0

    @UmSyncLastChangedBy
    var verbLastChangedBy: Int = 0

    companion object {

        const val TABLE_ID = 62
    }
}