package com.ustadmobile.port.sharedse.networkmanager

import com.ustadmobile.core.util.UMIOUtils
import com.ustadmobile.port.sharedse.networkmanager.NetworkManagerBle.Companion.DEFAULT_MTU_SIZE
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * This class converts bytes
 *
 *
 *
 *
 *
 * Use [BleMessage.BleMessage] to receive message
 * sent by peer device.
 *
 *
 * Use [BleMessage.getPackets] to get the packets to be sent
 * to the peer device.
 *
 *
 * Bluetooth Low Energy can use a packet size between 20 and 512 bytes. BleMessage will packetize
 * a payload accordingly, and use Gzip compression if that reduces the size.
 *
 *
 * **Packet Structure**
 *
 *
 * Byte 0: Request code (8 bit integer value between 0 and 255)
 * Byte 1-3: Maximum Transfer Unit (MTU)
 * Byte 4-7: Payload length
 * Byte 8-onwards: Payload
 *
 *
 * Use [BleMessage.getPayload] to get the actual payload sent from the peer device
 *
 *
 * Use [BleMessage.getLength] to get the payload length
 *
 *
 * Use [BleMessage.getRequestType] to get request type
 *
 *
 * Use [BleMessage.getMtu] to get the MTU used to send th packets
 *
 * @author kileha3
 */
class BleMessage {

    /**
     * Get received message payload
     * @return Message payload in byte array.
     */
    var payload: ByteArray? = null
        private set

    /**
     * Get request type from the received message payload.
     * @return Request status as byte
     */
    var requestType: Byte = 0
        private set

    /**
     * Get Maximum Transfer Unit from message packets
     * @return MTU value
     */
    var mtu: Int = 0
        private set

    /**
     * Get received length from message payload
     * @return Length in integer
     */
    var length: Int = 0
        private set

    /**
     *
     * @return unique identifier of the message under process.
     */
    var messageId: Byte = 0
        private set


    private var packetReceiveBuffer: Array<ByteArray>? = null

    private var onPacketReceivedCount = 0


    /**
     * Constructor which will be used when receiving packets
     */
    constructor() {}


    /**
     * Constructor which will be used when sending the message
     * @param requestType Type of the request that will be contained.
     * @param payload The actual payload to be sent
     */
    constructor(requestType: Byte, messageId: Byte, payload: ByteArray) {
        this.requestType = requestType
        this.payload = payload
        this.length = payload.size
        this.messageId = messageId
    }


    /**
     * Constructor which will be used when receiving the message
     * @param packetsReceived Received packets
     */
    constructor(packetsReceived: Array<ByteArray>) {
        constructFromPackets(packetsReceived)
    }

    private fun constructFromPackets(packetsReceived: Array<ByteArray>) {
        messageId = packetsReceived[0][0]
        val messageBytes = depacketizePayload(packetsReceived)
        assignHeaderValuesFromFirstPacket(packetsReceived[0])

        val receivedPayload = ByteArray(length)
        System.arraycopy(messageBytes, HEADER_SIZE, receivedPayload, 0, length)

        val isCompressed = (receivedPayload.size > 0 &&
                receivedPayload[0] == GZIPInputStream.GZIP_MAGIC.toByte()
                && receivedPayload[1] == (GZIPInputStream.GZIP_MAGIC shr 8).toByte())
        this.payload = receivedPayload
        if (isCompressed) {
            this.payload = decompressPayload(receivedPayload)
        }
    }

    private fun assignHeaderValuesFromFirstPacket(packet: ByteArray) {
        messageId = packet[0]
        requestType = packet[1]
        mtu = ByteBuffer.wrap(byteArrayOf(packet[2], packet[3])).short.toInt()
        length = ByteBuffer.wrap(Arrays.copyOfRange(packet, payloadLengthStartIndex + 1,
                payLoadStartIndex + 1)).int
    }


    /**
     * Get constructed payload packets to be transferred
     * @param mtu Packets maximum transfer unit
     * @return Constructed packets in byte arrays
     */
    fun getPackets(mtu: Int): Array<ByteArray> {
        this.mtu = mtu
        val compressedPayload = compressPayload(this.payload)

        return if (compressedPayload!!.size < this.payload!!.size) {
            packetizePayload(compressedPayload)
        } else {
            packetizePayload(this.payload!!)
        }

    }

    /**
     * Internal message to compress message payload
     * @param payload payload to be compressed
     * @return Compressed payload in byte array, null if something goes wrong
     */
    private fun compressPayload(payload: ByteArray?): ByteArray? {
        try {
            ByteArrayOutputStream().use { bos ->
                GZIPOutputStream(bos).use { gos ->
                    gos.write(payload!!)
                    gos.flush()
                    gos.close()
                    return bos.toByteArray()
                }
            }
        } catch (e: IOException) {
            //Very unlikely as we are reading from memory into memory
            e.printStackTrace()
        }

        return null
    }

