package com.ustadmobile.core.db.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ustadmobile.core.impl.UmCallback
import com.ustadmobile.lib.database.annotation.UmDao
import com.ustadmobile.lib.database.annotation.UmOnConflictStrategy
import com.ustadmobile.lib.database.annotation.UmRepository
import com.ustadmobile.lib.db.entities.ClazzLog
import com.ustadmobile.lib.db.entities.ClazzLogWithScheduleStartEndTimes
import com.ustadmobile.lib.db.entities.Role


@UmDao(permissionJoin = "INNER JOIN Clazz ON ClazzLog.clazzLogClazzUid = Clazz.clazzUid", 
        selectPermissionCondition = ClazzDao.ENTITY_LEVEL_PERMISSION_CONDITION1 +
        Role.PERMISSION_CLAZZ_LOG_ATTENDANCE_SELECT + ClazzDao.ENTITY_LEVEL_PERMISSION_CONDITION2, 
        updatePermissionCondition = ClazzDao.ENTITY_LEVEL_PERMISSION_CONDITION1 +
        Role.PERMISSION_CLAZZ_LOG_ATTENDANCE_UPDATE + ClazzDao.ENTITY_LEVEL_PERMISSION_CONDITION2, 
        insertPermissionCondition = ClazzDao.TABLE_LEVEL_PERMISSION_CONDITION1 +
        Role.PERMISSION_CLAZZ_LOG_ATTENDANCE_INSERT + ClazzDao.TABLE_LEVEL_PERMISSION_CONDITION2)
@UmRepository
@Dao
abstract class ClazzLogDao : BaseDao<ClazzLog> {

    class NumberOfDaysClazzesOpen {
        var date: Long = 0
        var number: Int = 0
    }

    /**
     * Small POJO used by the attendance screen to get a list of valid dates for the class (to show
     * in a list) and their UID so they can be looked up.
     */
    class ClazzLogUidAndDate {

        var clazzLogUid: Long = 0

        var logDate: Long = 0

        constructor(clazzLog: ClazzLog) {
            this.clazzLogUid = clazzLog.clazzLogUid
            this.logDate = clazzLog.logDate
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null ) return false

            val that = o as ClazzLogUidAndDate?

            return if (clazzLogUid != that!!.clazzLogUid) false else logDate == that.logDate
        }

