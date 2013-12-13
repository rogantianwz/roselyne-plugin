package cn.focus.m.plugin.roselyne.mojos;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import cn.focus.m.plugin.roselyne.ConfigurationSource;
import cn.focus.m.plugin.roselyne.InvalidConfigurationException;
import cn.focus.m.plugin.roselyne.Message;
import cn.focus.m.plugin.roselyne.compress.CompressConfig;
import cn.focus.m.plugin.roselyne.compress.ResourceCompressor;
import cn.focus.m.plugin.roselyne.descriptor.Config;
import cn.focus.m.plugin.roselyne.io.DefaultConfigurationReader;
import cn.focus.m.plugin.roselyne.io.ConfigurationReadException;
import cn.focus.m.plugin.roselyne.io.ConfigurationReader;
import cn.focus.m.plugin.roselyne.io.resource.DefaultResourceResolver;
import cn.focus.m.plugin.roselyne.io.resource.MagicResource;
import cn.focus.m.plugin.roselyne.io.resource.Resource;
import cn.focus.m.plugin.roselyne.io.resource.ResourceAcquirerFactory;
import cn.focus.m.plugin.roselyne.io.resource.ResourceProcessHandler;
import cn.focus.m.plugin.roselyne.io.resource.ResourceResolveException;
import cn.focus.m.plugin.roselyne.io.resource.ResourceResolveSupport;
import cn.focus.m.plugin.roselyne.io.resource.ResourceResolver;


@Mojo(name="go", requiresDependencyResolution=ResolutionScope.RUNTIME_PLUS_SYSTEM)
public class Roselyne extends AbstractMojo implements ConfigurationSource, ResourceResolveSupport {

    @Component
    private MavenProject project;
    
    @Component
    private MavenSession mavenSession;
    
    //@Component
    private ConfigurationReader configReader;
    
    @Parameter(property="descriptor")
    protected String descriptor;
    
    @Parameter(defaultValue="${project.basedir}", required=true, readonly=true)
    private File basedir;
    
    @Parameter(defaultValue="${localRepository}", required=true, readonly=true)
    private ArtifactRepository localRepository;
    
    @Parameter(property="ignoreMissingDescriptor", defaultValue="false")
    protected boolean ignoreMissingDescriptor;
    
    @Parameter(property="skip", defaultValue="false")
    private boolean skip;
    
    @Parameter(property="runOnlyAtExecutionRoot", defaultValue="false")
    private boolean runOnlyAtExecutionRoot;
    
    @Parameter(defaultValue="${project.basedir}\\target\\roselyne\\temp\\", required=true, readonly=true)
    private String tempDir;
    
    @Parameter(property="encoding")
    private String encoding;
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        long start = System.currentTimeMillis();
        super.getLog().info("Roselyne begin");
        
        if (this.skip) {
            
            getLog().info("Roselyne have been skipped by parameter 'roselyne.skip'");
            end(start);
            
        }
        
        if ((this.runOnlyAtExecutionRoot) && (!isThisTheExecutionRoot())) {
            
          getLog().info("Skipping the Roselyne in this project because it's not the Execution Root");
          end(start);
          
        }
        
        List<Config> configs = null;
        
        super.getLog().info("roselyne start reader configuration");
        
        try {
            configReader = new DefaultConfigurationReader();
            configs = configReader.readConfigurations(this);
        } catch (ConfigurationReadException e) {
            super.getLog().info("roselyne run error @ read configuration");
            end(start);
            throw new MojoExecutionException("Error reading assemblies: " + e.getMessage(), e);
            
        } catch (InvalidConfigurationException e) {
            super.getLog().info("roselyne run error @ read configuration");
            end(start);
            throw new MojoFailureException(this.configReader, e.getMessage(), "Mojo configuration is invalid: " + e.getMessage());
            
        }
        
        if (null != configs && configs.size() > 0) {
            for (Config config : configs) {
                getLog().info("Config[" + config.getId() + "]\n\n" + config);
            }
        } else {
            getLog().info("roselyne run error @ Can not parse configuration");
        }
        
        debug1(configs);
        
        ResourceAcquirerFactory resourceAcquirerFactory = new ResourceAcquirerFactory();
        ResourceResolver resourceResolver = new DefaultResourceResolver(resourceAcquirerFactory);
        ResourceCompressor resourceCompressor = new ResourceCompressor(this);
        
