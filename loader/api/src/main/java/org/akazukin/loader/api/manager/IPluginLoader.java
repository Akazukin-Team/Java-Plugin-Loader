package org.akazukin.loader.api.manager;

public interface IPluginLoader {
    void registerPluginFromZip(String path);

    void registerPluginsFromClasspath();

    void registerPluginFromFolder(String path);

    void registerPluginsInDirectory(String path);
}
