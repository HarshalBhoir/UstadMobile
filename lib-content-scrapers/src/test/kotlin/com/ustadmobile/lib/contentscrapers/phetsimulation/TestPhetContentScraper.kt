package com.ustadmobile.lib.contentscrapers.phetsimulation

import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.db.dao.ContentEntryContentCategoryJoinDao
import com.ustadmobile.core.db.dao.ContentEntryDao
import com.ustadmobile.core.db.dao.ContentEntryParentChildJoinDao
import com.ustadmobile.core.db.dao.ContentEntryRelatedEntryJoinDao
import com.ustadmobile.lib.contentscrapers.ScraperConstants
import com.ustadmobile.lib.db.entities.ContentEntry
import com.ustadmobile.lib.db.entities.ContentEntryParentChildJoin
import com.ustadmobile.lib.db.entities.ContentEntryRelatedEntryJoin

import org.junit.Assert
import org.junit.Test

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.util.ArrayList

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import okio.BufferedSource
import okio.Okio

class TestPhetContentScraper {


    private val EN_LOCATION_FILE = "/com/ustadmobile/lib/contentscrapers/phetsimulation/simulation_en.html"
    private val ES_LOCATION_FILE = "/com/ustadmobile/lib/contentscrapers/phetsimulation/simulation_es.html"
    private val HTML_FILE_LOCATION = "/com/ustadmobile/lib/contentscrapers/phetsimulation/phet-html-detail.html"
    private val JAR_FILE_LOCATION = "/com/ustadmobile/lib/contentscrapers/phetsimulation/phet-jar-detail.html"
    private val FLASH_FILE_LOCATION = "/com/ustadmobile/lib/contentscrapers/phetsimulation/phet-flash-detail.html"

    private val PHET_MAIN_CONTENT = "/com/ustadmobile/lib/contentscrapers/phetsimulation/phet-main-content.html"

    private val SIM_EN = "simulation_en.html"
    private val SIM_ES = "simulation_es.html"


    internal val dispatcher: Dispatcher = object : Dispatcher() {


        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {

            try {

                if (request.path.startsWith("/en/api/simulation")) {

                    val buffer = readFile(HTML_FILE_LOCATION)
                    val response = MockResponse().setResponseCode(200)
                    response.setHeader("ETag", (buffer.size().toString() + HTML_FILE_LOCATION).hashCode())
                    if (!request.method.equals("HEAD", ignoreCase = true))
                        response.body = buffer

                    return response

                } else if (request.path.contains(PHET_MAIN_CONTENT)) {

                    val buffer = readFile(PHET_MAIN_CONTENT)
                    val response = MockResponse().setResponseCode(200)
                    response.setHeader("ETag", (buffer.size().toString() + HTML_FILE_LOCATION).hashCode())
                    if (!request.method.equals("HEAD", ignoreCase = true))
                        response.body = buffer

                    return response


                } else if (request.path == "/media/simulation_en.html?download") {

                    val buffer = readFile(EN_LOCATION_FILE)
                    val response = MockResponse().setResponseCode(200)
                    response.addHeader("ETag", "16adca-5717010854ac0")
                    response.addHeader("Last-Modified", "Fri, 20 Jul 2018 15:36:51 GMT")
                    if (!request.method.equals("HEAD", ignoreCase = true))
                        response.body = buffer

                    return response

                } else if (request.path.contains("/media/simulation_es.html?download")) {

                    val buffer = readFile(ES_LOCATION_FILE)
                    val response = MockResponse().setResponseCode(200)
                    response.addHeader("ETag", "16adca-5717010854ac0")
                    response.addHeader("Last-Modified", "Fri, 20 Jul 2018 15:36:51 GMT")
                    if (!request.method.equals("HEAD", ignoreCase = true))
                        response.body = buffer

                    return response

                } else if (request.path.contains("flash")) {

                    val buffer = readFile(FLASH_FILE_LOCATION)
                    val response = MockResponse().setResponseCode(200)
                    response.addHeader("ETag", "16adca-5717010854ac0")
                    response.addHeader("Last-Modified", "Fri, 20 Jul 2018 15:36:51 GMT")
                    if (!request.method.equals("HEAD", ignoreCase = true))
                        response.body = buffer

                    return response

                } else if (request.path.contains("jar")) {

                    val buffer = readFile(JAR_FILE_LOCATION)
                    val response = MockResponse().setResponseCode(200)
                    response.addHeader("ETag", "16adca-5717010854ac0")
                    response.addHeader("Last-Modified", "Fri, 20 Jul 2018 15:36:51 GMT")
                    if (!request.method.equals("HEAD", ignoreCase = true))
                        response.body = buffer

                    return response
                }

            } catch (e: IOException) {
                e.printStackTrace()
            }

            return MockResponse().setResponseCode(404)
        }
    }

