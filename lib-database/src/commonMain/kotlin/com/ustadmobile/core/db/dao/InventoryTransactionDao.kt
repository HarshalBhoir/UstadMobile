package com.ustadmobile.core.db.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import com.ustadmobile.lib.database.annotation.UmDao
import com.ustadmobile.lib.database.annotation.UmRepository
import com.ustadmobile.lib.db.entities.InventoryTransaction
import com.ustadmobile.lib.db.entities.InventoryTransactionDetail


@UmDao(updatePermissionCondition = RoleDao.SELECT_ACCOUNT_IS_ADMIN,
        insertPermissionCondition = RoleDao.SELECT_ACCOUNT_IS_ADMIN)
@UmRepository
@Dao
abstract class InventoryTransactionDao: BaseDao<InventoryTransaction> {


    @Query("""
    SELECT 
        COUNT(*) as stockCount,  
        GROUP_CONCAT(DISTINCT WE.firstNames||' '||WE.lastName) as weNames,  
        LE.firstNames||' '||LE.lastName as leName,
        LE.personUid AS fromLeUid,
        Sale.saleUid, TOLE.personUid as toLeUid,
        CASE WHEN Sale.saleUid THEN Sale.saleCreationDate ELSE InventoryItem.InventoryItemDateAdded END as transactionDate
    FROM InventoryTransaction
    LEFT JOIN InventoryItem ON InventoryTransaction.inventoryTransactionInventoryItemUid = InventoryItem.inventoryItemUid
    LEFT JOIN SaleProduct ON InventoryItem.InventoryItemSaleProductUid = SaleProduct.saleProductUid 
    LEFT JOIN Person as TOLE ON InventoryTransaction.InventoryTransactionToLeUid = TOLE.personUid 
    LEFT JOIN Person as LE ON InventoryTransaction.inventoryTransactionFromLeUid = LE.personUid
    LEFT JOIN Person as WE ON InventoryItem.InventoryItemWeUid = WE.personUid
    LEFT JOIN Sale ON InventoryTransaction.inventoryTransactionSaleUid = Sale.saleUid
    LEFT JOIN SaleItem ON InventoryTransaction.inventoryTransactionSaleItemUid = SaleItem.saleItemUid
    LEFT JOIN Person as MLE ON MLE.personUid = :leUid 
    WHERE
        CAST(InventoryTransaction.inventoryTransactionActive AS INTEGER) = 1  
        AND CAST(InventoryItem.inventoryItemActive AS INTEGER) = 1 AND 
        SaleProduct.saleProductUid = :saleProductUid 
        AND ( CASE WHEN (CAST(MLE.admin as INTEGER) = 0) THEN LE.personUid = MLE.personUid ELSE 1 END )
        AND CAST(InventoryTransaction.inventoryTransactionActive AS INTEGER) = 1
        AND 
            (InventoryItem.inventoryItemWeUid IN (
                SELECT MEMBER.personUid FROM PersonGroupMember 
                LEFT JOIN PERSON AS MEMBER ON MEMBER.personUid = PersonGroupMember.groupMemberPersonUid
                LEFT JOIN PERSON AS LE ON LE.personUid = MLE.personUid
                 WHERE groupMemberGroupUid = LE.mPersonGroupUid 
                AND CAST(groupMemberActive  AS INTEGER) = 1
                ) 
            OR 
                CASE WHEN (CAST(MLE.admin as INTEGER) = 1) THEN 1 ELSE 0 END 
            )
    GROUP BY saleUid, transactionDate
    ORDER BY transactionDate DESC
    """)
    abstract fun findAllInventoryByProduct(saleProductUid: Long, leUid: Long)
            : DataSource.Factory<Int, InventoryTransactionDetail>

    @Query("""
        UPDATE InventoryTransaction SET inventoryTransactionActive = 1 
        WHERE InventoryTransaction.inventoryTransactionSaleUid = :saleUid AND 
        InventoryTransaction.inventoryTransactionFromLeUid = :leUid AND 
        CAST(InventoryTransaction.inventoryTransactionActive AS INTEGER) = 0 
    """)
    abstract suspend fun activateAllTransactionsBySaleAndLe(saleUid: Long, leUid: Long): Int


}