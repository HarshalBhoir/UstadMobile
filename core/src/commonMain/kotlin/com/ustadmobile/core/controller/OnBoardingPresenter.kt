package com.ustadmobile.core.controller

import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.view.HomeView
import com.ustadmobile.core.view.OnBoardingView
import com.ustadmobile.core.view.OnBoardingView.Companion.PREF_TAG

class OnBoardingPresenter(context: Any, arguments: Map<String, String>, view: OnBoardingView, val impl: UstadMobileSystemImpl) :
        UstadBaseController<OnBoardingView>(context, arguments, view) {

    private lateinit var  languageOptions: List<Pair<String, String>>

    override fun onCreate(savedState: Map<String, String?> ?) {
        super.onCreate(savedState)

        languageOptions = impl.getUiLanguageOptions(context)

        view.setLanguageOptions(languageOptions.map { it.second} )
        view.setScreenList()

        val wasShown = impl.getAppPref(PREF_TAG, view.viewContext)
        if (wasShown!= null && wasShown.toBoolean()) {
            handleGetStarted()
        }

    }

    fun handleGetStarted() {
        val args: Map<String,String?> = arguments
        impl.setAppPref(PREF_TAG, true.toString(), view.viewContext)
        impl.go(HomeView.VIEW_NAME, args, context)
    }

    fun handleLanguageSelected(position: Int){
        val localeCode = languageOptions[position].first
        //val localeCode = getLocaleCode(languageName)
        if(position > 0 && impl.getDisplayedLocale(context) != localeCode){
            impl.setLocale(localeCode, context)
            view.restartUI()
        }
    }

}
