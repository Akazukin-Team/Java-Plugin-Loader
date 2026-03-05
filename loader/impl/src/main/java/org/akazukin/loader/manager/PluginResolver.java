package org.akazukin.loader.manager;

import org.akazukin.loader.api.context.dependency.IPluginDependency;
import org.akazukin.loader.api.manager.IPluginResolver;
import org.akazukin.loader.context.PluginContext;
import org.akazukin.loader.context.PluginContextManager;
import org.akazukin.semver.range.ISemverRange;

import java.util.Arrays;
import java.util.Objects;

/**
 * プラグイン検索機能を提供
 */
public class PluginResolver implements IPluginResolver {
    private final PluginContextManager ctxMgr;

    public PluginResolver(final PluginContextManager ctxMgr) {
        this.ctxMgr = ctxMgr;
    }

    @Override
    public PluginContext findById(final String id) {
        return this.ctxMgr.getPluginContext(id);
    }

    @Override
    public PluginContext[] findByName(final String name) {
        return Arrays.stream(this.ctxMgr.getContexts())
                .filter(plugin -> plugin != null && plugin.getMetadata().getName().equals(name))
                .toArray(PluginContext[]::new);
    }

    @Override
    public PluginContext[] getAllPlugins() {
        return this.ctxMgr.getContexts();
    }

    @Override
    public PluginContext[] getLoadedPlugins() {
        return Arrays.stream(this.ctxMgr.getContexts())
                .filter(Objects::nonNull)
                .toArray(PluginContext[]::new);
    }

    @Override
    public PluginContext[] getEnabledPlugins() {
        return Arrays.stream(this.ctxMgr.getContexts())
                .filter(plugin -> plugin != null && plugin.getState().isEnabled())
                .toArray(PluginContext[]::new);
    }

    @Override
    public PluginContext[] getDependents(final String pluginId) {
        return Arrays.stream(this.ctxMgr.getContexts())
                .filter(plugin -> {
                    if (plugin == null) {
                        return false;
                    }
                    final IPluginDependency[] dependencies = plugin.getMetadata().getDependencies();
                    return dependencies != null && Arrays.stream(dependencies)
                            .anyMatch(dep -> dep.getId().equals(pluginId));
                })
                .toArray(PluginContext[]::new);
    }

    @Override
    public PluginContext[] getDependencies(final String pluginId) {
        final PluginContext plugin = this.ctxMgr.getPluginContext(pluginId);
        if (plugin == null) {
            return PluginContext.EMPTY_ARR;
        }

        final IPluginDependency[] dependencies = plugin.getMetadata().getDependencies();
        if (dependencies == null) {
            return PluginContext.EMPTY_ARR;
        }

        return Arrays.stream(dependencies)
                .map(dep -> this.ctxMgr.getPluginContext(dep.getId()))
                .filter(Objects::nonNull)
                .toArray(PluginContext[]::new);
    }

    @Override
    public PluginContext findByIdAndVersion(final String id, final ISemverRange range) {
        final PluginContext ctx = this.findById(id);
        if (ctx != null
                && range.isSuitable(ctx.getMetadata().getVersion().getVersionCore())) {
            return ctx;
        }
        return null;
    }
}
