package org.akazukin.loader.context;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.IPlugin;
import org.akazukin.loader.api.IPluginContext;
import org.akazukin.loader.api.IPluginMetadata;
import org.akazukin.loader.api.PluginDynamicState;
import org.akazukin.loader.api.PluginState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public final class PluginContext implements IPluginContext {
    public static final PluginContext[] EMPTY_ARR = new PluginContext[0];

    final IPluginMetadata metadata;
    final URL url;
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

    PluginContext(final IPluginMetadata metadata, final URL url) {
        this.metadata = metadata;
        this.url = url;
    }
}
