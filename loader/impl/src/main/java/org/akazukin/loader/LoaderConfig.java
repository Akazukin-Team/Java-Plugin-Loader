package org.akazukin.loader;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.ILoaderConfig;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Builder(setterPrefix = "set")
public class LoaderConfig implements ILoaderConfig {
    @Getter
    String[] pluginPaths;
    @Getter
    String[] pluginDirectories;
    @Getter
    boolean loadFromClassPath;
}
