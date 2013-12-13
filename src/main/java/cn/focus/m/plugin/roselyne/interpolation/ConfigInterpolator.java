package cn.focus.m.plugin.roselyne.interpolation;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.InterpolationPostProcessor;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PrefixedPropertiesValueSource;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.interpolation.object.FieldBasedObjectInterpolator;
import org.codehaus.plexus.interpolation.object.ObjectInterpolationWarning;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.cli.CommandLineUtils;

import cn.focus.m.plugin.roselyne.ConfigurationSource;
import cn.focus.m.plugin.roselyne.descriptor.Config;
import cn.focus.m.plugin.roselyne.utils.RoselyneFileUtils;
import cn.focus.m.plugin.roselyne.utils.InterpolationConstants;

/**
 * 变量替换器
 * @author rogantian
 * @date 2013-12-12
 * @email rogantianwz@gmail.com
 */
public class ConfigInterpolator extends AbstractLogEnabled {

    private static final Set<String> INTERPOLATION_BLACKLIST;

    private static final Properties ENVIRONMENT_VARIABLES;

    static {

        Set<String> blacklist = new HashSet<String>();
        INTERPOLATION_BLACKLIST = blacklist;

        Properties enviromentVariables;
        try {
            enviromentVariables = CommandLineUtils.getSystemEnvVars(false);
        } catch (IOException e) {
            enviromentVariables = new Properties();
        }
        ENVIRONMENT_VARIABLES = enviromentVariables;
    }

    @SuppressWarnings("unchecked")
    public Config interpolate(Config config, MavenProject project, ConfigurationSource configurationSource)
            throws ConfigInterpolationException {

        Set<String> blacklistFields = new HashSet<String>(FieldBasedObjectInterpolator.DEFAULT_BLACKLISTED_FIELD_NAMES);

        blacklistFields.addAll(INTERPOLATION_BLACKLIST);

        Set<String> blacklistPkgs = FieldBasedObjectInterpolator.DEFAULT_BLACKLISTED_PACKAGE_PREFIXES;

        FieldBasedObjectInterpolator objectInterpolator = new FieldBasedObjectInterpolator(blacklistFields,
                blacklistPkgs);

        Interpolator interpolator = buildInterpolator(project, configurationSource);

        RecursionInterceptor interceptor = new PrefixAwareRecursionInterceptor(InterpolationConstants.PROJECT_PREFIXES,
                true);

        try {
            objectInterpolator.interpolate(config, interpolator, interceptor);
        } catch (InterpolationException e) {
            throw new ConfigInterpolationException("Failed to interpolate fisConfig with ID: " + config.getId()
                    + ". Reason:" + e.getMessage(), e);
        } finally {
            interpolator.clearAnswers();
        }

        if ((objectInterpolator.hasWarnings()) && (getLogger().isDebugEnabled())) {
            StringBuilder sb = new StringBuilder();

            sb.append("One or more minor errors occurred while interpolating the fisConfig with ID: "
                    + config.getId() + ":\n");

            List<ObjectInterpolationWarning> warnings = objectInterpolator.getWarnings();
            for (Iterator<ObjectInterpolationWarning> it = warnings.iterator(); it.hasNext();) {
                ObjectInterpolationWarning warning = it.next();

                sb.append('\n').append(warning);
            }

            sb.append("\n\nThese values were SKIPPED, but the fis process will continue.\n");

            getLogger().debug(sb.toString());
        }

        return config;
    }

    public static Interpolator buildInterpolator(MavenProject project, ConfigurationSource configurationSource) {

        StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.setCacheAnswers(true);

        MavenSession session = configurationSource.getMavenSession();
        if (null != session) {
            Properties userProperties = null;
            userProperties = session.getExecutionProperties();

            if (null != userProperties) {
                interpolator.addValueSource(new PropertiesBasedValueSource(userProperties));
            }
        }

        interpolator.addValueSource(new PrefixedPropertiesValueSource(
                InterpolationConstants.PROJECT_PROPERTIES_PREFIXES, project.getProperties(), true));

        interpolator.addValueSource(new PrefixedObjectValueSource(InterpolationConstants.PROJECT_PREFIXES, project,
                true));

        Properties settingsProperties = new Properties();
        if (configurationSource.getLocalRepository() != null) {
            settingsProperties.setProperty("localRepository", configurationSource.getLocalRepository().getBasedir());
            settingsProperties.setProperty("settings.localRepository", configurationSource.getLocalRepository()
                    .getBasedir());
        } else if ((session != null) && (session.getSettings() != null)) {
            settingsProperties.setProperty("localRepository", session.getSettings().getLocalRepository());
            settingsProperties.setProperty("settings.localRepository", configurationSource.getLocalRepository()
                    .getBasedir());
        }

        interpolator.addValueSource(new PropertiesBasedValueSource(settingsProperties));

        Properties commandLineProperties = System.getProperties();
        if (session != null) {
            commandLineProperties = new Properties();
            if (session.getExecutionProperties() != null) {
                commandLineProperties.putAll(session.getExecutionProperties());
            }

            if (session.getUserProperties() != null) {
                commandLineProperties.putAll(session.getUserProperties());
            }

        }

        interpolator.addValueSource(new PropertiesBasedValueSource(commandLineProperties));
        interpolator.addValueSource(new PrefixedPropertiesValueSource(Collections.singletonList("env."),
                ENVIRONMENT_VARIABLES, true));

        interpolator.addPostProcessor(new PathTranslatingPostProcessor(project.getBasedir()));

        return interpolator;
    }

    protected Logger getLogger() {
        Logger logger = super.getLogger();

        if (logger == null) {
            logger = new ConsoleLogger(1, "interpolator-internal");

            enableLogging(logger);
        }

        return logger;
    }

    private static final class PathTranslatingPostProcessor implements InterpolationPostProcessor {

        private final File basedir;

        public PathTranslatingPostProcessor(File basedir) {
            this.basedir = basedir;
        }

        public Object execute(String expression, Object value) {
            String path = String.valueOf(value);
            return RoselyneFileUtils.makePathRelativeTo(path, this.basedir);
        }
    }
}