    @Throws(IOException::class)
    fun readFile(location: String): Buffer {
        val videoIn = javaClass.getResourceAsStream(location)
        val source = Okio.buffer(Okio.source(videoIn))
        val buffer = Buffer()
        source.readAll(buffer)

        return buffer
    }


    fun AssertAllFiles(tmpDir: File, scraper: PhetContentScraper) {

        val englishLocation = File(tmpDir, "en")
        Assert.assertTrue("English Folder exists", englishLocation.isDirectory)

        val titleDirectory = File(englishLocation, scraper.title)
        Assert.assertTrue("English Simulation Folder exists", titleDirectory.isDirectory)

        val aboutFile = File(titleDirectory, ScraperConstants.ABOUT_HTML)
        Assert.assertTrue("About File English Exists", aboutFile.length() > 0)

        val englishSimulation = File(titleDirectory, SIM_EN)
        Assert.assertTrue("English Simulation exists", englishSimulation.length() > 0)

        val engETag = File(englishLocation, "simulation_en" + ScraperConstants.ETAG_TXT)
        Assert.assertTrue("English ETag exists", engETag.length() > 0)

        val spanishDir = File(tmpDir, "es")
        Assert.assertTrue("Spanish Folder exists", spanishDir.isDirectory)

        val spanishTitleDirectory = File(spanishDir, scraper.title)
        Assert.assertTrue("Spanish File Folder exists", spanishTitleDirectory.isDirectory)

        val aboutSpanishFile = File(spanishTitleDirectory, ScraperConstants.ABOUT_HTML)
        Assert.assertTrue("About File English Exists", aboutSpanishFile.length() > 0)

        val spanishSimulation = File(spanishTitleDirectory, SIM_ES)
        Assert.assertTrue("Spanish Simulation exists", spanishSimulation.length() > 0)

        val spanishETag = File(spanishDir, "simulation_es" + ScraperConstants.ETAG_TXT)
        Assert.assertTrue("Spanish ETag exists", spanishETag.length() > 0)

    }


    @Test
    @Throws(IOException::class)
    fun givenServerOnline_whenPhetContentScraped_thenShouldConvertAndDownload() {
        val tmpDir = Files.createTempDirectory("testphetcontentscraper").toFile()
        val containerDir = Files.createTempDirectory("container").toFile()

        val mockWebServer = MockWebServer()
        mockWebServer.setDispatcher(dispatcher)

        val mockServerUrl = mockWebServer.url("/en/api/simulation/equality-explorer-two-variables").toString()
        val scraper = PhetContentScraper(mockServerUrl, tmpDir, containerDir)
        scraper.scrapeContent()

        AssertAllFiles(tmpDir, scraper)
    }

    @Test
    @Throws(IOException::class)
    fun givenServerOnline_whenPhetContentScrapedAgain_thenShouldNotDownloadFilesAgain() {
        val tmpDir = Files.createTempDirectory("testphetcontentscraper").toFile()
        val containerDir = Files.createTempDirectory("container").toFile()

        val mockWebServer = MockWebServer()
        mockWebServer.setDispatcher(dispatcher)

        val mockServerUrl = mockWebServer.url("/en/api/simulation/equality-explorer-two-variables").toString()
        val scraper = PhetContentScraper(mockServerUrl, tmpDir, containerDir)
        scraper.scrapeContent()
        val englishLocation = File(tmpDir, "en")
        val titleDirectory = File(englishLocation, scraper.title)
        val englishSimulation = File(titleDirectory, SIM_EN)

        val firstSimDownload = englishSimulation.lastModified()

        scraper.scrapeContent()

        val lastModified = englishSimulation.lastModified()

        Assert.assertEquals("didnt download 2nd time", firstSimDownload, lastModified)

    }

    @Test(expected = IllegalArgumentException::class)
    @Throws(IOException::class)
    fun givenServerOnline_whenPhetContentScraped_thenShouldThrowIllegalArgumentJarNotSupported() {
        val tmpDir = Files.createTempDirectory("testphetcontentscraper").toFile()
        val containerDir = Files.createTempDirectory("container").toFile()

        val mockWebServer = MockWebServer()
        mockWebServer.setDispatcher(dispatcher)

        val scraper = PhetContentScraper(mockWebServer.url("/legacy/jar").toString(), tmpDir, containerDir)
        scraper.scrapeContent()

    }

