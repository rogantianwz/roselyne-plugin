package cn.focus.m.plugin.roselyne.io.resource;

import java.io.File;
import java.util.Random;

import org.codehaus.plexus.util.FileUtils;

public class LocalSystemResourceAcquirer implements ResourceAcquirer {

    public File acquireResource(String resourceName, String tempDir) throws ResourceNotFoundException, ResourceInvalidException {
        File inputFile = new File(resourceName);
        if (!inputFile.exists()) {
            throw new ResourceNotFoundException();
        }
        File outputDir = new File(tempDir);
        outputDir.mkdirs();
        File outputFile = new File(outputDir, String.valueOf(System.currentTimeMillis() + new Random().nextInt()));
        try {
            FileUtils.copyFile(new File(resourceName), outputFile);
        } catch (Exception e) {
            throw new ResourceInvalidException();
        }
        return outputFile;
    }

}
