package com.ustadmobile.lib.contentscrapers.ddl;

import com.ustadmobile.core.db.UmAppDatabase;
import com.ustadmobile.core.db.dao.ContentEntryContentCategoryJoinDao;
import com.ustadmobile.core.db.dao.ContentEntryDao;
import com.ustadmobile.core.db.dao.ContentEntryParentChildJoinDao;
import com.ustadmobile.core.db.dao.LanguageDao;
import com.ustadmobile.lib.contentscrapers.ContentScraperUtil;
import com.ustadmobile.lib.contentscrapers.LanguageList;
import com.ustadmobile.lib.contentscrapers.UMLogUtil;
import com.ustadmobile.lib.db.entities.ContentCategory;
import com.ustadmobile.lib.db.entities.ContentEntry;
import com.ustadmobile.lib.db.entities.Language;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static com.ustadmobile.lib.contentscrapers.ScraperConstants.EMPTY_STRING;
import static com.ustadmobile.lib.contentscrapers.ScraperConstants.ROOT;
import static com.ustadmobile.lib.contentscrapers.ScraperConstants.USTAD_MOBILE;
import static com.ustadmobile.lib.db.entities.ContentEntry.LICENSE_TYPE_CC_BY;


/**
 * The DDL Website comes in 3 languages - English, Farsi and Pashto
 * To scrape all content, we would need to go to each page and traverse the list
 * First we find our the max number of pages for each language by using the css selector on a.page-link
 * Once we found the max number, open each page on ddl website with the parameters /resources/list?page= and the page number until you hit the max
 * <p>
 * Every resource is found by searching the html with a[href] and checking if href url contains "resource/"
 * Traverse all the pages until you hit Max number and then move to next language
 */
public class IndexDdlContent {


    static final String DDL = "DDL";
    private File destinationDirectory;

    private int maxNumber;
    private ContentEntry parentDdl;
    private ContentEntry langEntry;
    private int langCount = 0;
    private ContentEntryDao contentEntryDao;
    private ContentEntryParentChildJoinDao contentParentChildJoinDao;
    private ContentEntryContentCategoryJoinDao contentCategoryChildJoinDao;
    private LanguageDao languageDao;
    private File containerDir;


    public static void main(String[] args) {

        if (args.length < 3) {
            System.err.println("Usage:<file destination><container destination><optional log{trace, debug, info, warn, error, fatal}>");
            System.exit(1);
        }

        UMLogUtil.setLevel(args.length == 3 ? args[2] : "");

        UMLogUtil.logTrace(args[0]);
        UMLogUtil.logTrace(args[1]);
        try {
            new IndexDdlContent().findContent(new File(args[0]), new File(args[1]));
        } catch (Exception e) {
            UMLogUtil.logFatal(ExceptionUtils.getStackTrace(e));
            UMLogUtil.logFatal("Exception running findContent DDL Scraper");
        }
    }


    public void findContent(File destinationDir, File containerDir) throws IOException {

        destinationDir.mkdirs();
        destinationDirectory = destinationDir;
        containerDir.mkdirs();
        this.containerDir = containerDir;

        UmAppDatabase db = UmAppDatabase.getInstance(null);
        UmAppDatabase repository = db.getRepository("https://localhost", "");
        contentEntryDao = repository.getContentEntryDao();
        contentParentChildJoinDao = repository.getContentEntryParentChildJoinDao();
        contentCategoryChildJoinDao = repository.getContentEntryContentCategoryJoinDao();
        languageDao = repository.getLanguageDao();

        new LanguageList().addAllLanguages();

        Language englishLang = ContentScraperUtil.insertOrUpdateLanguageByName(languageDao, "English");
        Language farsiLang = ContentScraperUtil.insertOrUpdateLanguageByName(languageDao, "Persian");
        Language pashtoLang = ContentScraperUtil.insertOrUpdateLanguageByName(languageDao, "Pashto");


        ContentEntry masterRootParent = ContentScraperUtil.createOrUpdateContentEntry(ROOT, USTAD_MOBILE,
                ROOT, USTAD_MOBILE, LICENSE_TYPE_CC_BY, englishLang.getLangUid(), null,
                EMPTY_STRING, false, EMPTY_STRING, EMPTY_STRING,
                EMPTY_STRING, EMPTY_STRING, contentEntryDao);


        parentDdl = ContentScraperUtil.createOrUpdateContentEntry("https://www.ddl.af/", "Darakht-e Danesh",
                "https://www.ddl.af/", DDL, LICENSE_TYPE_CC_BY, englishLang.getLangUid(), null,
                "Free and open educational resources for Afghanistan", false, EMPTY_STRING,
                "https://ddl.af/storage/files/logo-dd.png", EMPTY_STRING, EMPTY_STRING, contentEntryDao);


        ContentScraperUtil.insertOrUpdateParentChildJoin(contentParentChildJoinDao, masterRootParent, parentDdl, 5);

        UMLogUtil.logTrace("browse English");
        browseLanguages("en", englishLang);
        UMLogUtil.logTrace("browse Farsi");
        browseLanguages("fa", farsiLang);
        UMLogUtil.logTrace("browse Pashto");
        browseLanguages("ps", pashtoLang);

    }

