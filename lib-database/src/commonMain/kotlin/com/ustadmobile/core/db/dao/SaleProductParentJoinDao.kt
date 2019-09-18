package com.ustadmobile.core.db.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import com.ustadmobile.door.DoorLiveData
import com.ustadmobile.lib.database.annotation.UmDao
import com.ustadmobile.lib.database.annotation.UmRepository
import com.ustadmobile.lib.db.entities.SaleNameWithImage
import com.ustadmobile.lib.db.entities.SaleProduct
import com.ustadmobile.lib.db.entities.SaleProductParentJoin
import com.ustadmobile.lib.db.entities.SaleProductSelected




@UmDao(updatePermissionCondition = RoleDao.SELECT_ACCOUNT_IS_ADMIN, insertPermissionCondition = RoleDao.SELECT_ACCOUNT_IS_ADMIN)
@UmRepository
@Dao
abstract class SaleProductParentJoinDao : BaseDao<SaleProductParentJoin> {


    /**
     * Find all SaleProduct a given SaleProduct is a child of.
     * @param childSaleProductUid   The child sale product uid
     * @param resultListCallback    Return callback of list of SaleProduct
     */
    @Query("SELECT SaleProduct.* FROM SaleProductParentJoin LEFT JOIN SaleProduct ON " +
            " SaleProduct.saleProductUid = SaleProductParentJoin.saleProductParentJoinParentUid " +
            " WHERE SaleProductParentJoin.saleProductParentJoinChildUid = :childSaleProductUid")
    abstract suspend fun findAllJoinByChildSaleProductAsync(childSaleProductUid: Long):List<SaleProduct>

    //Find all categories selected for a sale product
    @Query("SELECT CASE WHEN (SELECT COUNT(*) FROM SaleProductParentJoin " +
            " WHERE SaleProductParentJoin.saleProductParentJoinChildUid = :saleProductUid " +
            " AND SaleProductParentJoin.saleProductParentJoinParentUid = SaleProduct.saleProductUid " +
            " AND SaleProductParentJoin.saleProductParentJoinActive = 1) " +
            " > 0 THEN 1 ELSE 0 END AS isSelected, " +
            " SaleProduct.* " +
            "FROM SaleProduct WHERE SaleProduct.saleProductCategory = 1 " +
            " AND SaleProduct.saleProductActive = 1")
    abstract fun findAllSelectedCategoriesForSaleProductProvider(
            saleProductUid: Long): DataSource.Factory<Int,SaleProductSelected>

    @Query(QUERY_SELECT_ALL_SALE_PRODUCT +
            " AND SaleProductParentJoin.saleProductParentJoinParentUid = :saleProductCategoryUid " +
            " AND child.saleProductCategory = 0 ")
    abstract fun findAllItemsInACategory(saleProductCategoryUid: Long): DataSource.Factory<Int,SaleNameWithImage>

    @Query(QUERY_SELECT_ALL_SALE_PRODUCT +
            " AND SaleProductParentJoin.saleProductParentJoinParentUid = :saleProductCategoryUid " +
            " AND child.saleProductCategory = 1 ")
    abstract fun findAllCategoriesInACategory(saleProductCategoryUid: Long): DataSource.Factory<Int,SaleNameWithImage>


    @Query(QUERY_SELECT_ALL_SALE_PRODUCT +
            " AND SaleProductParentJoin.saleProductParentJoinParentUid = " +
            "   (SELECT SaleProduct.saleProductUid FROM SaleProduct " +
            "   WHERE SaleProduct.saleProductName = 'Collection' " +
            "   ORDER BY saleProductDateAdded ASC LIMIT 1) " +
            " AND child.saleProductCategory = 1 ")
    abstract fun findAllCategoriesInCollection(): DataSource.Factory<Int, SaleNameWithImage>


