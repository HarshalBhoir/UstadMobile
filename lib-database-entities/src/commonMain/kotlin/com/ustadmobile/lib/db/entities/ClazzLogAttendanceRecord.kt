package com.ustadmobile.lib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ustadmobile.lib.database.annotation.*

@UmEntity(tableId = 15)
@Entity
open class ClazzLogAttendanceRecord() {

    @UmPrimaryKey(autoIncrement = true)
    @PrimaryKey(autoGenerate = true)
    var clazzLogAttendanceRecordUid: Long = 0

    var clazzLogAttendanceRecordClazzLogUid: Long = 0

    var clazzLogAttendanceRecordClazzMemberUid: Long = 0

    var attendanceStatus: Int = 0

    @UmSyncMasterChangeSeqNum
    var clazzLogAttendanceRecordMasterChangeSeqNum: Long = 0

    @UmSyncLocalChangeSeqNum
    var clazzLogAttendanceRecordLocalChangeSeqNum: Long = 0

    @UmSyncLastChangedBy
    var clazzLogAttendanceRecordLastChangedBy: Int = 0

    companion object {

        const val STATUS_ATTENDED = 1

        const val STATUS_ABSENT = 2

        const val STATUS_PARTIAL = 4
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true

        if (o == null ) return false

        val that = o as ClazzLogAttendanceRecord?

        if (clazzLogAttendanceRecordUid != that!!.clazzLogAttendanceRecordUid) return false
        if (clazzLogAttendanceRecordClazzLogUid != that.clazzLogAttendanceRecordClazzLogUid)
            return false
        return if (clazzLogAttendanceRecordClazzMemberUid != that.clazzLogAttendanceRecordClazzMemberUid) false else attendanceStatus == that.attendanceStatus
    }

    override fun hashCode(): Int {
        var result = (clazzLogAttendanceRecordUid xor clazzLogAttendanceRecordUid.ushr(32)).toInt()
        result = 31 * result + (clazzLogAttendanceRecordClazzLogUid xor clazzLogAttendanceRecordClazzLogUid.ushr(32)).toInt()
        result = 31 * result + (clazzLogAttendanceRecordClazzMemberUid xor clazzLogAttendanceRecordClazzMemberUid.ushr(32)).toInt()
        result = 31 * result + attendanceStatus
        return result
    }
}
