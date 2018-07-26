package com.ustadmobile.lib.contentscrapers;

import com.google.gson.GsonBuilder;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Okio;
import static com.ustadmobile.lib.contentscrapers.ScraperConstants.*;

public class TestEdraakContentScraper {

    private final String DETAIL_COMPONENT_ID = "5a608cc76380a6049b33feb6";

    final String MAIN_CONTENT_ID = "5a6087f46380a6049b33fc19";

    private final String MALFORMED_COMPONENT_ID = "eada";

    private final String DETAIL_JSON_CONTENT_FILE = "/com/ustadmobile/lib/contentscrapers/edraak-detail.txt";

    private final String MAIN_CONTENT_CONTENT_FILE = "/com/ustadmobile/lib/contentscrapers/edraak-main-content.txt";

    private final String MAIN_DETAIL_WITHOUT_TARGET_FILE = "/com/ustadmobile/lib/contentscrapers/edraak-detail-without-target.txt";

    private final String MAIN_DETAIL_WITHOUT_CHILDREN_FILE = "/com/ustadmobile/lib/contentscrapers/edraak-detail-without-children.txt";

    private final String MAIN_DETAIL_NO_VIDEO_FOUND = "/com/ustadmobile/lib/contentscrapers/edraak-detail-no-video-info.txt";

    private final String MAIN_DETAIL_NO_QUESTIONS_FOUND = "/com/ustadmobile/lib/contentscrapers/edraak-detail-no-question-set-children.txt";

    private final String VIDEO_LOCATION_FILE = "/com/ustadmobile/lib/contentscrapers/video.mp4";

    final String COMPONENT_API_PREFIX = "/api/component/";

