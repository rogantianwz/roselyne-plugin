package cn.focus.m.plugin.roselyne.io.resource;

import cn.focus.m.plugin.roselyne.Message;
import cn.focus.m.plugin.roselyne.MessageHolder.MessageType;
import cn.focus.m.plugin.roselyne.compress.CompressConfig;
import cn.focus.m.plugin.roselyne.compress.ResourceCompressor;
import cn.focus.m.plugin.roselyne.processor.HTMLResourceProcessor;
import cn.focus.m.plugin.roselyne.processor.ImageResourceProcessor;
import cn.focus.m.plugin.roselyne.processor.JSResourceProcessor;
import cn.focus.m.plugin.roselyne.processor.RequireJSResourceProcessor;
import cn.focus.m.plugin.roselyne.processor.ResourceProcessException;
import cn.focus.m.plugin.roselyne.processor.ResourceProcessor;
import cn.focus.m.plugin.roselyne.processor.StyleResourceProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.codehaus.plexus.util.FileUtils;

public class ResourceProcessHandler {

    private ResourceResolveSupport resourceResolveSupport;
    
    private ResourceAcquirerFactory resourceAcquirerFactory;
    
    private ResourceCompressor resourceCompressor;
    
    private List<Resource> resources;
    
    private List<MagicResource> magicResources;
    
    private Map<String, Resource> staticResources = new HashMap<String, Resource>();
    
    private ResourceProcessor htmlProcessor;
    
    private ResourceProcessor jsProcessor;
    
    private ResourceProcessor styleProcessor;
    
    private ResourceProcessor imageProcessor;
    
    private ResourceProcessor requireJSProcessor;
    
    public ResourceProcessHandler(ResourceResolveSupport resolveSupport, ResourceAcquirerFactory resourceAcquirerFactory,
            ResourceCompressor resourceCompressor, List<Resource> resources, List<MagicResource> magicResources) {
        this.resourceResolveSupport = resolveSupport;
        this.resourceAcquirerFactory = resourceAcquirerFactory;
        this.resourceCompressor = resourceCompressor;
        this.resources = resources;
        this.magicResources = magicResources;
        
        init();
    }
    
    private void init() {
        if (null != resources) {
            for (Resource resource : resources) {
                staticResources.put(resource.getSourceAddr(), resource);
            }
        }
        
        htmlProcessor = new HTMLResourceProcessor(resourceResolveSupport, resourceAcquirerFactory, resourceCompressor);
        jsProcessor = new JSResourceProcessor(resourceResolveSupport, resourceAcquirerFactory, resourceCompressor);
        styleProcessor = new StyleResourceProcessor(resourceResolveSupport, resourceAcquirerFactory, resourceCompressor);
        imageProcessor = new ImageResourceProcessor(resourceResolveSupport, resourceAcquirerFactory, resourceCompressor);
        requireJSProcessor = new RequireJSResourceProcessor(resourceResolveSupport, resourceAcquirerFactory, resourceCompressor);
        
    }
    
    public void handle() throws ResourceInvalidException, ResourceProcessException, ResourceNotFoundException {
        if (null == resources) {
            return;
        }
        debugStaticResource();
        for (Resource resource : resources) {
            processResource(resource);
        }
        debugStaticResource();
    }
    
    public boolean processResource(Resource resource) throws ResourceInvalidException, ResourceProcessException, ResourceNotFoundException {
        
        if (!validateResource(resource)) {
            throw new ResourceInvalidException();
        }
        
        ResourceProcessor processor = chooseProcessor(resource);
        
        if (null == processor) {
            throw new ResourceProcessException("can not match a ResourceProcesor for resource[" + resource.getSourceAddr() + "]");
        }
        
        return processor.processResource(resource, staticResources, magicResources, this);
    }
    
    /**
     * TODO
     * @param resource
     * @return
     */
    private boolean validateResource(Resource resource) {
        return true;
    }
    
