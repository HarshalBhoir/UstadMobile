package com.ustadmobile.core.db.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ustadmobile.door.DoorLiveData
import com.ustadmobile.lib.database.annotation.UmDao
import com.ustadmobile.lib.database.annotation.UmRepository
import com.ustadmobile.lib.db.entities.*


@UmDao(updatePermissionCondition = RoleDao.SELECT_ACCOUNT_IS_ADMIN,
        insertPermissionCondition = RoleDao.SELECT_ACCOUNT_IS_ADMIN)
@Dao
@UmRepository
abstract class SaleDao : BaseDao<Sale> {


    @Query("SELECT COUNT(*) FROM SALE WHERE salePreOrder = 1 AND saleActive = 1")
    abstract fun removeMe(): DataSource.Factory<Int, Int>


    @Query("SELECT COUNT(*) FROM SALE WHERE salePreOrder = 1 AND saleActive = 1")
    abstract fun preOrderSaleCountProvider(): DataSource.Factory<Int, Int>

    //    @Query("select count(*) from sale where salePreOrder = 1 AND saleActive = 1")
    @Query(" SELECT COUNT(*) FROM (SELECT (select (case  when  " +
            " (SELECT count(*) from SaleItem sip where sip.saleItemSaleUid = sl.saleUid " +
            " and sip.saleItemPreOrder = 1 ) > 0 then 1  else 0 end) from Sale)  as saleItemPreOrder " +
            " FROM Sale sl WHERE sl.saleActive = 1  AND (saleItemPreOrder = 1 OR salePreOrder = 1)) ")
    abstract fun preOrderSaleCountLive(): Int
    
    //INSERT

    @Insert
    abstract override suspend fun insertAsync(entity: Sale): Long

    @Query(ALL_SALES_QUERY)
    abstract fun findAllList(): List<Sale>

    @Query(ALL_SALES_ACTIVE_QUERY)
    abstract fun findAllActiveLive(): DoorLiveData<List<Sale>>

    @Query(ALL_SALES_ACTIVE_QUERY)
    abstract fun findAllActiveList(): List<Sale>

    @Query(ALL_SALES_ACTIVE_QUERY)
    abstract suspend fun findAllActiveAsync():List<Sale>

    @Query(ALL_SALES_ACTIVE_QUERY)
    abstract fun findAllActiveProvider(): DataSource.Factory<Int,Sale>

    @Query("$ALL_SALES_ACTIVE_QUERY AND salePreOrder = 1 ")
    abstract fun findAllActivePreorderSalesLive(): DoorLiveData<List<Sale>>

    @Query("$ALL_SALES_ACTIVE_QUERY AND salePaymentDone = 0")
    abstract fun findAllActivePaymentDueSalesLive(): DoorLiveData<List<Sale>>

    @Query("SELECT * FROM Sale WHERE saleTitle = :saleTitle AND saleActive = 1")
    abstract suspend fun findAllSaleWithTitleAsync(saleTitle: String): List<Sale>

    @Query("SELECT * FROM Sale WHERE saleTitle = :saleTitle AND saleActive = 1")
    abstract fun findAllSaleWithTitle(saleTitle: String): List<Sale>


    @Query(ALL_SALE_LIST)
    abstract fun findAllActiveAsSaleListDetailLive(): DoorLiveData<List<SaleListDetail>>

    @Query(ALL_SALE_LIST)
    abstract fun findAllActiveAsSaleListDetailList(): List<SaleListDetail>

    @Query(ALL_SALE_LIST)
    abstract suspend fun findAllActiveAsSaleListDetailAsync(): List<SaleListDetail>

    @Query("$ALL_SALE_LIST AND salePreOrder = 1")
    abstract fun findAllActiveSaleListDetailPreOrdersLive(): DoorLiveData<List<SaleListDetail>>

    @Query("$ALL_SALE_LIST AND salePaymentDone = 1")
    abstract fun findAllActiveSaleListDetailPaymentDueLive(): DoorLiveData<List<SaleListDetail>>

