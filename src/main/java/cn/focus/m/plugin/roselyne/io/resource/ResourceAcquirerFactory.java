package cn.focus.m.plugin.roselyne.io.resource;

import java.io.File;

public class ResourceAcquirerFactory {

    private ResourceAcquirer httpResourceAcquirer = new HttpResourceAcquirer();
    
    private ResourceAcquirer localSystemResourceAcquirer = new LocalSystemResourceAcquirer();

    public ResourceAcquirer getResourceAcquirer(String resourceName) throws ResourceInvalidException{
        ResourceType resourceType = this.determineResourceType(resourceName);
        ResourceAcquirer acquirer = null;
        switch (resourceType) {
        case LocalSystemResource:
            acquirer =  localSystemResourceAcquirer;
            break;
        case HttpResource:
            acquirer = httpResourceAcquirer;
            break;
        default:
            break;
        }
        return acquirer;
    }
    
    private ResourceType determineResourceType(String resourceName) throws ResourceInvalidException {
        try {
            File f = new File(resourceName);
            if (f.exists()) {
                return ResourceType.LocalSystemResource;
            }
        } catch (Exception e) {
        }
        
        if (resourceName.startsWith("http://")) {
            return ResourceType.HttpResource;
        }
        
        throw new ResourceInvalidException(resourceName + "invalid");
    }
}
