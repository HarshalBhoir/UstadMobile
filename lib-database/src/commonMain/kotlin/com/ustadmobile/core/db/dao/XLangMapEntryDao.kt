package com.ustadmobile.core.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.ustadmobile.lib.database.annotation.UmRepository
import com.ustadmobile.lib.db.entities.XLangMapEntry

@Dao
@UmRepository
abstract class XLangMapEntryDao : BaseDao<XLangMapEntry> {

    @Query("SELECT verbLangMapUid, valueLangMap FROM XLangMapEntry " +
            "WHERE verbLangMapUid != 0 " +
            "AND valueLangMap LIKE :verb AND verbLangMapUid NOT IN (:uidList)")
    abstract suspend fun getAllVerbs(verb: String, uidList: List<Long>): List<Verb>

    @Query("SELECT objectLangMapUid, valueLangMap FROM XLangMapEntry WHERE objectLangMapUid != 0")
    abstract suspend fun getAllObjects(): List<XObject>

    @Query("SELECT * FROM XLangMapEntry WHERE objectLangMapUid IN (:ids)")
    abstract suspend fun getValuesWithListOfId(ids: List<Int>): List<XLangMapEntry>

    data class Verb(var verbLangMapUid: Long = 0, var valueLangMap: String = "") {

        override fun toString(): String {
            return valueLangMap
        }
    }

    data class XObject(var objectLangMapUid: Long = 0, var valueLangMap: String = "") {

        override fun toString(): String {
            return valueLangMap
        }
    }
}
