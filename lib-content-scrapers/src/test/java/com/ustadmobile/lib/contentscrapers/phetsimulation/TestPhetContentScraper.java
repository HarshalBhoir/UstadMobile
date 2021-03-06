package com.ustadmobile.lib.contentscrapers.phetsimulation;

import com.ustadmobile.core.db.UmAppDatabase;
import com.ustadmobile.core.db.dao.ContentEntryContentCategoryJoinDao;
import com.ustadmobile.core.db.dao.ContentEntryDao;
import com.ustadmobile.core.db.dao.ContentEntryParentChildJoinDao;
import com.ustadmobile.core.db.dao.ContentEntryRelatedEntryJoinDao;
import com.ustadmobile.lib.contentscrapers.ScraperConstants;
import com.ustadmobile.lib.db.entities.ContentEntry;
import com.ustadmobile.lib.db.entities.ContentEntryParentChildJoin;
import com.ustadmobile.lib.db.entities.ContentEntryRelatedEntryJoin;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import okio.BufferedSource;
import okio.Okio;

public class TestPhetContentScraper {


    private String EN_LOCATION_FILE = "/com/ustadmobile/lib/contentscrapers/phetsimulation/simulation_en.html";
    private String ES_LOCATION_FILE = "/com/ustadmobile/lib/contentscrapers/phetsimulation/simulation_es.html";
    private String HTML_FILE_LOCATION = "/com/ustadmobile/lib/contentscrapers/phetsimulation/phet-html-detail.html";
    private String JAR_FILE_LOCATION = "/com/ustadmobile/lib/contentscrapers/phetsimulation/phet-jar-detail.html";
    private String FLASH_FILE_LOCATION = "/com/ustadmobile/lib/contentscrapers/phetsimulation/phet-flash-detail.html";

    private final String PHET_MAIN_CONTENT = "/com/ustadmobile/lib/contentscrapers/phetsimulation/phet-main-content.html";

    private String SIM_EN = "simulation_en.html";
    private String SIM_ES = "simulation_es.html";


