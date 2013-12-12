package cn.focus.m.plugin.roselyne.io.resource;

import cn.focus.m.plugin.roselyne.descriptor.Config;

import java.util.List;

public interface ResourceResolver {

    /**
     * 解析能在一开始就能加载进来的资源（即loadImmeditely=true的fileSet)
     * @param fisConfig
     * @param support
     * @return
     * @throws ResourceResolveException
     */
    public List<Resource> resolve(Config fisConfig, ResourceResolveSupport support) throws ResourceResolveException;
    
    
    /**
     * 解析loadImmetitely=false(默认)的fileSet
     * @param fisConfig
     * @param support
     * @return
     * @throws ResourceResolveException
     */
    public List<MagicResource> resolveMagic(Config fisConfig, ResourceResolveSupport support) throws ResourceResolveException;
    
}