        List<Resource> resources = new ArrayList<Resource>();
        List<MagicResource> magicResources = new ArrayList<MagicResource>();
        
        for (Config config : configs) {
            
            try {
                List<Resource> rs = resourceResolver.resolve(config, this);
                List<MagicResource> ms = resourceResolver.resolveMagic(config, this);
                
                if (null != rs && rs.size() > 0) {
                    resources.addAll(rs); 
                }
                
                if (null != ms && ms.size() > 0) {
                    magicResources.addAll(ms);
                }
                //debug2(resources);
                debug3(magicResources);
            } catch (ResourceResolveException e) {
                super.getLog().error("resourceResolverError", e);
            }
        }
        
        ResourceProcessHandler handler = new ResourceProcessHandler(this, resourceAcquirerFactory, resourceCompressor, resources, magicResources);
        
        try {
            handler.handle();
        } catch (Exception e) {
            end(start);
            throw new MojoFailureException("", e);
        }
        end(start);
        
    }
    
    protected void end(long start) {
        StringBuilder info = new StringBuilder("Roselyne process complete, use ").append(System.currentTimeMillis() - start).append(" ms");
        super.getLog().info(info);
    }
    
    protected boolean isThisTheExecutionRoot() {
        Log log = getLog();
        log.debug("Root Folder:" + this.mavenSession.getExecutionRootDirectory());
        log.debug("Current Folder:" + this.basedir);
        boolean result = this.mavenSession.getExecutionRootDirectory().equalsIgnoreCase(this.basedir.toString());

        if (result) {
            log.debug("This is the execution root.");
        } else {
            log.debug("This is NOT the execution root.");
        }

        return result;
    }
    
    /**
     * 测试resolveImmeditely为true的fileSet
     * @param resources
     */
    protected void debug2(List<Resource> resources) {
        if (null == resources) {
            return;
        }
        for (Resource resource : resources) {
            super.getLog().info("resource:" + resource + "\n\n");
        }
    }
    
    /**
     * 测试resolveImmeditely为false的fileSet
     * @param resources
     */
    protected void debug3(List<MagicResource> resources) {
        if (null == resources) {
            return;
        }
        for (MagicResource resource : resources) {
            super.getLog().info("magicresource:" + resource + "\n\n");
        }
    }
    
    /**
     * 测试fisConfig
     * @param fisConfigs
     */
    protected void debug1(List<Config> configs) {
        if (null != configs && configs.size() > 0) {
            Config config = configs.get(0);
            String directory = config.getFileSets().get(0).getDirectory();
            super.getLog().info("directory:" + directory);
            
        }
    }
    
    public void addMessage(MessageType type, String content) {
        
    }

    public String[] getDiscriptors() {
        // TODO
        return null;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public File getBaseDir() {
        return basedir;
    }

    public MavenProject getProject() {
        return project;
    }

    public boolean isIgnoreMissingDescriptor() {
        return ignoreMissingDescriptor;
    }

    public MavenSession getMavenSession() {
        return mavenSession;
    }

    public ArtifactRepository getLocalRepository() {
        return localRepository;
    }

    public ConfigurationReader getConfigReader() {
        return configReader;
    }

    public void setConfigReader(ConfigurationReader configReader) {
        this.configReader = configReader;
    }

    public String getTempDir() {
        return tempDir;
    }

    public String getEncoding() {
        return encoding;
    }
    
    public void printResolveLog(Message msg) {
        if (null == msg) {
            return;
        }
        MessageType type = msg.getType();
        Log logger = super.getLog();
        switch(type) {
        case DEBUG: 
            if (logger.isDebugEnabled()) {
                logger.debug(msg.getContent());
            }
            break;
        case INFO:
            if (logger.isInfoEnabled()) {
                logger.info(msg.getContent());
            }
            break;
        case WARN:
            if (logger.isWarnEnabled()) {
                logger.warn(msg.getContent());
            }
        case ERROR:
            if (logger.isErrorEnabled()) {
                logger.error(msg.getContent());
            }
        case SEPERATOR:
            if (logger.isInfoEnabled()) {
                logger.info("");
                logger.info(msg.getContent());
                logger.info("");
            }
            break;
        default:
            break;
        }
        
    }

    public CompressConfig getCompressConfig() {
        return null;
    }

    public void addMessage(Resource resource, Message msg) {
        // TODO Auto-generated method stub
        
    }
    
}
