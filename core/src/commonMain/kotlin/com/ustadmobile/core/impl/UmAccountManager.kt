package com.ustadmobile.core.impl

import com.ustadmobile.lib.db.entities.UmAccount
import com.ustadmobile.core.db.UmAppDatabase
import kotlin.js.JsName
import kotlin.jvm.Synchronized
import kotlin.jvm.Volatile

object UmAccountManager {

    @Volatile
    private var activeAccount: UmAccount? = null

    private const val PREFKEY_PERSON_ID = "umaccount.personid"

    private const val PREFKEY_USERNAME = "umaccount.username"

    private const val PREFKEY_ACCESS_TOKEN = "umaccount.accesstoken"

    private const val PREFKEY_ENDPOINT_URL = "umaccount.endpointurl"

    private const val PREFKEY_PASSWORD_HASH = "umaccount.passwordhash"

    private const val PREFKEY_FINGERPRINT_USERNAME = "umaccount.fingerprintusername"
    private const val PREFKEY_FINGERPRINT_PERSON_ID = "umaccount.fingerprintpersonid"
    private const val PREFKEY_PASSWORD_HASH_PERSONUID = "umaccount.passwordhashpersonuid"
    const val PREFKEY_PASSWORD_HASH_USERNAME = "umaccount.passwordhashusername"
    private const val PREFKEY_FINGERPRIT_ACCESS_TOKEN = "umaccount.fingerprintaccesstoken"

    @Synchronized
    fun getActiveAccount(context: Any, impl: UstadMobileSystemImpl): UmAccount? {
        if (activeAccount == null) {
            val personUid = impl.getAppPref(PREFKEY_PERSON_ID, "0", context).toLong()
            if (personUid == 0L)
                return null

            activeAccount = UmAccount(personUid, impl.getAppPref(PREFKEY_USERNAME, context),
                    impl.getAppPref(PREFKEY_ACCESS_TOKEN, context),
                    impl.getAppPref(PREFKEY_ENDPOINT_URL, context))
        }

        return activeAccount
    }

    @Synchronized
    fun getActivePersonUid(context: Any, impl: UstadMobileSystemImpl): Long {
        val activeAccount = getActiveAccount(context, impl)
        return activeAccount?.personUid ?: 0L
    }

    @Synchronized
    fun getActivePersonUid(context: Any): Long {
        return getActivePersonUid(context, UstadMobileSystemImpl.instance)
    }

    fun getActiveAccount(context: Any): UmAccount? {
        return getActiveAccount(context, UstadMobileSystemImpl.instance)
    }

    fun updatePasswordHash(password: String?, context: Any, impl: UstadMobileSystemImpl) {
        impl.setAppPref(PREFKEY_PASSWORD_HASH, password, context)
    }

    @Synchronized
    fun setActiveAccount(account: UmAccount?, context: Any,
                         impl: UstadMobileSystemImpl) {
        activeAccount = account
        if (account != null) {
            impl.setAppPref(PREFKEY_PERSON_ID, account.personUid.toString(), context)
            impl.setAppPref(PREFKEY_USERNAME, account.username, context)
            impl.setAppPref(PREFKEY_ACCESS_TOKEN, account.auth, context)
            impl.setAppPref(PREFKEY_ENDPOINT_URL, account.endpointUrl, context)
        } else {
            impl.setAppPref(PREFKEY_PERSON_ID, "0", context)
            impl.setAppPref(PREFKEY_USERNAME, "", context)
            impl.setAppPref(PREFKEY_ACCESS_TOKEN, null, context)
            impl.setAppPref(PREFKEY_ENDPOINT_URL, null, context)
        }
    }

    fun setActiveAccount(account: UmAccount, context: Any) {
        setActiveAccount(account, context, UstadMobileSystemImpl.instance)
    }

    @JsName("getRepositoryForActiveAccount")
    fun getRepositoryForActiveAccount(context: Any): UmAppDatabase {
        if (activeAccount == null)
            /*return UmAppDatabase.getInstance(context).getRepository(
                    UstadMobileSystemImpl.instance.getAppConfigString("apiUrl",
                            "http://localhost", context), "")*/
            return UmAppDatabase.getInstance(context)

        val activeAccount = getActiveAccount(context)
      /*  return UmAppDatabase.getInstance(context).getRepository(activeAccount!!.endpointUrl,
                activeAccount.auth)*/

        return UmAppDatabase.getInstance(context)
    }

    fun getActiveEndpoint(context: Any): String? {
        val activeAccount = getActiveAccount(context)
        return if (activeAccount != null) {
            activeAccount.endpointUrl
        } else {
            UstadMobileSystemImpl.instance.getAppConfigString("apiUrl",
                    "http://localhost", context)
        }
    }

    @Synchronized fun setFingerprintUsername(username:String?, context:Any,
                                             impl:UstadMobileSystemImpl) {
        impl.setAppPref(PREFKEY_FINGERPRINT_USERNAME, username, context)
    }
    @Synchronized fun getFingerprintUsername(context:Any,
                                             impl:UstadMobileSystemImpl):String? {
        return impl.getAppPref(PREFKEY_FINGERPRINT_USERNAME, context)
    }
    @Synchronized fun setFingerprintPersonId(personId:Long, context:Any,
                                             impl:UstadMobileSystemImpl) {
        impl.setAppPref(PREFKEY_FINGERPRINT_PERSON_ID, (personId).toString(), context)
    }
    @Synchronized fun getFingerprintPersonId(context:Any,
                                             impl:UstadMobileSystemImpl):String? {
        return impl.getAppPref(PREFKEY_FINGERPRINT_PERSON_ID, context)
    }
    @Synchronized fun getFingerprintAuth(context:Any,
                                         impl:UstadMobileSystemImpl):String? {
        return impl.getAppPref(PREFKEY_FINGERPRIT_ACCESS_TOKEN, context)
    }
    @Synchronized fun setFringerprintAuth(auth:String?, context:Any,
                                          impl:UstadMobileSystemImpl) {
        impl.setAppPref(PREFKEY_FINGERPRIT_ACCESS_TOKEN, auth, context)
    }
    fun updateCredCache(username:String, personUid:Long, passwordHash:String, context:Any,
                        impl:UstadMobileSystemImpl) {
        impl.setAppPref(PREFKEY_PASSWORD_HASH, passwordHash, context)
        impl.setAppPref(PREFKEY_PASSWORD_HASH_PERSONUID, (personUid).toString(), context)
        impl.setAppPref(PREFKEY_PASSWORD_HASH_USERNAME, username, context)
    }
    fun checkCredCache(username:String, passwordHash:String, context:Any,
                       impl:UstadMobileSystemImpl):Boolean {
        if (username == impl.getAppPref(PREFKEY_PASSWORD_HASH_USERNAME, context))
        {
            if (passwordHash == impl.getAppPref(PREFKEY_PASSWORD_HASH, context))
            {
                return true
            }
        }
        return false
    }
    fun getCachedPersonUid(context:Any, impl:UstadMobileSystemImpl):Long {
        return impl.getAppPref(PREFKEY_PASSWORD_HASH_PERSONUID, context)!!.toLong()
    }

}
