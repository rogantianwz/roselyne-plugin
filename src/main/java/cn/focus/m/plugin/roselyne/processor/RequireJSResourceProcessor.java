package cn.focus.m.plugin.roselyne.processor;

import com.alibaba.fastjson.JSONObject;

import cn.focus.m.plugin.roselyne.Message;
import cn.focus.m.plugin.roselyne.compress.ResourceCompressor;
import cn.focus.m.plugin.roselyne.io.resource.HttpResourceAcquirer;
import cn.focus.m.plugin.roselyne.io.resource.Resource;
import cn.focus.m.plugin.roselyne.io.resource.ResourceAcquirerFactory;
import cn.focus.m.plugin.roselyne.io.resource.ResourceInvalidException;
import cn.focus.m.plugin.roselyne.io.resource.ResourceResolveSupport;
import cn.focus.m.plugin.roselyne.io.resource.ResourceResolveSupportAdaptor;
import cn.focus.m.plugin.roselyne.utils.RoselyneFileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * 处理requirejs的配置文件
 * @author rogantian
 * @date 2014-8-25
 * @email rogantianwz@gmail.com
 */
public class RequireJSResourceProcessor extends AbstractResourceProcessor {
    
    private static String REQUIREJS_CONIFG_NAME = "requirejs.config";
    
    private static int REQUIREJS_CONIFG_NAME_LEN = REQUIREJS_CONIFG_NAME.length();
    
    private static String REQUIREJS_BASE_URL = "baseUrl";
    
    private static String REQUIREJS_PATHS = "paths";
    
    private static String JS_COMMENT_SCRIPT = "([^:]|^)//.*$|/\\*[\\s\\S]*?\\*/";
    
    private static Pattern JSCOMMENTPATTERN = Pattern.compile(JS_COMMENT_SCRIPT, Pattern.MULTILINE);

    public RequireJSResourceProcessor (ResourceResolveSupport resourceResolveSupport, ResourceAcquirerFactory resourceAcquirerFactory,
            ResourceCompressor resourceCompressor) {
        super.setResourceAcquirerFactory(resourceAcquirerFactory);
        super.setResourceResolveSupport(resourceResolveSupport);
        super.setResourceCompressor(resourceCompressor);
    }
    
    @Override
    protected Set<Reference> searchReference(Resource resource) throws ResourceProcessException {

        if (null == resource) {
            return null;
        }
        Set<Reference> ret = new HashSet<Reference>();
        
        File f = resource.getTempAddr();
        if (null == f || !f.exists()) {
            StringBuilder errorBuilder = new StringBuilder("Resource[").append(resource.getSourceAddr())
                    .append("] does not have valid tempAddr");
            String error = errorBuilder.toString();
            super.addMessage(resource, new Message(MessageType.ERROR, error));
            throw new ResourceProcessException(error);
        }
        
        try {
            String content = RoselyneFileUtils.fileRead(f, super.getResourceResolveSupport().getEncoding());
            content = content.replaceAll("\r", "").replaceAll("\n", "");
            Location cfgJsonLoc = resolveCfgJsonPos(content, resource);
            String cfgJson = content.substring(cfgJsonLoc.getStart() + 1, cfgJsonLoc.getEnd()).trim();
            if (StringUtils.isBlank(cfgJson)) {
                return ret;
            }
            
            JSONObject requireJSCfg = JSONObject.parseObject(cfgJson);
            String baseUrl = requireJSCfg.getString(REQUIREJS_BASE_URL);
            if (StringUtils.isBlank(baseUrl)) {
                String sourceAddr = resource.getSourceAddr();
                int lastBackSlash = sourceAddr.lastIndexOf("/");
                if (lastBackSlash > 0) {
                    baseUrl = sourceAddr.substring(0, lastBackSlash);
                }
            }
            
            if (null == baseUrl) {
                StringBuilder warnBuilder = new StringBuilder("Resource[").append(resource.getSourceAddr())
                        .append("] does not have proporite baseUrl");
                super.addMessage(resource, new Message(MessageType.WARN, warnBuilder.toString()));
                baseUrl = "";
            }
            
            if (!baseUrl.endsWith("/")) {
                baseUrl = baseUrl + "/";
            }
            
            JSONObject requireJSPaths = requireJSCfg.getJSONObject(REQUIREJS_PATHS);
            if (null == requireJSPaths) {
                return ret;
            }
            
            Set<String> sourceAddrs = new HashSet<String>();            
            Set<Entry<String, Object>> pathSet = requireJSPaths.entrySet();
            for(Entry<String, Object> entry : pathSet) {
                String key  = (String)entry.getKey();
                String value = (String) entry.getValue();
                
                
                //和前端同学约定不使用以"/"开头的路径配置，这里遇到这种情况直接抛出异常。
                //同样约定paths中的key不会出现目录形式
                //同样约定路径不要以.js结尾
                if (value.startsWith("/") || value.endsWith(".js") || key.contains("/")) {
                    StringBuilder errorBuilder = new StringBuilder("Resource[").append(resource.getSourceAddr())
                            .append("] can not process the paths begin with '/' or contains '/' or end with '.js' like '" + value + "'");
                    String error = errorBuilder.toString();
                    super.addMessage(resource, new Message(MessageType.ERROR, error));
                    throw new ResourceProcessException(error);
                }
                
                //这里会判断是否有多个Ref指向同一个sourceAddr，如果发现则抛出异常
                String sourceAddr = null;
                if (value.startsWith("http://")) {
//                    pathMap.put(key, value + ".js");
                    sourceAddr = value + ".js";
                } else {
                    sourceAddr = baseUrl + value + ".js";
                    URI uri = new URI(sourceAddr);
                    URI normalize = uri.normalize();
                    sourceAddr = normalize.toString();
                }
                
                if (sourceAddrs.contains(sourceAddr)) {
                    StringBuilder errorBuilder = new StringBuilder("Resource[").append(resource.getSourceAddr())
                            .append("] can not have the same two resouce like '" + sourceAddr + "'");
                    String error = errorBuilder.toString();
                    super.addMessage(resource, new Message(MessageType.ERROR, error));
                    throw new ResourceProcessException(error);
                } else {
                    Reference ref = new Reference(sourceAddr, key);
                    ret.add(ref);
                }
            }
            
        } catch (FileNotFoundException e) {
            StringBuilder errorBuilder = new StringBuilder("Resource[").append(resource.getSourceAddr())
                    .append("] does not have valid tempAddr ");
            String error = errorBuilder.toString();
            super.addMessage(resource, new Message(MessageType.ERROR, error));
            throw new ResourceProcessException(error);
        } catch (Exception e) {
            throw new ResourceProcessException("",e);
        }
        
        return ret;
        
    }