    @Test(expected = IllegalArgumentException::class)
    @Throws(IOException::class)
    fun givenServerOnline_whenPhetContentScraped_thenShouldThrowIllegalArgumentFlashNotSupported() {
        val tmpDir = Files.createTempDirectory("testphetcontentscraper").toFile()
        val containerDir = Files.createTempDirectory("container").toFile()

        val mockWebServer = MockWebServer()
        mockWebServer.setDispatcher(dispatcher)

        val scraper = PhetContentScraper(mockWebServer.url("/legacy/flash").toString(), tmpDir, containerDir)
        scraper.scrapeContent()

    }

    @Test
    @Throws(IOException::class)
    fun givenServerOnline_whenUrlFound_findAllSimulations() {

        val db = UmAppDatabase.getInstance(null)
        val repo = db.getRepository("https://localhost", "")
        db.clearAllTables()

        val index = IndexPhetContentScraper()
        val mockWebServer = MockWebServer()
        mockWebServer.setDispatcher(dispatcher)

        val tmpDir = Files.createTempDirectory("testphetindexscraper").toFile()
        val containerDir = Files.createTempDirectory("container").toFile()

        index.findContent(mockWebServer.url(PHET_MAIN_CONTENT).toString(), tmpDir, containerDir)

        val contentEntryDao = repo.contentEntryDao
        val parentChildDaoJoin = repo.contentEntryParentChildJoinDao
        val categoryJoinDao = repo.contentEntryContentCategoryJoinDao
        val relatedJoin = repo.contentEntryRelatedEntryJoinDao

        val urlPrefix = "http://" + mockWebServer.hostName + ":" + mockWebServer.port

        val parentEntry = contentEntryDao.findBySourceUrl("https://phet.colorado.edu/")
        Assert.assertEquals("Main parent content entry exsits", true, parentEntry!!.entryId!!.equals("https://phet.colorado.edu/", ignoreCase = true))

        val categoryEntry = contentEntryDao.findBySourceUrl("$urlPrefix/en/simulations/category/math")
        val parentChildJoinEntry = parentChildDaoJoin.findParentByChildUuids(categoryEntry!!.contentEntryUid)
        Assert.assertEquals("Category Math entry exists", true, parentChildJoinEntry!!.cepcjParentContentEntryUid == parentEntry!!.contentEntryUid)

        val englishSimulationEntry = contentEntryDao.findBySourceUrl("$urlPrefix/en/api/simulation/test")
        Assert.assertEquals("Simulation entry english exists", true, englishSimulationEntry!!.entryId!!.equals("/en/api/simulation/test", ignoreCase = true))

        val categorySimulationEntryLists = parentChildDaoJoin.findListOfParentsByChildUuid(englishSimulationEntry!!.contentEntryUid)
        var hasMathCategory = false
        for (category in categorySimulationEntryLists) {

            if (category.cepcjParentContentEntryUid == categoryEntry!!.contentEntryUid) {
                hasMathCategory = true
                break
            }
        }
        Assert.assertEquals("Parent child join between category and simulation exists", true, hasMathCategory)

        val spanishEntry = contentEntryDao.findBySourceUrl("$urlPrefix/es/api/simulation/test")
        Assert.assertEquals("Simulation entry spanish exists", true, spanishEntry!!.entryId!!.equals("/es/api/simulation/test", ignoreCase = true))

        val spanishEnglishJoin = relatedJoin.findPrimaryByTranslation(spanishEntry!!.contentEntryUid)
        Assert.assertEquals("Related Join with Simulation Exists - Spanish Match", true, spanishEnglishJoin!!.cerejRelatedEntryUid == spanishEntry!!.contentEntryUid)
        Assert.assertEquals("Related Join with Simulation Exists - English Match", true, spanishEnglishJoin!!.cerejContentEntryUid == englishSimulationEntry!!.contentEntryUid)

    }


    @Test
    @Throws(IOException::class)
    fun givenDirectoryOfTranslationsIsCreated_findAllTranslationRelations() {

        val db = UmAppDatabase.getInstance(null)
        val repo = db.getRepository("https://localhost", "")
        db.clearAllTables()

        val tmpDir = Files.createTempDirectory("testphetcontentscraper").toFile()
        val containerDir = Files.createTempDirectory("container").toFile()

        val mockWebServer = MockWebServer()
        mockWebServer.setDispatcher(dispatcher)

        val scraper = PhetContentScraper(mockWebServer.url("/en/api/simulation/equality-explorer-two-variables").toString(), tmpDir, containerDir)
        scraper.scrapeContent()

        val translationList = scraper.getTranslations(tmpDir, repo.contentEntryDao, "", repo.languageDao, db.languageVariantDao)

    }

}
