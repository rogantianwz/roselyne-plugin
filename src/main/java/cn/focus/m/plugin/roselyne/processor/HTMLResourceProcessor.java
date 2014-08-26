package cn.focus.m.plugin.roselyne.processor;

import cn.focus.m.plugin.roselyne.Message;
import cn.focus.m.plugin.roselyne.compress.ResourceCompressor;
import cn.focus.m.plugin.roselyne.io.resource.MagicResource;
import cn.focus.m.plugin.roselyne.io.resource.Resource;
import cn.focus.m.plugin.roselyne.io.resource.ResourceAcquirerFactory;
import cn.focus.m.plugin.roselyne.io.resource.ResourceInvalidException;
import cn.focus.m.plugin.roselyne.io.resource.ResourceNotFoundException;
import cn.focus.m.plugin.roselyne.io.resource.ResourceResolveSupport;
import cn.focus.m.plugin.roselyne.io.resource.ResourceResolveSupportAdaptor;
import cn.focus.m.plugin.roselyne.utils.RoselyneFileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * html类型资源处理器
 * @author rogantian
 * @date 2013-11-1
 * @email rogantianwz@gmail.com
 */
public class HTMLResourceProcessor extends AbstractResourceProcessor {
    
    private  static String SEARCH_SCRIPT = "(<script(?:(?=\\s)[\\s\\S]*?[\"'\\s\\w\\/\\-]>|>))([\\s\\S]*?)(?=<\\/script\\s*>|$)" +
    		                               "|(<style(?:(?=\\s)[\\s\\S]*?[\"'\\s\\w\\/\\-]>|>))([\\s\\S]*?)(?=<\\/style\\s*>|$)" +
    		                               "|<(img|embed|audio|video|link|object)\\s+[\\s\\S]*?[\"'\\s\\w\\/\\-](?:>|$)";
    
    /**
     * 查找html中所有资源的正则表达式,
     * 参考测试函数main2(..)
     */
    private static Pattern PSEARCHSCRIPT = Pattern.compile(SEARCH_SCRIPT);
    
    private static String ATTRIBUTE_SRC = "\\bsrc\\s*?=\\s*?('[^']+'|\"[^\"]+\"|[^\\s\\/>]+)";
    
    /**
     * 查找类似&lt;script ... src="..." /&gt;的js引用中的src值，
     * 参考测试函数main4(..)
     */
    private static Pattern PJSSRC = Pattern.compile(ATTRIBUTE_SRC);
    
    /**
     * 查找<(img|embed|audio|video|object)... src="..." />中引用的src值
     */
    private static Pattern MONDAYSRC = Pattern.compile(ATTRIBUTE_SRC);
    
    private static String ATTRIBUTE_DATA_MAIN = "\\bdata-main\\s*?=\\s*?('[^']+'|\"[^\"]+\"|[^\\s\\/>]+)";
    
    /**
     * 查找类似&lt;script data-main="..." /&gt;的js引用中的data-main值，为了支持require.js
     */
    private static Pattern PJSDATAMAIN = Pattern.compile(ATTRIBUTE_DATA_MAIN);
    
    
    private static String ATTRIBUTE_HREF = "\\bhref\\s*?=\\s*?('[^']+'|\"[^\"]+\"|[^\\s\\/>]+)";
    
    /**
     * 查找<link... href="..." />中引用的href值
     */
    private static Pattern MONDAYHREF = Pattern.compile(ATTRIBUTE_HREF);
    
    private static String ATTRIBUTE_DATA = "\\bdata\\s*?=\\s*?('[^']+'|\"[^\"]+\"|[^\\s\\/>]+)";
    
    /**
     * 查找<object... data="..." />中引用的data值
     */
    private static Pattern MONDAYDATA = Pattern.compile(ATTRIBUTE_DATA);
    
    
    private static String ATTRIBUTE_IGNORE = "\\$\\{[^\\}]*?\\}";
    
    /**
     * 判断属性中是否含有类似${...}的内容
     */
    private static Pattern PATTRIGNORE = Pattern.compile(ATTRIBUTE_IGNORE);
    

    /**
     * 处理<script>标签的js引用，<link>标签的css引用，<script>标签内的url(***)引用，
     * 参考测试函数main3(..)
     */
    
