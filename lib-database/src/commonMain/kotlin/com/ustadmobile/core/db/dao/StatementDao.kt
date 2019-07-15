package com.ustadmobile.core.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import com.ustadmobile.door.DoorQuery
import com.ustadmobile.lib.database.annotation.UmRepository
import com.ustadmobile.lib.db.entities.StatementEntity

@Dao
@UmRepository
abstract class StatementDao : BaseDao<StatementEntity> {

    @Query("SELECT * From StatementEntity")
    abstract fun all(): List<StatementEntity>

    @Query("SELECT * FROM StatementEntity WHERE statementId = :id LIMIT 1")
    abstract fun findByStatementId(id: String): StatementEntity?

    @Query("SELECT * FROM StatementEntity WHERE statementId IN (:id)")
    abstract fun findByStatementIdList(id: List<String>): List<StatementEntity>

    @RawQuery
    abstract fun getResults(query: DoorQuery): List<ReportData>

    @RawQuery
    abstract fun getListResults(query: DoorQuery): List<StatementEntity>

    data class ReportData(var yAxis: Float, var xAxis: String, var subgroup: String)
}
