package com.ustadmobile.port.sharedse.util;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class UmZipUtilTest {

    @Test
    public void test() throws IOException {

        File tmpDir = Files.createTempDirectory("testZipUtils").toFile();
        File targetFile = new File("test.epub");
        FileUtils.copyToFile(getClass().getResourceAsStream("/com/ustadmobile/test.epub"), targetFile);

        UmZipUtils.unzip(targetFile, tmpDir);


    }

}
