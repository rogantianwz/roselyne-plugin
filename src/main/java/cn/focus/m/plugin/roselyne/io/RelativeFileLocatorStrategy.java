package cn.focus.m.plugin.roselyne.io;

import java.io.File;

import org.apache.maven.shared.io.location.FileLocation;
import org.apache.maven.shared.io.location.Location;
import org.apache.maven.shared.io.location.LocatorStrategy;
import org.apache.maven.shared.io.logging.MessageHolder;

public class RelativeFileLocatorStrategy implements LocatorStrategy {

    private File baseDir;
    
    public RelativeFileLocatorStrategy(File baseDir) {
        this.baseDir = baseDir;
    }

    public Location resolve(String locationSpecification, MessageHolder messageHolder) {
        File file = new File(baseDir, locationSpecification);
        
        messageHolder.addMessage("Searching for file location: " + file.getAbsolutePath());
        
        Location location = null;
        
        if (file.exists()) {
            location = new FileLocation(file, locationSpecification);
        } else {
            messageHolder.addMessage("File: " + file.getAbsolutePath() + "does not exists.");
        }
        
        return location;
    }

}
