package org.akazukin.loader.api.context;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IPluginContext {
    IPluginContext[] EMPTY_ARR = new IPluginContext[0];

    @NotNull
    IPluginMetadata getMetadata();

    @Nullable
    IPlugin getPlugin();

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
