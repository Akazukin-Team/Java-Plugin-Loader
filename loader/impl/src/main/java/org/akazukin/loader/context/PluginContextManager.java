package org.akazukin.loader.context;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.context.IPlugin;
import org.akazukin.loader.api.context.IPluginContext;
import org.akazukin.loader.api.context.IPluginMetadata;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PluginContextManager {
    Set<PluginContext> ctxs = new HashSet<>();
    @Getter
    ClassLoader parentLoader;

    public PluginContextManager(final ClassLoader parentLoader) {
        this.parentLoader = parentLoader;
    }

    public PluginContext[] getContexts() {
        return this.ctxs.toArray(PluginContext.EMPTY_ARR);
    }

    @Nullable
    public synchronized IPlugin getPlugin(final String pluginId) {
        return this.ctxs.stream()
                .filter(c -> c.getMetadata().getId().equals(pluginId))
                .findFirst()
                .map(IPluginContext::getPlugin)
                .orElse(null);
    }

    @Nullable
    public synchronized PluginContext getPluginContext(final String pluginId) {
        return this.ctxs.stream()
                .filter(c -> c.getMetadata().getId().equals(pluginId))
                .findFirst()
                .orElse(null);
    }

    public synchronized PluginContext initPluginContext(final IPluginMetadata metadata, final URL url) {
        if (this.ctxs.stream().anyMatch(c -> c.getMetadata().getId().equals(metadata.getId()))) {
            throw new IllegalArgumentException("Plugin already exists: " + metadata.getId());
        }

        final PluginContext context = new PluginContext(metadata, url);
        this.ctxs.add(context);
        return context;
    }

    public synchronized void removeContext(final IPluginContext context) {
        this.ctxs.remove(context);
    }
}
