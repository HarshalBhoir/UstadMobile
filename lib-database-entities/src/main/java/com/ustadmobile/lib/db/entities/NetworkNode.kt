package com.ustadmobile.lib.db.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.ustadmobile.lib.database.annotation.UmEntity
import com.ustadmobile.lib.database.annotation.UmIndexField
import com.ustadmobile.lib.database.annotation.UmPrimaryKey

/**
 * Created by mike on 1/29/18.
 */
@UmEntity
@Entity
class NetworkNode {

    @UmPrimaryKey(autoIncrement = true)
    @PrimaryKey(autoGenerate = true)
    var nodeId: Long = 0

    /**
     * Method which is used to get NetworkNode's Bluetooth address
     * @return String: Device bluetooth address.
     */
    /**
     * Method which is used to set NetworkNode's Bluetooth address
     * @param bluetoothMacAddress Device bluetooth address
     */
    @UmIndexField
    var bluetoothMacAddress: String? = null

    /**
     * Method which is used to get NetworkNode's IP address
     * @return String: Device IP address
     */
    /**
     * Method which is used to set NetworkNode's IP address
     * @param ipAddress Device IP address
     */
    var ipAddress: String? = null

    /**
     * Method which is used to get NetworkNode's MAC address
     * @return String: Device MAC address
     */
    /**
     * Method which is used to set NetworkNode's MAC address
     * @param wifiDirectMacAddress Device MAC address
     */
    var wifiDirectMacAddress: String? = null

    /**
     * The name of the device as specified by WiFi direct (normally the same as it's bluetooth name)
     *
     * @return Name of the device as specified by WiFi direct
     */
    /**
     * The name of the device as specified by WiFi direct (normally the same as it's bluetooth name)
     *
     * @param deviceWifiDirectName Name of the device as specified by WiFi direct
     */
    var deviceWifiDirectName: String? = null

    var endpointUrl: String? = null

    /**
     * Method which used to get last node update time by Wi-Fi Direct service.
     * @return long: Time in milliseconds
     */
    /**
     * Method which used to set last node update time by Wi-Fi Direct service.
     */
    @UmIndexField
    var lastUpdateTimeStamp: Long = 0

    /**
     * Method which used to get last node update time by local network service.
     * @return long: Time in milliseconds
     */
    /**
     * Method which is responsible to set time when this node was last updated
     * by local network service
     * @param networkServiceLastUpdated time in milliseconds
     */
    var networkServiceLastUpdated: Long = 0

    /**
     * The name of the nsd service as it was discovered. Normally the bluetooth name of the device.
     *
     * @return Nsd service name if present, null if not available.
     */
    /**
     * The name of the nsd service as it was discovered. Normally the bluetooth name of the device.
     *
     * @param nsdServiceName Nsd service name as above.
     */
    var nsdServiceName: String? = null

    /**
     * Method which is used to get the HTTP service port.
     * @return int: HTTP Service port
     */
    /**
     * Method which is used to set HTTP service port
     * @param port
     */
    var port: Int = 0

    var numFailureCount: Int = 0

    var wifiDirectDeviceStatus: Int = 0

    var groupSsid: String? = null

    /**
     *
     * @return
     */
    val timeSinceWifiDirectLastUpdated: Long
        get() = System.currentTimeMillis() - lastUpdateTimeStamp

    val timeSinceNetworkServiceLastUpdated: Long
        get() = System.currentTimeMillis() - networkServiceLastUpdated


    /**
     * List of acquisition operations that have been performed from this node - used by the
     * acquisition task to determine how successful a node has been and avoid nodes that frequently
     * fail
     */
    //    private List<AcquisitionTaskHistoryEntry> acquisitionTaskHistory;

    /**
     * Creating a NetworkNode
     * @param wifiDirectMacAddress Device Wi-Fi MAC address
     * @param ipAddress Device IP address
     */
    constructor(wifiDirectMacAddress: String, ipAddress: String) {
        this.wifiDirectMacAddress = wifiDirectMacAddress
        this.ipAddress = ipAddress
    }

    constructor()


    /**
     * Method which is responsible to set time when this node was last updated
     * @param lastUpdateTimeStamp Update timestamp
     */
    fun setNetworkNodeLastUpdated(lastUpdateTimeStamp: Long) {
        this.lastUpdateTimeStamp = lastUpdateTimeStamp
    }

    override fun equals(other: Any?): Boolean {
        return other is NetworkNode && (wifiDirectMacAddress != null && wifiDirectMacAddress == wifiDirectMacAddress || ipAddress != null && ipAddress == ipAddress)
    }

    companion object {

        const val STATUS_CONNECTED = 0

        const val STATUS_INVITED = 1

        const val STATUS_FAILED = 2

        const val STATUS_AVAILABLE = 3

        const val STATUS_UNAVAILABLE = 4


        /**
         * The timeout after which if we have heard nothing we consider a wifi direct node inactive.
         * Normally we should hear from the node every 2min.
         */
        const val WIFI_DIRECT_TIMEOUT = 6 * 60 * 1000 + 30000
    }
}
