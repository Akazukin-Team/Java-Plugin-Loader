package org.akazukin.loader;

import lombok.Getter;
import org.akazukin.loader.api.ILoader;
import org.akazukin.loader.api.ILoaderConfig;
import org.akazukin.loader.context.PluginContextManager;
import org.akazukin.loader.event.LifecycleEventManager;
import org.akazukin.loader.manager.PluginLoader;
import org.akazukin.loader.manager.PluginManager;
import org.akazukin.loader.manager.PluginResolver;

public class Loader implements ILoader {
    ILoaderConfig cfg;
    @Getter
    LifecycleEventManager eventMgr;
    PluginContextManager ctxMgr;
    @Getter
    PluginManager pluginMgr;
    @Getter
    PluginResolver pluginResolver;
    @Getter
    PluginLoader loader;

    public Loader(final ILoaderConfig cfg) {
        this.cfg = cfg;
        this.eventMgr = new LifecycleEventManager();
        {
            final ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
            this.ctxMgr = new PluginContextManager(parentLoader);
        }
        this.pluginResolver = new PluginResolver(this.ctxMgr);
        this.pluginMgr = new PluginManager(this, this.eventMgr, this.pluginResolver, this.ctxMgr);

        {
            this.loader = new PluginLoader(cfg, this.pluginMgr);
            this.loader.registerPlugins();
        }
    }
}