    final Dispatcher dispatcher = new Dispatcher() {


        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {

            try {

                if (request.getPath().startsWith("/en/api/simulation")) {

                    Buffer buffer = readFile(HTML_FILE_LOCATION);
                    MockResponse response = new MockResponse().setResponseCode(200);
                    response.setHeader("ETag", (String.valueOf(buffer.size())
                            + HTML_FILE_LOCATION).hashCode());
                    if (!request.getMethod().equalsIgnoreCase("HEAD"))
                        response.setBody(buffer);

                    return response;

                } else if (request.getPath().contains(PHET_MAIN_CONTENT)) {

                    Buffer buffer = readFile(PHET_MAIN_CONTENT);
                    MockResponse response = new MockResponse().setResponseCode(200);
                    response.setHeader("ETag", (String.valueOf(buffer.size())
                            + HTML_FILE_LOCATION).hashCode());
                    if (!request.getMethod().equalsIgnoreCase("HEAD"))
                        response.setBody(buffer);

                    return response;


                } else if (request.getPath().equals("/media/simulation_en.html?download")) {

                    Buffer buffer = readFile(EN_LOCATION_FILE);
                    MockResponse response = new MockResponse().setResponseCode(200);
                    response.addHeader("ETag", "16adca-5717010854ac0");
                    response.addHeader("Last-Modified", "Fri, 20 Jul 2018 15:36:51 GMT");
                    if (!request.getMethod().equalsIgnoreCase("HEAD"))
                        response.setBody(buffer);

                    return response;

                } else if (request.getPath().contains("/media/simulation_es.html?download")) {

                    Buffer buffer = readFile(ES_LOCATION_FILE);
                    MockResponse response = new MockResponse().setResponseCode(200);
                    response.addHeader("ETag", "16adca-5717010854ac0");
                    response.addHeader("Last-Modified", "Fri, 20 Jul 2018 15:36:51 GMT");
                    if (!request.getMethod().equalsIgnoreCase("HEAD"))
                        response.setBody(buffer);

                    return response;

                } else if (request.getPath().contains("flash")) {

                    Buffer buffer = readFile(FLASH_FILE_LOCATION);
                    MockResponse response = new MockResponse().setResponseCode(200);
                    response.addHeader("ETag", "16adca-5717010854ac0");
                    response.addHeader("Last-Modified", "Fri, 20 Jul 2018 15:36:51 GMT");
                    if (!request.getMethod().equalsIgnoreCase("HEAD"))
                        response.setBody(buffer);

                    return response;

                } else if (request.getPath().contains("jar")) {

                    Buffer buffer = readFile(JAR_FILE_LOCATION);
                    MockResponse response = new MockResponse().setResponseCode(200);
                    response.addHeader("ETag", "16adca-5717010854ac0");
                    response.addHeader("Last-Modified", "Fri, 20 Jul 2018 15:36:51 GMT");
                    if (!request.getMethod().equalsIgnoreCase("HEAD"))
                        response.setBody(buffer);

                    return response;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return new MockResponse().setResponseCode(404);
        }
    };

    public Buffer readFile(String location) throws IOException {
        InputStream videoIn = getClass().getResourceAsStream(location);
        BufferedSource source = Okio.buffer(Okio.source(videoIn));
        Buffer buffer = new Buffer();
        source.readAll(buffer);

        return buffer;
    }


    public void AssertAllFiles(File tmpDir, PhetContentScraper scraper) {

        File englishLocation = new File(tmpDir, "en");
        Assert.assertTrue("English Folder exists", englishLocation.isDirectory());

        File titleDirectory = new File(englishLocation, scraper.getTitle());
        Assert.assertTrue("English Simulation Folder exists", titleDirectory.isDirectory());

        File aboutFile = new File(titleDirectory, ScraperConstants.ABOUT_HTML);
        Assert.assertTrue("About File English Exists", aboutFile.length() > 0);

        File englishSimulation = new File(titleDirectory, SIM_EN);
        Assert.assertTrue("English Simulation exists", englishSimulation.length() > 0);

        File engETag = new File(englishLocation, "simulation_en" + ScraperConstants.ETAG_TXT);
        Assert.assertTrue("English ETag exists", engETag.length() > 0);

        File spanishDir = new File(tmpDir, "es");
        Assert.assertTrue("Spanish Folder exists", spanishDir.isDirectory());

        File spanishTitleDirectory = new File(spanishDir, scraper.getTitle());
        Assert.assertTrue("Spanish File Folder exists", spanishTitleDirectory.isDirectory());

        File aboutSpanishFile = new File(spanishTitleDirectory, ScraperConstants.ABOUT_HTML);
        Assert.assertTrue("About File English Exists", aboutSpanishFile.length() > 0);

        File spanishSimulation = new File(spanishTitleDirectory, SIM_ES);
        Assert.assertTrue("Spanish Simulation exists", spanishSimulation.length() > 0);

        File spanishETag = new File(spanishDir, "simulation_es" + ScraperConstants.ETAG_TXT);
        Assert.assertTrue("Spanish ETag exists", spanishETag.length() > 0);

    }


    @Test
    public void givenServerOnline_whenPhetContentScraped_thenShouldConvertAndDownload() throws IOException {
        File tmpDir = Files.createTempDirectory("testphetcontentscraper").toFile();
        File containerDir = Files.createTempDirectory("container").toFile();

        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher);

        String mockServerUrl = mockWebServer.url("/en/api/simulation/equality-explorer-two-variables").toString();
        PhetContentScraper scraper = new PhetContentScraper(mockServerUrl, tmpDir, containerDir);
        scraper.scrapeContent();

        AssertAllFiles(tmpDir, scraper);
    }

    @Test
    public void givenServerOnline_whenPhetContentScrapedAgain_thenShouldNotDownloadFilesAgain() throws IOException {
        File tmpDir = Files.createTempDirectory("testphetcontentscraper").toFile();
        File containerDir = Files.createTempDirectory("container").toFile();

        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher);

        String mockServerUrl = mockWebServer.url("/en/api/simulation/equality-explorer-two-variables").toString();
        PhetContentScraper scraper = new PhetContentScraper(mockServerUrl, tmpDir, containerDir);
        scraper.scrapeContent();
        File englishLocation = new File(tmpDir, "en");
        File titleDirectory = new File(englishLocation, scraper.getTitle());
        File englishSimulation = new File(titleDirectory, SIM_EN);

        long firstSimDownload = englishSimulation.lastModified();

        scraper.scrapeContent();

        long lastModified = englishSimulation.lastModified();

        Assert.assertEquals("didnt download 2nd time", firstSimDownload, lastModified);

    }

    @Test(expected = IllegalArgumentException.class)
    public void givenServerOnline_whenPhetContentScraped_thenShouldThrowIllegalArgumentJarNotSupported() throws IOException {
        File tmpDir = Files.createTempDirectory("testphetcontentscraper").toFile();
        File containerDir = Files.createTempDirectory("container").toFile();

        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher);