    public HTMLResourceProcessor(ResourceResolveSupport resourceResolveSupport, ResourceAcquirerFactory resourceAcquirerFactory,
            ResourceCompressor resourceCompressor) {
        super.setResourceAcquirerFactory(resourceAcquirerFactory);
        super.setResourceResolveSupport(resourceResolveSupport);
        super.setResourceCompressor(resourceCompressor);
    }
    
    @Override
    protected Set<Reference> searchReference(Resource resource) throws ResourceProcessException{
        
        Set<Reference> ret = new HashSet<Reference>();
        
        if (null == resource) {
            return null;
        }
        File f = resource.getTempAddr();
        
        if (null == f || !f.exists()) {
            StringBuilder errorBuilder = new StringBuilder("Resource[").append(resource.getSourceAddr())
                    .append("] does not have valid tempAddr");
            String error = errorBuilder.toString();
            super.addMessage(resource, new Message(MessageType.ERROR, error));
            throw new ResourceProcessException(error);
        }
        
        try {
            String sb = RoselyneFileUtils.fileRead(f, super.getResourceResolveSupport().getEncoding());
            Matcher matcher = PSEARCHSCRIPT.matcher(sb);
            boolean matched = matcher.find();
            List<MR> mrs = new ArrayList<MR>();
            while (matched) {
                MR mr = genMR(matcher);
                if (null != mr) {
                    mrs.add(mr);
                    /*StringBuilder infoBuilder = new StringBuilder("Resource[").append(resource.getSourceAddr())
                            .append("] searched script [").append(mr.getWhole().getContent()).append("]");
                    super.addMessage(MessageType.INFO, infoBuilder.toString());*/
                }
                matched = matcher.find();
            }
            
            Map<String, Reference> refs = new HashMap<String, Reference>();
            for (MR mr : mrs) {
                if (null != mr.getScriptTag()) {
                    searchScript(mr, resource, refs);
                } else if (null != mr.getStyleTag()) {
                    searchStyle(mr, resource.getSourceAddr(), refs);
                } else {
                    searchMonday(mr, resource, refs);
                }
            }
            
            printRefs(resource, refs);
            
            Set<Entry<String, Reference>> entries = refs.entrySet();
            
            for (Entry<String, Reference> entry : entries) {
                ret.add(entry.getValue());
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
    
    
    /**
     * 生成匹配结果集
     * @param matcher
     */
    private MR genMR(Matcher matcher) {
        if (null == matcher) {
            return null;
        }
        SG whole = new SG(matcher.group(), matcher.start(), matcher.end());
        
        SG scriptTag = null;
        if (StringUtils.isNotBlank(matcher.group(1))) {
            scriptTag = new SG(matcher.group(1), matcher.start(1), matcher.end(1));
        }
        
        SG scriptConent = null;
        if (StringUtils.isNotBlank(matcher.group(2))) {
            scriptConent = new SG(matcher.group(2), matcher.start(2), matcher.end(2));
        }
        
        SG styleTag = null;
        if (StringUtils.isNotBlank(matcher.group(3))) {
            styleTag = new SG(matcher.group(3), matcher.start(3), matcher.end(3));
        }
        
        SG styleContent = null;
        if (StringUtils.isNotBlank(matcher.group(4))) {
            styleContent = new SG(matcher.group(4), matcher.start(4), matcher.end(4));
        }
        
        SG MondayTag = null;
        if (StringUtils.isNotBlank(matcher.group(5))) {
            MondayTag = new SG(matcher.group(5), matcher.start(5), matcher.end(5));
        }
        
        return new MR(whole, scriptTag, scriptConent, styleTag, styleContent, MondayTag);
        
    }
    
    protected void searchScript(MR mr, Resource resource, Map<String, Reference> refs) throws IOException {
        SG scriptTag = mr.getScriptTag();
        SG scriptContent = mr.getScriptConent();
        Reference ref = null;
        Reference dataMainRef = null;
        
        if (null == scriptContent) {
            ref = extractRefFromTag(resource, PJSSRC, scriptTag);
            //处理requirejs的data-main
            if (null != ref) {
                dataMainRef = extractRefFromTag(resource, PJSDATAMAIN, scriptTag);
            }
        } else {
            ref = genTempRef(scriptContent, "js");
        }
        
        if (null != ref) {
            addReference(refs, ref);
        }
        
        if (null != dataMainRef) {
            String dataMainUrl = dataMainRef.getSourceAddr();
            if (!dataMainUrl.endsWith(".js")) {
                dataMainRef.setSourceAddr(dataMainUrl + ".js");
            }
            dataMainRef.setRequirejsDataMain(true);
            addReference(refs, dataMainRef);
        }
    }
    
    protected void searchStyle(MR mr, String sourceAddr, Map<String, Reference> refs) throws IOException {
        SG styleContent = mr.getStyleContent();
        if (null == styleContent) {
            return;
        }
        
        Reference ref = genTempRef(styleContent, "css");
        
        if (null != ref) {
            addReference(refs, ref);
        }
    }
    
    protected void searchMonday(MR mr, Resource resource, Map<String, Reference> refs) throws Exception {
        
        String tagName = mr.getMondayTag().getContent().trim();
        
        Reference ref = null;
        
        if ("img".equals(tagName) || "embed".equals(tagName) || "audio".equals(tagName) 
                || "video".equals(tagName)) {
            ref = extractRefFromTag(resource, MONDAYSRC, mr.getWhole());
        } else if ("link".equals(tagName)) {
            ref = extractRefFromTag(resource, MONDAYHREF, mr.getWhole());
        } else if ("object".equals(tagName)) {
            ref = extractRefFromTag(resource, MONDAYDATA, mr.getWhole());
        } else {
            throw new Exception("can not process '" + tagName + "' tag");
        }
        
        if (null != ref) {
            addReference(refs, ref);
        }
    }
    
    /**
     * 根据RefPattern从标签tagSG中抽取出引用ref,比如从<script ... src="abc.com"...中抽取出abc.com引用
     * @param sourceAddr 标签所属的源文件
     * @param RefPattern 需要抽取的引用的匹配规则
     * @param tagSG 标签
     * @return
     * 
     * TODO 现在默认取RefPattern匹配结果的第一个捕获分组，下一步增加一个参数int groupId,表示要取的捕获分组的编号
     */
    protected Reference extractRefFromTag(Resource resource, Pattern RefPattern, SG tagSG) {
        Matcher m = RefPattern.matcher(tagSG.getContent());
        if (m.find()) {
            String url = m.group(1);
            int startAtTag = m.start(1);
            if ((url.startsWith("'") && url.endsWith("'"))
                    || (url.startsWith("\"") && url.endsWith("\""))){
                url = url.substring(1, url.length()-1);
                startAtTag ++;
            }
            
            int startAtFile = tagSG.getStart() + startAtTag;
            int endAtFile = startAtFile + url.length();
            
            /*
             * TODO
             * 1. 需要将url后边的查询字符串滤掉再处理
             * 2. 如果url中包含有类似${url}的字符则不处理
             */
            Matcher ignoreMatcher = PATTRIGNORE.matcher(url);
            if (ignoreMatcher.find()) {
                StringBuilder sb = new StringBuilder("Ignore refence 【").append(url).append("】 @(")
                        .append(startAtFile).append(",").append(endAtFile).append(") beacauseof 【")
                        .append(ignoreMatcher.group()).append("】");
                addMessage(resource, new Message(MessageType.INFO, sb.toString()));
                return null;
            }
            
            if (url.startsWith("http://")) {
                //http 资源
                return new Reference(url, startAtFile, endAtFile, false);
                
            } else if (url.startsWith("/")) {
                //相对应用根的路径
                // TODO
            } else {
                //相对当前文件的路径
                String absoluteSrc = RoselyneFileUtils.makeAbsolutePath(resource.getSourceAddr(), url);
                return new Reference(absoluteSrc, startAtFile, endAtFile, false);
                
            }
            
        }
        return null;
    }
    
    /**
     * 将页面中类似<script>abc</script>或者<style>abc</style>中的脚本和样式生成一个临时引用，这个临时引用会有一个临时资源，此临时资源中的内容即是abc
     * @param contentSG
     * @param suffix
     * @return
     * @throws IOException
     */
    protected Reference genTempRef(SG contentSG, String suffix) throws IOException {
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append(getResourceResolveSupport().getTempDir()).append(DigestUtils.md5Hex(contentSG.getContent()))
            .append(System.currentTimeMillis()).append(".").append(suffix);
        String tempRefName = nameBuilder.toString();
        Reference ref = new Reference(tempRefName, contentSG.getStart(), contentSG.getEnd(), true);
        File tempAddr = new File( tempRefName);
        
        Resource resource = new Resource();
        resource.setTemp(true);
        resource.setTempAddr(tempAddr);
        resource.setOutputAddr(tempRefName + ".output");
        resource.setSourceAddr(tempRefName);
        resource.setReleaseAddr(tempRefName);
        resource.setMd5(false);
        ref.setResource(resource);
        
        RoselyneFileUtils.fileWrite(tempAddr, super.getResourceResolveSupport().getEncoding(), contentSG.getContent());
        return ref;
    }

    @Override
    protected String getProcessorName() {
        return null;
    }
    
    public static void main7(String[] args) {
        String source = "<img src=\"${proj.url}\" width=\"120\" height=\"90\" alt=\"${proj.projName}\"/>";
        Matcher m = PATTRIGNORE.matcher(source);
        if (m.find()){
            System.out.println(m.group());
        }
    }
    
    public static void main5(String[] args) {
        String source = "<script> _this_parent.style.height = (_hzy_arrow1_content_height)+\"px\";that.style.backgroundImage = \"url(http://10.10.90.156/sceapp/focus_static/wap/images/213.png)\"; //折叠_hzy_arowBtn1 = false";
        Matcher m = Pattern.compile("\\burl\\s*?\\(\\s*([\\w:\\/\\\\\\.]+)\\s*?\\)").matcher(source);
        if (m.find()) {
            System.out.println(m.group());
        }
    }
    
    /**
     * 测试JS_SRC
     * @param args
     */
    public static void main4(String[] args) {
        String source = "<script type=\"text/javascript\" src=\"http://10.10.90.156/sceapp/focus_static/wap/js/jquery2.0.3.min.js\">";
        Matcher m = PJSSRC.matcher(source);
        if (m.find()) {
            System.out.println(m.group());
        }
    }
    
    /**
     * 测试searchReference方法
     * @param args
     * @throws ResourceProcessException
     */
    public static void main6(String[] args) throws ResourceProcessException {
        HTMLResourceProcessor processor = new HTMLResourceProcessor(null, null, null);
        Resource r = new Resource();
        r.setTempAddr(new File("D:\\workspace1\\test\\target\\fis\\temp\\1383212607293"));
        processor.searchReference(r);
    }
    
    /**
     * 测试PSEARCHSCRIPT正则表达式
     * @param args
     * @throws ResourceProcessException
     */
    public static void main2(String[] args) throws ResourceProcessException {
        String sourceAddr = "D:\\workspace1\\test\\target\\fis\\temp\\1383212607293";
        File f = new File(sourceAddr);
        BufferedReader br = null;
        try {
            FileReader fr = new FileReader(f);
            br = new BufferedReader(fr);
            StringBuffer sb = new StringBuffer();
            String line = br.readLine();
            while (null != line) {
                sb.append(line);
                line = br.readLine();
            }
            //System.out.println(sb);
            Matcher matcher = PSEARCHSCRIPT.matcher(sb);
            boolean matched = matcher.find();
            while (matched) {
                int groupCount = matcher.groupCount();
                System.out.println("groupCount:" + groupCount + " start:" + matcher.start() + " end:" + matcher.end());
                String script = matcher.group();
                StringBuilder infoBuilder = new StringBuilder("Resource[").append(sourceAddr)
                        .append("] searched script [").append(script).append("]");
                System.out.println(infoBuilder.toString());
                for (int i=1; i<=groupCount; i++) {
                    System.out.println("group" + i + ": " + matcher.group(i));
                }
                
                System.out.println("-------------------------------------------");
                matched = matcher.find();
            }
            
        } catch (FileNotFoundException e) {
            StringBuilder errorBuilder = new StringBuilder("Resource[").append(sourceAddr)
                    .append("] does not have valid tempAddr ");
            String error = errorBuilder.toString();
            System.out.println(errorBuilder.toString());
            throw new ResourceProcessException(error);
        } catch (Exception e) {
            throw new ResourceProcessException("",e);
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                throw new ResourceProcessException("", e);
            }
        }
    }
    
    /**
     * 测试processResource(...)方法
     * @param args
     * @throws ResourceNotFoundException 
     * @throws ResourceInvalidException 
     * @throws ResourceProcessException 
     */
    public static void main(String[] args) {
        
        Resource resource = new Resource();
        resource.setSourceAddr("D:\\workspace1\\test\\src\\main\\webapp\\views\\home.jsp");
        resource.setReleaseAddr("http://m.focus.cn/home");
        resource.setOutputAddr("D:\\workspace1\\test\\target\\fis\\output\\home.jsp");
        resource.setTempAddr(new File("D:\\workspace1\\test\\target\\fis\\temp\\1383212607293"));
        
        MagicResource magicResource = new MagicResource();
        magicResource.setSourceAddr("");
        magicResource.setReleaseAddr("http://a1.itc.cn/sceapp/focus_static/wap/$1");
        magicResource.setOutputAddr("D:\\workspace1\\test\\target\\fis\\output\\$1");
        magicResource.setTempAddr(null);
        List<Pattern> includes = new ArrayList<Pattern>();
        includes.add(Pattern.compile("http://10.10.90.156/sceapp/focus_static/wap/(js/.*\\.js)"));
        includes.add(Pattern.compile("http://10.10.90.156/sceapp/focus_static/wap/(images/.*\\.png)"));
        includes.add(Pattern.compile("http://10.10.90.156/sceapp/focus_static/wap/(css/.*\\.css)"));
                
        magicResource.setIncludePatterns(includes);
        magicResource.setExcludePatterns(null);
        
        Map<String, Resource> staticResources = new HashMap<String, Resource>();
        staticResources.put(resource.getSourceAddr(), resource);
        List<MagicResource> magicResourceList = new ArrayList<MagicResource>();
        magicResourceList.add(magicResource);
        
        HTMLResourceProcessor processor = new HTMLResourceProcessor(new ResourceResolveSupportAdaptor(){

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
                
            }
            
        }, new ResourceAcquirerFactory(), null);
        
        try {
            processor.processResource(resource, staticResources, magicResourceList, null);
        } catch (ResourceProcessException e) {
            e.printStackTrace();
        } catch (ResourceInvalidException e) {
            e.printStackTrace();
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
        }
        
    }
    
    /**
     * 匹配结果
     * @author rogantian
     * @date 2013-11-4
     * @email rogantianwz@gmail.com
     */
    private class MR {
        
        private SG whole;
        
        private SG scriptTag;
        
        private SG scriptConent;
        
        private SG styleTag;
        
        private SG styleContent;
        
        private SG MondayTag;

        public SG getWhole() {
            return whole;
        }

        public SG getScriptTag() {
            return scriptTag;
        }

        public SG getScriptConent() {
            return scriptConent;
        }

        public SG getStyleTag() {
            return styleTag;
        }

        public SG getStyleContent() {
            return styleContent;
        }

        public SG getMondayTag() {
            return MondayTag;
        }

        public MR(SG whole, SG scriptTag, SG scriptConent, SG styleTag, SG styleContent, SG mondayTag) {
            super();
            this.whole = whole;
            this.scriptTag = scriptTag;
            this.scriptConent = scriptConent;
            this.styleTag = styleTag;
            this.styleContent = styleContent;
            MondayTag = mondayTag;
        }

        @Override
        public String toString() {
            return "MR [whole=" + whole + "\n    scriptTag=" + scriptTag + "\n    scriptConent=" + scriptConent + "\n    styleTag="
                    + styleTag + "\n    styleContent=" + styleContent + "\n    MondayTag=" + MondayTag + "]";
        }
        
        
    }

    /**
     * 不压缩，so do nothing
     */
    @Override
    protected void compressResource(Resource resource, ResourceCompressor compressor) throws ResourceProcessException {
        // do nothing
        addMessage(resource, new Message(MessageType.INFO, "does not need compress because of it's a html resource"));
    }
    
}