    /**
     * Internal method for decompressing compressed payload
     * @param receivedPayload Compressed payload
     * @return Decompressed payload in byte array.
     */
    private fun decompressPayload(receivedPayload: ByteArray): ByteArray {
        val BUFFER_SIZE = Math.min(32, receivedPayload.size)
        val `is` = ByteArrayInputStream(receivedPayload)
        val bout = ByteArrayOutputStream()
        try {
            val gis = GZIPInputStream(`is`, BUFFER_SIZE)
            val data = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (gis.read(data).also { bytesRead = it } != -1) {
                bout.write(data, 0, bytesRead)
            }
            bout.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            UMIOUtils.closeOutputStream(bout)
        }
        return bout.toByteArray()
    }


    private fun calculateNumPackets(payloadLength: Int, mtu: Int): Int {
        return Math.ceil((payloadLength + HEADER_SIZE) / (mtu - 1).toDouble()).toInt()
    }

    /**
     * Internal method for constructing payload packets from the message payload
     * @param payload Message payload to be packetized
     * @return Constructed payload packets in byte arrays
     */
    private fun packetizePayload(payload: ByteArray): Array<ByteArray> {
        if (payload.size == 0) {
            throw IllegalArgumentException()
        } else {
            val numPackets = calculateNumPackets(payload.size, mtu)
            val headerBuffer = ByteBuffer.allocate(HEADER_SIZE)
            val header = headerBuffer
                    .put(requestType)
                    .putShort(mtu.toShort())
                    .putInt(payload.size).array()
            val outputStream = ByteArrayOutputStream()
            try {
                outputStream.write(header)
                outputStream.write(payload)
            } catch (e: IOException) {
                e.printStackTrace()
                return arrayOf()
            } finally {
                UMIOUtils.closeOutputStream(outputStream)
            }
            val totalPayLoad = outputStream.toByteArray()
            val packets = Array(numPackets) { ByteArray(mtu) }
            for (i in packets.indices) {
                packets[i][0] = messageId
                val payloadPos = i * (mtu - 1)
                System.arraycopy(totalPayLoad, payloadPos, packets[i], 1,
                        Math.min(mtu - 1, totalPayLoad.size - payloadPos))
            }

            return packets
        }
    }

    /**
     * Internal method for reconstructing the payload from packets. Strips out the mesage id
     *
     * @param packets Payload packets to be depacketized
     *
     * @return Constructed payload in byte array
     */
    //TODO: Throw an exception if any packet has a different message id
    private fun depacketizePayload(packets: Array<ByteArray>): ByteArray {
        if (packets.size == 0) {
            throw NullPointerException()
        } else {
            val outputStream = ByteArrayOutputStream()
            for (packetContent in packets) {
                try {
                    outputStream.write(packetContent, 1, packetContent.size - 1)
                } finally {
                    UMIOUtils.closeOutputStream(outputStream)
                }
            }
            return outputStream.toByteArray()
        }
    }


    /**
     * Called when packet is received from the other peer for assembling
     * @param packet packet received from the other peer
     * @return True if the packets are all received else False.
     */
    fun onPackageReceived(packet: ByteArray): Boolean {
        if (onPacketReceivedCount == 0) {
            assignHeaderValuesFromFirstPacket(packet)
            packetReceiveBuffer = Array(calculateNumPackets(length, mtu)) { ByteArray(mtu) }
        }

        if (onPacketReceivedCount < packetReceiveBuffer!!.size) {
            packetReceiveBuffer!![onPacketReceivedCount++] = packet
        }

        if (onPacketReceivedCount == packetReceiveBuffer!!.size) {
            constructFromPackets(packetReceiveBuffer!!)
            return true
        } else {
            return false
        }
    }


    /**
     * Reset a message
     */
    fun reset() {
        requestType = 0
        length = 0
        mtu = DEFAULT_MTU_SIZE
        payload = byteArrayOf()
    }

    companion object {

        private val payloadLengthStartIndex = 3

        private val payLoadStartIndex = 7

        val HEADER_SIZE = 1 + 2 + 4//Request type (byte - 1byte), mtu (short - 2 byts), length (int - 4bytes)

        private val messageIds = Hashtable<String, Byte>()

        /**
         * Find message ID from received packets
         * @param packet received packets
         * @return message identifier
         */
        fun findMessageId(packet: ByteArray): Byte {
            return packet[0]
        }

        /**
         * Generate next message identifier for the node
         * @param receiverAddr bluetooth address
         * @return unique message identifier
         */
        fun getNextMessageIdForReceiver(receiverAddr: String): Byte {
            val lastMessageId = messageIds[receiverAddr] ?: (-128).toByte()
            val nextMessageId: Byte
            if (lastMessageId == 127.toByte()) {
                nextMessageId = (-128).toByte()
            } else {
                nextMessageId = (lastMessageId + 1).toByte()
            }

            messageIds[receiverAddr] = nextMessageId

            return nextMessageId
        }
    }
}