    @Override
    protected String getProcessorName() {
        // TODO Auto-generated method stub
        return this.getClass().getSimpleName();
    }
    
    
    @Override
    protected void replaceReference(Resource resource, Set<Reference> references) throws ResourceProcessException {
        String source = null;
        StringBuilder dest = new StringBuilder();
        try {
            source = RoselyneFileUtils.fileRead(resource.getTempAddr(), super.getResourceResolveSupport().getEncoding());
        } catch (IOException e) {
            throw new ResourceProcessException(e);
        }
        
        if (null != references && references.size() > 0) {
            //需要先移除脚本中的注释，否则在删除\r\n之后，可能会把正常代码移到了注释后边了。
            Matcher m = JSCOMMENTPATTERN.matcher(source);
            source = m.replaceAll("");
            source = source.replaceAll("\r", "").replaceAll("\n", "");
            Location cfgJsonLoc = resolveCfgJsonPos(source, resource);
            String cfgJson = source.substring(cfgJsonLoc.getStart() + 1, cfgJsonLoc.getEnd()).trim();
            if (StringUtils.isBlank(cfgJson)) {
                dest.append(source);
            }
            JSONObject requireJSCfg = JSONObject.parseObject(cfgJson);
            String oriBaseUrl = requireJSCfg.getString(REQUIREJS_BASE_URL);
            JSONObject requireJSPaths = requireJSCfg.getJSONObject(REQUIREJS_PATHS);
            
            Map<String, Reference> refMap = new HashMap<String, Reference>();
            for(Reference ref : references) {
                refMap.put(ref.getRequiredCfgPathKey(), ref);
            }
            
            String baseUrl = "";
            Set<Entry<String, Object>> entrySet = requireJSPaths.entrySet();
            for (Entry<String ,Object> entry : entrySet) {
                String key = entry.getKey();
                String value = (String) entry.getValue();
                
                Reference ref = refMap.get(key);
                if (value.startsWith("http://")) {
                    String releaseAddr = ref.getResource().getReleaseAddr();
                    releaseAddr = StringUtils.chomp(releaseAddr, ".");//移除结尾的".js"
                    entry.setValue(releaseAddr);
                    continue;
                }
                
                entry.setValue(value + "_" + ref.getResource().getVersionCode());
                
                
                if (StringUtils.isNotBlank(oriBaseUrl)) {
                    String relativeSourceAddr = entry.getValue() + ".js";
                    //移除路径中的../和./
                    relativeSourceAddr = relativeSourceAddr.replaceAll("\\./", "");
                    while (relativeSourceAddr.startsWith("../")) {
                        relativeSourceAddr = relativeSourceAddr.substring(3);
                    }
                    
                    String releaseAddr = ref.getResource().getReleaseAddr();
                    int tempPoint = releaseAddr.lastIndexOf(relativeSourceAddr);
                    String tempBaseUrl = releaseAddr.substring(0, tempPoint);
                    if (tempBaseUrl.length() > baseUrl.length()) {
                        baseUrl = tempBaseUrl;
                    }
                }
            }
            
            if (StringUtils.isNotBlank(baseUrl)) {
                if (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
                requireJSCfg.put(REQUIREJS_BASE_URL, baseUrl);
            }
            
            requireJSCfg.put(REQUIREJS_PATHS, requireJSPaths);
            
            dest.append(source.substring(0, cfgJsonLoc.getStart() + 1)).append(requireJSCfg.toJSONString()).append(source.substring(cfgJsonLoc.getEnd()));
            
        } else {
            dest.append(source);
        }
        
        try {
            File destFile = new File(resource.getOutputAddr());
            String destStr = dest.toString();
            String versionCode = DigestUtils.md5Hex(destStr);
            resource.setVersionCode(versionCode);
            RoselyneFileUtils.fileWrite(destFile, super.getResourceResolveSupport().getEncoding(), destStr);
        } catch (IOException e) {
            throw new ResourceProcessException(e);
        }
        
    }
    
    private class Location {
        private int start;
        
        private int end;

        public Location(int start, int end) {
            super();
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }
    }
    
    /**
     * 从requirejs的配置文件中解析出json配置字符串的位置
     * @param content
     * @param resource
     * @return
     * @throws ResourceProcessException
     */
    private Location resolveCfgJsonPos(String content, Resource resource) throws ResourceProcessException {
        int begin = content.indexOf(REQUIREJS_CONIFG_NAME) + REQUIREJS_CONIFG_NAME_LEN;
        if (begin < REQUIREJS_CONIFG_NAME_LEN) {
            StringBuilder errorBuilder = new StringBuilder("Resource[").append(resource.getSourceAddr())
                    .append("] is not a valid requirejs resource");
            String error = errorBuilder.toString();
            super.addMessage(resource, new Message(MessageType.ERROR, error));
            throw new ResourceProcessException(error);
        }
        
        
        char[] chars = content.toCharArray();
        int charLen = chars.length;
        int leftParentheses = 0;
        int rightParentheses = 0;
        int countOfParenthesesPairs = 0;
        for (int point = begin; point < charLen; point ++) {
            if ('(' == chars[point]) {
                if (0 == leftParentheses) { // present the first leftParentheses
                    leftParentheses = point;
                }
                countOfParenthesesPairs ++;
            } else if (')' == chars[point]) {
                if (0 == --countOfParenthesesPairs && 0 != leftParentheses) {
                    rightParentheses = point;
                    break;
                }
            }
        }
        
        if (0 == leftParentheses || 0 == rightParentheses) {
            return null;
        }
        
        return new Location(leftParentheses, rightParentheses);
    }
    
    public static void main(String[] args) throws Exception, ResourceInvalidException {
 /*       HttpResourceAcquirer httpResourceAcquirer = new HttpResourceAcquirer();
        File f = httpResourceAcquirer.acquireResource("http://192.168.242.44/sceapp/focus_static/wap/pad/snippets/modulejs/app_tuangou.js", "d:\\");
        String content = RoselyneFileUtils.fileRead(f, "utf-8");
        content = content.replaceAll("\r", "").replaceAll("\n", "");
        System.out.println(content);
        int begin = content.indexOf(REQUIREJS_CONIFG_NAME) + REQUIREJS_CONIFG_NAME_LEN;
        if (begin < REQUIREJS_CONIFG_NAME_LEN) {
            System.out.println("error 1");
        }
        char[] chars = content.toCharArray();
        int charLen = chars.length;
        int leftParentheses = 0;
        int rightParentheses = 0;
        int countOfParenthesesPairs = 0;
        for (int point = begin; point < charLen; point ++) {
            if ('(' == chars[point]) {
                if (0 == leftParentheses) { // present the first leftParentheses
                    leftParentheses = point;
                }
                countOfParenthesesPairs ++;
            } else if (')' == chars[point]) {
                if (0 == --countOfParenthesesPairs && 0 != leftParentheses) {
                    rightParentheses = point;
                    break;
                }
            }
        }
        
        if (0 == leftParentheses || 0 == rightParentheses) {
           System.out.println("error 2");
        }
        
        String cfgJson = content.substring(leftParentheses + 1, rightParentheses);
        System.out.println(cfgJson);*/
        
        
        
        
        Resource r = new Resource();
        r.setSourceAddr("http://192.168.242.44/sceapp/focus_static/wap/pad/snippets/test/app_tuangou_test.js");
        
        HttpResourceAcquirer httpResourceAcquirer = new HttpResourceAcquirer();
        File f = httpResourceAcquirer.acquireResource("http://192.168.242.44/sceapp/focus_static/wap/pad/snippets/test/app_tuangou_test.js", "d:\\");
        r.setTempAddr(f);
        
        RequireJSResourceProcessor processor = new RequireJSResourceProcessor(new ResourceResolveSupportAdaptor(){
            @Override
            public String getEncoding() {
                return "utf-8";
            }
        }, null, null);
        
        Set<Reference> searchReference = processor.searchReference(r);
        for (Reference ref : searchReference) {
            System.out.println(ref.getSourceAddr() + "--->" + ref.getRequiredCfgPathKey());
        }
    }


}
