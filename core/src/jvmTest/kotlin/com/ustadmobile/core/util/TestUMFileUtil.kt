package com.ustadmobile.core.util

import com.ustadmobile.core.catalog.contenttype.EPUBType
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Created by mike on 6/18/17.
 *
 * TODO: Refactor this to use test name conventions
 */
class TestUMFileUtil {

    @Test
    fun testAppendExtensionToFilenameIfNeeded() {
        assertEquals("Will append file extension when needed", "foo.bar.epub",
                UMFileUtil.appendExtensionToFilenameIfNeeded("foo.bar",
                        EPUBType.MIME_TYPES[0]))
        assertEquals("Will leave filename when extension is already there", "foo.epub",
                UMFileUtil.appendExtensionToFilenameIfNeeded("foo.epub",
                        EPUBType.MIME_TYPES[0]))
        assertEquals("Will leave filename when extension is unknown", "foo.bar",
                UMFileUtil.appendExtensionToFilenameIfNeeded("foo.bar",
                        "application/x-foo-bar"))

    }

    @Test
    fun testRemoveExtension() {
        assertEquals("Can remove extension", UMFileUtil.removeExtension("filename.txt"),
                "filename")
        assertEquals("If file has no extension, same value is returned",
                UMFileUtil.removeExtension("filename"), "filename")
    }


    @Test
    fun testFileUtilFilename() {
        assertEquals("Will return the same for a name only entry",
                UMFileUtil.getFilename("testfile.txt"), "testfile.txt")
        assertEquals("Will return the the basename of a directory with trailing /",
                "somedir/", UMFileUtil.getFilename("/somepath/somedir/"))
        assertEquals("Can handle . as filename",
                ".", UMFileUtil.getFilename("."))
        assertEquals("Can handle ./ as filename",
                "./", UMFileUtil.getFilename("./"))

        assertEquals("Will return the same for a name only entry with trailing /",
                UMFileUtil.getFilename("somedir/"), "somedir/")
        assertEquals("Will cut the path off and return filnemae",
                UMFileUtil.getFilename("/somedir/file.txt"), "file.txt")
        assertEquals("Will cut off query string",
                UMFileUtil.getFilename("http://someplace.com/somedir/file.txt"),
                "file.txt")


        assertEquals("Will correctly find extension: mp3",
                "mp3", UMFileUtil.getExtension("http://server.com/dir/file.mp3"))
        assertEquals("Will return null in case of no extension", null, UMFileUtil.getExtension("http://server.com/some/dir"))

        assertEquals("Can strip file:// prefix as expected",
                "/path/to/file.mp3",
                UMFileUtil.stripPrefixIfPresent("file://", "file:///path/to/file.mp3"))

        assertEquals("Can get the parent of a file name", "file:///some/path/",
                UMFileUtil.getParentFilename("file:///some/path/file.mp3"))
        assertTrue("Parent filename return nulls when there is no parent in path",
                UMFileUtil.getParentFilename("file.mp3") == null)
        assertTrue("Parent filename return nulls when path is one char long",
                UMFileUtil.getParentFilename(".") == null)


        //test mime type parsing (will replace getMimeTypeParameters
        var header: UMFileUtil.TypeWithParamHeader = UMFileUtil.parseTypeWithParamHeader(
                "application/atom+xml;profile=opds-catalog;kind=navigation")
        assertEquals("Correct type from header1", "application/atom+xml",
                header.typeName)
        assertEquals("Correct profile parameter in header1", "opds-catalog",
                header.params!!["profile"])
        assertEquals("Correct kind parameter in header1", "navigation",
                header.params!!["kind"])

        header = UMFileUtil.parseTypeWithParamHeader(
                "attachment; filename=\"some book.epub\"")
        assertEquals("parse content-disposition type", "attachment",
                header.typeName)
        assertEquals("parse content disposition filename", "some book.epub",
                header.params!!["filename"])

        header = UMFileUtil.parseTypeWithParamHeader("application/atom+xml")
        assertEquals("Can parse header with no params", "application/atom+xml",
                header.typeName)
        assertEquals("Header with no params results in null param ht", null,
                header.params)

        val cacheHeader = "private, community=UCI, maxage=600"
        val cacheTable = UMFileUtil.parseParams(cacheHeader, ',')

        assertEquals("Cache control parsed private", "",
                cacheTable["private"])
        assertEquals("Cache control get community", "UCI",
                cacheTable["community"])
        assertEquals("Cache control get maxage", "600",
                cacheTable["maxage"])

        //test filtering nasty characters
        assertEquals("removes security hazard characters from filename",
                "nastyname.so", UMFileUtil.filterFilename("/nastyname.*so"))

        var fileSize = 500
        assertEquals("Format filename in bytes", "500.0 bytes",
                UMFileUtil.formatFileSize(500))
        fileSize *= 1024
        assertEquals("Format filename in kB", "500.0 kB",
                UMFileUtil.formatFileSize(fileSize.toLong()))
        fileSize *= 1024
        assertEquals("Format filename in kB", "500.0 MB",
                UMFileUtil.formatFileSize(fileSize.toLong()))
    }

    @Test
    fun testUMFileUtilJoin() {
        assertEquals("Can handle basic join with to single slash",
                "testpath/somefile.txt",
                UMFileUtil.joinPaths(*arrayOf("testpath/", "/somefile.txt")))
        assertEquals("Will not remove first slash", "/testpath/somefile.txt",
                UMFileUtil.joinPaths(*arrayOf("/testpath/", "/somefile.txt")))
        assertEquals("Will not remove trailing slash", "/testpath/somedir/",
                UMFileUtil.joinPaths(*arrayOf("/testpath/", "/somedir/")))
    }

    @Test
    fun testUMFileUtilResolveLink() {
        assertEquals("Absolute path returns same path back",
                "http://www.server2.com/somewhere",
                UMFileUtil.resolveLink("http://server1.com/some/place",
                        "http://www.server2.com/somewhere"))
        assertEquals("Can resolve prtocol only link",
                "http://www.server2.com/somewhere",
                UMFileUtil.resolveLink("http://server1.com/some/place",
                        "//www.server2.com/somewhere"))
        assertEquals("Can resolve relative to server link",
                "http://server1.com/somewhere",
                UMFileUtil.resolveLink("http://server1.com/some/place",
                        "/somewhere"))
        assertEquals("Can handle basic relative path",
                "http://server1.com/some/file.jpg",
                UMFileUtil.resolveLink("http://server1.com/some/other.html",
                        "file.jpg"))
        assertEquals("Can handle .. in relative path",
                "http://server1.com/file.jpg",
                UMFileUtil.resolveLink("http://server1.com/some/other.html",
                        "../file.jpg"))

        assertEquals("Can handle base link with no folder", "images/thumb.png",
                UMFileUtil.resolveLink("content.opf", "images/thumb.png"))

    }

    @Test
    fun givenLastReferrerPath_whenGetLastReferrerCalled_thenArgsShouldMatch() {
        val referralPath = "/HomeView?/ContentEntryList?entryid=40/ContentEntryList?entryid=41/" + "ContentEntryDetail?entryid=42/ContentEntryDetail?entryid=43"
        val lastListArgs = UMFileUtil.getLastReferrerArgsByViewname("ContentEntryList",
                referralPath)
        Assert.assertEquals("Last entry list id = 41", "entryid=41", lastListArgs)

    }

}
