package cn.focus.m.plugin.roselyne.processor;

import cn.focus.m.plugin.roselyne.Message;
import cn.focus.m.plugin.roselyne.compress.ResourceCompressException;
import cn.focus.m.plugin.roselyne.compress.ResourceCompressor;
import cn.focus.m.plugin.roselyne.io.resource.MagicResource;
import cn.focus.m.plugin.roselyne.io.resource.Resource;
import cn.focus.m.plugin.roselyne.io.resource.ResourceAcquirer;
import cn.focus.m.plugin.roselyne.io.resource.ResourceAcquirerFactory;
import cn.focus.m.plugin.roselyne.io.resource.ResourceInvalidException;
import cn.focus.m.plugin.roselyne.io.resource.ResourceNotFoundException;
import cn.focus.m.plugin.roselyne.io.resource.ResourceProcessHandler;
import cn.focus.m.plugin.roselyne.io.resource.ResourceResolveSupport;
import cn.focus.m.plugin.roselyne.processor.Reference.Bound;
import cn.focus.m.plugin.roselyne.utils.RoselyneFileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;


/**
 * 资源处理流程模板
 * b. 搜索可能需要处理的引用
 *      c. 判断该引用是不是需要处理
 *      d. 如果需要处理的话则获取该引用所引用的资源
 *      e. 处理该资源(递归过程,重复资源处理流程)
 * f. 使用处理过的资源来替换引用, 将资源输出到outputAddr
 * g. 资源压缩和混淆
 * h. md5Process
 * i. 打印处理日志
 * @author rogantian
 * @date 2013-11-1
 * @email rogantianwz@gmail.com
 */
public abstract class AbstractResourceProcessor implements ResourceProcessor{
    
    private ResourceAcquirerFactory resourceAcquirerFactory;
    
    private ResourceResolveSupport resourceResolveSupport;
    
    private ResourceCompressor resourceCompressor;
    
    private Map<Resource, List<Message>> messages = new HashMap<Resource, List<Message>>();
    
    public void addMessage(MessageType type, String content) {
        StringBuilder sb = new StringBuilder("【").append(type.toString()).append("】:").append(content);
        System.out.println(sb);
    }
    
    public void addMessage(Resource resource, Message msg) {
        List<Message> resourceMessages = messages.get(resource);
        if (null == resourceMessages) {
            resourceMessages = new ArrayList<Message>();
        }
        resourceMessages.add(msg);
        messages.put(resource, resourceMessages);
    }

    public boolean processResource(Resource resource, Map<String, Resource> staticResources,
            List<MagicResource> magicResources, ResourceProcessHandler handler) 
                    throws ResourceProcessException, ResourceInvalidException, ResourceNotFoundException {
        
        //a.
        Resource oldResource = searchOldResource(resource, staticResources);
        if (null != oldResource) {
            resource = oldResource;
            return true;
        }
        
        //b.
        Set<Reference> references = searchReference(resource);
        
        if (null != references) {
            Iterator<Reference> it = references.iterator();
            while (it.hasNext()) {
                
                Reference reference = it.next();
                //c.
                boolean referenceNeededProcess = false;
                try {
                    referenceNeededProcess = judgeReference(reference, staticResources, magicResources);
                } catch (IOException e) {
                    throw new ResourceProcessException(e);
                }
                if (!referenceNeededProcess) {
                    
                    //debug:
                    addMessage(resource, new Message(MessageType.DEBUG, "Reference not need process 【" + reference.getSourceAddr() + "】"));
                    
                    it.remove();
                    continue;
                }
                
                //d.
                Resource referenceResource = acquireResource(reference);
                if (null == referenceResource) {
                    
                    //debug
                    addMessage(resource, new Message(MessageType.DEBUG, "Can not acquire reference's resource【" + reference.getSourceAddr() + "】"));
                    
                    it.remove();
                    continue;
                }
                
                //e.
                boolean processResult = resourceProcess(referenceResource, handler);
                if (!processResult) {
                    
                    //debug
                    addMessage(resource, new Message(MessageType.DEBUG, "Reference process failure 【" + reference.getSourceAddr() + "】"));
                    
                    it.remove();
                    continue;
                }
                
                reference.setResource(referenceResource);
                
                staticResources.put(reference.getSourceAddr(), referenceResource); 
                
            }
        }
        
        //f.
        replaceReference(resource, references);
        
        completeReferencesProcess(references);
        
        //g.
        compressResource(resource, resourceCompressor);
        
        //h.
        md5Process(resource, resourceResolveSupport);
        
        //i.
        printResolveLog(resource, handler);
        
        //标识该资源已经处理完成
        resource.setDone(true);
        
        return true;
    }
    
