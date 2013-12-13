package cn.focus.m.plugin.roselyne.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.io.location.FileLocatorStrategy;
import org.apache.maven.shared.io.location.Location;
import org.apache.maven.shared.io.location.Locator;
import org.apache.maven.shared.io.location.LocatorStrategy;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.IOUtil;

import cn.focus.m.plugin.roselyne.ConfigurationSource;
import cn.focus.m.plugin.roselyne.InvalidConfigurationException;
import cn.focus.m.plugin.roselyne.descriptor.Config;
import cn.focus.m.plugin.roselyne.interpolation.ConfigInterpolationException;
import cn.focus.m.plugin.roselyne.interpolation.ConfigInterpolator;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.StaxDriver;

@org.codehaus.plexus.component.annotations.Component(role=ConfigurationReader.class)
public class DefaultConfigurationReader extends AbstractLogEnabled 
    implements ConfigurationReader {

    public List<Config> readConfigurations(ConfigurationSource configurationSource) 
            throws ConfigurationReadException, InvalidConfigurationException{
        
        Locator locator = new Locator();
        
        List<LocatorStrategy> strategies = new ArrayList<LocatorStrategy>();
        
        strategies.add(new RelativeFileLocatorStrategy(configurationSource.getBaseDir()));
        strategies.add(new FileLocatorStrategy());
        
        List<Config> configs = new ArrayList<Config>();
        
        //TODO:可以支持多个descriptor文件，目前只支持了一个
        String descriptor = configurationSource.getDescriptor();
        
        if (null != descriptor) {
            locator.setStrategies(strategies);
            addConfigurationFromDescriptor(descriptor, locator, configurationSource, configs);
        }
        
        return configs;
    }
               
    private Config addConfigurationFromDescriptor(String descriptor, Locator locator, 
            ConfigurationSource configurationSource, List<Config> configs) 
                    throws ConfigurationReadException, InvalidConfigurationException{
        
        Location location = locator.resolve(descriptor);
        
        if (null == location) {
            if (configurationSource.isIgnoreMissingDescriptor()) {
                getLog().debug("Ignoring missing config descriptor with ID '" + descriptor + "'\n\n");
                return null;
            } else {
                throw new ConfigurationReadException("Error locating config descriptor: " + descriptor + "\n\n");
            }
        }
        
        Reader reader = null;
        try {
          reader = new InputStreamReader(location.getInputStream());
          
          File dir = null;
          if (null != location.getFile()) {
              dir = location.getFile().getParentFile();
          }
          
          Config config = readConfig(reader, descriptor, dir, configurationSource);
          
          configs.add(config);
          
          Config configLocal = config;
          
          return configLocal;
        } catch (IOException e) {
            throw new ConfigurationReadException("Error reading fisConfig descriptor : '" + descriptor + "'\n\n", e);
        } catch (XStreamException e) {
            throw new InvalidConfigurationException("Can not parse fisConfig descriptor : '" + descriptor + "'\n\n", e);
        }finally {
            IOUtil.close(reader);
        }
    }
    
    private Config readConfig(Reader reader, String locationDescription, File configDir, 
            ConfigurationSource configurationSource) throws ConfigurationReadException {
        MavenProject project = configurationSource.getProject();
        
        Config config = null;
        try {
            StaxDriver staxDriver = new StaxDriver(new NoNameCoder());
            XStream stream = new XStream(staxDriver);
            
            stream.processAnnotations(Config.class);
            config = (Config) stream.fromXML(reader);
            
            debugPringConfig("Before cofing is interpolated:", config);
            
            config = new ConfigInterpolator().interpolate(config, project, configurationSource);
            
            debugPringConfig("After cofing is interpolated:", config);
        } catch (XStreamException e) {
            throw new ConfigurationReadException("Error Read descriptor: " + locationDescription + ": " + e.getMessage(), e);
        } catch (ConfigInterpolationException e) {
            throw new ConfigurationReadException("Error Read descriptor: " + locationDescription + ": " + e.getMessage(), e);
        } finally {
            IOUtil.close(reader);
        }
        return config;
    }
    
    private void debugPringConfig(String message, Config fisConfig) {
        getLog().debug(message + "\n\n" + fisConfig.toString() + "\n\n");
    }
    
    private Logger getLog() {
        Logger logger = super.getLogger();
        
        if (null == logger) {
            logger = new ConsoleLogger(1, "defaultConfigruationReader");
            enableLogging(logger);
        }
        
        return logger;
    }

}
