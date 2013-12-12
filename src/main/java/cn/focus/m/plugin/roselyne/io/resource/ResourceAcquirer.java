package cn.focus.m.plugin.roselyne.io.resource;

import java.io.File;

public interface ResourceAcquirer {

    public File acquireResource(String resourceName, String tempDir) throws ResourceNotFoundException, ResourceInvalidException;
}
