package org.akazukin.loader.api;

public interface ILoaderConfig {
    String[] getPluginPaths();

    String[] getPluginDirectories();

    boolean isLoadFromClassPath();
}