    /**
     * FIXME
     * 根据资源类别选择资源处理器
     * @param resource
     * @return
     */
    public ResourceProcessor chooseProcessor(Resource resource) {
        
        if (resource.isRequirejsDataMain()) {
            return requireJSProcessor;
        }
        
        String sourceAddr = resource.getSourceAddr();
        String extension = FileUtils.extension(sourceAddr).toLowerCase();
        
        if ("jsp".equals(extension) || "html".equals(extension) || "php".equals(extension)) {
            return htmlProcessor;
        } else if ("js".equals(extension)) {
            return jsProcessor;
        } else if ("css".equals(extension)) {
            return styleProcessor;
        } else if ("png".equals(extension) || "jpg".equals(extension) || "jpeg".equals(extension)
                || "gif".equals(extension) || "ico".equals(extension)) {
            return imageProcessor;
        } else {
            return null;
        }
    }
    
    private void debugStaticResource() {
        Set<Entry<String, Resource>> entries = staticResources.entrySet();
        printLog(new Message(MessageType.SEPERATOR, "Debug static Resources---size:" + staticResources.size()));
        for (Entry<String, Resource> entry : entries) {
            printLog(new Message(MessageType.INFO, entry.getValue().toString()));
        }
    }
    
    public void printLog(Message msg) {
        resourceResolveSupport.printResolveLog(msg);
    }
    
    public static void main (String[] args) {
        ResourceResolveSupport support = new ResourceResolveSupportAdaptor(){
            public void addMessage(MessageType type, String content) {
            }
            public String getTempDir() {
                return "D:\\workspace1\\test\\target\\fis\\temp\\";
            }
            public String getEncoding() {
                return "UTF-8";
            }
            public void printResolveLog(Message msg) {
                // TODO Auto-generated method stub
                System.out.println(msg.getContent());
            }
            @Override
            public CompressConfig getCompressConfig() {
                return null;
            }
            };
        ResourceAcquirerFactory acquireFactory = new ResourceAcquirerFactory();
        ResourceCompressor resourceCompressor = new ResourceCompressor(support);
        List<Resource> resources = new ArrayList<Resource>();
        Resource resource = new Resource();
        resource.setSourceAddr("D:\\workspace1\\test\\src\\main\\webapp\\views\\home.jsp");
        resource.setReleaseAddr("http://m.focus.cn/home");
        resource.setOutputAddr("D:\\workspace1\\test\\target\\fis\\output\\home.jsp");
        resource.setTempAddr(new File("D:\\workspace1\\test\\target\\fis\\temp\\1386066197945"));
        
        MagicResource magicResource = new MagicResource();
        magicResource.setSourceAddr("");
        magicResource.setReleaseAddr("http://a1.itc.cn/sceapp/focus_static/wap/$1");
        magicResource.setOutputAddr("D:\\workspace1\\test\\target\\fis\\output\\$1");
        magicResource.setTempAddr(null);
        magicResource.setMd5(true);
        List<Pattern> includes = new ArrayList<Pattern>();
        includes.add(Pattern.compile("http://10.10.90.156/sceapp/focus_static/wap/(js/.*\\.js)"));
        includes.add(Pattern.compile("http://10.10.90.156/sceapp/focus_static/wap/(images/.*\\.png)"));
        includes.add(Pattern.compile("http://10.10.90.156/sceapp/focus_static/wap/(css/.*\\.css)"));
                
        magicResource.setIncludePatterns(includes);
        magicResource.setExcludePatterns(null);
        
        resources.add(resource);
        List<MagicResource> magicResources = new ArrayList<MagicResource>();
        magicResources.add(magicResource);
        ResourceProcessHandler handler = new ResourceProcessHandler(support, acquireFactory,resourceCompressor, resources, magicResources);
        try {
            handler.handle();
        } catch (ResourceInvalidException e) {
            e.printStackTrace();
        } catch (ResourceProcessException e) {
            e.printStackTrace();
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
        }
    }
}
