package com.ustadmobile.core.db.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ustadmobile.door.DoorLiveData
import com.ustadmobile.lib.database.annotation.UmDao
import com.ustadmobile.lib.database.annotation.UmRepository
import com.ustadmobile.lib.db.entities.SalePayment


@UmDao(updatePermissionCondition = RoleDao.SELECT_ACCOUNT_IS_ADMIN, insertPermissionCondition = RoleDao.SELECT_ACCOUNT_IS_ADMIN)
@UmRepository
@Dao
abstract class SalePaymentDao : BaseDao<SalePayment> {


    @Query(" SELECT COUNT(*) FROM " +
            " (SELECT " +
            " COALESCE( (SELECT SUM(SaleItem.saleItemPricePerPiece * SaleItem.saleItemQuantity) - " +
            "            SUM(Sale.saleDiscount)  FROM Sale LEFT JOIN SaleItem on SaleItem.saleItemSaleUid = " +
            "            Sale.saleUid WHERE Sale.saleUid = sl.saleUid) ,0 " +
            " ) AS saleAmount, " +
            " COALESCE((SELECT SUM(SalePayment.salePaymentPaidAmount) FROM SalePayment " +
            "  WHERE SalePayment.salePaymentSaleUid = sl.saleUid " +
            "  AND SalePayment.salePaymentDone = 1 AND SalePayment.salePaymentActive = 1) ,0) " +
            "  AS saleAmountPaid " +
            " FROM Sale sl " +
            " WHERE sl.saleActive = 1 AND saleAmountPaid < saleAmount " +
            " )")
    abstract fun paymentsDueCountLive(): DoorLiveData<Int>

    //INSERT

    @Insert
    abstract override suspend fun insertAsync(entity: SalePayment):Long

    @Query(ALL_ACTIVE_QUERY)
    abstract fun findAllActiveLive(): DoorLiveData<List<SalePayment>>

    @Query(ALL_ACTIVE_QUERY)
    abstract fun findAllActiveList(): List<SalePayment>

    @Query(ALL_ACTIVE_QUERY)
    abstract suspend fun findAllActiveAsync():List<SalePayment>

    @Query(ALL_ACTIVE_QUERY)
    abstract fun findAllActiveProvider(): DataSource.Factory<Int, SalePayment>


    @Query(FIND_ALL_BY_SALE_UID_QUERY)
    abstract fun findBySaleUidLive(saleUid: Long): DoorLiveData<List<SalePayment>>

    @Query(FIND_ALL_BY_SALE_UID_QUERY)
    abstract suspend fun findBySaleUidAsync(saleUid: Long):List<SalePayment>

    @Query(FIND_ALL_BY_SALE_UID_QUERY)
    abstract fun findBySaleAsList(saleUid: Long): List<SalePayment>


    @Query(FIND_ALL_BY_SALE_UID_QUERY)
    abstract fun findBySaleProvider(saleUid: Long): DataSource.Factory<Int,SalePayment>

    @Query(TOTAL_PAID_BY_SALE_UID)
    abstract fun findTotalPaidPaymentsInASale(saleUid: Long): Int

    @Query(TOTAL_PAID_BY_SALE_UID)
    abstract suspend fun findTotalPaidBySaleAsync(saleUid: Long):Int


    @Query(FIND_BY_UID_QUERY)
    abstract fun findByUid(uid: Long): SalePayment?

    @Query(FIND_BY_UID_QUERY)
    abstract suspend fun findByUidAsync(uid: Long):SalePayment?

    @Query(FIND_BY_UID_QUERY)
    abstract fun findByUidLive(uid: Long): DoorLiveData<SalePayment?>

    @Query(INACTIVATE_QUERY)
    abstract fun inactivateEntity(uid: Long)

    @Query(INACTIVATE_QUERY)
    abstract suspend fun inactivateEntityAsync(uid: Long):Int

    //UPDATE:

    @Update
    abstract suspend fun updateAsync(entity: SalePayment): Int

    @Query(" SELECT COUNT(*) FROM " +
            " (SELECT " +
            " COALESCE( (SELECT SUM(SaleItem.saleItemPricePerPiece * SaleItem.saleItemQuantity) - " +
            "            SUM(Sale.saleDiscount)  FROM Sale LEFT JOIN SaleItem on SaleItem.saleItemSaleUid = " +
            "            Sale.saleUid WHERE Sale.saleUid = sl.saleUid) ,0 " +
            " ) AS saleAmount, " +
            " COALESCE((SELECT SUM(SalePayment.salePaymentPaidAmount) FROM SalePayment " +
            "  WHERE SalePayment.salePaymentSaleUid = sl.saleUid " +
            "  AND SalePayment.salePaymentDone = 1 AND SalePayment.salePaymentActive = 1) ,0) " +
            "  AS saleAmountPaid " +
            " FROM Sale sl " +
            " WHERE sl.saleActive = 1 AND saleAmountPaid < saleAmount " +
            " )")
    abstract fun getPaymentsDueCountLive(): DoorLiveData<Int>

    companion object {

        //FIND ALL ACTIVE

        const val ALL_ACTIVE_QUERY = "SELECT * FROM SalePayment WHERE salePaymentActive = 1"
        const val FIND_ALL_BY_SALE_UID_QUERY = "SELECT * FROM SalePayment WHERE salePaymentSaleUid = :saleUid AND " + "salePaymentActive = 1 ORDER BY salePaymentPaidDate DESC"

        const val TOTAL_PAID_BY_SALE_UID = "SELECT SUM(salePaymentPaidAmount) FROM SalePayment " +
                "WHERE salePaymentSaleUid = :saleUid AND salePaymentActive = 1 " +
                "AND salePaymentDone = 1"
        //LOOK UP

        const val FIND_BY_UID_QUERY = "SELECT * FROM SalePayment WHERE salePaymentUid = :uid"

        //INACTIVATE:
        const val INACTIVATE_QUERY = "UPDATE SalePayment SET salePaymentActive = 0 WHERE salePaymentUid = :uid"
    }

}
