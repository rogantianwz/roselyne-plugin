package cn.focus.m.plugin.roselyne.io;

import java.util.List;

import cn.focus.m.plugin.roselyne.ConfigurationSource;
import cn.focus.m.plugin.roselyne.InvalidConfigurationException;
import cn.focus.m.plugin.roselyne.descriptor.Config;

public abstract interface ConfigurationReader {

    List<Config> readConfigurations(ConfigurationSource configurationSource) throws ConfigurationReadException, InvalidConfigurationException;
}