    /**
     * 查找是否处理过该资源，如果处理过则返回该resource
     * @param resource
     * @param staticResources
     * @return
     */
    protected Resource searchOldResource(Resource resource, Map<String, Resource> staticResources) {
        String sourceAddr = resource.getSourceAddr();
        
        Resource oldResource = staticResources.get(sourceAddr);
        if (null != oldResource && oldResource.isDone()) {
            return oldResource;
        }
        return null;
    }
    
    /**
     * 搜索可能需要处理的引用
     */
    protected abstract Set<Reference> searchReference(Resource resource) throws ResourceProcessException;
    
    protected void printRefs(Resource resource, Map<String, Reference> refs) {
        Set<Entry<String, Reference>> entries = refs.entrySet();
        addMessage(resource, new Message(MessageType.INFO, "Ref size:" + entries.size()));
        for (Entry<String, Reference> entry : entries) {
            Reference ref = entry.getValue();
            StringBuilder infoBuilder = new StringBuilder("Searched reference [")
                    .append(ref.getSourceAddr()).append("] @ ").append(ref.getBounds());
            addMessage(resource, new Message(MessageType.INFO, infoBuilder.toString()));
        }
    }
    
    /**
     * 判断该引用是不是需要处理<br>
     * 并获取该引用关联的resource,可能是staticResource，也有可能是根据magicResource生成的resource(此时会初始化releaseAddr和outputAddr)<br>
     * 如果reference是一个临时引用，会特别处理(因为临时引用已经关联了tempResource)
     * @return
     * @throws IOException 
     */
    protected boolean judgeReference(Reference reference, Map<String, Resource> staticResources,
            List<MagicResource> magicResources) throws IOException {
        if (null == reference) {
            return false;
        }
        
        if (reference.isTemp()) {
            return true;
        }
        
        String sourceAddr = reference.getSourceAddr();
        
        Resource resource = staticResources.get(sourceAddr);
        if (null == resource) {
            if (null != magicResources) {
                for (MagicResource magicResource : magicResources) {
                    resource = matchMagicResource(reference, magicResource);
                    if (null != resource) {
                        resource.setMd5(magicResource.isMd5());
                        resource.setMd5len(magicResource.getMd5len());
                        break;
                    }
                }
            }
        }
        
        if (null == resource) {
            return false;
        } else {
            reference.setResource(resource);
            return true;
        }
    }
    
    /**
     * 在magicResource中匹配reference
     * @param reference
     * @param magicResource
     * @return
     * @throws IOException 
     */
    protected Resource matchMagicResource(Reference reference, MagicResource magicResource) throws IOException {
        if (null == reference || null == magicResource) {
            return null;
        }
        
        String sourceAddr = reference.getSourceAddr();
        
        List<Pattern> includes = magicResource.getIncludePatterns();
        Pattern matchedPattern = null;
        if (null != includes) {
            for (Pattern include : includes) {
                if (include.matcher(sourceAddr).matches()) {
                    matchedPattern = include;
                    break;
                }
            }
        }
        
        if (null == matchedPattern) {
            return null;
        }
        
        List<Pattern> excludes = magicResource.getExcludePatterns();
        if (null != excludes) {
            for (Pattern exclude : excludes) {
                if (exclude.matcher(sourceAddr).matches()) {
                    matchedPattern = null;
                    break;
                }
            }
        }
        
        if (null == matchedPattern) {
            return null;
        } else {
            String outputAddr = magicResource.getOutputAddr();
            String releaseAddr = magicResource.getReleaseAddr();
            //String tempAddr = magicResource.getTempAddr().getAbsolutePath();
            
            Matcher matcher= matchedPattern.matcher(sourceAddr);
            
            Resource resource = new Resource();
            //resource.setSourceAddr(new File(sourceAddr).getCanonicalPath());
            resource.setSourceAddr(sourceAddr);
            /**
             * TODO 本地路径中\\\\的问题
             */
            String newOutput = matcher.replaceAll(outputAddr.replaceAll("\\\\", "\\\\\\\\"));
            resource.setOutputAddr(new File(newOutput).getCanonicalPath());
            
            //resource.setOutputAddr(matcher.replaceAll(outputAddr.replaceAll("\\\\", "\\\\\\\\")));
            //resource.setOutputAddr(matcher.replaceAll(outputAddr));
            
            resource.setReleaseAddr(matcher.replaceAll(releaseAddr).replaceAll("\\\\", "\\\\\\\\"));
            //resource.setTempAddr(new File(matcher.replaceAll(tempAddr)));
            return resource;
        }
    }
    
