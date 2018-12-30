package com.ustadmobile.core.db.dao;

import com.ustadmobile.core.impl.UmCallback;
import com.ustadmobile.lib.database.annotation.UmDao;
import com.ustadmobile.lib.database.annotation.UmQuery;
import com.ustadmobile.lib.db.entities.EntityRole;
import com.ustadmobile.lib.db.sync.dao.SyncableDao;

@UmDao(selectPermissionCondition = "(:accountPersonUid = :accountPersonUid)")
public abstract class EntityRoleDao implements SyncableDao<EntityRole, EntityRoleDao> {

    @UmQuery("SELECT (SELECT admin FROM Person WHERE personUid = :accountPersonUid) " +
            "OR EXISTS(SELECT EntityRole.erUid FROM EntityRole " +
            " JOIN Role ON EntityRole.erRoleUid = Role.roleUid " +
            " JOIN PersonGroupMember ON EntityRole.erGroupUid = PersonGroupMember.groupMemberGroupUid" +
            " WHERE " +
            " PersonGroupMember.groupMemberPersonUid = :accountPersonUid " +
            " AND EntityRole.erTableId = :tableId " +
            " AND (Role.rolePermissions & :permission) > 0) AS hasPermission")
    public abstract void userHasTableLevelPermission(long accountPersonUid, int tableId, long permission,
                                              UmCallback<Boolean> callback);

}
