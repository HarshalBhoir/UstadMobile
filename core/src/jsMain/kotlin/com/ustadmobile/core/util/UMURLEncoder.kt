package com.ustadmobile.core.util

/**
 * External JS function to encode URI
 */
external fun encodeURI(uri: String?): String

/**
 * External JS function to decode URI
 */
external fun decodeURI(uri: String?): String

actual class UMURLEncoder {

    actual companion object {
        /**
         * Encode url string
         */
        actual fun encodeUTF8(text: String): String {
            return encodeURI(text)
        }

        /**
         * Decode url string
         */
        actual fun decodeUTF8(text: String): String {
            return decodeURI(text)
        }

    }

}