    final Dispatcher dispatcher = new Dispatcher() {
        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {


            try {

                if (request.getPath().startsWith(COMPONENT_API_PREFIX)) {

                    int prefixLength = COMPONENT_API_PREFIX.length();
                    String fileName = request.getPath().substring(prefixLength,
                            request.getPath().indexOf(".txt", prefixLength));
                    return new MockResponse().setBody(IOUtils.toString(getClass().getResourceAsStream(fileName + ".txt"), UTF_ENCODING));

                } else if (request.getPath().equals("/media/video.mp4")) {
                    InputStream videoIn = getClass().getResourceAsStream(VIDEO_LOCATION_FILE);
                    return new MockResponse().setResponseCode(200).setBody(Okio.buffer(Okio.source(videoIn)).buffer());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return new MockResponse().setResponseCode(404);
        }
    };


    @Test
    public void givenServerOnline_whenEdXContentScraped_thenShouldConvertAndDownload() throws IOException{
        EdraakContentScraper scraper = new EdraakContentScraper();
        File tmpDir = Files.createTempDirectory("testedxcontentscraper").toFile();
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher);
        scraper.convert(DETAIL_JSON_CONTENT_FILE, 41, mockWebServer.url("/api/").toString(),
                tmpDir);

        File jsonFile = new File(tmpDir, CONTENT_JSON);
        Assert.assertTrue("Downloaded content info json exists", jsonFile.exists());
        String jsonStr = new String(Files.readAllBytes(jsonFile.toPath()), "UTF-8");
        ContentResponse gsonContent = new GsonBuilder().disableHtmlEscaping().create().fromJson(jsonStr,ContentResponse.class);
        Assert.assertNotNull("Created Gson POJO Object", gsonContent);

        if(ComponentType.ONLINE.getType().equalsIgnoreCase(gsonContent.target_component.component_type)){
            Assert.assertTrue("Downloaded video exists", new File(tmpDir, VIDEO_MP4).exists());
        }

        Assert.assertTrue("Downloaded Questions json exist", new File(tmpDir, QUESTIONS_JSON).exists());
        Assert.assertTrue("Downloaded zip exists", new File(tmpDir.getParent(), gsonContent.id + ".zip").exists());

        //add assertions that the content info and video info are present in the JSON
        List<ContentResponse> tests = gsonContent.target_component.children;

        int videoCount = 0, questionSet = 0;
        if(ComponentType.TEST.getType().equalsIgnoreCase(gsonContent.target_component.component_type)){

            ContentResponse questionList = gsonContent.target_component.question_set;
            if(questionList != null){
                questionSet++;
            }

        }else if(ComponentType.ONLINE.getType().equalsIgnoreCase(gsonContent.target_component.component_type)){

            for(ContentResponse content: tests){
                if(ComponentType.VIDEO.getType().equalsIgnoreCase(content.component_type)){

                    videoCount++;
                    Assert.assertTrue("Video info content is not empty", !content.video_info.url.isEmpty());

                }else if(QUESTION_SET_HOLDER_TYPES.contains(content.component_type)){

                    questionSet++;
                    //load JSON classes - assert that the quiz exists, and has > 0 questinos
                    Assert.assertNotNull("Has Questions Set",content.question_set);
                    Assert.assertTrue("Has more than 1 question", content.question_set.children.size() > 0);

                }
            }

        }



        if(ComponentType.ONLINE.getType().equalsIgnoreCase(gsonContent.target_component.component_type)){
            Assert.assertEquals("Found 1 video", 1, videoCount);
        }


        Assert.assertEquals("Found 1 question set", 1, questionSet);

    }

    @Test(expected = IllegalArgumentException.class)
    public void givenNotImportedContent_whenEdXContentScraped_thenShouldThrowIllegalArgumentException() throws IOException {
        EdraakContentScraper scraper = new EdraakContentScraper();

        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher);
        scraper.convert(MAIN_CONTENT_CONTENT_FILE, 41, mockWebServer.url("/api/").toString(),
                    null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenNullTargetComponent_whenEdXContentScraped_thenShouldThrowIllegalArgumentException() throws IOException {

        EdraakContentScraper scraper = new EdraakContentScraper();
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher);

        scraper.convert(MAIN_DETAIL_WITHOUT_TARGET_FILE, 41, mockWebServer.url("/api/").toString(),
                    null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenNullTargetComponentChildren_whenEdXContentScraped_thenShouldThrowIllegalArgumentException() throws IOException{
        EdraakContentScraper scraper = new EdraakContentScraper();
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher);
        scraper.convert(MAIN_DETAIL_WITHOUT_CHILDREN_FILE, 41, mockWebServer.url("/api/").toString(),
                    null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEncodedVideoListIsEmpty_whenEdXContentScraped_thenShouldThrowIllegalArgumentException() throws IOException {
        EdraakContentScraper scraper = new EdraakContentScraper();
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher);
        scraper.convert(MAIN_DETAIL_NO_VIDEO_FOUND, 41, mockWebServer.url("/api/").toString(),
                    null);

    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyQuestionSet_whenEdXContentScraped_thenShouldThrowIOException() throws IOException {

        EdraakContentScraper scraper = new EdraakContentScraper();
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher);
        scraper.convert(MAIN_DETAIL_NO_QUESTIONS_FOUND, 41, mockWebServer.url("/api/").toString(),
                null);

    }


    @Test(expected = IllegalArgumentException.class)
    public void givenMalformedContent_whenEdXContentScaped_thenShouldThrowIllegalArgumentException() throws IOException{
            EdraakContentScraper scraper = new EdraakContentScraper();

            File tmpDir = Files.createTempDirectory("testedxcontentscraper").toFile();

            MockWebServer mockWebServer = new MockWebServer();

            try{
                mockWebServer.enqueue(new MockResponse().setBody("{id"));

                mockWebServer.start();

                scraper.convert(MALFORMED_COMPONENT_ID, 41, mockWebServer.url("/api/").toString(),
                        tmpDir);

            } finally {
                mockWebServer.close();
            }
    }



}
