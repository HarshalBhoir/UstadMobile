package com.ustadmobile.test.port.android.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment

import androidx.test.InstrumentationRegistry
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.AndroidJUnit4

import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.db.dao.ContainerDao
import com.ustadmobile.core.db.dao.ContentEntryDao
import com.ustadmobile.core.view.VideoPlayerView
import com.ustadmobile.lib.db.entities.Container
import com.ustadmobile.lib.db.entities.ContentEntry
import com.ustadmobile.port.android.view.VideoPlayerActivity
import com.ustadmobile.port.sharedse.container.ContainerManager
import com.ustadmobile.test.port.android.UmAndroidTestUtil

import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.HashMap

@RunWith(AndroidJUnit4::class)
class VideoPlayerTest {

    @Rule
    var mActivityRule = IntentsTestRule(VideoPlayerActivity::class.java, false, false)
    @Rule
    var permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION)
    private var containerUid: Long = 0


    val db: UmAppDatabase
        get() {
            val context = InstrumentationRegistry.getTargetContext()
            val db = UmAppDatabase.getInstance(context)
            db.clearAllTables()
            return  UmAppDatabase.getInstance(context)//db.getRepository("https://localhost", "")
        }

    @Throws(IOException::class)
    fun createDummyContent() {
        val db = UmAppDatabase.getInstance(InstrumentationRegistry.getTargetContext())
        val repo = db
        val contentDao = repo.contentEntryDao
        val containerDao = repo.containerDao

        val spanishQuiz = ContentEntry()
        spanishQuiz.contentEntryUid = 14
        spanishQuiz.title = "tiempo de prueba"
        spanishQuiz.thumbnailUrl = "https://www.africanstorybook.org/img/asb120.png"
        spanishQuiz.description = "todo el contenido"
        spanishQuiz.publisher = "CK12"
        spanishQuiz.author = "borrachera"
        spanishQuiz.primaryLanguageUid = 3
        spanishQuiz.leaf = true
        contentDao.insert(spanishQuiz)

        val tmpDir = Files.createTempDirectory("testVideoPlayer").toFile()
        tmpDir.mkdirs()
        val videoFile = File(tmpDir, "video1.webm")
        val audioTempFile = File(tmpDir, "audio.c2")
        val srtTmpFile = File(tmpDir, "subtitle.srt")

        FileUtils.copyInputStreamToFile(
                javaClass.getResourceAsStream("/com/ustadmobile/app/android/video1.webm")!!,
                videoFile)

        FileUtils.copyInputStreamToFile(
                javaClass.getResourceAsStream("/com/ustadmobile/app/android/video1-codec2-version2.c2")!!,
                audioTempFile)
        FileUtils.copyInputStreamToFile(
                javaClass.getResourceAsStream("/com/ustadmobile/app/android/srtfile.srt")!!,
                srtTmpFile)

        val dir = Environment.getExternalStorageDirectory()

        val container = Container()
        container.containerContentEntryUid = 14L
        containerUid = containerDao.insert(container)
        container.containerUid = containerUid

        val manager = ContainerManager(container, db,
                repo, dir.absolutePath)

        val fileMap = HashMap<File, String>()
        fileMap[videoFile] = "video1.webm"
        fileMap[audioTempFile] = "audio.c2"
        fileMap[srtTmpFile] = "subtitle.srt"
        manager.addEntries(fileMap, true)

    }


    @Test
    @Throws(IOException::class)
    fun givenServerOffline_whenPlixZippedIsOpened_WebviewLoads() {
        val launchActivityIntent = Intent()

        createDummyContent()

        UmAndroidTestUtil.setAirplaneModeEnabled(true)
        val b = Bundle()
        b.putString(VideoPlayerView.ARG_CONTAINER_UID, containerUid.toString())
        b.putString(VideoPlayerView.ARG_CONTENT_ENTRY_ID, 14L.toString())
        launchActivityIntent.putExtras(b)
        mActivityRule.launchActivity(launchActivityIntent)

        //  mActivityRule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // the webview looks for an element "questionController" which is the start button of plix.
        // This is only available once plix has fully loaded and displayed to the user
    }


}