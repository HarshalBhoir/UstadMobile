package com.ustadmobile.core.controller

import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.networkmanager.defaultHttpClient
import com.ustadmobile.core.view.ContentEntryImportLinkView
import com.ustadmobile.core.view.ContentEntryImportLinkView.Companion.CONTENT_ENTRY_PARENT_UID
import com.ustadmobile.lib.db.entities.H5PImportData
import io.ktor.client.call.receive
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.parameter
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.discardRemaining
import io.ktor.http.URLParserException
import io.ktor.http.Url
import kotlinx.coroutines.Runnable

class ContentEntryImportLinkPresenter(context: Any, arguments: Map<String, String?>, view: ContentEntryImportLinkView, var endpointUrl: String) :
        UstadBaseController<ContentEntryImportLinkView>(context, arguments, view) {

    private var videoTitle: String? = null

    private var parentContentEntryUid: Long = 0

    private var hp5Url: String = ""

    private var contentType = -1

    var isDoneEnabled = false

    override fun onCreate(savedState: Map<String, String?>?) {
        super.onCreate(savedState)

        parentContentEntryUid = arguments.getValue(CONTENT_ENTRY_PARENT_UID)!!.toLong()
    }


    suspend fun handleUrlTextUpdated(url: String) {
        view.showHideVideoTitle(false)
        isDoneEnabled = false
        view.checkDoneButton()

        var response: HttpResponse?
        try {
            response = defaultHttpClient().head<HttpResponse>(url)
        } catch (e: Exception) {
            view.showUrlStatus(false, UstadMobileSystemImpl.instance.getString(MessageID.import_link_invalid_url, context))
            return
        }

        contentType = -1

        if (response.status.value != 200) {
            view.showUrlStatus(false, UstadMobileSystemImpl.instance.getString(MessageID.import_link_invalid_url, context))
            return
        }

        var contentTypeHeader = response.headers["Content-Type"]
        if (contentTypeHeader?.contains(";") == true) {
            contentTypeHeader = contentTypeHeader.split(";")[0]
        }

        val length = response.headers["Content-Length"]?.toInt() ?: FILE_SIZE

        response.discardRemaining()
        response.close()

        if (contentTypeHeader?.startsWith("video/") == true) {

            if (length >= FILE_SIZE) {
                view.showUrlStatus(false, UstadMobileSystemImpl.instance.getString(MessageID.import_link_big_size, context))
                return
            }

            contentType = VIDEO
            this.hp5Url = url
            view.showUrlStatus(true, "")
            view.showHideVideoTitle(true)
            return

        } else if (!listOfHtmlContentType.contains(contentTypeHeader)) {
            view.showUrlStatus(false, UstadMobileSystemImpl.instance.getString(MessageID.import_link_content_not_supported, context))
            return
        }

        val content = checkIfH5PValidAndReturnItsContent(url)

        if (content.isNullOrEmpty()) {
            view.showUrlStatus(false, UstadMobileSystemImpl.instance.getString(MessageID.import_link_invalid_url, context))
            return
        }

        val isValid = content.contains("H5PIntegration")
        if (isValid) {
            contentType = HTML
            this.hp5Url = url
            view.showUrlStatus(isValid, "")
            view.displayUrl(url)
            isDoneEnabled = true
            view.checkDoneButton()
        } else {
            view.showUrlStatus(isValid, UstadMobileSystemImpl.instance.getString(MessageID.import_link_invalid_url, context))
        }
    }


    suspend fun handleClickImport() {
        val client = defaultHttpClient()
        var response: HttpResponse? = null
        view.showProgress(true)
        view.enableDisableEditText(false)
        view.showHideErrorMessage(false)

        try {
            when (contentType) {
                HTML -> {

                    response = client.get<HttpResponse>("$endpointUrl/ImportH5P/importUrl") {
                        parameter("hp5Url", hp5Url)
                        parameter("parentUid", parentContentEntryUid)
                    }

                }
                VIDEO -> {

                    if (videoTitle.isNullOrEmpty()) {
                        view.showNoTitleEntered(UstadMobileSystemImpl.instance.getString(MessageID.import_title_not_entered, context))
                        view.showProgress(false)
                        return
                    }

                    response = client.get<HttpResponse>("$endpointUrl/ImportH5P/importVideo") {
                        parameter("hp5Url", hp5Url)
                        parameter("parentUid", parentContentEntryUid)
                        parameter("title", videoTitle)
                    }
                }
            }
        } catch (e: Exception) {
            response = null
        }


        if (response?.status?.value == 200) {

            val content = response.receive<H5PImportData>()

            view.showProgress(false)
            view.enableDisableEditText(true)

            val db = UmAppDatabase.getInstance(context)
            db.contentEntryDao.insert(content.contentEntry)
            db.contentEntryParentChildJoinDao.insert(content.parentChildJoin)
            db.containerDao.insert(content.container)

            view.runOnUiThread(Runnable {
                view.returnResult()
            })

        } else {

            view.showProgress(false)
            view.enableDisableEditText(true)
            view.showHideErrorMessage(true)

        }


    }

    fun handleTitleChanged(title: String) {
        this.videoTitle = title
        if (title.isNotEmpty()) {
            isDoneEnabled = true
            view.checkDoneButton()
            view.showNoTitleEntered("")
        }
    }

    companion object {

        const val HTML = 1

        const val VIDEO = 2

        const val FILE_SIZE = 104857600

        val listOfHtmlContentType = listOf("text/html", "application/xhtml+xml", "application/xml", "text/xml")


    }

}


suspend fun checkIfH5PValidAndReturnItsContent(url: String): String? {
    var urlLink: Url?
    try {
        urlLink = Url(url)
    } catch (exception: URLParserException) {
        return null
    }

    return try {
        defaultHttpClient().get<String>(urlLink)
    } catch (e: Exception) {
        null
    }


}