    /**
     * 如果需要处理的话则获取该引用所引用的资源</br>
     * 临时资源不需要，已经处理过的资源不需要
     * @return
     * @throws ResourceInvalidException 
     * @throws ResourceNotFoundException 
     */
    protected Resource acquireResource(Reference reference) throws ResourceInvalidException, ResourceNotFoundException {
        Resource resource = reference.getResource();
        if (reference.isTemp() || resource.isDone()) {
            return resource;
        }
        
        ResourceAcquirer resourceAcquire = resourceAcquirerFactory.getResourceAcquirer(reference.getSourceAddr());
        
        File file = resourceAcquire.acquireResource(resource.getSourceAddr(), resourceResolveSupport.getTempDir());
        resource.setTempAddr(file);
        return resource;
    }
    
    /**
     * 处理该资源(递归过程,重复资源处理流程)
     * @return
     * @throws ResourceProcessException 
     * @throws ResourceInvalidException 
     * @throws ResourceNotFoundException 
     */
    protected boolean resourceProcess(Resource resource, ResourceProcessHandler handler) 
            throws ResourceProcessException, ResourceInvalidException, ResourceNotFoundException {
        
        return handler.processResource(resource);
    }
    
    /**
     * 使用处理过的资源来替换引用（可以在completeReferencesProcess中完成）
     * @throws ResourceProcessException 
     */
    protected void replaceReference(Resource resource, Set<Reference> references) throws ResourceProcessException {
        //String sourceAddr = resource.getSourceAddr();
        String source = null;
        StringBuilder dest = new StringBuilder();
        try {
            source = RoselyneFileUtils.fileRead(resource.getTempAddr(), resourceResolveSupport.getEncoding());
        } catch (IOException e) {
            throw new ResourceProcessException(e);
        }
        
        if (null != references && references.size() > 0) {
            
            Map<Integer, Reference> boundRefRel = new HashMap<Integer, Reference>();
            Set<Bound> treeBounds = new TreeSet<Bound>();
           
            for (Reference ref : references) {
                Set<Bound> bounds = ref.getBounds();
                if (null == bounds || bounds.size() == 0) {
                    continue;
                }
                for (Bound bound : bounds) {
                    boundRefRel.put(bound.getStartAtResource(), ref);
                    treeBounds.add(bound);
                }
                
            }
            List<String> splitedSources = new ArrayList<String>();
            Map<Integer, Reference> splitedSourceRefRel = new HashMap<Integer, Reference>();
            int lastIndex = 0;
            for (Bound bound : treeBounds) {
                int start = bound.getStartAtResource();
                int end = bound.getEndAtResource();
                
                splitedSources.add(source.substring(lastIndex, start));
                
                splitedSources.add(source.substring(start, end));
                splitedSourceRefRel.put((splitedSources.size()-1), boundRefRel.get(start));
                
                lastIndex = end;
            }
            
            splitedSources.add(source.substring(lastIndex));
            
            Set<Entry<Integer, Reference>> entries = splitedSourceRefRel.entrySet();
            for (Entry<Integer, Reference> entry : entries) {
                int splitedSourcesIndex = entry.getKey();
                Reference ref = entry.getValue();
                if (ref.isTemp()) {
                    try {
                     String tempResourceContent = RoselyneFileUtils.fileRead(new File(ref.getResource().getOutputAddr()), resourceResolveSupport.getEncoding());
                     StringBuilder sb = new StringBuilder("引用替换【").append(ref.getResource().getTempAddr().getAbsolutePath())
                             .append("】==》【").append(ref.getResource().getOutputAddr()).append("】");
                     addMessage(resource, new Message(MessageType.INFO, sb.toString()));
                     splitedSources.set(splitedSourcesIndex, tempResourceContent);
                 } catch (IOException e) {
                     throw new ResourceProcessException(e);
                 }
                } else {
                    String releaseAddr = ref.getResource().getReleaseAddr();
                    StringBuilder sb = new StringBuilder("引用替换【").append(splitedSources.get(splitedSourcesIndex))
                            .append("】==》【").append(releaseAddr).append("】");
                    addMessage(resource, new Message(MessageType.INFO, sb.toString()));
                    splitedSources.set(splitedSourcesIndex, ref.getResource().getReleaseAddr());
                }
            }
            for (String str : splitedSources) {
                dest.append(str);
            }
        } else {
            dest.append(source);
        }
        
         try {
             File destFile = new File(resource.getOutputAddr());
             String destStr = dest.toString();
             String versionCode = DigestUtils.md5Hex(destStr);
             resource.setVersionCode(versionCode);
             RoselyneFileUtils.fileWrite(destFile, resourceResolveSupport.getEncoding(), destStr);
         } catch (IOException e) {
             throw new ResourceProcessException(e);
         }
    }
    
    
    /**
     * 所有引用处理完成之后，MD5Process之前的处理(可以在此统一处理引用替换）
     * @param references
     */
    protected void completeReferencesProcess(Set<Reference> references) {
        
    }
    
