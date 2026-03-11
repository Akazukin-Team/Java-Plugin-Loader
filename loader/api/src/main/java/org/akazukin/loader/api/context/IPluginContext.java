package org.akazukin.loader.api.context;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public interface IPluginContext {
    IPluginContext[] EMPTY_ARR = new IPluginContext[0];

    @NotNull
    IPluginMetadata getMetadata();

    @Nullable
    IPlugin getPlugin();

    @NotNull
    Path getPluginDir();

    @Nullable
    ClassLoader getClassLoader();

    @NotNull
    PluginState getState();

    @Nullable
    PluginState getStateSpec();

    @NotNull
    PluginDynamicState getDynamicState();

    @NotNull
    IPluginContext[] getDependencies();
}
