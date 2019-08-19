package com.ustadmobile.lib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ustadmobile.door.annotation.LastChangedBy
import com.ustadmobile.door.annotation.LocalChangeSeqNum
import com.ustadmobile.door.annotation.MasterChangeSeqNum
import com.ustadmobile.door.annotation.SyncableEntity
import com.ustadmobile.lib.database.annotation.UmEntity
import com.ustadmobile.lib.database.annotation.UmPrimaryKey
import com.ustadmobile.lib.db.entities.XLangMapEntry.Companion.TABLE_ID

@Entity
@SyncableEntity(tableId = TABLE_ID)
data class XLangMapEntry(
        var verbLangMapUid: Long = 0L,
        var objectLangMapUid: Long = 0L,
        var languageLangMapUid: Long = 0L,
        var languageVariantLangMapUid: Long = 0L,
        var valueLangMap: String = "",

        @LocalChangeSeqNum
        var statementLangMapLocalCsn: Int = 0,

        @MasterChangeSeqNum
        var statementLangMapMasterCsn: Int = 0,

        @LastChangedBy
        var statementLangMapLastChangedBy: Int = 0

) {

    @PrimaryKey(autoGenerate = true)
    var statementLangMapUid: Long = 0

    companion object {

        const val TABLE_ID = 74
    }


}