    @Query(ALL_SALE_LIST)
    abstract fun findAllActiveAsSaleListDetailProvider(): DataSource.Factory<Int,SaleListDetail>

    @Query(ALL_SALE_LIST + FILTER_PREORDER)
    abstract fun findAllActiveSaleListDetailPreOrdersProvider(): DataSource.Factory<Int,SaleListDetail>

    //WE filter
    @Query(ALL_SALE_LIST_WE_FILTER)
    abstract fun findAllSalesWithWEFilter(weUid:Long):DataSource.Factory<Int, SaleListDetail>

    //Payments due shows the payment amount pending vs the total amount of the sale.
    @Query(ALL_SALE_LIST + FILTER_PAYMENT_DUE)
    abstract fun findAllActiveSaleListDetailPaymentDueProvider(): DataSource.Factory<Int,SaleListDetail>

    @Query(ALL_SALE_LIST + SORT_NAME_ASC)
    abstract fun findAllSaleFilterAllSortNameAscProvider(): DataSource.Factory<Int,SaleListDetail>

    @Query(ALL_SALE_LIST + SORT_NAME_DEC)
    abstract fun findAllSaleFilterAllSortNameDescProvider(): DataSource.Factory<Int,SaleListDetail>

    @Query(ALL_SALE_LIST + SORT_TOTAL_AMOUNT_DESC)
    abstract fun findAllSaleFilterAllSortTotalAscProvider(): DataSource.Factory<Int,SaleListDetail>

    @Query(ALL_SALE_LIST + SORT_TOTAL_AMOUNT_ASC)
    abstract fun findAllSaleFilterAllSortTotalDescProvider(): DataSource.Factory<Int,SaleListDetail>

    @Query(ALL_SALE_LIST + SORT_ORDER_DATE_DESC)
    abstract fun findAllSaleFilterAllSortDateAscProvider(): DataSource.Factory<Int,SaleListDetail>

    @Query(ALL_SALE_LIST + SORT_ORDER_DATE_ASC)
    abstract fun findAllSaleFilterAllSortDateDescProvider(): DataSource.Factory<Int,SaleListDetail>

    @Query(ALL_SALE_LIST + FILTER_PREORDER + SORT_NAME_ASC)
    abstract fun findAllSaleFilterPreOrderSortNameAscProvider(): DataSource.Factory<Int,SaleListDetail>

    @Query(ALL_SALE_LIST + FILTER_PREORDER + SORT_NAME_DEC)
    abstract fun findAllSaleFilterPreOrderSortNameDescProvider(): DataSource.Factory<Int,SaleListDetail>

    @Query(ALL_SALE_LIST + FILTER_PREORDER + SORT_TOTAL_AMOUNT_DESC)
    abstract fun findAllSaleFilterPreOrderSortTotalAscProvider(): DataSource.Factory<Int,SaleListDetail>

    @Query(ALL_SALE_LIST + FILTER_PREORDER + SORT_TOTAL_AMOUNT_ASC)
    abstract fun findAllSaleFilterPreOrderSortTotalDescProvider(): DataSource.Factory<Int,SaleListDetail>

    @Query(ALL_SALE_LIST + FILTER_PREORDER + SORT_ORDER_DATE_DESC)
    abstract fun findAllSaleFilterPreOrderSortDateAscProvider(): DataSource.Factory<Int,SaleListDetail>

    @Query(ALL_SALE_LIST + FILTER_PREORDER + SORT_ORDER_DATE_ASC)
    abstract fun findAllSaleFilterPreOrderSortDateDescProvider(): DataSource.Factory<Int,SaleListDetail>


