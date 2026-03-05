package org.akazukin.loader;

import lombok.Getter;
import org.akazukin.loader.api.ILoaderConfig;

public class LoaderConfig implements ILoaderConfig {
    @Getter
    String[] pluginPaths;
    @Getter
    String[] pluginDirectories;
    @Getter
    boolean loadFromClassPath;
}
