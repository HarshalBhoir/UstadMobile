package com.ustadmobile.lib.contentscrapers;

import com.ustadmobile.core.contentformats.epub.ocf.OcfDocument;
import com.ustadmobile.core.contentformats.epub.opf.OpfDocument;
import com.ustadmobile.core.contentformats.epub.opf.OpfItem;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;
import com.ustadmobile.core.util.UMFileUtil;
import com.ustadmobile.core.util.UMIOUtils;
import com.ustadmobile.lib.contentscrapers.buildconfig.ScraperBuildConfig;
import com.ustadmobile.port.sharedse.util.UmZipUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.ustadmobile.lib.contentscrapers.ScraperConstants.MIMETYPE_CSS;
import static com.ustadmobile.lib.contentscrapers.ScraperConstants.MIMETYPE_JPG;
import static com.ustadmobile.lib.contentscrapers.ScraperConstants.PNG_EXT;
import static com.ustadmobile.lib.contentscrapers.ScraperConstants.UTF_ENCODING;

public class ShrinkerUtil {

    public static final List<String> HTML_MIME_TYPES = Arrays.asList("application/xhtml+xml", "text/html");
    public static final List<String> IMAGE_MIME_TYPES = Arrays.asList(MIMETYPE_JPG, "image/png", "image/jpeg");


    public static final int STYLE_OUTSOURCE_TO_LINKED_CSS = 0;

    public static final int STYLE_KEEP = 1;

    public static final int STYLE_DROP = 2;

    public interface StyleElementHelper {

        int decide(Element styleElement);

    }

    public interface LinkElementHelper {

        String addLinksToHtml();

    }

    public interface HtmlDocumentEditor {

        Document modifyDoc(Document html);

    }

    public static class EpubShrinkerOptions {

        public StyleElementHelper styleElementHelper;

        public LinkElementHelper linkHelper;

        public HtmlDocumentEditor editor;

    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage:<file location><optional log{trace, debug, info, warn, error, fatal}>");
            System.exit(1);
        }
        UMLogUtil.setLevel(args.length == 2 ? args[1] : "");

