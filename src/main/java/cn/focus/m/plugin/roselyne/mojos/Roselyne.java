package cn.focus.m.plugin.roselyne.mojos;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    private Log log;
    
 // 定义script的正则表达式{或<script[^>]*?>[//s//S]*?<///script>
    private static String REGEX_SCRIPT = "<[\\s]*?script[^>]*?><[\\s]*?/[\\s]*?script[\\s]*?>";
    
    private static Pattern PSCRIPT = Pattern.compile(REGEX_SCRIPT);
    
    @Parameter(property="descriptor")
    protected String descriptor;
    
    @Parameter(defaultValue="${project.basedir}", required=true, readonly=true)
    private File basedir;
    
    @Parameter(defaultValue="${localRepository}", required=true, readonly=true)
    private ArtifactRepository localRepository;
    
    @Parameter(property="fis.ignoreMissingDescriptor", defaultValue="false")
    protected boolean ignoreMissingDescriptor;
    
    @Parameter(property="fis.skipFIS", defaultValue="false")
    private boolean skipFIS;
    
    @Parameter(property="fis.runOnlyAtExecutionRoot", defaultValue="false")
    private boolean runOnlyAtExecutionRoot;
    
    @Parameter(defaultValue="${project.basedir}\\target\\fis\\temp\\", required=true, readonly=true)
    private String tempDir;
    
    @Parameter(property="encoding")
    private String encoding;
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        long start = System.currentTimeMillis();
        super.getLog().info("fis begin");
        
        if (this.skipFIS) {
            
            getLog().info("FIS have been skipped by parameter 'fis.skipFIS'");
            end(start);
            
        }
        
        if ((this.runOnlyAtExecutionRoot) && (!isThisTheExecutionRoot())) {
            
          getLog().info("Skipping the FIS in this project because it's not the Execution Root");
          end(start);
          
        }
        
        List<Config> fisConfigs = null;
        
        super.getLog().info("fis start reader configuration");
        
        try {
            configReader = new DefaultConfigurationReader();
            fisConfigs = configReader.readConfigurations(this);
        } catch (ConfigurationReadException e) {
            super.getLog().info("fis run error @ read configuration");
            end(start);
            throw new MojoExecutionException("Error reading assemblies: " + e.getMessage(), e);
            
        } catch (InvalidConfigurationException e) {
            super.getLog().info("fis run error @ read configuration");
            end(start);
            throw new MojoFailureException(this.configReader, e.getMessage(), "Mojo configuration is invalid: " + e.getMessage());
            
        }
        
        if (null != fisConfigs && fisConfigs.size() > 0) {
            for (Config fisConfig : fisConfigs) {
                getLog().info("FISConfig[" + fisConfig.getId() + "]\n\n" + fisConfig);
            }
        } else {
            getLog().info("fis run error @ Can not parse configuration");
        }
        
        debug1(fisConfigs);
        
        ResourceAcquirerFactory resourceAcquirerFactory = new ResourceAcquirerFactory();
        ResourceResolver resourceResolver = new DefaultResourceResolver(resourceAcquirerFactory);
        ResourceCompressor resourceCompressor = new ResourceCompressor(this);
        
        List<Resource> resources = new ArrayList<Resource>();
        List<MagicResource> magicResources = new ArrayList<MagicResource>();
        
        for (Config fisConfig : fisConfigs) {
            
            try {
                List<Resource> rs = resourceResolver.resolve(fisConfig, this);
                List<MagicResource> ms = resourceResolver.resolveMagic(fisConfig, this);
                
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
        StringBuilder info = new StringBuilder("Fis process complete, use ").append(System.currentTimeMillis() - start).append(" ms");
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
    protected void debug1(List<Config> fisConfigs) {
        if (null != fisConfigs && fisConfigs.size() > 0) {
            Config config = fisConfigs.get(0);
            String directory = config.getFileSets().get(0).getDirectory();
            super.getLog().info("directory:" + directory);
            
        }
    }
    
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        // TODO Auto-generated method stub
        log = super.getLog();
        if (null == project) {
            log.error("The MavenProject is null");
            return;
        }
        log.info("begin fis");
        File baseDir = project.getBasedir();
        File webappDir = new File(baseDir.getAbsolutePath() + "/src/main/webapp");
        List<File> files = new ArrayList<File>();
        itDirRecursion(webappDir, files);
        
        if (files.size() > 0) {
            log.info("the files to fis list:"); 
            for (File f : files) {
                log.info(f.getAbsolutePath());
            }
        } else {
            log.info("no file to fis");
        }
        
        for (File f : files) {
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
                Matcher matcher = PSCRIPT.matcher(sb);
                boolean matched = matcher.find();
                while (matched) {
                    log.info("Matched script ele @" + f.getName() + ":"+ matcher.group());
                    matched = matcher.find();
                }
                
            } catch (FileNotFoundException e) {
                log.error("", e);
            } catch (Exception e) {
                log.error("", e);
            } finally {
                try {
                    br.close();
                } catch (IOException e) {
                    log.error("", e);
                }
            }
        }
    }
    
    private void itDirRecursion(File dir, List<File> files) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        
        File[] fs = dir.listFiles();
        for (File f : fs) {
            if (f.isDirectory()) {
                itDirRecursion(f, files);
            } else if (f.getName().endsWith("jsp")){
                files.add(f);
            } else {
                log.info("ignore file: " + f.getAbsolutePath());
            }
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