    fun filterAndSortSale(filter: Int, sort: Int): DataSource.Factory<Int,SaleListDetail> {

        when (filter) {
            ALL_SELECTED -> when (sort) {
                SORT_ORDER_NAME_ASC -> return findAllSaleFilterAllSortNameAscProvider()
                SORT_ORDER_NAME_DESC -> return findAllSaleFilterAllSortNameDescProvider()
                SORT_ORDER_AMOUNT_ASC -> return findAllSaleFilterAllSortTotalAscProvider()
                SORT_ORDER_AMOUNT_DESC -> return findAllSaleFilterAllSortTotalDescProvider()
                SORT_ORDER_DATE_CREATED_DESC -> return findAllSaleFilterAllSortDateAscProvider()
                SORT_ORDER_DATE_CREATED_ASC -> return findAllSaleFilterAllSortDateDescProvider()
            }
            PREORDER_SELECTED -> when (sort) {
                SORT_ORDER_NAME_ASC -> return findAllSaleFilterPreOrderSortNameAscProvider()
                SORT_ORDER_NAME_DESC -> return findAllSaleFilterPreOrderSortNameDescProvider()
                SORT_ORDER_AMOUNT_ASC -> return findAllSaleFilterPreOrderSortTotalAscProvider()
                SORT_ORDER_AMOUNT_DESC -> return findAllSaleFilterPreOrderSortTotalDescProvider()
                SORT_ORDER_DATE_CREATED_DESC -> return findAllSaleFilterPreOrderSortDateAscProvider()
                SORT_ORDER_DATE_CREATED_ASC -> return findAllSaleFilterPreOrderSortDateDescProvider()
            }
            PAYMENT_SELECTED -> {
            }
        }
        return findAllActiveAsSaleListDetailProvider()
    }

    fun filterAndSortSale(filter: Int, search: String, sort: Int): DataSource.Factory<Int,SaleListDetail> {

        when (filter) {
            ALL_SELECTED -> when (sort) {
                SORT_ORDER_NAME_ASC -> return findAllSaleFilterAllSortNameAscProvider()
                SORT_ORDER_NAME_DESC -> return findAllSaleFilterAllSortNameDescProvider()
                SORT_ORDER_AMOUNT_ASC -> return findAllSaleFilterAllSortTotalAscProvider()
                SORT_ORDER_AMOUNT_DESC -> return findAllSaleFilterAllSortTotalDescProvider()
                SORT_ORDER_DATE_CREATED_DESC -> return findAllSaleFilterAllSortDateAscProvider()
                SORT_ORDER_DATE_CREATED_ASC -> return findAllSaleFilterAllSortDateDescProvider()
            }
            PREORDER_SELECTED -> when (sort) {
                SORT_ORDER_NAME_ASC -> return findAllSaleFilterPreOrderSortNameAscProvider()
                SORT_ORDER_NAME_DESC -> return findAllSaleFilterPreOrderSortNameDescProvider()
                SORT_ORDER_AMOUNT_ASC -> return findAllSaleFilterPreOrderSortTotalAscProvider()
                SORT_ORDER_AMOUNT_DESC -> return findAllSaleFilterPreOrderSortTotalDescProvider()
                SORT_ORDER_DATE_CREATED_DESC -> return findAllSaleFilterPreOrderSortDateAscProvider()
                SORT_ORDER_DATE_CREATED_ASC -> return findAllSaleFilterPreOrderSortDateDescProvider()
            }
            PAYMENT_SELECTED -> {
            }
        }
        return findAllActiveAsSaleListDetailProvider()
    }

    @Query(ALL_SALE_LIST + SEARCH_BY_QUERY)
    abstract fun findAllSaleItemsWithSearchFilter(locationuid: Long,
                                                  amountl: Long, amounth: Long, from: Long, to: Long, title: String): DataSource.Factory<Int,SaleListDetail>

    @Query(ALL_SALE_LIST + SEARCH_BY_QUERY + FILTER_ORDER_BY_DATE_ASC)
    abstract fun findAllSaleItemsWithSearchFilterOrderDateAsc(locationuid: Long,
                                                              amountl: Long, amounth: Long, from: Long, to: Long, title: String): DataSource.Factory<Int,SaleListDetail>

