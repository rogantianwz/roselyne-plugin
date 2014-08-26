package cn.focus.cn.m.plugin.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import cn.focus.m.plugin.roselyne.Message;
import cn.focus.m.plugin.roselyne.compress.CompressConfig;
import cn.focus.m.plugin.roselyne.compress.ResourceCompressor;
import cn.focus.m.plugin.roselyne.io.resource.MagicResource;
import cn.focus.m.plugin.roselyne.io.resource.Resource;
import cn.focus.m.plugin.roselyne.io.resource.ResourceAcquirerFactory;
import cn.focus.m.plugin.roselyne.io.resource.ResourceInvalidException;
import cn.focus.m.plugin.roselyne.io.resource.ResourceNotFoundException;
import cn.focus.m.plugin.roselyne.io.resource.ResourceProcessHandler;
import cn.focus.m.plugin.roselyne.io.resource.ResourceResolveSupport;
import cn.focus.m.plugin.roselyne.io.resource.ResourceResolveSupportAdaptor;
import cn.focus.m.plugin.roselyne.processor.ResourceProcessException;

/**
 * 测试RequireJSResourceProcessor
 * @author rogantian
 * @date 2014-8-26
 * @email rogantianwz@gmail.com
 */
public class TestReuirejsProcessor {

    /**
     * @param args
     */
    public static void main(String[] args) {
        
        ResourceResolveSupport support = new ResourceResolveSupportAdaptor(){
            public String getTempDir() {
                return "D:\\requirejstest\\temp\\";
            }
            public String getEncoding() {
                return "UTF-8";
            }
            public void printResolveLog(Message msg) {
                System.out.println(msg.getContent());
            }
            public CompressConfig getCompressConfig() {
                return null;
            }
       };
       
       ResourceAcquirerFactory acquireFactory = new ResourceAcquirerFactory();
       ResourceCompressor resourceCompressor = new ResourceCompressor(support);
       List<Resource> resources = new ArrayList<Resource>();
       Resource resource = new Resource();
       resource.setSourceAddr("D:\\requirejstest\\source\\tuangou.jsp");
       resource.setReleaseAddr("http://m.focus.cn/home");
       resource.setOutputAddr("D:\\requirejstest\\out\\web\\tuangou.jsp");
       resource.setTempAddr(new File("D:\\requirejstest\\temp\\1386066197945.jsp"));
       
       MagicResource magicResource = new MagicResource();
       magicResource.setSourceAddr("");
       magicResource.setReleaseAddr("http://a1.itc.cn/sceapp/focus_static/wap/$1");
       magicResource.setOutputAddr("D:\\requirejstest\\out\\static\\$1");
       magicResource.setTempAddr(null);
       magicResource.setMd5(true);
       List<Pattern> includes = new ArrayList<Pattern>();
       includes.add(Pattern.compile("http://192.168.242.44/sceapp/focus_static/wap/(.*\\.js)"));
       //includes.add(Pattern.compile("http://192.168.242.44/sceapp/focus_static/wap/(.*\\.png)"));
       //includes.add(Pattern.compile("http://192.168.242.44/sceapp/focus_static/wap/(.*\\.css)"));
               
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
