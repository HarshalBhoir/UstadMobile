package com.ustadmobile.core.db.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ustadmobile.lib.database.annotation.UmDao
import com.ustadmobile.lib.database.annotation.UmRepository
import com.ustadmobile.lib.database.annotation.UmUpdate
import com.ustadmobile.lib.db.entities.SelQuestionOption

@UmDao(selectPermissionCondition = "(:accountPersonUid = :accountPersonUid)")
@UmRepository
@Dao
abstract class SelQuestionOptionDao : BaseDao<SelQuestionOption> {

    @Insert
    abstract override fun insert(entity: SelQuestionOption): Long

    @UmUpdate
    abstract override fun update(entity: SelQuestionOption)

    @UmUpdate
    abstract suspend fun updateAsync(entity: SelQuestionOption) : Int

    @Query("SELECT * FROM SelQuestionOption " + " WHERE selQuestionOptionUid = :uid")
    abstract fun findByUid(uid: Long): SelQuestionOption

    @Query("SELECT * FROM SelQuestionOption " + " WHERE selQuestionOptionUid = :uid")
    abstract suspend fun findByUidAsync(uid: Long) : SelQuestionOption

    @Query("SELECT * FROM SelQuestionOption " + " WHERE selQuestionOptionQuestionUid = :questionUid")
    abstract suspend fun findAllOptionsByQuestionUid(questionUid: Long) : List<SelQuestionOption>

    @Query("SELECT * FROM SelQuestionOption " + " WHERE selQuestionOptionQuestionUid = :questionUid")
    abstract fun findAllOptionsByQuestionUidProvider(questionUid: Long): DataSource.Factory<Int, SelQuestionOption>

    @Query("SELECT * FROM SelQuestionOption " + " WHERE selQuestionOptionQuestionUid = :questionUid AND optionActive = 1")
    abstract fun findAllActiveOptionsByQuestionUidProvider(questionUid: Long): DataSource.Factory<Int, SelQuestionOption>
}