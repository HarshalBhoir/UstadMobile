package com.ustadmobile.core.db.dao;


import com.ustadmobile.core.db.UmLiveData;
import com.ustadmobile.core.db.UmProvider;
import com.ustadmobile.core.impl.UmCallback;
import com.ustadmobile.lib.database.annotation.UmDao;
import com.ustadmobile.lib.database.annotation.UmInsert;
import com.ustadmobile.lib.database.annotation.UmQuery;
import com.ustadmobile.lib.database.annotation.UmRepository;
import com.ustadmobile.lib.database.annotation.UmUpdate;
import com.ustadmobile.lib.db.entities.SaleProductGroupJoin;
import com.ustadmobile.lib.db.entities.SaleProduct;
import com.ustadmobile.lib.db.sync.dao.SyncableDao;

import java.util.List;

@UmDao(updatePermissionCondition = RoleDao.SELECT_ACCOUNT_IS_ADMIN, insertPermissionCondition =
        RoleDao.SELECT_ACCOUNT_IS_ADMIN)
@UmRepository
public abstract class SaleProductGroupJoinDao implements SyncableDao<SaleProductGroupJoin, SaleProductGroupJoinDao> {

    //INSERT

    @UmInsert
    public abstract void insertAsync(SaleProductGroupJoin entity, UmCallback<Long> insertCallback);


    //FIND ALL ACTIVE

    public static final String ALL_ACTIVE_QUERY = "SELECT * FROM SaleProductGroupJoin WHERE saleProductGroupJoinActive = 1";

    @UmQuery(ALL_ACTIVE_QUERY)
    public abstract UmLiveData<List<SaleProductGroupJoin>> findAllActiveLive();

    @UmQuery(ALL_ACTIVE_QUERY)
    public abstract List<SaleProductGroupJoin> findAllActiveList();

    @UmQuery(ALL_ACTIVE_QUERY)
    public abstract void findAllActiveAsync(UmCallback<List<SaleProductGroupJoin>> allActiveCallback);

    @UmQuery(ALL_ACTIVE_QUERY)
    public abstract UmProvider<SaleProductGroupJoin> findAllActiveProvider();

    @UmQuery("SELECT SaleProduct.* FROM SaleProductGroupJoin LEFT JOIN SaleProduct ON " +
            "SaleProductGroupJoin.saleProductGroupJoinProductUid = SaleProduct.saleProductUid " +
            "WHERE saleProductGroupJoinGroupUid = :collectionUid")
    public abstract UmLiveData<List<SaleProduct>> findListOfProductsInACollectionLive(long collectionUid);


    //LOOK UP

    public static final String FIND_BY_UID_QUERY = "SELECT * FROM SaleProductGroupJoin WHERE saleProductGroupJoinUid = :uid";

    @UmQuery(FIND_BY_UID_QUERY)
    public abstract SaleProductGroupJoin findByUid(long uid);

    @UmQuery(FIND_BY_UID_QUERY)
    public abstract void findByUidAsync(long uid, UmCallback<SaleProductGroupJoin> findByUidCallback);

    @UmQuery(FIND_BY_UID_QUERY)
    public abstract UmLiveData<SaleProductGroupJoin> findByUidLive(long uid);

    //INACTIVATE:

    public static final String INACTIVATE_QUERY =
            "UPDATE SaleProductGroupJoin SET saleProductGroupJoinActive = 0 WHERE saleProductGroupJoinUid = :uid";
    @UmQuery(INACTIVATE_QUERY)
    public abstract void inactivateEntity(long uid);

    @UmQuery(INACTIVATE_QUERY)
    public abstract void inactivateEntityAsync(long uid, UmCallback<Integer> inactivateCallback);


    //UPDATE:

    @UmUpdate
    public abstract void updateAsync(SaleProductGroupJoin entity, UmCallback<Integer> updateCallback);
}