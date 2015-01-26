package cn.focus.m.plugin.roselyne.processor;

import cn.focus.m.plugin.roselyne.Message;
import cn.focus.m.plugin.roselyne.compress.ResourceCompressor;
import cn.focus.m.plugin.roselyne.io.resource.Resource;
import cn.focus.m.plugin.roselyne.io.resource.ResourceAcquirerFactory;
import cn.focus.m.plugin.roselyne.io.resource.ResourceResolveSupport;
import cn.focus.m.plugin.roselyne.utils.RoselyneFileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StyleResourceProcessor extends AbstractResourceProcessor {
    
//    private static String STYLE_URL = "\\burl\\s*?\\(\\s*([-\\w:\\/\\\\\\.]+)\\s*?\\)";
	//支持url里有分号的情况
    private static String STYLE_URL = "\\burl\\s*?\\(\\s*[\"']?\\s*([-\\w:\\/\\\\\\.]+)\\s*[\"']?\\s*?\\)";
    
    /**
     * 查找类似<script>....."url(http://10.10.90.156/sceapp/focus_static/wap/images/213.png)"......</script>中的url，
     * 参考测试函数main5(..)
     */
    private static Pattern PSTYLEURL = Pattern.compile(STYLE_URL);
    
    public StyleResourceProcessor(ResourceResolveSupport resourceResolveSupport,
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
            Matcher matcher = PSTYLEURL.matcher(sb);
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
    
    private MR genMR(Matcher matcher) {
        if (null == matcher) {
            return null;
        }
        SG whole = new SG(matcher.group(), matcher.start(), matcher.end());
        
        SG urlContent = new SG(matcher.group(1), matcher.start(1), matcher.end(1));
        return new MR(whole, urlContent);
    }
    
    private void searchUrl(MR mr, String sourceAddr, Map<String, Reference> refs) {
        SG urlContent = mr.getUrlContent();
        String url = RoselyneFileUtils.normalizPathIfNeed(sourceAddr, urlContent.getContent());
        Reference ref = new Reference(url, urlContent.getStart(), urlContent.getEnd(), false);
        addReference(refs, ref);
    }
    
    protected void debugMRS(List<MR> mrs) {
        for (MR mr : mrs) {
            System.out.println(mr);
        }
    }
    
    protected void debugRefs(Map<String, Reference> refs) {
        Set<Entry<String, Reference>> entries = refs.entrySet();
        
        for (Entry<String, Reference> entry : entries) {
            Reference ref = entry.getValue();
            System.out.println(ref.getSourceAddr() + ": " + ref.getBounds());
        }
    }

    @Override
    protected String getProcessorName() {
        // TODO Auto-generated method stub
        return null;
    }
    
    private class MR {
        private SG whole;
        
        private SG urlContent;

        @SuppressWarnings("unused")
        public SG getWhole() {
            return whole;
        }

        public SG getUrlContent() {
            return urlContent;
        }

        public MR(SG whole, SG urlContent) {
            super();
            this.whole = whole;
            this.urlContent = urlContent;
        }

        @Override
        public String toString() {
            return "MR [whole=" + whole + ", urlContent=" + urlContent + "]";
        }
        
        
    }

}
