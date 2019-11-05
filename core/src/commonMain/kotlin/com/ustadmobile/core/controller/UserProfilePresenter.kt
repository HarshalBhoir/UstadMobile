package com.ustadmobile.core.controller

import com.ustadmobile.core.db.dao.PersonDao
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.view.HomeView
import com.ustadmobile.core.view.UserProfileView
import com.ustadmobile.lib.db.entities.Person
import com.ustadmobile.lib.db.entities.UmAccount
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UserProfilePresenter (context: Any, arguments: Map<String, String?>, view: UserProfileView,
                            val impl: UstadMobileSystemImpl, val personDao: PersonDao)
    : UstadBaseController<UserProfileView>(context, arguments, view){

    private val languageOptions = impl.getAllUiLanguage(context)

    private lateinit var account: UmAccount

    override fun onCreate(savedState: Map<String, String?>?) {
        super.onCreate(savedState)
        account = UmAccountManager.getActiveAccount(context)!!
        view.setUsername(account.username!!)
        view.setCurrentLanguage(languageOptions[impl.getDisplayedLocale(context)!!])
    }


    fun handleUserLogout(){
        UmAccountManager.setActiveAccount(UmAccount(0,
                null, null, null), context)
        GlobalScope.launch {
            personDao.removePerson(account.personUid)
        }

        val args = HashMap<String, String>()
        impl.go(HomeView.VIEW_NAME, args, context)
    }

    fun handleShowLanguageOptions(){
        view.setLanguageOption(languageOptions.values.sorted().toMutableList())
    }

    fun handleLanguageSelected(position: Int){
        val languageName = languageOptions.values.sorted().toMutableList()[position]
        val localeCode = getLocaleCode(languageName)
        if(impl.getDisplayedLocale(context) != localeCode){
            impl.setLocale(localeCode, context)
            view.restartUI()
        }
    }

    private fun getLocaleCode(name: String): String{
        for(pair in languageOptions){
            if(name == pair.value) return pair.key
        }
        return "en"
    }

}