    /**
     * 资源压缩和混淆
     * @param resource
     */
    protected void compressResource(Resource resource, ResourceCompressor compressor) throws ResourceProcessException{
        if (resource.isTemp()) {
            addMessage(resource, new Message(MessageType.INFO, "does not need compress because of it's a temp resource"));
            return;
        }
        try {
            //System.out.println("Begin Compress file:" + resource.getOutputAddr());
            compressor.compressResource(resource, this);
        } catch (ResourceCompressException e) {
            addMessage(resource, new Message(MessageType.ERROR,"resource compress error:" + e.getMessage()));
            e.printStackTrace();
            throw new ResourceProcessException();
        }
        addMessage(resource, new Message(MessageType.INFO, "compress complete"));
    }
    
    /**
     * md5处理，给资源文件加上类似MD5的prefix或者suffix</br>
     * 临时资源不需要再做md5处理,html、style、js在做replace的时候顺便做了一个md5，img则没有
     * @param resource
     * @throws ResourceProcessException 
     */
    protected void md5Process(Resource resource, ResourceResolveSupport resourceResolveSupport) throws ResourceProcessException {
        if (!resource.isTemp() && resource.isMd5()) {
            String vc = resource.getVersionCode();
            if (StringUtils.isBlank(vc)) {
                //图片资源需要在此计算md5
                try {
                    vc = RoselyneFileUtils.fileMd5(new File(resource.getOutputAddr()), resourceResolveSupport.getEncoding());
                } catch (IOException e) {
                    throw new ResourceProcessException(e);
                }
            }
            
            if (StringUtils.isBlank(vc)) {
                throw new ResourceProcessException("Can not get the version code of [" + resource.getSourceAddr() +"]");
            }
            String suffix = "_" + vc;
            String newOutputAddr = RoselyneFileUtils.addSuffixBeforeExtension(resource.getOutputAddr(), suffix);
            String newReleaseAddr = RoselyneFileUtils.addSuffixBeforeExtension(resource.getReleaseAddr(), suffix);
            
            if (StringUtils.isNotBlank(newOutputAddr) && StringUtils.isNotBlank(newReleaseAddr)) {
                try {
                    FileUtils.rename(new File(resource.getOutputAddr()), new File(newOutputAddr));
                } catch (IOException e) {
                    throw new ResourceProcessException(e);
                }
                resource.setOutputAddr(newOutputAddr);
                resource.setReleaseAddr(newReleaseAddr);
            } else {
                throw new ResourceProcessException("Can not generate the right path when do md5process @ " + resource.getSourceAddr());
            }
        }
    }
    
    protected void printResolveLog(Resource resource, ResourceProcessHandler handler) {
        if (null == resource) {
            return;
        }
        
        List<Message> resourceMessages = messages.get(resource);
        if (null != resourceMessages) {
            Message begin = new Message(MessageType.SEPERATOR, "-------------Resource【" + resource.getSourceAddr() + "】process log begin-------------");
            Message end = new Message(MessageType.SEPERATOR, "-------------Resource【" + resource.getSourceAddr() + "】process log end---------------");
            handler.printLog(begin);
            for (Message msg : resourceMessages) {
                handler.printLog(msg);
            }
            handler.printLog(end);
        }
    }
    
    /**
     * 获取处理器名称
     * @return
     */
    protected abstract String getProcessorName();
    
    protected void addReference(Map<String, Reference> refs, Reference ref) {
        String sourceAddr = ref.getSourceAddr();
        if (StringUtils.isBlank(sourceAddr)) {
            return;
        }
        
        Reference oldRef = refs.get(sourceAddr);
        if (null == oldRef) {
            oldRef = ref;
        } else {
            oldRef.addBounds(ref.getBounds());
        }
        
        refs.put(sourceAddr, oldRef);
    }

    public ResourceAcquirerFactory getResourceAcquirerFactory() {
        return resourceAcquirerFactory;
    }

    public void setResourceAcquirerFactory(ResourceAcquirerFactory resourceAcquirerFactory) {
        this.resourceAcquirerFactory = resourceAcquirerFactory;
    }

    public ResourceResolveSupport getResourceResolveSupport() {
        return resourceResolveSupport;
    }

    public void setResourceResolveSupport(ResourceResolveSupport resourceResolveSupport) {
        this.resourceResolveSupport = resourceResolveSupport;
    }

    public ResourceCompressor getResourceCompressor() {
        return resourceCompressor;
    }

    public void setResourceCompressor(ResourceCompressor resourceCompressor) {
        this.resourceCompressor = resourceCompressor;
    }
    
}