        override fun hashCode(): Int {
            var result = (clazzLogUid xor clazzLogUid.ushr(32)).toInt()
            result = 31 * result + (logDate xor logDate.ushr(32)).toInt()
            return result
        }
    }

    @Insert
    abstract override fun insert(entity: ClazzLog): Long

    @Insert(onConflict = UmOnConflictStrategy.REPLACE)
    abstract fun replace(entity: ClazzLog): Long

    @Insert
    abstract fun insertAsync(entity: ClazzLog, resultObject: UmCallback<Long>)

    @Query("SELECT * FROM ClazzLog WHERE clazzLogUid = :uid")
    abstract fun findByUid(uid: Long): ClazzLog

    @Query("SELECT * FROM ClazzLog WHERE clazzLogUid = :uid")
    abstract fun findByUidAsync(uid: Long, callback: UmCallback<ClazzLog>)

    @Query("SELECT * FROM ClazzLog WHERE clazzLogClazzUid = :clazzUid ORDER BY logDate DESC LIMIT 1")
    abstract fun findMostRecentByClazzUid(clazzUid: Long, callback: UmCallback<ClazzLog>)

    @Query("SELECT * FROM ClazzLog WHERE clazzLogClazzUid = :clazzid AND logDate = :date")
    abstract fun findByClazzIdAndDate(clazzid: Long, date: Long): ClazzLog

    @Query("SELECT * FROM ClazzLog WHERE clazzLogClazzUid = :clazzid and logDate = :date")
    abstract fun findByClazzIdAndDateAsync(clazzid: Long, date: Long,
                                           resultObject: UmCallback<ClazzLog>)

    @Query("SELECT * FROM ClazzLog")
    abstract fun findAll(): List<ClazzLog>

    @Query("UPDATE ClazzLog SET done = 1 where clazzLogUid = :clazzLogUid ")
    abstract fun updateDoneForClazzLogAsync(clazzLogUid: Long, callback: UmCallback<Int>)

    @Query("SELECT * FROM ClazzLog where clazzLogClazzUid = :clazzUid ORDER BY logDate DESC")
    abstract fun findByClazzUid(clazzUid: Long): DataSource.Factory<Int, ClazzLog>

    @Query("SELECT * FROM ClazzLog where clazzLogClazzUid = :clazzUid ORDER BY logDate DESC")
    abstract fun findByClazzUidAsList(clazzUid: Long): List<ClazzLog>

    @Query("SELECT * FROM ClazzLog WHERE clazzLogClazzUid = :clazzUid AND NOT canceled")
    abstract fun findByClazzUidNotCanceled(clazzUid: Long): DataSource.Factory<Int, ClazzLog>

    @Query("SELECT ClazzLog.*, Schedule.sceduleStartTime, Schedule.scheduleEndTime, " +
            "Schedule.scheduleFrequency FROM ClazzLog " +
            "LEFT JOIN Schedule ON Schedule.scheduleUid = ClazzLog.clazzLogScheduleUid " +
            "WHERE clazzLogClazzUid = :clazzUid AND NOT canceled ORDER BY logDate ASC")
    abstract fun findByClazzUidNotCancelledWithSchedule(clazzUid: Long): DataSource.Factory<Int, ClazzLogWithScheduleStartEndTimes>

    @Query("UPDATE ClazzLog SET numPresent = :numPresent,  numAbsent = :numAbsent, " + "numPartial = :numPartial WHERE clazzLogUid = :clazzLogUid")
    abstract fun updateClazzAttendanceNumbersAsync(clazzLogUid: Long, numPresent: Int,
                                                   numAbsent: Int, numPartial: Int,
                                                   callback: UmCallback<Any>)

    @Query("SELECT COUNT(Clazz.clazzName) as number, clazzLog.logDate as date from ClazzLog " +
            " LEFT JOIN Clazz ON ClazzLog.clazzLogClazzUid = Clazz.clazzUid" +
            "   WHERE ClazzLog.logDate > :fromDate and ClazzLog.logDate < :toDate " +
            " GROUP BY ClazzLog.logDate")
    abstract fun getNumberOfClassesOpenForDate(fromDate: Long, toDate: Long,
                                               resultList: UmCallback<List<NumberOfDaysClazzesOpen>>)

    @Query("SELECT COUNT(Clazz.clazzName) as number, clazzLog.logDate as date from ClazzLog " +
            " LEFT JOIN Clazz ON ClazzLog.clazzLogClazzUid = Clazz.clazzUid" +
            "   WHERE ClazzLog.logDate > :fromDate and ClazzLog.logDate < :toDate " +
            "       AND ClazzLog.clazzLogClazzUid in (:clazzes) " +
            " GROUP BY ClazzLog.logDate")
    abstract fun getNumberOfClassesOpenForDateClazzes(fromDate: Long, toDate: Long,
                                                      clazzes: List<Long>, resultList: UmCallback<List<NumberOfDaysClazzesOpen>>)

    @Query("SELECT COUNT(Clazz.clazzName) as number, clazzLog.logDate as date from ClazzLog " +
            " LEFT JOIN Clazz ON ClazzLog.clazzLogClazzUid = Clazz.clazzUid" +
            "   WHERE ClazzLog.logDate > :fromDate and ClazzLog.logDate < :toDate " +
            "       AND Clazz.clazzLocationUid in (:locations) " +
            " GROUP BY ClazzLog.logDate")
    abstract fun getNumberOfClassesOpenForDateLocations(fromDate: Long, toDate: Long,
                                                        locations: List<Long>, resultList: UmCallback<List<NumberOfDaysClazzesOpen>>)

    @Query("SELECT COUNT(Clazz.clazzName) as number, clazzLog.logDate as date from ClazzLog " +
            " LEFT JOIN Clazz ON ClazzLog.clazzLogClazzUid = Clazz.clazzUid" +
            "   WHERE ClazzLog.logDate > :fromDate and ClazzLog.logDate < :toDate " +
            "       AND ClazzLog.clazzLogClazzUid in (:clazzes) " +
            "       AND Clazz.clazzLocationUid in (:locations) " +
            " GROUP BY ClazzLog.logDate")
    abstract fun getNumberOfClassesOpenForDateClazzesLocation(fromDate: Long, toDate: Long,
                                                              clazzes: List<Long>, locations: List<Long>,
                                                              resultList: UmCallback<List<NumberOfDaysClazzesOpen>>)

    fun getNumberOfClassesOpenForDateClazzes(fromDate: Long, toDate: Long,
                                             clazzes: List<Long>, locations: List<Long>,
                                             resultList: UmCallback<List<NumberOfDaysClazzesOpen>>) {
        if (locations.isEmpty()) {
            if (clazzes.isEmpty()) {
                getNumberOfClassesOpenForDate(fromDate, toDate, resultList)
            } else {
                getNumberOfClassesOpenForDateClazzes(fromDate, toDate, clazzes, resultList)
            }
        } else {
            if (clazzes.isEmpty()) {
                getNumberOfClassesOpenForDateLocations(fromDate, toDate, locations, resultList)
            } else {
                getNumberOfClassesOpenForDateClazzesLocation(fromDate, toDate, clazzes, locations, resultList)
            }
        }

    }

    @Query("UPDATE ClazzLog SET canceled = :canceled WHERE clazzLogScheduleUid = :scheduleUid AND logDate >= :after ")
    abstract fun cancelFutureInstances(scheduleUid: Long, after: Long, canceled: Boolean)

    @Query("SELECT ClazzLog.clazzLogUid, ClazzLog.logDate FROM ClazzLog " + " WHERE clazzLogClazzUid = :clazzUid ORDER BY logDate ASC")
    abstract fun getListOfClazzLogUidsAndDatesForClazz(clazzUid: Long,
                                                       callback: UmCallback<List<ClazzLogUidAndDate>>)

    companion object {


        /**
         * As the ClazzLog object is added using a timer, we need to ensure that the object created for
         * a specific time should come with the same primary key. For this purposes, we generate a
         * a hashcode using the clazzuid and startTime.
         *
         * @param clazzUid UID of the clazz
         * @param startTime scheduled start time of this instance of the clazz
         * @return a hashcode computed from the above
         */
        fun generateClazzLogUid(clazzUid: Long, startTime: Long): Int {
            var hash = clazzUid.hashCode()
            hash = 31 * hash + startTime.hashCode()
            return hash
        }
    }

}
