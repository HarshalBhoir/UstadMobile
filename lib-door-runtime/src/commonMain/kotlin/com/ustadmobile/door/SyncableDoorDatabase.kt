package com.ustadmobile.door

import io.ktor.client.HttpClient

interface SyncableDoorDatabase {

    val nodeId: Int

    val master: Boolean

}

expect inline fun <reified  T> SyncableDoorDatabase.asRepository(endpoint: String, accessToken: String, httpClient: HttpClient): T
