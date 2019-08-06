package com.ustadmobile.core.db.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import com.ustadmobile.core.db.UmProvider
import com.ustadmobile.core.impl.UmCallback
import com.ustadmobile.lib.database.annotation.UmDao
import com.ustadmobile.lib.database.annotation.UmRepository
import com.ustadmobile.lib.db.entities.Person
import com.ustadmobile.lib.db.entities.PersonGroupMember
import com.ustadmobile.lib.db.entities.PersonWithEnrollment

@UmDao(updatePermissionCondition = RoleDao.SELECT_ACCOUNT_IS_ADMIN, 
        insertPermissionCondition = RoleDao.SELECT_ACCOUNT_IS_ADMIN)
@UmRepository
@Dao
abstract class PersonGroupMemberDao : BaseDao<PersonGroupMember> {

    @Query("SELECT * FROM PersonGroupMember WHERE groupMemberPersonUid = :personUid")
    abstract fun findAllGroupWherePersonIsIn(personUid: Long,
                                             resultList: UmCallback<List<PersonGroupMember>>)

    @Query("SELECT * FROM PersonGroupMember WHERE groupMemberPersonUid = :personUid")
    abstract fun findAllGroupWherePersonIsInSync(personUid: Long): List<PersonGroupMember>

    @Query("SELECT * FROM PersonGroupMember WHERE groupMemberGroupUid = :groupUid " + " AND groupMemberActive = 1")
    abstract fun finAllMembersWithGroupId(groupUid: Long): DataSource.Factory<Int, PersonGroupMember>

    @Query("SELECT Person.*, (0) AS clazzUid, " +
            "  (0) AS attendancePercentage, " +
            "  (0) AS clazzMemberRole,  " +
            "  (SELECT PersonPicture.personPictureUid FROM PersonPicture WHERE " +
            "  PersonPicture.personPicturePersonUid = Person.personUid ORDER BY picTimestamp " +
            "  DESC LIMIT 1) AS personPictureUid, " +
            "  (0) AS enrolled from PersonGroupMember " +
            " LEFT JOIN Person ON PersonGroupMember.groupMemberPersonUid = Person.personUid " +
            " WHERE groupMemberGroupUid = :groupUid AND groupMemberActive = 1 ")
    abstract fun findAllPersonWithEnrollmentWithGroupUid(groupUid: Long): DataSource.Factory<Int, PersonWithEnrollment>

    @Query("Select Person.* from PersonGroupMember " +
            " LEFT JOIN Person on PersonGroupMember.groupMemberPersonUid = Person.personUid " +
            " WHERE PersonGroupMember.groupMemberGroupUid = :groupUid")
    abstract fun findPersonByGroupUid(groupUid: Long): List<Person>

    @Query("SELECT * FROM PersonGroupMember WHERE groupMemberGroupUid = :groupUid AND " + " groupMemberPersonUid = :personUid ")
    abstract fun findMemberByGroupAndPersonAsync(groupUid: Long, personUid: Long,
                                                 resultObject: UmCallback<PersonGroupMember>)

    @Query("UPDATE PersonGroupMember SET groupMemberActive = 0 " + " WHERE groupMemberPersonUid = :personUid AND groupMemberGroupUid = :groupUid")
    abstract fun inactivateMemberFromGroupAsync(personUid: Long, groupUid: Long,
                                                resultObject: UmCallback<Int>)

}
