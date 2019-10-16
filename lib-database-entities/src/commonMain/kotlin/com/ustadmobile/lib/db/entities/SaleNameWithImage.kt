package com.ustadmobile.lib.db.entities

import kotlinx.serialization.Serializable

@Serializable
class SaleNameWithImage() {

    var name: String? = null
    var description: String? = null
    var pictureUid: Long = 0
    var type: Int = 0
    var productUid: Long = 0
    var productGroupUid: Long = 0
}