    @Query(ALL_SALE_LIST + SEARCH_BY_QUERY + FILTER_ORDER_BY_PRICE_ASC)
    abstract fun findAllSaleItemsWithSearchFilterOrderPriceAsc(locationuid: Long,
                                                               amountl: Long, amounth: Long, from: Long, to: Long, title: String): DataSource.Factory<Int,SaleListDetail>

    @Query(ALL_SALE_LIST + SEARCH_BY_QUERY + FILTER_ORDER_BY_PRICE_DESC)
    abstract fun findAllSaleItemsWithSearchFilterOrderPriceDesc(locationuid: Long,
                                                                amountl: Long, amounth: Long, from: Long, to: Long, title: String): DataSource.Factory<Int,SaleListDetail>


    fun findAllSaleFilterAndSearchProvider(locationUid: Long,
                                           spl: Long, sph: Long, from: Long, to: Long, searchQuery: String, sort: Int): DataSource.Factory<Int,SaleListDetail> {

        when (sort) {
            SORT_MOST_RECENT -> return findAllSaleItemsWithSearchFilterOrderDateAsc(
                    locationUid, spl, sph, from, to, searchQuery)
            SORT_LOWEST_PRICE -> return findAllSaleItemsWithSearchFilterOrderPriceAsc(
                    locationUid, spl, sph, from, to, searchQuery)
            SORT_HIGHEST_PRICE -> return findAllSaleItemsWithSearchFilterOrderPriceDesc(
                    locationUid, spl, sph, from, to, searchQuery)
            else -> return findAllSaleItemsWithSearchFilter(locationUid,
                    spl, sph, from, to, searchQuery)
        }

    }

    @Query(FIND_BY_UID_QUERY)
    abstract fun findByUid(saleUid: Long): Sale?

    @Query(FIND_BY_UID_QUERY)
    abstract suspend fun findByUidAsync(saleUid: Long):Sale?

    @Query(FIND_BY_UID_QUERY)
    abstract fun findByUidLive(saleUid: Long): DoorLiveData<Sale?>

    @Query(INACTIVATE_SALE_QUERY)
    abstract fun inactivateEntity(saleUid: Long)

    @Query(INACTIVATE_SALE_QUERY)
    abstract suspend fun inactivateEntityAsync(saleUid: Long): Int


    //UPDATE:

    @Update
    abstract suspend fun updateAsync(entity: Sale):Int


    //Get overdue sale count
    @Query("select count(*) from sale where Sale.saleDueDate < :today and Sale.saleDueDate > 0 AND Sale.saleActive = 1")
    abstract suspend fun getOverDueSaleCountAsync(today: Long): Int


    @Query(" SELECT COUNT(*) FROM (SELECT (select (case  when  " +
            " (SELECT count(*) from SaleItem sip where sip.saleItemSaleUid = sl.saleUid " +
            " and sip.saleItemPreOrder = 1 ) > 0 then 1  else 0 end) from Sale)  as saleItemPreOrder " +
            " FROM Sale sl WHERE sl.saleActive = 1  AND (saleItemPreOrder = 1 OR salePreOrder = 1)) ")
    abstract fun getPreOrderSaleCountLive(): DoorLiveData<Int>


    //REPORTING:

    @Query(SALE_PERFORMANCE_REPORT_1)
    abstract suspend fun getSalesPerformanceReportSumGroupedByLocation(leUids: List<Long> ,
                                                               producerUids:List<Long>, locationUids:List<Long> ,
                                                               productTypeUids:List<Long> , fromDate:Long, toDate:Long,
                                                               fromPrice:Int, toPrice:Int): List<ReportSalesPerformance>

    @Query("SELECT    " +
            " SUM(SaleItem.saleItemQuantity*SaleItem.saleItemPricePerPiece) as totalSalesValue,  " +
            "    LE.firstNames||' '||LE.lastName as leName,   " +
            "   '' as lastActiveOnApp, " +
            "   '' as leRank, " +
            "   LE.personUid as leUid " +
            " FROM SALE    LEFT JOIN SaleItem ON SaleItem.saleItemSaleUid = SALE.saleUid   " +
            " LEFT JOIN Person as LE ON Sale.salePersonUid = LE.personUid  WHERE   " +
            " SALE.saleActive = 1    AND SaleItem.saleItemActive = 1   " +
            " GROUP BY leUid " +
            "  ORDER BY    totalSalesValue DESC")
    abstract suspend fun getTopLEs(): List<ReportTopLEs>

