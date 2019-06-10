package com.ustadmobile.lib.db.entities

import androidx.room.Entity
import com.ustadmobile.lib.database.annotation.*
import com.ustadmobile.lib.db.entities.StateEntity.Companion.TABLE_ID

@UmEntity(tableId = TABLE_ID)
@Entity
class StateEntity {

    @UmPrimaryKey(autoGenerateSyncable = true)
    var stateUid: Long = 0

    var stateId: String? = null

    var agentUid: Long = 0

    var activityId: String? = null

    var registration: String? = null

    var isIsactive: Boolean = false

    var timestamp: Long = 0

    @UmSyncMasterChangeSeqNum
    var stateMasterChangeSeqNum: Long = 0

    @UmSyncLocalChangeSeqNum
    var stateLocalChangeSeqNum: Long = 0

    @UmSyncLastChangedBy
    var stateLastChangedBy: Int = 0

    constructor(activityId: String?, agentUid: Long, registration: String?, stateId: String?, isActive: Boolean, timestamp: Long) {
        this.activityId = activityId
        this.agentUid = agentUid
        this.registration = registration
        this.isIsactive = isActive
        this.stateId = stateId
        this.timestamp = timestamp
    }

    constructor() {

    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || this::class != o::class) return false

        val that = o as StateEntity?

        if (stateUid != that!!.stateUid) return false
        if (agentUid != that.agentUid) return false
        if (isIsactive != that.isIsactive) return false
        if (stateId != that.stateId) return false
        return if (activityId != that.activityId) false else registration == that.registration
    }

    override fun hashCode(): Int {
        var result = (stateUid xor stateUid.ushr(32)).toInt()
        result = 31 * result + if (stateId != null) stateId!!.hashCode() else 0
        result = 31 * result + (agentUid xor agentUid.ushr(32)).toInt()
        result = 31 * result + if (activityId != null) activityId!!.hashCode() else 0
        result = 31 * result + if (registration != null) registration!!.hashCode() else 0
        result = 31 * result + if (isIsactive) 1 else 0
        return result
    }

    companion object {

        const val TABLE_ID = 70
    }
}