    private void browseLanguages(String lang, Language langEntity) throws IOException {

        Document document = Jsoup.connect("https://www.ddl.af/" + lang + "/resources/list")
                .header("X-Requested-With", "XMLHttpRequest").get();

        Elements pageList = document.select("a.page-link");

        langEntry = ContentScraperUtil.createOrUpdateContentEntry(lang + "/resources/list", langEntity.getName(),
                "https://www.ddl.af/" + lang + "/resources/list", DDL, LICENSE_TYPE_CC_BY, langEntity.getLangUid(), null,
                EMPTY_STRING, false, EMPTY_STRING, EMPTY_STRING,
                EMPTY_STRING, EMPTY_STRING, contentEntryDao);

        ContentScraperUtil.insertOrUpdateParentChildJoin(contentParentChildJoinDao, parentDdl, langEntry, langCount);

        maxNumber = 0;
        for (Element page : pageList) {

            String num = page.text();
            try {
                int number = Integer.parseInt(num);
                if (number > maxNumber) {
                    maxNumber = number;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        UMLogUtil.logTrace("max number of pages: " + maxNumber);

        browseList(lang, 1);
        langCount++;
    }

    private void browseList(String lang, int count) throws IOException {

        if (count > maxNumber) {
            return;
        }
        UMLogUtil.logTrace("starting page: " + count);
        Document document = Jsoup.connect("https://www.ddl.af/" + lang + "/resources/list?page=" + count)
                .header("X-Requested-With", "XMLHttpRequest").get();

        Elements resourceList = document.select("article a[href]");
        UMLogUtil.logTrace("found " + resourceList.size() + " articles to download");
        for (Element resource : resourceList) {

            String url = resource.attr("href");
            if (url.contains("resource/")) {

                DdlContentScraper scraper = new DdlContentScraper(url, destinationDirectory, containerDir, lang);
                try {
                    scraper.scrapeContent();
                    UMLogUtil.logTrace("scraped url: " + url);
                    ArrayList<ContentEntry> subjectAreas = scraper.getParentSubjectAreas();
                    ContentEntry contentEntry = scraper.getContentEntries();
                    ArrayList<ContentCategory> contentCategories = scraper.getContentCategories();
                    int subjectAreaCount = 0;
                    UMLogUtil.logTrace("found " + subjectAreas.size() + " subjects in entry");
                    UMLogUtil.logTrace("found " + contentCategories.size() + " categories in entry");
                    for (ContentEntry subjectArea : subjectAreas) {

                        ContentScraperUtil.insertOrUpdateParentChildJoin(contentParentChildJoinDao,
                                langEntry, subjectArea, subjectAreaCount++);

                        ContentScraperUtil.insertOrUpdateChildWithMultipleParentsJoin(contentParentChildJoinDao,
                                subjectArea, contentEntry, 0);
                    }
                    for (ContentCategory category : contentCategories) {

                        ContentScraperUtil.insertOrUpdateChildWithMultipleCategoriesJoin(
                                contentCategoryChildJoinDao, category, contentEntry);

                    }


                } catch (IOException e) {
                    UMLogUtil.logError("Error downloading resource at " + url);
                    UMLogUtil.logError(ExceptionUtils.getStackTrace(e));
                }

            }


        }

        browseList(lang, ++count);

    }

}
