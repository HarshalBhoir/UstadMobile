package com.ustadmobile.lib.contentscrapers.ddl;

import com.ustadmobile.core.db.UmAppDatabase;
import com.ustadmobile.core.db.dao.ContainerDao;
import com.ustadmobile.core.db.dao.ContentCategoryDao;
import com.ustadmobile.core.db.dao.ContentCategorySchemaDao;
import com.ustadmobile.core.db.dao.ContentEntryDao;
import com.ustadmobile.core.db.dao.LanguageDao;
import com.ustadmobile.lib.contentscrapers.ContentScraperUtil;
import com.ustadmobile.lib.contentscrapers.ScraperConstants;
import com.ustadmobile.lib.contentscrapers.UMLogUtil;
import com.ustadmobile.lib.db.entities.ContentCategory;
import com.ustadmobile.lib.db.entities.ContentCategorySchema;
import com.ustadmobile.lib.db.entities.ContentEntry;
import com.ustadmobile.lib.db.entities.Language;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;

import static com.ustadmobile.lib.contentscrapers.ScraperConstants.EMPTY_STRING;
import static com.ustadmobile.lib.contentscrapers.ScraperConstants.REQUEST_HEAD;
import static com.ustadmobile.lib.contentscrapers.ddl.IndexDdlContent.DDL;
import static com.ustadmobile.lib.db.entities.ContentEntry.LICENSE_TYPE_CC_BY;


/**
 * Once the resource page is opened.
 * You can download the list of files by searching with css selector - span.download-item a[href]
 * The url may contain spaces and needs to be encoded. This is done by constructing the url into a uri
 * Check if the file was downloaded before with etag or last modified
 * Create the content entry
 */
public class DdlContentScraper {

    private final String urlString;
    private final File destinationDirectory;
    private final URL url;
    private final ContentEntryDao contentEntryDao;
    private final ContentCategorySchemaDao categorySchemaDao;
    private final ContentCategoryDao contentCategoryDao;
    private final LanguageDao languageDao;
    private final ContainerDao containerDao;
    private final UmAppDatabase db;
    private final UmAppDatabase repository;
    private final File containerDir;
    private final Language language;
    private Document doc;
    ContentEntry contentEntry;

