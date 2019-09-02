package com.ustadmobile.port.android.view

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.GsonBuilder
import com.toughra.ustadmobile.R
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.view.ContentEntryImportLinkView
import com.ustadmobile.lib.db.entities.Container
import com.ustadmobile.lib.db.entities.ContentEntry
import com.ustadmobile.lib.db.entities.ContentEntryParentChildJoin
import com.ustadmobile.lib.db.entities.H5PImportData
import com.ustadmobile.port.android.generated.MessageIDMap
import com.ustadmobile.util.test.AbstractImportLinkTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ContentEntryImportLinkEspressoTest : AbstractImportLinkTest() {

    @get:Rule
    var mActivityRule = IntentsTestRule(ContentEntryImportLinkActivity::class.java, false, false)

    private var context = InstrumentationRegistry.getInstrumentation().context

    private lateinit var serverDb: UmAppDatabase

    private lateinit var defaultDb: UmAppDatabase

    private lateinit var repo: UmAppDatabase

    var mockWebServer = MockWebServer()

    var h5pServer = MockWebServer()

    @Before
    @Throws(IOException::class)
    fun setup() {
        UstadMobileSystemImpl.instance.messageIdMap = MessageIDMap.ID_MAP
        serverDb = UmAppDatabase.getInstance(context, "serverdb")
        defaultDb = UmAppDatabase.getInstance(context)



        repo = defaultDb //db!!.getRepository("http://localhost/dummy/", "")
        defaultDb.clearAllTables()
        serverDb.clearAllTables()

        createDb(defaultDb)
        createDb(serverDb)
    }

    @Test
    fun givenUserTypesInvalidUrl_thenShowUserErrorMessageWithInvalidUrl() {

        val intent = Intent()
        intent.putExtra(ContentEntryImportLinkView.CONTENT_ENTRY_PARENT_UID, (-101).toString())
        mActivityRule.launchActivity(intent)
        var activity = mActivityRule.activity

        onView(withId(R.id.entry_import_link_editText)).perform(click())
        onView(withId(R.id.entry_import_link_editText)).perform(replaceText("hello"), ViewActions.closeSoftKeyboard())

        Thread.sleep(500)

        val textInput = activity.findViewById<TextInputLayout>(R.id.entry_import_link_textInput)
        Assert.assertTrue(textInput.error == UstadMobileSystemImpl.instance.getString(MessageID.import_link_invalid_url, context))
    }

    @Test
    fun givenUserTypesNonH5PUrl_thenShowUserErrorMessageWithUnSupportedContent() {


        mockWebServer.enqueue(MockResponse().setBody("no h5p here").setResponseCode(200))
        mockWebServer.start()

        val intent = Intent()
        intent.putExtra(ContentEntryImportLinkView.CONTENT_ENTRY_PARENT_UID, (-101).toString())
        mActivityRule.launchActivity(intent)
        var activity = mActivityRule.activity



        onView(withId(R.id.entry_import_link_editText)).perform(click())
        onView(withId(R.id.entry_import_link_editText)).perform(replaceText(mockWebServer.url("/noh5p").toString()), ViewActions.closeSoftKeyboard())

        Thread.sleep(500)

        val textInput = activity.findViewById<TextInputLayout>(R.id.entry_import_link_textInput)
        Assert.assertTrue(textInput.error == UstadMobileSystemImpl.instance.getString(MessageID.import_link_content_not_supported, context))

    }

    @Test
    fun givenUserTypesH5PUrl_thenShowNoErrorShouldAppear() {


        mockWebServer.enqueue(MockResponse().setHeader("Content-Type", "text/html; charset=utf-8").setResponseCode(200))
        mockWebServer.start()

        val intent = Intent()
        intent.putExtra(ContentEntryImportLinkView.CONTENT_ENTRY_PARENT_UID, (-101).toString())
        mActivityRule.launchActivity(intent)
        var activity = mActivityRule.activity

        var urlString = mockWebServer.url("/somehp5here").toString()


        onView(withId(R.id.entry_import_link_editText)).perform(click())
        onView(withId(R.id.entry_import_link_editText)).perform(replaceText(urlString), ViewActions.closeSoftKeyboard())

        val textInput = activity.findViewById<TextInputLayout>(R.id.entry_import_link_textInput)
        Assert.assertTrue(textInput.error.isNullOrBlank())

    }

    @Test
    fun givenClicksOnDone() {

        mockWebServer.enqueue(MockResponse().setHeader("Content-Type", "text/html; charset=utf-8").setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setBody("H5PIntegration").setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setBody("H5PIntegration").setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setBody("H5PIntegration").setResponseCode(200))
        mockWebServer.start()

        val entryDao = serverDb.contentEntryDao
        val parentChildJoinDao = serverDb.contentEntryParentChildJoinDao
        val containerDao = serverDb.containerDao

        val contentEntry = ContentEntry()
        contentEntry.contentEntryUid = entryDao.insert(contentEntry)

        val parentChildJoin = ContentEntryParentChildJoin()
        parentChildJoin.cepcjParentContentEntryUid = -101
        parentChildJoin.cepcjChildContentEntryUid = contentEntry.contentEntryUid
        parentChildJoinDao.insert(parentChildJoin)

        val container = Container(contentEntry)
        container.containerUid = containerDao.insert(container)

        val import = H5PImportData(contentEntry, container, parentChildJoin)

        val gson = GsonBuilder().create()
        val importJson = gson.toJson(import)

        var mockResponse = MockResponse()
        mockResponse.addHeader("Content-Type", "application/json")
        mockResponse.setBody(importJson)
        mockResponse.setResponseCode(200)

        h5pServer.enqueue(mockResponse)
        h5pServer.enqueue(mockResponse)
        h5pServer.start()

        var urlString = mockWebServer.url("/somehp5here").toString()

        val intent = Intent()
        intent.putExtra(ContentEntryImportLinkView.CONTENT_ENTRY_PARENT_UID, (-101).toString())
        intent.putExtra(ContentEntryImportLinkView.END_POINT_URL, h5pServer.url("").toString())
        mActivityRule.launchActivity(intent)


        onView(withId(R.id.entry_import_link_editText)).perform(click())
        onView(withId(R.id.entry_import_link_editText)).perform(replaceText(urlString), ViewActions.closeSoftKeyboard())

        Thread.sleep(250)

        runBlocking {

            delay(250)
            onView(withId(R.id.import_link_done)).perform(click())
            delay(500)
            Assert.assertTrue(defaultDb.contentEntryParentChildJoinDao.findListOfChildsByParentUuid(-101).isNotEmpty())
        }
    }

    @Test
    fun givenUserTypesVideoLink_thenShowVideoTitle() {

        mockWebServer.enqueue(MockResponse().setHeader("Content-Length", 11).setHeader("Content-Type", "video/").setResponseCode(200))
        mockWebServer.start()

        val intent = Intent()
        intent.putExtra(ContentEntryImportLinkView.CONTENT_ENTRY_PARENT_UID, (-101).toString())
        mActivityRule.launchActivity(intent)

        var urlString = mockWebServer.url("/videohere").toString()

        onView(withId(R.id.entry_import_link_titleInput)).check(matches(not(isDisplayed())))

        onView(withId(R.id.entry_import_link_editText)).perform(click())
        onView(withId(R.id.entry_import_link_editText)).perform(replaceText(urlString), ViewActions.closeSoftKeyboard())

        Thread.sleep(500)

        onView(withId(R.id.entry_import_link_titleInput)).check(matches(isDisplayed()))

        onView(withId(R.id.import_link_done)).check(matches(not(isEnabled())))

        onView(withId(R.id.entry_import_link_title_editText)).perform(replaceText("Video Title"), ViewActions.closeSoftKeyboard())

        onView(withId(R.id.import_link_done)).check(matches(isEnabled()))

    }

    @Test
    fun givenUserTypesVideoLink_whenFileSizeTooBig_showError() {

        mockWebServer.enqueue(MockResponse().setHeader("Content-Length", 104857600).setHeader("Content-Type", "video/").setResponseCode(200))
        mockWebServer.start()

        val intent = Intent()
        intent.putExtra(ContentEntryImportLinkView.CONTENT_ENTRY_PARENT_UID, (-101).toString())
        mActivityRule.launchActivity(intent)
        var activity = mActivityRule.activity

        var urlString = mockWebServer.url("/videohere").toString()

        onView(withId(R.id.entry_import_link_titleInput)).check(matches(not(isDisplayed())))

        onView(withId(R.id.entry_import_link_editText)).perform(click())
        onView(withId(R.id.entry_import_link_editText)).perform(replaceText(urlString), ViewActions.closeSoftKeyboard())

        Thread.sleep(500)

        onView(withId(R.id.entry_import_link_titleInput)).check(matches(not(isDisplayed())))

        onView(withId(R.id.import_link_done)).check(matches(not(isEnabled())))

        val textInput = activity.findViewById<TextInputLayout>(R.id.entry_import_link_textInput)
        Assert.assertTrue(textInput.error == UstadMobileSystemImpl.instance.getString(MessageID.import_link_big_size, context))


    }


}