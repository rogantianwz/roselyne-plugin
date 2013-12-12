package cn.focus.m.plugin.roselyne.processor;

import cn.focus.m.plugin.roselyne.Message;
import cn.focus.m.plugin.roselyne.compress.ResourceCompressor;
import cn.focus.m.plugin.roselyne.io.resource.Resource;
import cn.focus.m.plugin.roselyne.io.resource.ResourceAcquirerFactory;
import cn.focus.m.plugin.roselyne.io.resource.ResourceResolveSupport;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.codehaus.plexus.util.FileUtils;

public class ImageResourceProcessor extends AbstractResourceProcessor {

    public ImageResourceProcessor(ResourceResolveSupport resourceResolveSupport,
            ResourceAcquirerFactory resourceAcquirerFactory, ResourceCompressor resourceCompressor) {
        super();
        super.setResourceAcquirerFactory(resourceAcquirerFactory);
        super.setResourceResolveSupport(resourceResolveSupport);
        super.setResourceCompressor(resourceCompressor);
    }

    @Override
    protected Set<Reference> searchReference(Resource resource) throws ResourceProcessException {
        return null;
    }

    @Override
    protected String getProcessorName() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * 父类中该方法是以文本的方式读取源文件，然后在以文本的方式写入到output的，不适合ImageResourceProcessor，所以这里要重载该方法
     */
    @Override
    protected void replaceReference(Resource resource, Set<Reference> references) throws ResourceProcessException {
        File source = resource.getTempAddr();
        File dest = new File(resource.getOutputAddr());
        try {
            FileUtils.copyFile(source, dest);
        } catch (IOException e) {
            throw new ResourceProcessException(e);
        }
    }

    /**
     * 图片资源不需要压缩，所以重载父类该方法AND DO NOTHING IN IT
     */
    @Override
    protected void compressResource(Resource resource, ResourceCompressor compressor) throws ResourceProcessException {
        // does not compress image
        addMessage(resource, new Message(MessageType.INFO, "does not need compress because of it's an image resource"));
    }

}