        PhetContentScraper scraper = new PhetContentScraper(mockWebServer.url("/legacy/jar").toString(), tmpDir, containerDir);
        scraper.scrapeContent();

    }

    @Test(expected = IllegalArgumentException.class)
    public void givenServerOnline_whenPhetContentScraped_thenShouldThrowIllegalArgumentFlashNotSupported() throws IOException {
        File tmpDir = Files.createTempDirectory("testphetcontentscraper").toFile();
        File containerDir = Files.createTempDirectory("container").toFile();

        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher);

        PhetContentScraper scraper = new PhetContentScraper(mockWebServer.url("/legacy/flash").toString(), tmpDir, containerDir);
        scraper.scrapeContent();

    }

    @Test
    public void givenServerOnline_whenUrlFound_findAllSimulations() throws IOException {

        UmAppDatabase db = UmAppDatabase.getInstance(null);
        UmAppDatabase repo = db.getRepository("https://localhost", "");
        db.clearAllTables();

        IndexPhetContentScraper index = new IndexPhetContentScraper();
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher);

        File tmpDir = Files.createTempDirectory("testphetindexscraper").toFile();
        File containerDir = Files.createTempDirectory("container").toFile();

        index.findContent(mockWebServer.url(PHET_MAIN_CONTENT).toString(), tmpDir, containerDir);

        ContentEntryDao contentEntryDao = repo.getContentEntryDao();
        ContentEntryParentChildJoinDao parentChildDaoJoin = repo.getContentEntryParentChildJoinDao();
        ContentEntryContentCategoryJoinDao categoryJoinDao = repo.getContentEntryContentCategoryJoinDao();
        ContentEntryRelatedEntryJoinDao relatedJoin = repo.getContentEntryRelatedEntryJoinDao();

        String urlPrefix = "http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort();

        ContentEntry parentEntry = contentEntryDao.findBySourceUrl("https://phet.colorado.edu/");
        Assert.assertEquals("Main parent content entry exsits", true, parentEntry.getEntryId().equalsIgnoreCase("https://phet.colorado.edu/"));

        ContentEntry categoryEntry = contentEntryDao.findBySourceUrl(urlPrefix + "/en/simulations/category/math");
        ContentEntryParentChildJoin parentChildJoinEntry = parentChildDaoJoin.findParentByChildUuids(categoryEntry.getContentEntryUid());
        Assert.assertEquals("Category Math entry exists", true, parentChildJoinEntry.getCepcjParentContentEntryUid() == parentEntry.getContentEntryUid());

        ContentEntry englishSimulationEntry = contentEntryDao.findBySourceUrl(urlPrefix + "/en/api/simulation/test");
        Assert.assertEquals("Simulation entry english exists", true, englishSimulationEntry.getEntryId().equalsIgnoreCase("/en/api/simulation/test"));

        List<ContentEntryParentChildJoin> categorySimulationEntryLists = parentChildDaoJoin.findListOfParentsByChildUuid(englishSimulationEntry.getContentEntryUid());
        boolean hasMathCategory = false;
        for (ContentEntryParentChildJoin category : categorySimulationEntryLists) {

            if (category.getCepcjParentContentEntryUid() == categoryEntry.getContentEntryUid()) {
                hasMathCategory = true;
                break;
            }
        }
        Assert.assertEquals("Parent child join between category and simulation exists", true, hasMathCategory);

        ContentEntry spanishEntry = contentEntryDao.findBySourceUrl(urlPrefix + "/es/api/simulation/test");
        Assert.assertEquals("Simulation entry spanish exists", true, spanishEntry.getEntryId().equalsIgnoreCase("/es/api/simulation/test"));

        ContentEntryRelatedEntryJoin spanishEnglishJoin = relatedJoin.findPrimaryByTranslation(spanishEntry.getContentEntryUid());
        Assert.assertEquals("Related Join with Simulation Exists - Spanish Match", true, spanishEnglishJoin.getCerejRelatedEntryUid() == spanishEntry.getContentEntryUid());
        Assert.assertEquals("Related Join with Simulation Exists - English Match", true, spanishEnglishJoin.getCerejContentEntryUid() == englishSimulationEntry.getContentEntryUid());

    }


    @Test
    public void givenDirectoryOfTranslationsIsCreated_findAllTranslationRelations() throws IOException {

        UmAppDatabase db = UmAppDatabase.getInstance(null);
        UmAppDatabase repo = db.getRepository("https://localhost", "");
        db.clearAllTables();

        File tmpDir = Files.createTempDirectory("testphetcontentscraper").toFile();
        File containerDir = Files.createTempDirectory("container").toFile();

        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher);

        PhetContentScraper scraper = new PhetContentScraper(mockWebServer.url("/en/api/simulation/equality-explorer-two-variables").toString(), tmpDir, containerDir);
        scraper.scrapeContent();

        ArrayList<ContentEntry> translationList = scraper.getTranslations(tmpDir, repo.getContentEntryDao(), "", repo.getLanguageDao(), db.getLanguageVariantDao());

    }

}
