package com.ustadmobile.core.controller

import com.nhaarman.mockitokotlin2.*
import com.ustadmobile.core.impl.AppConfig
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.impl.UstadMobileConstants
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.view.HomeView
import com.ustadmobile.core.view.UserProfileView
import com.ustadmobile.lib.db.entities.UmAccount
import org.junit.Before
import org.junit.Test


class UserProfileTest {

    private lateinit var impl: UstadMobileSystemImpl

    private lateinit var view: UserProfileView

    private lateinit var presenter:UserProfilePresenter

    private val context = Any()

    @Before
    fun setUp(){
        view = mock()
        impl = mock ()

        doAnswer {
            "en-US,fa-AF,ps-AF,ar-AE"
        }.`when`(impl).getAppConfigString(any(), any(), any())

        doAnswer {
            UstadMobileConstants.LANGUAGE_NAMES
        }.`when`(impl).getAllUiLanguage(any())

        doAnswer {
            "en"
        }.`when`(impl).getDisplayedLocale(any())

        presenter = UserProfilePresenter(context, mapOf(),view,impl)

        UmAccountManager.setActiveAccount(UmAccount(11,"username",
                "",""), context)
    }


    @Test
    fun givenUserIsLoggedIn_WhenClickedLogout_thenShouldLogout(){

        presenter.onCreate(mapOf())

        presenter.handleUserLogout()

        val firstDest = impl.getAppConfigString(
                AppConfig.KEY_FIRST_DEST, "BasePoint", context)

        verify(impl).go(eq(firstDest!!), any(), any())
    }


    @Test
    fun givenUserIsLoggedIn_WhenClickedLanguageOption_thenShouldShowTheOptions(){

        presenter.onCreate(mapOf())

        presenter.handleShowLanguageOptions()

        verify(view).setLanguageOption(any())
    }

}
