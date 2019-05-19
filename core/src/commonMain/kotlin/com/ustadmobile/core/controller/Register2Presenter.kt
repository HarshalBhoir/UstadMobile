package com.ustadmobile.core.controller

import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.impl.AppConfig
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.view.Register2View
import com.ustadmobile.lib.db.entities.Person
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch

class Register2Presenter(context: Any, arguments: Map<String, String?>, view: Register2View)
    : UstadBaseController<Register2View>(context, arguments, view) {

    private var mNextDest: String? = null

    private var umAppDatabase: UmAppDatabase? = null

    private var repo: UmAppDatabase? = null

    init {
        if (arguments.containsKey(ARG_NEXT)) {
            mNextDest = arguments[ARG_NEXT]
        }
    }

    override fun onCreate(savedState: Map<String, String?>?) {
        super.onCreate(savedState)

        if (arguments.containsKey(ARG_SERVER_URL)) {
            view.setServerUrl(arguments[ARG_SERVER_URL]!!)
        } else {
            view.setServerUrl(UstadMobileSystemImpl.instance.getAppConfigString(
                    AppConfig.KEY_API_URL, "http://localhost", context)!!)
        }
    }

    //only for testing
    fun setClientDb(database: UmAppDatabase) {
        this.umAppDatabase = database
    }

    fun setRepo(repo: UmAppDatabase) {
        this.repo = repo
    }

    /**
     * Registering new user's account
     * @param person Person object to be registered
     * @param password Person password to be associated with the account.
     * @param serverUrl Server url where the account should be created
     */
    fun handleClickRegister(person: Person, password: String, serverUrl: String) {
        view.runOnUiThread(Runnable { view.setInProgress(true) })

        val systemImpl = UstadMobileSystemImpl.instance
        if (umAppDatabase === null) {
            umAppDatabase = UmAppDatabase.getInstance(context).getRepository(serverUrl,
                    "")
        }

        if (repo === null) {
            repo = UmAccountManager.getRepositoryForActiveAccount(context)
        }

        GlobalScope.launch {

            try {
                val result = repo!!.personDao.registerAsync(person, password)
                if (result != null) {
                    person.personUid = result.personUid
                    umAppDatabase!!.personDao.insertAsync(person)
                    result.endpointUrl = serverUrl
                    view.runOnUiThread(Runnable { view.setInProgress(false) })
                    UmAccountManager.setActiveAccount(result, context)
                    systemImpl.go(mNextDest, context)
                }
            } catch (e: Exception) {
                view.runOnUiThread(Runnable {
                    view.setErrorMessageView(systemImpl.getString(MessageID.err_registering_new_user,
                            context))
                    view.setInProgress(false)
                })
            }

        }
    }

    companion object {

        const val ARG_NEXT = "next"

        const val ARG_SERVER_URL = "apiUrl"
    }
}