        try {
            File epubFile = new File(args[1]);
            ShrinkerUtil.shrinkEpub(epubFile);

        } catch (Exception e) {
            UMLogUtil.logError(ExceptionUtils.getStackTrace(e));
            UMLogUtil.logError("Failed to shrink epub " + args[1]);
        }

    }


    public static File shrinkEpub(File epub) throws IOException {
        File tmpFolder;
        try {
            tmpFolder = createTmpFolderForZipAndUnZip(epub);
            shrinkEpubFiles(tmpFolder, null);
        } catch (IOException e) {
            UMLogUtil.logError(ExceptionUtils.getStackTrace(e));
            throw e;
        }
        return tmpFolder;
    }

    public static File shrinkEpub(File epub, EpubShrinkerOptions options) throws IOException {
        File tmpFolder;
        try {
            tmpFolder = createTmpFolderForZipAndUnZip(epub);
            shrinkEpubFiles(tmpFolder, options);
        } catch (IOException e) {
            UMLogUtil.logError(ExceptionUtils.getStackTrace(e));
            throw e;
        }
        return tmpFolder;
    }


    private static File createTmpFolderForZipAndUnZip(File contentFile) throws IOException {
        File parentFolder = contentFile.getParentFile();
        File tmpFolder = new File(parentFolder, UMFileUtil.INSTANCE.stripExtensionIfPresent(contentFile.getName()));
        UmZipUtils.unzip(contentFile, tmpFolder);
        return tmpFolder;
    }


    public static void cleanXml(File xmlFile) throws IOException {

        try (InputStream is = FileUtils.openInputStream(xmlFile)) {
            Document doc = Jsoup.parse(is, UTF_ENCODING, "", Parser.xmlParser());
            doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
            doc.outputSettings().prettyPrint(false);
            FileUtils.writeStringToFile(xmlFile, doc.toString(), UTF_ENCODING);
        }
    }


    private static boolean shrinkEpubFiles(File directory, EpubShrinkerOptions options) throws IOException {
        FileInputStream opfFileInputStream = null;
        FileInputStream ocfFileInputStream = null;
        FileOutputStream opfFileOutputStream = null;
        try {
            OcfDocument ocfDoc = new OcfDocument();
            File ocfFile = new File(directory, Paths.get("META-INF", "container.xml").toString());
            ocfFileInputStream = new FileInputStream(ocfFile);
            XmlPullParser ocfParser = UstadMobileSystemImpl.Companion.getInstance()
                    .newPullParser(ocfFileInputStream);
            ocfDoc.loadFromParser(ocfParser);

            File opfFile = new File(directory, ocfDoc.getRootFiles().get(0).getFullPath());
            File opfDir = opfFile.getParentFile();

            cleanXml(opfFile);
            OpfDocument document = new OpfDocument();
            opfFileInputStream = new FileInputStream(opfFile);
            XmlPullParser xmlPullParser = UstadMobileSystemImpl.Companion.getInstance()
                    .newPullParser(opfFileInputStream);
            document.loadFromOPF(xmlPullParser);

            Map<String, OpfItem> manifestList = document.getManifestItems();
            Map<File, File> replacedFiles = new HashMap<>();
            Map<String, String> styleMap = new HashMap<>();
            List<OpfItem> newOpfItems = new ArrayList<>();

            for (OpfItem itemValue : manifestList.values()) {

                if (IMAGE_MIME_TYPES.contains(itemValue.getMediaType())) {
                    String oldHrefValue = itemValue.getHref();
                    String newHref = UMFileUtil.INSTANCE.stripExtensionIfPresent(oldHrefValue) +
                            ScraperConstants.WEBP_EXT;

                    File inputFile = new File(opfDir, oldHrefValue);
                    File outputFile = new File(opfDir, newHref);

                    try {
                        convertImageToWebp(inputFile, outputFile);
                    } catch (Exception e) {
                        UMLogUtil.logError(ExceptionUtils.getStackTrace(e));
                        continue;
                    }
                    replacedFiles.put(inputFile, outputFile);

                    if (ContentScraperUtil.fileHasContent(outputFile)) {
                        itemValue.setHref(newHref);
                        itemValue.setMimeType(ScraperConstants.MIMETYPE_WEBP);
                    }
                }
            }

            int countStyle = 0;
            for (OpfItem opfItem : manifestList.values()) {
                if (HTML_MIME_TYPES.contains(opfItem.getMediaType())) {

                    File htmlFile = new File(opfDir, opfItem.getHref());
                    try (FileInputStream htmlFileInputStream = new FileInputStream(htmlFile)) {
                        String html = UMIOUtils.INSTANCE.readToString(htmlFileInputStream, UTF_ENCODING);
                        /*
                         * Pratham uses an entity code to map &nbsp; to &#160; - this confuses jsoup
                         */
                        html = html.replaceAll("&nbsp;", "&#160;");
                        html = html.replaceAll("\\u2029", "");
                        html = html.replace("<!DOCTYPE html[<!ENTITY nbsp \"&#160;\">]>",
                                "<!DOCTYPE html>");

                        Document doc = Jsoup.parse(html, "", Parser.xmlParser());
                        doc.outputSettings().prettyPrint(false);
                        doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml);

                        doc = options != null && options.editor != null ? options.editor.modifyDoc(doc) : doc;

                        if (replacedFiles.size() != 0) {
                            Elements elements = doc.select("[src]");
                            for (Element element : elements) {
                                cleanUpAttributeListWithMultipleSrc(element, replacedFiles, htmlFile);
                            }
                        }
                        Elements styleList = doc.select("style[type=text/css]");
                        for (Element style : styleList) {
                            int styleAction = options != null && options.styleElementHelper != null ?
                                    options.styleElementHelper.decide(style) : STYLE_OUTSOURCE_TO_LINKED_CSS;

                            if (styleAction == STYLE_DROP) {
                                continue;
                            }

                            String cssText = style.text();
                            if (cssText != null && !cssText.isEmpty()) {
                                String pathToStyleFile = styleMap.get(cssText);
                                if (pathToStyleFile == null) {
                                    File styleFile = new File(htmlFile.getParentFile(), "style_" + ++countStyle + ".css");
                                    FileUtils.writeStringToFile(styleFile, cssText, UTF_ENCODING);
                                    pathToStyleFile = Paths.get(htmlFile.getParentFile().toURI())
                                            .relativize(Paths.get(styleFile.toURI()))
                                            .toString().replaceAll(Pattern.quote("\\"), "/");

                                    String pathFromOpfToStyleFile = Paths.get(opfDir.toURI())
                                            .relativize(Paths.get(styleFile.toURI()))
                                            .toString().replaceAll(Pattern.quote("\\"), "/");

                                    OpfItem styleOpf = new OpfItem();
                                    styleOpf.setHref(pathFromOpfToStyleFile);
                                    styleOpf.setMediaType(MIMETYPE_CSS);
                                    styleOpf.setId("style_" + countStyle);
                                    newOpfItems.add(styleOpf);

                                    styleMap.put(cssText, pathToStyleFile);
                                }
                                doc.head().append("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + pathToStyleFile + "\"/>");
                            }
                        }
                        String cssToAdd = options != null && options.linkHelper != null ?
                                options.linkHelper.addLinksToHtml() : null;
                        if (cssToAdd != null) {

                            File cssFile = new File(htmlFile.getParentFile(), "cssHelper.css");
                            FileUtils.writeStringToFile(cssFile, cssToAdd, UTF_ENCODING);
                            String pathToCss = Paths.get(htmlFile.getParentFile().toURI())
                                    .relativize(Paths.get(cssFile.toURI()))
                                    .toString().replaceAll(Pattern.quote("\\"), "/");

                            String pathFromOpfToCssFile = Paths.get(opfDir.toURI())
                                    .relativize(Paths.get(cssFile.toURI()))
                                    .toString().replaceAll(Pattern.quote("\\"), "/");

                            OpfItem styleOpf = new OpfItem();
                            styleOpf.setHref(pathFromOpfToCssFile);
                            styleOpf.setMediaType(MIMETYPE_CSS);
                            styleOpf.setId("cssHelper");

                            newOpfItems.add(styleOpf);

                            doc.head().append("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + pathToCss + "\"/>");
                            styleMap.put(cssToAdd, pathFromOpfToCssFile);
                        }

                        styleList.remove();
                        FileUtils.writeStringToFile(htmlFile, doc.toString(), UTF_ENCODING);
                    }

                }
            }

            for (OpfItem item : newOpfItems) {
                manifestList.put(item.getId(), item);
            }

            if (newOpfItems.size() == 0 && replacedFiles.size() == 0 && styleMap.size() == 0) {
                return false;
            }

            XmlSerializer xmlSerializer = UstadMobileSystemImpl.Companion.getInstance().newXMLSerializer();
            opfFileOutputStream = new FileOutputStream(opfFile);
            xmlSerializer.setOutput(opfFileOutputStream, UTF_ENCODING);
            document.serialize(xmlSerializer);
            opfFileOutputStream.flush();

            for (File replacedFile : replacedFiles.keySet()) {
                if (!replacedFile.delete()) {
                    throw new IllegalStateException("Could not delete: " + replacedFile);
                }
            }
            return true;
        } catch (XmlPullParserException e) {
            UMLogUtil.logError(ExceptionUtils.getStackTrace(e));
            UMLogUtil.logError("Failed to xmlpullparse for directory " + directory.getPath());
        } catch (IOException e) {
            UMLogUtil.logError("IO Exception for directory " + directory.getPath());
            throw e;
        } finally {
            UMIOUtils.INSTANCE.closeQuietly(opfFileInputStream);
            UMIOUtils.INSTANCE.closeQuietly(opfFileOutputStream);
            UMIOUtils.INSTANCE.closeQuietly(ocfFileInputStream);
        }

        return false;
    }

    public static void cleanUpAttributeListWithMultipleSrc(Element element, Map<File, File> replacedFiles, File htmlFile) {
        List<Attribute> attrList = element.attributes().asList();
        boolean foundReplaced = false;
        for (Attribute attr : attrList) {

            if (attr.getKey().contains("src")) {

                try {
                    String srcValue = attr.getValue();
                    File srcFile = new File(htmlFile.getParentFile(), srcValue);
                    srcFile = Paths.get(srcFile.getPath()).normalize().toFile();

                    File newFile = replacedFiles.get(srcFile);
                    if (newFile != null) {
                        foundReplaced = true;
                        String newHref = Paths.get(htmlFile.getParentFile().toURI())
                                .relativize(Paths.get(newFile.toURI()))
                                .toString().replaceAll(Pattern.quote("\\"), "/");

                        deleteAllAttributesWithSrc(element);
                        element.attr("src", newHref);
                        break;
                    }
                } catch (InvalidPathException ignored) {

                }
            }
        }
        if (!foundReplaced) {
            UMLogUtil.logInfo("Did not find the replacement file for " + element.selectFirst("[src]").attr("src"));
        }
    }

    private static void deleteAllAttributesWithSrc(Element element) {
        List<Attribute> attrList = element.attributes().asList();
        ArrayList<String> attrToDelete = new ArrayList<>();
        for (Attribute attribute : attrList) {
            if (attribute.getKey().contains("src")) {
                attrToDelete.add(attribute.getKey());
            }
        }
        for (String attr : attrToDelete) {
            element.removeAttr(attr);
        }

    }

    /**
     * Given a source file and a destination file, convert the image(src) to webp(dest)
     *
     * @param src  file image path
     * @param dest webp file path
     */
    public static void convertImageToWebp(File src, File dest) throws IOException {
        if (!ContentScraperUtil.fileHasContent(src)) {
            throw new FileNotFoundException("convertImageToWebp: Source file: " + src.getAbsolutePath() + " does not exist");
        }

        File webpExecutableFile = new File(ScraperBuildConfig.CWEBP_PATH);
        if (!webpExecutableFile.exists()) {
            throw new IOException("Webp executable does not exist: " + ScraperBuildConfig.CWEBP_PATH);
        }
        File pngFile = null;
        Process process = null;
        ProcessBuilder builder = new ProcessBuilder(ScraperBuildConfig.CWEBP_PATH, src.getPath(), "-o", dest.getPath());
        try {
            process = builder.start();
            process.waitFor();
            int exitValue = process.exitValue();
            if (exitValue != 0) {
                UMLogUtil.logError("Error Stream for src " + src.getPath() + UMIOUtils.readStreamToString(process.getErrorStream()));
                pngFile = new File(UMFileUtil.INSTANCE.stripExtensionIfPresent(src.getPath()) + PNG_EXT);
                convertJpgToPng(src, pngFile);
                convertImageToWebp(pngFile, dest);
                pngFile.delete();
            }
        } catch (IOException e) {
            throw e;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (pngFile != null) {
                pngFile.delete();
            }
            if (process != null) {
                process.destroy();
            }
        }

        if (!ContentScraperUtil.fileHasContent(dest)) {
            throw new IOException("convertImaegToWebP: source existed, but output does not " +
                    dest.getPath());
        }

    }


    /**
     * Given a source file and a destination file, convert the jpg(src) to png(dest)
     *
     * @param src  file image path
     * @param dest webp file path
     */
    private static void convertJpgToPng(File src, File dest) throws IOException {
        if (!ContentScraperUtil.fileHasContent(src)) {
            throw new FileNotFoundException("convertImageToWebp: Source file: " + src.getAbsolutePath() + " does not exist");
        }

        File webpExecutableFile = new File(ScraperBuildConfig.CWEBP_PATH);
        if (!webpExecutableFile.exists()) {
            throw new IOException("Webp executable does not exist: " + ScraperBuildConfig.CWEBP_PATH);
        }

        Process process = null;
        ProcessBuilder builder = new ProcessBuilder("/usr/bin/mogrify", "-format", "png", src.getPath(), dest.getPath());
        try {
            process = builder.start();
            process.waitFor();
            int exitValue = process.exitValue();
            if (exitValue != 0) {
                UMLogUtil.logError("Error Stream for src " + src.getPath() + UMIOUtils.readStreamToString(process.getErrorStream()));
                throw new IOException();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }

        if (!ContentScraperUtil.fileHasContent(dest)) {
            throw new IOException("convertJpegToPng: source existed, but output does not " +
                    dest.getPath());
        }

    }


    public static void convertVideoToWebM(File src, File dest) throws IOException {
        if (!ContentScraperUtil.fileHasContent(src)) {
            throw new FileNotFoundException("convertVideoToWebm: Source file: " + src.getAbsolutePath() + " does not exist");
        }

        File webpExecutableFile = new File(ScraperBuildConfig.FFMPEG_PATH);
        if (!webpExecutableFile.exists()) {
            throw new IOException("ffmpeg executable does not exist: " + ScraperBuildConfig.FFMPEG_PATH);
        }

        ProcessBuilder builder = new ProcessBuilder(ScraperBuildConfig.FFMPEG_PATH, "-i",
                src.getPath(), "-vf", "scale=480x270", "-r", "20", "-c:v", "vp9", "-crf", "40", "-b:v", "0", "-c:a"
                , "libopus", "-b:a", "12000", "-vbr", "on", dest.getPath());
        builder.redirectErrorStream(true);
        Process process = null;
        try {
            process = builder.start();
            UMIOUtils.readStreamToByteArray(process.getInputStream());
            process.waitFor();
            int exitValue = process.exitValue();
            if (exitValue != 0) {
                UMLogUtil.logError("Error Stream for src " + src.getPath() + UMIOUtils.readStreamToString(process.getErrorStream()));
                throw new IOException();
            }
            process.destroy();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        if (!ContentScraperUtil.fileHasContent(dest)) {
            throw new IOException("convertVideoToWebm: source existed, but output does not " +
                    dest.getPath());
        }

    }

    public static void convertAudioToOpos(File src, File dest) throws IOException {
        if (!ContentScraperUtil.fileHasContent(src)) {
            throw new FileNotFoundException("convertAudioToOpos: Source file: " + src.getAbsolutePath() + " does not exist");
        }

        File webpExecutableFile = new File(ScraperBuildConfig.FFMPEG_PATH);
        if (!webpExecutableFile.exists()) {
            throw new IOException("ffmpeg executable does not exist: " + ScraperBuildConfig.FFMPEG_PATH);
        }

        ProcessBuilder builder = new ProcessBuilder(ScraperBuildConfig.FFMPEG_PATH, "-i", src.getPath()
                , "-c:a", "libopus", "-b:a", "12000", "-vbr", "on", dest.getPath());
        builder.redirectErrorStream(true);
        Process process = null;
        try {
            process = builder.start();
            UMIOUtils.readStreamToByteArray(process.getInputStream());
            process.waitFor();
            int exitValue = process.exitValue();
            if (exitValue != 0) {
                UMLogUtil.logError("Error Stream for src " + src.getPath() + UMIOUtils.readStreamToString(process.getErrorStream()));
            }
            process.destroy();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        if (!ContentScraperUtil.fileHasContent(dest)) {
            throw new IOException("convertAudioToOpos: source existed, but output does not " +
                    dest.getPath());
        }

    }


    public static void convertKhanVideoToWebMAndCodec2(File src, File dest) throws IOException {

        if (!ContentScraperUtil.fileHasContent(src)) {
            throw new FileNotFoundException("convertKhanToWebmAndCodec2: Source file: " + src.getAbsolutePath() + " does not exist");
        }

        File webpExecutableFile = new File(ScraperBuildConfig.FFMPEG_PATH);
        if (!webpExecutableFile.exists()) {
            throw new IOException("ffmpeg executable does not exist: " + ScraperBuildConfig.FFMPEG_PATH);
        }

        ProcessBuilder videoBuilder = new ProcessBuilder(ScraperBuildConfig.FFMPEG_PATH, "-i", src.getPath()
                , "-vf", "scale=480x270", "-r", "5", "-c:v", "vp9", "-b:v", "0", "-crf", "40", "-an", "-y", dest.getPath());
        videoBuilder.redirectErrorStream(true);

        File rawFile = new File(dest.getParentFile(), "audio.raw");
        ProcessBuilder rawBuilder = new ProcessBuilder(ScraperBuildConfig.FFMPEG_PATH, "-i", src.getPath()
                , "-vn", "-c:a", "pcm_s16le", "-ar", "8000", "-ac", "1", "-f", "s16le", "-y", rawFile.getPath());
        rawBuilder.redirectErrorStream(true);

        File audioFile = new File(dest.getParentFile(), "audio.c2");
        ProcessBuilder audioBuilder = new ProcessBuilder(ScraperBuildConfig.CODEC2_PATH, "3200", rawFile.getPath(), audioFile.getPath());
        audioBuilder.redirectErrorStream(true);

        Process process = null;
        try {
            process = videoBuilder.start();
            startProcess(process);

            UMLogUtil.logTrace("got the webm file");

            process = rawBuilder.start();
            startProcess(process);

            UMLogUtil.logTrace("got the raw file");

            process = audioBuilder.start();
            startProcess(process);

            UMLogUtil.logTrace("got the c2 file");

        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
            ContentScraperUtil.deleteFile(rawFile);
        }

        if (!ContentScraperUtil.fileHasContent(dest)) {
            throw new IOException("convertVideoToWebMAndCodec: source existed, but webm output does not " +
                    dest.getPath());
        }

        if (!ContentScraperUtil.fileHasContent(audioFile)) {
            throw new IOException("convertVideoToWebMAndCodec: source existed, but audio output does not " +
                    dest.getPath());
        }


    }

    private static void startProcess(Process process) throws IOException, InterruptedException {
        UMIOUtils.readStreamToByteArray(process.getInputStream());
        process.waitFor();
        int exitValue = process.exitValue();
        if (exitValue != 0) {
            UMLogUtil.logError("Error Stream for src " + UMIOUtils.readStreamToString(process.getErrorStream()));
        }
        process.destroy();
    }


}
