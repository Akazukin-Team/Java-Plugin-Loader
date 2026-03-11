package org.akazukin.loader.context;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.context.IPlugin;
import org.akazukin.loader.api.context.IPluginContext;
import org.akazukin.loader.api.context.IPluginMetadata;
import org.akazukin.loader.api.context.PluginDynamicState;
import org.akazukin.loader.api.context.PluginState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantLock;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public final class PluginContext implements IPluginContext {
    public static final PluginContext[] EMPTY_ARR = new PluginContext[0];

    final IPluginMetadata metadata;
    final URL url;
    final Path pluginDir;
    @Nullable
    PluginClassLoader classLoader;
    @Nullable
    IPlugin plugin;
    @NotNull
    PluginState state = PluginState.NONE;
    @NotNull
    PluginDynamicState dynamicState = PluginDynamicState.NONE;
    @Nullable
    PluginState stateSpec;
    @Nullable
    IPluginContext[] dependencies;
    ReentrantLock lock = new ReentrantLock();

    PluginContext(final IPluginMetadata metadata, final URL url, final Path pluginDir) {
        this.metadata = metadata;
        this.url = url;
        this.pluginDir = pluginDir;
    }
}