    @Query("SELECT   LE.firstNames||' '||LE.lastName as leName, " +
            " (SaleItem.saleItemQuantity*SaleItem.saleItemPricePerPiece) as saleValue,    " +
            "  Sale.saleCreationDate AS saleDate,  " +
            "  SaleProduct.saleProductName as productNames, " +
            "Location.title as locationName " +
            " FROM SALE    LEFT JOIN SaleItem ON SaleItem.saleItemSaleUid = SALE.saleUid   " +
            " LEFT JOIN Location ON Sale.saleLocationUid = Location.locationUid  " +
            " LEFT JOIN SaleProduct ON SaleProduct.saleProductUid = SaleItem.saleItemProductUid " +
            " LEFT JOIN Person as LE ON Sale.salePersonUid = LE.personUid  WHERE   " +
            " SALE.saleActive = 1    AND SaleItem.saleItemActive = 1    " +
            "  ORDER BY    saleDate DESC ")
    abstract suspend fun getSaleLog(): List<ReportSalesLog>


    //My Women Entrepreneurs
    @Query("SELECT " +
            " SUM((SaleItem.saleItemQuantity*SaleItem.saleItemPricePerPiece)-SaleItem.saleItemDiscount) AS totalSale, " +
            " 'Product list goes here' AS topProducts, WE.* " +
            " From SALE LEFT JOIN SaleItem ON SaleItem.saleItemSaleUid = SALE.saleUid " +
            " LEFT JOIN Person as WE ON SaleItem.saleItemProducerUid = WE.personUid " +
            " LEFT JOIN Person as LE ON Sale.salePersonUid = LE.personUid " +
            " WHERE LE.personUid = :weUid AND Sale.saleActive =1  AND WE.active = 1 " +
            " GROUP BY WE.personUid")
    abstract fun getMyWomenEntrepreneurs(weUid :Long):DataSource.Factory<Int, PersonWithSaleInfo>