    public DdlContentScraper(String url, File destination, File containerDir, String lang) throws MalformedURLException {
        this.urlString = url;
        this.destinationDirectory = destination;
        this.containerDir = containerDir;
        this.url = new URL(url);
        destinationDirectory.mkdirs();
        db = UmAppDatabase.getInstance(null);
        repository = db.getRepository("https://localhost", "");
        contentEntryDao = repository.getContentEntryDao();
        categorySchemaDao = repository.getContentCategorySchemaDao();
        contentCategoryDao = repository.getContentCategoryDao();
        languageDao = repository.getLanguageDao();
        containerDao = repository.getContainerDao();
        language = ContentScraperUtil.insertOrUpdateLanguageByTwoCode(languageDao, lang);
    }


    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: <ddl website url> <file destination><container destination><lang en or fa or ps><optional log{trace, debug, info, warn, error, fatal}>");
            System.exit(1);
        }
        UMLogUtil.setLevel(args.length == 4 ? args[3] : "");
        UMLogUtil.logInfo(args[0]);
        UMLogUtil.logInfo(args[1]);
        try {
            new DdlContentScraper(args[0], new File(args[1]), new File(args[2]), args[3]).scrapeContent();
        } catch (IOException e) {
            UMLogUtil.logError(ExceptionUtils.getStackTrace(e));
            UMLogUtil.logError("Exception running scrapeContent ddl");
        }

    }


    public void scrapeContent() throws IOException {

        File resourceFolder = new File(destinationDirectory, FilenameUtils.getBaseName(urlString));
        resourceFolder.mkdirs();

        doc = Jsoup.connect(urlString).get();

        Elements downloadList = doc.select("span.download-item a[href]");

        Element imgTag = doc.selectFirst("aside img");
        String thumbnail = imgTag != null ? imgTag.attr("src") : EMPTY_STRING;

        String description = doc.selectFirst("meta[name=description]").attr("content");
        Element authorTag = doc.selectFirst("article.resource-view-details h3:contains(Author) ~ p");
        Element farsiAuthorTag = doc.selectFirst("article.resource-view-details h3:contains(نویسنده) ~ p");
        Element pashtoAuthorTag = doc.selectFirst("article.resource-view-details h3:contains(لیکونکی) ~ p");
        String author = authorTag != null ? authorTag.text() :
                farsiAuthorTag != null ? farsiAuthorTag.text() :
                        pashtoAuthorTag != null ? pashtoAuthorTag.text() : EMPTY_STRING;
        Element publisherTag = doc.selectFirst("article.resource-view-details a[href*=publisher]");
        String publisher = publisherTag != null ? publisherTag.text() : EMPTY_STRING;


        contentEntry = ContentScraperUtil.createOrUpdateContentEntry(urlString, doc.title(),
                urlString, (publisher != null && !publisher.isEmpty() ? publisher : DDL),
                LICENSE_TYPE_CC_BY, language.getLangUid(), null, description, true, author,
                thumbnail, EMPTY_STRING, EMPTY_STRING, contentEntryDao);

        for (int downloadCount = 0; downloadCount < downloadList.size(); downloadCount++) {

            Element downloadItem = downloadList.get(downloadCount);
            String href = downloadItem.attr("href");
            File modifiedFile = getEtagOrModifiedFile(resourceFolder, FilenameUtils.getBaseName(FilenameUtils.getName(href)));
            HttpURLConnection conn = null;
            try {
                URL fileUrl = new URL(url, href);

                // this was done to encode url that had empty spaces in the name or other illegal characters
                URI uri = new URI(fileUrl.getProtocol(), fileUrl.getUserInfo(), fileUrl.getHost(), fileUrl.getPort(), fileUrl.getPath(), fileUrl.getQuery(), fileUrl.getRef());

                conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestMethod(REQUEST_HEAD);
                File resourceFile = new File(resourceFolder, FilenameUtils.getName(href));
                String mimeType = Files.probeContentType(resourceFile.toPath());

                boolean isUpdated = ContentScraperUtil.isFileModified(conn, resourceFolder, FilenameUtils.getName(href));

                isUpdated = true;

                if (!isUpdated) {
                    continue;
                }

                FileUtils.copyURLToFile(uri.toURL(), resourceFile);

                ContentScraperUtil.insertContainer(containerDao, contentEntry, true, mimeType,
                        resourceFile.lastModified(), resourceFile, db, repository, containerDir);

            } catch (Exception e) {
                UMLogUtil.logError("Error downloading resource from url " + url + "/" + href);
                if (modifiedFile != null) {
                    ContentScraperUtil.deleteFile(modifiedFile);
                }

            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
    }

    private File getEtagOrModifiedFile(File resourceFolder, String name) {
        File eTag = new File(resourceFolder, name + ScraperConstants.ETAG_TXT);
        if (ContentScraperUtil.fileHasContent(eTag)) {
            return eTag;
        }
        File modified = new File(resourceFolder, name + ScraperConstants.LAST_MODIFIED_TXT);
        if (ContentScraperUtil.fileHasContent(modified)) {
            return modified;
        }
        return null;
    }

    protected ContentEntry getContentEntries() {
        return contentEntry;
    }

    public ArrayList<ContentEntry> getParentSubjectAreas() {

        ArrayList<ContentEntry> subjectAreaList = new ArrayList<>();
        Elements subjectContainer = doc.select("article.resource-view-details a[href*=subject_area]");

        Elements subjectList = subjectContainer.select("a");
        for (Element subject : subjectList) {

            String title = subject.attr("title");
            String href = subject.attr("href");

            ContentEntry contentEntry = ContentScraperUtil.createOrUpdateContentEntry(href, title, href,
                    DDL, LICENSE_TYPE_CC_BY, language.getLangUid(), null,
                    EMPTY_STRING, false, EMPTY_STRING, EMPTY_STRING,
                    EMPTY_STRING, EMPTY_STRING, contentEntryDao);

            subjectAreaList.add(contentEntry);

        }

        return subjectAreaList;
    }


    public ArrayList<ContentCategory> getContentCategories() {


        ArrayList<ContentCategory> categoryRelations = new ArrayList<>();
        Elements subjectContainer = doc.select("article.resource-view-details a[href*=level]");

        ContentCategorySchema ddlSchema = ContentScraperUtil.insertOrUpdateSchema(categorySchemaDao, "DDL Resource Level", "ddl/resource-level/");

        Elements subjectList = subjectContainer.select("a");
        for (Element subject : subjectList) {

            String title = subject.attr("title");

            ContentCategory categoryEntry = ContentScraperUtil.insertOrUpdateCategoryContent(contentCategoryDao, ddlSchema, title);

            categoryRelations.add(categoryEntry);

        }

        return categoryRelations;
    }
}
