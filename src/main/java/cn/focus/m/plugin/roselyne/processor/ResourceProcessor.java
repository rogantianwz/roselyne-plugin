package cn.focus.m.plugin.roselyne.processor;

import cn.focus.m.plugin.roselyne.MessageHolder;
import cn.focus.m.plugin.roselyne.io.resource.MagicResource;
import cn.focus.m.plugin.roselyne.io.resource.Resource;
import cn.focus.m.plugin.roselyne.io.resource.ResourceInvalidException;
import cn.focus.m.plugin.roselyne.io.resource.ResourceNotFoundException;
import cn.focus.m.plugin.roselyne.io.resource.ResourceProcessHandler;

import java.util.List;
import java.util.Map;

/**
 * 资源处理器
 * @author rogantian
 *
 */
public interface ResourceProcessor extends MessageHolder {

    boolean processResource(Resource resource, Map<String, Resource> staticResources,
            List<MagicResource> magicResources, ResourceProcessHandler handler) 
                    throws ResourceProcessException, ResourceInvalidException, ResourceNotFoundException;
}