    companion object {

        const val ALL_SELECTED = 1
        const val PREORDER_SELECTED = 2
        const val PAYMENT_SELECTED = 3

        const val SORT_ORDER_NAME_ASC = 1
        const val SORT_ORDER_NAME_DESC = 2
        const val SORT_ORDER_AMOUNT_ASC = 3
        const val SORT_ORDER_AMOUNT_DESC = 4
        const val SORT_ORDER_DATE_CREATED_DESC = 5
        const val SORT_ORDER_DATE_CREATED_ASC = 6


        const val SORT_MOST_RECENT = 1
        const val SORT_LOWEST_PRICE = 2
        const val SORT_HIGHEST_PRICE = 3


        //FIND ALL ACTIVE

        const val ALL_SALES_QUERY = "SELECT * FROM Sale"

        const val ALL_SALES_ACTIVE_QUERY = "SELECT * FROM Sale WHERE saleActive = 1"

        const val ALL_SALE_LIST_SELECT =
                " SELECT sl.*, " +
                " (SELECT SaleItem.saleItemQuantity " +
                " FROM Sale stg " +
                " LEFT JOIN SaleItem ON SaleItem.saleItemSaleUid = stg.saleUid " +
                " WHERE stg.saleUid = sl.saleUid AND SaleItem.saleItemActive = 1 " +
                " ORDER BY stg.saleCreationDate ASC LIMIT 1 " +
                " )  " +
                " || 'x ' || " +
                " (SELECT SaleProduct.saleProductName " +
                " FROM SaleItem sitg " +
                " LEFT JOIN SaleProduct ON SaleProduct.saleProductUid = sitg.saleItemProductUid " +
                " WHERE sitg.saleItemSaleUid = sl.saleUid AND sitg.saleItemActive = 1 " +
                " ORDER BY sitg.saleItemCreationDate ASC LIMIT 1) " +
                " || " +
                " (select " +
                "  (case  " +
                "   when  " +
                "   (SELECT count(*) from SaleItem sid where sid.saleItemSaleUid = sl.saleUid) > 1 " +
                "   then '...'  " +
                "   else '' " +
                "  end) " +
                " from sale) " +
                " AS saleTitleGen, " +
                " (Select GROUP_CONCAT(SaleProduct.saleProductName)  FROM SaleItem " +
                "   LEFT JOIN SaleProduct ON SaleProduct.saleProductUid = SaleItem.saleItemProductUid " +
                "   WHERE SaleItem.saleItemSaleUid = sl.saleUid) AS saleProductNames," +
                " Location.title AS locationName, " +
                " COALESCE( (SELECT SUM(SaleItem.saleItemPricePerPiece * SaleItem.saleItemQuantity) - " +
                "            SUM(Sale.saleDiscount)  FROM Sale LEFT JOIN SaleItem on SaleItem.saleItemSaleUid = " +
                "            Sale.saleUid WHERE Sale.saleUid = sl.saleUid) ,0 " +
                " ) AS saleAmount, " +
                " (COALESCE( (SELECT SUM(SaleItem.saleItemPricePerPiece * SaleItem.saleItemQuantity) - " +
                "            SUM(Sale.saleDiscount)  FROM Sale LEFT JOIN SaleItem on SaleItem.saleItemSaleUid = " +
                "            Sale.saleUid WHERE Sale.saleUid = sl.saleUid) ,0 " +
                "           ) - COALESCE((SELECT SUM(SalePayment.salePaymentPaidAmount) FROM SalePayment " +
                "               WHERE SalePayment.salePaymentSaleUid = sl.saleUid " +
                "                AND SalePayment.salePaymentDone = 1 AND SalePayment.salePaymentActive = 1) ," +
                "           0)" +
                " ) AS saleAmountDue, " +
                " 'Afs' AS saleCurrency,  " +
                " coalesce(" +
                "    ( " +
                "    SELECT SaleItem.saleItemDueDate FROM SaleItem LEFT JOIN Sale on Sale.saleUid = " +
                "       SaleItem.saleItemSaleUid WHERE SaleItem.saleItemSaleUid = sl.saleUid  " +
                "       AND Sale.saleActive = 1 AND SaleItem.saleItemPreOrder = 1 " +
                "     ORDER BY SaleItem.saleItemDueDate ASC LIMIT 1 " +
                "    ) ,0) AS earliestDueDate, " +
                " (SELECT count(*) FROM SaleItem WHERE SaleItem.saleItemSaleUid = sl.saleUid) AS saleItemCount," +
                " COALESCE((SELECT SUM(SalePayment.salePaymentPaidAmount) FROM SalePayment  " +
                "   WHERE SalePayment.salePaymentSaleUid = sl.saleUid " +
                "   AND SalePayment.salePaymentDone = 1 AND SalePayment.salePaymentActive = 1) ,0) " +
                " AS saleAmountPaid, " +
                " (select (case  when  " +
                "   (SELECT count(*) from SaleItem sip where sip.saleItemSaleUid = sl.saleUid " +
                "       AND sip.saleItemPreOrder = 1 ) > 0  then 1  else 0 end)  from Sale)  " +
                " AS saleItemPreOrder " +
                " FROM Sale sl "

        const val ALL_SALE_LIST_LJ1 =
                " LEFT JOIN Location ON Location.locationUid = sl.saleLocationUid  " +
                " LEFT JOIN SaleItem ON SaleItem.saleItemSaleUid = sl.saleUid "
        const val ALL_SALE_LIST_LJ2 =
                " LEFT JOIN Person as WE ON SaleItem.saleItemProducerUid = WE.personUid " +
                " LEFT JOIN Person as LE ON sl.salePersonUid = LE.personUid "

        const val ALL_SALE_LIST_WHERE = " WHERE sl.saleActive = 1 "
        const val ALL_SALE_LIST_WHERE_WE =" AND WE.personUid = :weUid "


        const val ALL_SALE_LIST = ALL_SALE_LIST_SELECT + ALL_SALE_LIST_LJ1 +  ALL_SALE_LIST_WHERE
        const val ALL_SALE_LIST_WE_FILTER = ALL_SALE_LIST_SELECT + ALL_SALE_LIST_LJ1 +
                ALL_SALE_LIST_LJ2 + ALL_SALE_LIST_WHERE + ALL_SALE_LIST_WHERE_WE
        //filter and sort

        const val FILTER_PREORDER = " AND (saleItemPreOrder = 1 OR salePreOrder = 1)"
        const val FILTER_PAYMENT_DUE = " AND saleAmountPaid < saleAmount "


        const val SORT_NAME_ASC = " ORDER BY sl.saleTitle ASC "
        const val SORT_NAME_DEC = " ORDER BY sl.saleTitle DESC "
        const val SORT_TOTAL_AMOUNT_DESC = " ORDER BY saleAmount DESC "
        const val SORT_TOTAL_AMOUNT_ASC = " ORDER BY saleAmount ASC "
        const val SORT_ORDER_DATE_DESC = " ORDER BY sl.saleCreationDate DESC "
        const val SORT_ORDER_DATE_ASC = " ORDER BY sl.saleCreationDate ASC "

        //Filter queries
        //ALL_SALE_LIST
        const val SEARCH_BY_QUERY = " AND " +
                " sl.saleLocationUid = :locationuid " +
                " AND saleAmount > :amountl AND saleAmount < :amounth " +
                " AND saleProductNames LIKE :title " +
                " OR (sl.saleCreationDate > :from AND sl.saleCreationDate < :to )"
        const private val FILTER_ORDER_BY_DATE_ASC = " ORDER BY sl.saleCreationDate ASC "
        const private val FILTER_ORDER_BY_PRICE_ASC = " ORDER BY saleAmount ASC "
        const private val FILTER_ORDER_BY_PRICE_DESC = " ORDER BY saleAmount DESC "

        //LOOK UP

        const val FIND_BY_UID_QUERY = "SELECT * FROM Sale WHERE saleUid = :saleUid"

        //INACTIVATE:

        const val INACTIVATE_SALE_QUERY = "UPDATE Sale SET saleActive = 0 WHERE saleUid = :saleUid"


        const val SALE_PERFORMANCE_REPORT_SELECT_SALE_AMOUNT_SUM = " SELECT " +
        "   SUM(SaleItem.saleItemQuantity*SaleItem.saleItemPricePerPiece) as saleAmount, ";

        const val SALE_PERFORMANCE_REPORT_SELECT_SALE_AMOUNT_AVERAGE = " SELECT " +
        "   AVERAGE(SaleItem.saleItemQuantity*SaleItem.saleItemPricePerPiece) as saleAmount, ";

        const val SALE_PERFORMANCE_REPORT_SELECT_BIT1 =
        "   Location.title as locationName,  " +
        "   Location.locationUid as locationUid, " +
        "   Sale.saleUid, " +
        "   strftime('%Y-%m-%d', Sale.saleCreationDate/1000, 'unixepoch') AS saleCreationDate, " +
        "   strftime('%W-%Y', Sale.saleCreationDate/1000, 'unixepoch') AS dateGroup, ";
        const val SALE_PERFORMANCE_REPORT_SELECT_DATE_WEEKLY =
        "   strftime('%Y-%m-%d', Sale.saleCreationDate/1000, 'unixepoch', 'weekday 6', '-6 day') AS firstDateOccurence, ";
        //TODO:
        const val SALE_PERFORMANCE_REPORT_SELECT_DATE_MONTHLY =
        "   strftime('%Y-%m-%d', Sale.saleCreationDate/1000, 'unixepoch', 'weekday 6', '-6 day') AS firstDateOccurence, ";
        //TODO:
        const val SALE_PERFORMANCE_REPORT_SELECT_DATE_YEARLY =
        "   strftime('%Y-%m-%d', Sale.saleCreationDate/1000, 'unixepoch', 'weekday 6', '-6 day') AS firstDateOccurence, ";


        const val SALE_PERFORMANCE_REPORT_SELECT_BIT2 =
        "   SaleProduct.saleProductName, " +
        "   SaleItem.saleItemQuantity, " +
        "   WE.firstNames||' '||WE.lastName as producerName, " +
        "   WE.personUid as producerUid, " +
        "   LE.firstNames||' '||LE.lastName as leName, " +
        "   LE.personUid as leUid, " +
        "   ''  AS grantee, " +
        "   (SELECT PP.saleProductName FROM SaleProductParentJoin " +
        "   LEFT JOIN SaleProduct AS PP ON SaleProductParentJoin.saleProductParentJoinParentUid = PP.saleProductUid" +
        "   WHERE SaleProductParentJoin.saleProductParentJoinChildUid = SaleItem.saleItemProductUid) as productTypeName, " +
        "   (SELECT PP.saleProductUid FROM SaleProductParentJoin " +
        "   LEFT JOIN SaleProduct AS PP ON SaleProductParentJoin.saleProductParentJoinParentUid = PP.saleProductUid" +
        "   WHERE SaleProductParentJoin.saleProductParentJoinChildUid = SaleItem.saleItemProductUid) as productTypeUid " +
        " FROM SALE " +
        "   LEFT JOIN SaleItem ON SaleItem.saleItemSaleUid = SALE.saleUid " +
        "   LEFT JOIN Location ON Sale.saleLocationUid = Location.locationUid " +
        "   LEFT JOIN SaleProduct ON SaleProduct.saleProductUid = SaleItem.saleItemProductUid " +
        "   LEFT JOIN Person as WE ON SaleItem.saleItemProducerUid = WE.personUid " +
        "   LEFT JOIN Person as LE ON Sale.salePersonUid = LE.personUid " +
        " WHERE " +
        "   SALE.saleActive = 1 " +
        "   AND SaleItem.saleItemActive = 1 " +
        "   OR leUid in (:leUids) " +
        "   OR producerUid in (:producerUids) " +
        "   OR locationUid in (:locationUids) " +
        "   OR productTypeUid in (:productTypeUids) " +
        "   AND Sale.saleCreationDate > :fromDate " +
        "   AND Sale.saleCreationDate < :toDate " ;
        const val SALE_PERFORMANCE_REPORT_GROUP_BY_LOCATION =
        " GROUP BY locationName, firstDateOccurence " ;
        const val SALE_PERFORMANCE_REPORT_GROUP_BY_PRODUCT_TYPE =
        " GROUP BY productType, firstDateOccurence " ;
        const val SALE_PERFORMANCE_REPORT_GROUP_BY_GRANTEE =
        " GROUP BY grantee, firstDateOccurence " ;
        const val SALE_PERFORMANCE_REPORT_HAVING_BIT =
        "   HAVING saleAmount > :fromPrice " +
        "   AND saleAmount < :toPrice ";
        const val SALE_PERFORMANCE_REPORT_ORDER_BY_SALE_CREATION_DESC =
        " ORDER BY " +
        "   firstDateOccurence ASC ";

        const val SALE_PERFORMANCE_REPORT_1 =
        SALE_PERFORMANCE_REPORT_SELECT_SALE_AMOUNT_SUM + SALE_PERFORMANCE_REPORT_SELECT_BIT1 +
        SALE_PERFORMANCE_REPORT_SELECT_DATE_WEEKLY + SALE_PERFORMANCE_REPORT_SELECT_BIT2 +
        SALE_PERFORMANCE_REPORT_GROUP_BY_LOCATION + SALE_PERFORMANCE_REPORT_HAVING_BIT +
        SALE_PERFORMANCE_REPORT_ORDER_BY_SALE_CREATION_DESC;
    }


}
