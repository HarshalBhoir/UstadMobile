package com.ustadmobile.sharedse.network

import com.ustadmobile.lib.db.entities.NetworkNode
import kotlinx.coroutines.CoroutineDispatcher

actual class NetworkManagerBle actual constructor(context: Any, singleThreadDispatcher: CoroutineDispatcher) : NetworkManagerBleCommon() {
    actual override val isWiFiEnabled: Boolean
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    actual override val isBleCapable: Boolean
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    actual override val isBluetoothEnabled: Boolean
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    actual override val isVersionLollipopOrAbove: Boolean
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    actual override val isVersionKitKatOrBelow: Boolean
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    actual override fun canDeviceAdvertise(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual override fun openBluetoothSettings() {
    }

    actual override fun setWifiEnabled(enabled: Boolean): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual override fun connectToWiFi(ssid: String, passphrase: String, timeout: Int) {
    }

    actual override fun restoreWifi() {
    }

    actual override fun makeEntryStatusTask(context: Any?, entryUidsToCheck: List<Long>, peerToCheck: NetworkNode?): BleEntryStatusTask? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual override fun makeEntryStatusTask(context: Any, message: BleMessage, peerToSendMessageTo: NetworkNode, responseListener: BleMessageResponseListener): BleEntryStatusTask? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual override fun makeDeleteJobTask(`object`: Any?, args: Map<String, String>): DeleteJobTaskRunner {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}