package com.ustadmobile.core.controller

import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.util.UMCalendarUtil
import com.ustadmobile.core.view.SaleDeliveryDetailView
import com.ustadmobile.core.view.SaleDeliveryDetailView.Companion.ARG_SALE_DELIVERY_SALE_UID
import com.ustadmobile.core.view.SaleDeliveryDetailView.Companion.ARG_SALE_DELIVERY_UID
import com.ustadmobile.lib.db.entities.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch

/**
 *  Presenter for SaleDeliveryDetail view
 **/
class SaleDeliveryDetailPresenter(context: Any,
                                  arguments: Map<String, String>?,
                                  view: SaleDeliveryDetailView,
                                  val systemImpl: UstadMobileSystemImpl = UstadMobileSystemImpl.instance,
                                  private val repository: UmAppDatabase =
                                          UmAccountManager.getRepositoryForActiveAccount(context))
    : CommonInventorySelectionPresenter<SaleDeliveryDetailView>(context, arguments!!, view) {

    private var saleUid : Long = 0
    private var saleDeliveryUid : Long = 0
    private lateinit var saleDelivery: SaleDelivery
    private var currentSignSvg: String? = null
    private var sale : Sale? = null

    val database = UmAccountManager.getActiveDatabase(context)
    internal var saleItemToWeCounter: HashMap<Long, HashMap<Long, Int>> = HashMap()
    private var saleItemToProducerSelection : HashMap<SaleItemListDetail, List<PersonWithInventory>>
    var saleItemToSaleItemListDetail : HashMap<Long, SaleItemListDetail>

    private val inventoryTransactionDB = database.inventoryTransactionDao
    private val inventoryTransactionDao = repository.inventoryTransactionDao
    private val inventoryItemDB = database.inventoryItemDao
    private val saleDeliveryDB = database.saleDeliveryDao
    private val saleItemDB = database.saleItemDao
    private val saleItemDao = repository.saleItemDao
    private val saleDaoDB = database.saleDao
    val saleDao = repository.saleDao
    private val saleDeliveryDao = repository.saleDeliveryDao
    var loggedInPersonUid : Long = 0


    init {
        //Initialise Daos, etc here.
        saleItemToProducerSelection = HashMap()
        saleItemToSaleItemListDetail = HashMap()
        loggedInPersonUid = UmAccountManager.getActivePersonUid(context)
    }


    override fun getWeCountMap(): HashMap<Long, HashMap<Long, Int>> {
        return saleItemToWeCounter
    }


    override fun onCreate(savedState: Map<String, String?>?) {
        super.onCreate(savedState)

        deliveryMode = true

        if(arguments.containsKey(ARG_SALE_DELIVERY_SALE_UID)){
            saleUid = arguments[ARG_SALE_DELIVERY_SALE_UID].toString().toLong()

            GlobalScope.launch {
                sale = saleDaoDB.findByUidAsync(saleUid)
            }
        }

        if(arguments.containsKey(ARG_SALE_DELIVERY_UID)){
            saleDeliveryUid = arguments[ARG_SALE_DELIVERY_UID].toString().toLong()

            GlobalScope.launch {
                saleDelivery = saleDeliveryDB.findByUidAsync(saleDeliveryUid)!!
                newDelivery = false
                initFromSaleDelivery()
            }
        }else {
            saleDelivery = SaleDelivery()
            saleDelivery.saleDeliverySaleUid = saleUid
            saleDelivery.saleDeliveryActive = false
            saleDelivery.saleDeliveryDate = UMCalendarUtil.getDateInMilliPlusDays(0)

            GlobalScope.launch {
                saleDelivery.saleDeliveryUid = saleDeliveryDao.insertAsync(saleDelivery)
                newDelivery = true
                initFromSaleDelivery()
            }
        }

    }

    private fun initFromSaleDelivery(){
        view.runOnUiThread(Runnable {
            view.updateSaleDeliveryOnView(saleDelivery)
        })

        val loggedInPersonUid = UmAccountManager.getActivePersonUid(context)
        GlobalScope.launch {
            val saleItems = saleItemDB.findAllSaleItemListDetailActiveBySaleList(saleUid)

            //Get all producer selection for this salItem

            saleItemToProducerSelection = HashMap()
            saleItemToSaleItemListDetail = HashMap()

            for (item in saleItems){

                // If SaleItem is a sale, then set max selection to selected

                saleItemPreOrder = item.saleItemPreorder

                //Get producers
                val producers : List<PersonWithInventory>

                val saleProductUid = item.saleItemProductUid
                val saleUid = item.saleItemSaleUid
                val saleItemUid = item.saleItemUid
                producers = inventoryItemDB.findStockBySaleItemAndSale(
                        saleUid, saleItemUid, saleProductUid, loggedInPersonUid, saleDeliveryUid)

                saleItemToProducerSelection.put(item, producers)
                saleItemToSaleItemListDetail.put(item.saleItemUid, item)
            }

            view.runOnUiThread(Runnable {
                view.setUpAllViews(saleItemToProducerSelection)

            })
        }
    }


    override fun updateWeCount(weUid: Long, count: Int, saleItemUid: Long){
        if(saleItemToWeCounter.containsKey(saleItemUid)){
            val existing = saleItemToWeCounter[saleItemUid]
            existing!!.put(weUid, count)
        }else{
            val newMap = HashMap<Long, Int>()
            newMap.put(weUid, count)
            saleItemToWeCounter.put(saleItemUid, newMap)
        }

    }

    fun handleClickAccept(){

        //1. Save the signature
        saveSignature()
        //2. Loop over inventory assignment and create transactions if needed and also assign deliveries

        // a. If its a preorder > create new inventorytransactions
        // b. If it is a sale > use existing transactions


        GlobalScope.launch {
            val dateTime = UMCalendarUtil.getDateInMilliPlusDays(0)
            val date = UMCalendarUtil.getToday000000()
            var allSaleItemDone = true
            for (saleItemUid in saleItemToWeCounter.keys) {

                var ok = false
                val saleItem = saleItemDB.findByUidAsync(saleItemUid)

                val itemDetail = saleItemToSaleItemListDetail.get(saleItemUid)
                val saleItemMap = saleItemToWeCounter.get(saleItemUid)
                var totalSaleItemSelected = 0
                for (weUid in saleItemMap!!.keys) {
                    val weCount = saleItemMap.get(weUid)
                    totalSaleItemSelected += weCount!!
                    if (saleItem!!.saleItemPreorder) {
                        //Create new inventoryTransactions

                        // Get count number of unique InventoryItems and build Transactions for them.
                        val availableItems = inventoryItemDB.findAvailableInventoryItemsByProductLimit(
                                itemDetail!!.saleItemProductUid, weCount, loggedInPersonUid, weUid)
                        if (availableItems.count() != weCount) {
                            //ERROR: We are asking for more than we have.
                            println("Asked for more than required")
                            ok=false
                        }else {
                            ok=true
                            for (item in availableItems) {
                                val newInventoryTransaction = InventoryTransaction(item.inventoryItemUid,
                                        loggedInPersonUid, saleUid, dateTime)
                                newInventoryTransaction.inventoryTransactionDay = date
                                newInventoryTransaction.inventoryTransactionActive = true
                                newInventoryTransaction.inventoryTransactionSaleItemUid = saleItemUid
                                newInventoryTransaction.inventoryTransactionSaleDeliveryUid = saleDelivery.saleDeliveryUid
                                inventoryTransactionDao.insertAsync(newInventoryTransaction)
                            }
                        }

                    } else {
                        //Use existing transactions
                        val transactions = inventoryTransactionDB.findUnDeliveredTransactionsByWeLeSaleUids(saleUid,
                                loggedInPersonUid, weUid, saleItemUid, weCount)
                        if(transactions.size != weCount) {
                            //Something went wrong. Unable to find it back
                            println("Got less than required")
                            ok = false
                        }else{
                            ok = true
                            for (everyTransaction in transactions){
                                everyTransaction.inventoryTransactionSaleDeliveryUid = saleDelivery.saleDeliveryUid
                                inventoryTransactionDao.update(everyTransaction)
                            }
                        }
                    }
                }

                if(ok) {
                    saleItem!!.saleItemPreorder = false
                    saleItemDao.updateAsync(saleItem)
                }else{
                    if(allSaleItemDone) {
                        allSaleItemDone = false
                    }
                }
            }

            if(allSaleItemDone){
                sale!!.salePreOrder = false
                saleDao.updateAsync(sale!!)
            }

            //3. Persist and close
            saleDelivery.saleDeliveryActive = true
            saleDeliveryDao.updateAsync(saleDelivery)
            view.finish()
        }
    }

    fun saveSignature() {
        if (currentSignSvg != null && !currentSignSvg!!.isEmpty()) {
            saleDelivery.saleDeliverySignature = currentSignSvg!!
        }else{
            saleDelivery.saleDeliverySignature = ""
        }
    }

    fun updateSignatureSvg(signSvg: String) {
        currentSignSvg = signSvg
    }




}