    @Query("SELECT * FROM SaleProductParentJoin WHERE " +
            " SaleProductParentJoin.saleProductParentJoinParentUid = :parentUid AND " +
            " SaleProductParentJoin.saleProductParentJoinChildUid = :childUid ")
    abstract suspend fun findByChildAndParentUid(childUid: Long, parentUid: Long)
            :SaleProductParentJoin?


    suspend fun createJoin(childProductUid: Long, parentProductUid: Long, activate: Boolean) {

        //1. Find existing mapping
        val result = findByChildAndParentUid(childProductUid, parentProductUid)
        if (result != null) {
            //Exists
            if (result.saleProductParentJoinActive != activate) {
                // Is not active
                result.saleProductParentJoinActive = activate
                update(result)
            } else {
                //Exists but is already set. Ignore
            }
        } else {
            //Create new with activate set
            val npj = SaleProductParentJoin(childProductUid,
                    parentProductUid, activate)
            insert(npj)
        }
    }


    //Find Top
    @Query("SELECT Parent.* FROM SaleProductParentJoin " +
            "LEFT JOIN SaleProduct as Parent ON " +
            "   Parent.saleProductUid = SaleProductParentJoin.saleProductParentJoinParentUid " +
            " WHERE SaleProductParentJoinActive = 1  " +
            "   AND (SELECT COUNT(*) FROM SaleProductParentJoin AS sp " +
            "       WHERE sp.saleProductParentJoinChildUid = Parent.saleProductUid ) = 0 " +
            " GROUP BY saleProductParentJoinParentUid")
    abstract suspend fun findTopSaleProductsAsync():List<SaleProduct>


    @Query("SELECT Parent.* FROM SaleProductParentJoin " +
            "LEFT JOIN SaleProduct as Parent ON " +
            "   Parent.saleProductUid = SaleProductParentJoin.saleProductParentJoinParentUid " +
            " WHERE SaleProductParentJoinActive = 1  " +
            "   AND (SELECT COUNT(*) FROM SaleProductParentJoin AS sp " +
            "       WHERE sp.saleProductParentJoinChildUid = Parent.saleProductUid ) = 0 " +
            " GROUP BY saleProductParentJoinParentUid")
    abstract fun findTopSaleProductsLive():DoorLiveData<List<SaleProduct>>


    //Find categories in a category uid
    @Query("SELECT Child.* FROM SaleProductParentJoin " +
            "   LEFT JOIN SaleProduct as Parent " +
            "   ON Parent.saleProductUid = SaleProductParentJoin.saleProductParentJoinParentUid " +
            "   LEFT JOIN SaleProduct as Child " +
            "   ON Child.saleProductUid = SaleProductParentJoin.saleProductParentJoinChildUid " +
            " WHERE SaleProductParentJoinActive = 1 AND Child.saleProductActive = 1 " +
            "   AND Child.saleProductCategory = 1 " +
            " AND SaleProductParentJoinParentUid = :uid ")
    abstract suspend fun findAllChildProductTypesForUidAsync(uid: Long):List<SaleProduct>

    companion object {

        const val QUERY_SELECT_ALL_SALE_PRODUCT = "SELECT child.saleProductName as name, child.saleProductDesc as description, productPicture.saleProductPictureUid as pictureUid, " +
                " '' as type, child.saleProductUid as productUid, parent.saleProductUid as productGroupUid  " +
                " FROM SaleProductParentJoin " +
                " LEFT JOIN SaleProduct child ON child.saleProductUid = SaleProductParentJoin.saleProductParentJoinChildUid " +
                " LEFT JOIN SaleProduct parent ON parent.saleProductUid = SaleProductParentJoin.saleProductParentJoinParentUid " +
                " LEFT JOIN SaleProductPicture productPicture ON productPicture.saleProductPictureSaleProductUid = child.saleProductUid " +
                " WHERE SaleProductParentJoin.saleProductParentJoinActive = 1 AND child.saleProductActive = 1 "
    }



}
