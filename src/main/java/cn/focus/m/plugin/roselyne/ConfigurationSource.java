package cn.focus.m.plugin.roselyne;

import java.io.File;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

/**
 * 获取配置文件的接口
 * @author rogantian
 * @date 2013-12-12
 * @email rogantianwz@gmail.com
 */
public interface ConfigurationSource extends MessageHolder{

    /**
     * 获取多个描述文件
     * @return
     */
    String[] getDiscriptors();
    
    /**
     * 获取描述文件
     * @return
     */
    String getDescriptor();
    
    /**
     * 获取文件基本路径
     * @return
     */
    File getBaseDir();
    
    /**
     * 获取Project的Maven对象
     * @return
     */
    MavenProject getProject();
    
    /**
     * 是否允许忽略不存在的描述文件
     * @return
     */
    boolean isIgnoreMissingDescriptor();
    
    MavenSession getMavenSession();
    
    ArtifactRepository getLocalRepository();
    
}