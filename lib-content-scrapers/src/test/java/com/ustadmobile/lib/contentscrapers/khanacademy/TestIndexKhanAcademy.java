package com.ustadmobile.lib.contentscrapers.khanacademy;

import com.ustadmobile.core.db.UmAppDatabase;
import com.ustadmobile.core.db.dao.ContentEntryDao;
import com.ustadmobile.core.db.dao.ContentEntryParentChildJoinDao;
import com.ustadmobile.core.db.dao.ScrapeQueueItemDao;
import com.ustadmobile.core.db.dao.ScrapeRunDao;
import com.ustadmobile.lib.db.entities.ContentEntry;
import com.ustadmobile.lib.db.entities.ContentEntryParentChildJoin;
import com.ustadmobile.lib.db.entities.ScrapeRun;

import org.apache.commons.io.IOUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import okio.BufferedSource;
import okio.Okio;

import static com.ustadmobile.lib.contentscrapers.ScraperConstants.UTF_ENCODING;
import static com.ustadmobile.lib.contentscrapers.ScraperConstants.ZIP_EXT;

public class TestIndexKhanAcademy {

    final Dispatcher dispatcher = new Dispatcher() {
        @Override
        public MockResponse dispatch(RecordedRequest request) {

            try {

                if (request.getPath().contains("json")) {

                    String fileName = request.getPath().substring(5
                    );
                    String body = IOUtils.toString(getClass().getResourceAsStream(fileName), UTF_ENCODING);
                    return new MockResponse().setBody(body);

                } else if (request.getPath().contains("content")) {

                    String fileLocation = request.getPath().substring(8
                    );
                    InputStream videoIn = getClass().getResourceAsStream(fileLocation);
                    BufferedSource source = Okio.buffer(Okio.source(videoIn));
                    Buffer buffer = new Buffer();
                    source.readAll(buffer);

                    return new MockResponse().setResponseCode(200).setHeader("ETag", "ABC").setBody(buffer);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return new MockResponse().setResponseCode(404);
        }
    };
    private MockWebServer mockWebServer;
    private ChromeDriver driver;
    private GenericObjectPool<ChromeDriver> factory;

    @Before
    public void setUpDriver() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher);

        factory = new GenericObjectPool<>(new KhanDriverFactory());
        driver = factory.borrowObject();
    }

    @After
    public void closeDriver() {
        driver.close();
        driver.quit();
        factory.close();
    }


    @Test
    public void givenServerOnline_whenKhanContentScraped_thenShouldConvertAndDownloadAllFiles() throws IOException {

        UmAppDatabase db = UmAppDatabase.getInstance(null);
        UmAppDatabase repo = db.getRepository("https://localhost", "");
        ScrapeRunDao runDao = db.getScrapeRunDao();
        ScrapeRun run = new ScrapeRun();
        run.setScrapeRunUid(999);
        run.setScrapeType("Khan-Test");
        run.setStatus(ScrapeQueueItemDao.STATUS_PENDING);
        runDao.insert(run);

        File tmpDir = Files.createTempDirectory("testIndexKhancontentscraper").toFile();

        KhanContentIndexer.startScrape(mockWebServer.
                        url("/json/com/ustadmobile/lib/contentscrapers/khanacademy/mainpage.txt").toString(),
                tmpDir, run.getScrapeRunUid());

        File englishFolder = new File(tmpDir, "en");
        Assert.assertEquals(true, englishFolder.isDirectory());

        File courseFolder = new File(englishFolder, "x9b4a5e7a");
        Assert.assertEquals(true, courseFolder.isDirectory());

        File directoryForCourse = new File(courseFolder, "x9b4a5e7a");
        Assert.assertEquals(true, courseFolder.isDirectory());

        File videoFile = new File(directoryForCourse, "video.mp4");
        Assert.assertEquals(true, videoFile.isFile());

        ContentEntryDao contentEntryDao = repo.getContentEntryDao();
        ContentEntryParentChildJoinDao parentChildDaoJoin = repo.getContentEntryParentChildJoinDao();

        ContentEntry parentEntry = contentEntryDao.findBySourceUrl("https://www.khanacademy.org/");
        Assert.assertEquals("Main parent content entry exsits", true, parentEntry.getEntryId().equalsIgnoreCase("https://www.khanacademy.org/"));

        ContentEntry subjectEntry = contentEntryDao.findBySourceUrl(mockWebServer.url("/json/com/ustadmobile/lib/contentscrapers/khanacademy/topicspage.txt").toString());
        ContentEntryParentChildJoin parentChildJoinEntry = parentChildDaoJoin.findParentByChildUuids(subjectEntry.getContentEntryUid());
        Assert.assertEquals(true, parentChildJoinEntry.getCepcjParentContentEntryUid() == parentEntry.getContentEntryUid());

        ContentEntry gradeEntry = contentEntryDao.findBySourceUrl(mockWebServer.url("/json/com/ustadmobile/lib/contentscrapers/khanacademy/subjectpage.txt").toString());
        ContentEntryParentChildJoin gradeSubjectJoin = parentChildDaoJoin.findParentByChildUuids(gradeEntry.getContentEntryUid());
        Assert.assertEquals(true, gradeSubjectJoin.getCepcjParentContentEntryUid() == subjectEntry.getContentEntryUid());

        ContentEntry headingTopicEntry = contentEntryDao.findBySourceUrl(mockWebServer.url("/json/com/ustadmobile/lib/contentscrapers/khanacademy/coursespage.txt").toString());
        ContentEntryParentChildJoin subjectHeadingJoin = parentChildDaoJoin.findParentByChildUuids(headingTopicEntry.getContentEntryUid());
        Assert.assertEquals(true, subjectHeadingJoin.getCepcjParentContentEntryUid() == gradeEntry.getContentEntryUid());


    }

    @Test
    public void givenServerOnline_whenKhanVideoContentScrapedAGAIN_thenShouldNotDownloadFilesAgain() throws Exception {

        File tmpDir = Files.createTempDirectory("testIndexKhancontentscraper").toFile();

        KhanContentScraper scraper = new KhanContentScraper(tmpDir, driver);
        scraper.scrapeVideoContent(mockWebServer.url("/content/com/ustadmobile/lib/contentscrapers/files/video.mp4").toString());

        long firstDownloadTime = new File(tmpDir, tmpDir.getName() + ZIP_EXT).lastModified();
        //now run scrapeContent again...

        scraper.scrapeVideoContent(mockWebServer.url("/content/com/ustadmobile/lib/contentscrapers/files/video.mp4").toString());

        long lastModified = new File(tmpDir, tmpDir.getName() + ZIP_EXT).lastModified();
        //Assert that last modified dates are lower than firstDownloadCompleteTime
        Assert.assertTrue(lastModified == firstDownloadTime);

    }




}