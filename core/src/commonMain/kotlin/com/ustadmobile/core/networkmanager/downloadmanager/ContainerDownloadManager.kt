package com.ustadmobile.core.networkmanager.downloadmanager

import com.ustadmobile.core.db.JobStatus
import com.ustadmobile.door.DoorLiveData
import com.ustadmobile.lib.db.entities.*

abstract class ContainerDownloadManager {

    abstract suspend fun getDownloadJobItemByJobItemUid(jobItemUid: Int): DoorLiveData<DownloadJobItem?>

    abstract suspend fun getDownloadJobItemByContentEntryUid(contentEntryUid: Long): DoorLiveData<DownloadJobItem?>

    abstract suspend fun getDownloadJob(jobUid: Int): DoorLiveData<DownloadJob?>

    abstract suspend fun createDownloadJob(downloadJob: DownloadJob)

    abstract suspend fun addItemsToDownloadJob(newItems: List<DownloadJobItemWithParents>)

    abstract suspend fun handleDownloadJobItemUpdated(downloadJobItem: DownloadJobItem)

    abstract suspend fun enqueue(downloadJobId: Int)

    abstract suspend fun pause(downloadJobId: Int)

    abstract suspend fun cancel(downloadJobId: Int)

    abstract suspend fun setMeteredDataAllowed(downloadJobUid: Int, meteredDataAllowed: Boolean)

    abstract suspend fun handleConnectivityChanged(status: ConnectivityStatus)

    fun determineParentStatusFromChildStatuses(childStatuses: List<DownloadJobItemUidAndStatus>): Int {
        return when {
            childStatuses.all { it.djiStatus > JobStatus.COMPLETE } -> {
                childStatuses.maxBy { it.djiStatus }?.djiStatus ?: JobStatus.FAILED
            }
            childStatuses.any { it.djiStatus == JobStatus.RUNNING } -> JobStatus.RUNNING
            childStatuses.any { it.djiStatus == JobStatus.QUEUED } -> JobStatus.QUEUED
            else -> childStatuses.minBy { it.djiStatus }?.djiStatus ?: 0
        }
    }

}