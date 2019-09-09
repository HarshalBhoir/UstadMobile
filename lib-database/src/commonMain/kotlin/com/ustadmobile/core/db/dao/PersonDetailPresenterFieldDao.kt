package com.ustadmobile.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ustadmobile.lib.database.annotation.UmDao
import com.ustadmobile.lib.database.annotation.UmRepository
import com.ustadmobile.lib.db.entities.PersonDetailPresenterField

@UmDao(updatePermissionCondition = RoleDao.SELECT_ACCOUNT_IS_ADMIN,
        insertPermissionCondition = RoleDao.SELECT_ACCOUNT_IS_ADMIN)
@UmRepository
@Dao
abstract class PersonDetailPresenterFieldDao : BaseDao<PersonDetailPresenterField> {

    @Insert
    abstract override fun insert(entity: PersonDetailPresenterField): Long

    @Query("SELECT * FROM PersonDetailPresenterField WHERE personDetailPresenterFieldUid = :uid")
    abstract fun findByUid(uid: Long): PersonDetailPresenterField?

    @Query("SELECT * FROM PersonDetailPresenterField ORDER BY fieldIndex")
    abstract suspend fun findAllPersonDetailPresenterFields() : List<PersonDetailPresenterField>

    //TODOne: KMP Check MutableList return type.
    //Update: Nope. List it is.
    @Query("SELECT * FROM PersonDetailPresenterField WHERE viewModeVisible = 1 ORDER BY fieldIndex")
    abstract suspend fun findAllPersonDetailPresenterFieldsViewMode() :
            List<PersonDetailPresenterField>

    @Query("SELECT * FROM PersonDetailPresenterField WHERE editModeVisible = 1 ORDER BY fieldIndex")
    abstract suspend fun findAllPersonDetailPresenterFieldsEditMode() :
            List<PersonDetailPresenterField>

    @Query("SELECT * FROM PersonDetailPresenterField WHERE fieldUid = :uid")
    abstract suspend fun findAllByFieldUid(uid: Long) : List<PersonDetailPresenterField>

    @Query("SELECT * FROM PersonDetailPresenterField WHERE labelMessageId = :id")
    abstract suspend fun findAllByLabelMessageId(id: Int) : List<PersonDetailPresenterField>

    @Query("SELECT * FROM PersonDetailPresenterField WHERE fieldIndex = :id")
    abstract suspend fun findAllByFieldIndex(id: Int) : List<PersonDetailPresenterField>
}
