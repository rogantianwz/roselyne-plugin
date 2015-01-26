package cn.focus.m.plugin.roselyne.processor;

import cn.focus.m.plugin.roselyne.Message;
import cn.focus.m.plugin.roselyne.compress.ResourceCompressor;
import cn.focus.m.plugin.roselyne.io.resource.Resource;
import cn.focus.m.plugin.roselyne.io.resource.ResourceAcquirerFactory;
import cn.focus.m.plugin.roselyne.io.resource.ResourceResolveSupport;
import cn.focus.m.plugin.roselyne.io.resource.ResourceResolveSupportAdaptor;
import cn.focus.m.plugin.roselyne.utils.RoselyneFileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSResourceProcessor extends AbstractResourceProcessor {

//    private static String JS_URL = "\\burl\\s*?\\(\\s*([\\w:\\/\\\\\\.]+)\\s*?\\)";
    private static String JS_URL = "\\burl\\s*?\\(\\s*[\"']?\\s*([\\w:\\/\\\\\\.]+)\\s*[\"']?\\s*?\\)";
    
    /**
     * 查找类似<script>....."url(http://10.10.90.156/sceapp/focus_static/wap/images/213.png)"......</script>中的url，
     * 参考测试函数main5(..)
     */
    private static Pattern PJSURL = Pattern.compile(JS_URL);
    
    public JSResourceProcessor(ResourceResolveSupport resourceResolveSupport,
            ResourceAcquirerFactory resourceAcquirerFactory, ResourceCompressor resourceCompressor) {
        super();
        super.setResourceAcquirerFactory(resourceAcquirerFactory);
        super.setResourceResolveSupport(resourceResolveSupport);
        super.setResourceCompressor(resourceCompressor);
    }

    @Override
    protected Set<Reference> searchReference(Resource resource) throws ResourceProcessException {
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
            Matcher matcher = PJSURL.matcher(sb);
            boolean matched = matcher.find();
            List<MR> mrs = new ArrayList<MR>();
            while (matched) {
              MR mr = genMR(matcher);
              if (null != mr) {
                  mrs.add(mr);
                  /*StringBuilder infoBuilder = new StringBuilder("Resource[").append(resource.getSourceAddr())
                          .append("] searched reference [").append(mr.getWhole().getContent()).append("]");
                  super.addMessage(MessageType.INFO, infoBuilder.toString());*/
              }
              matched = matcher.find();
            }
            
            Map<String, Reference> refs = new HashMap<String, Reference>();
            for (MR mr : mrs) {
                if (null != mr) {
                    searchUrl(mr, resource.getSourceAddr(), refs);
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
    
    private void searchUrl(MR mr, String sourceAddr, Map<String, Reference> refs) {
        SG urlContent = mr.getUrlContent();
        Reference ref = new Reference(urlContent.getContent(), urlContent.getStart(), urlContent.getEnd(), false);
        addReference(refs, ref);
    }
    
    private MR genMR(Matcher matcher) {
        if (null == matcher) {
            return null;
        }
        SG whole = new SG(matcher.group(), matcher.start(), matcher.end());
        
        SG urlContent = new SG(matcher.group(1), matcher.start(1), matcher.end(1));
        return new MR(whole, urlContent);
    }

    @Override
    protected String getProcessorName() {
        // TODO Auto-generated method stub
        return null;
    }
    
    public static void main(String[] args) {
        Resource r = new Resource();
        r.setTempAddr(new File("D:\\workspace1\\test\\target\\fis\\temp\\f7e4ee018321f08287333dad43d40581.js"));
        JSResourceProcessor p = new JSResourceProcessor(new ResourceResolveSupportAdaptor(){

            public void addMessage(MessageType type, String content) {
            }

            public String getTempDir() {
                return "D:\\workspace1\\test\\target\\fis\\temp";
            }

            public String getEncoding() {
                return "UTF-8";
            }

            public void printResolveLog(Message msg) {
                // TODO Auto-generated method stub
                
            }
            
        }, new ResourceAcquirerFactory(), null);
        
        try {
            p.searchReference(r);
        } catch (ResourceProcessException e) {
            e.printStackTrace();
        }
    }
    
    private class MR {
        private SG whole;
        
        private SG urlContent;
        
        public MR(SG whole, SG urlContent) {
            this.whole = whole;
            this.urlContent = urlContent;
        }

        @SuppressWarnings("unused")
        public SG getWhole() {
            return whole;
        }

        public SG getUrlContent() {
            return urlContent;
        }

        @Override
        public String toString() {
            return "MR [whole=" + whole + ", urlContent=" + urlContent + "]";
        }
    }

}
