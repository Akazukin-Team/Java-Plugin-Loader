package org.akazukin.loader.manager;

import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.akazukin.loader.api.IPlugin;
import org.akazukin.loader.api.IPluginContext;
import org.akazukin.loader.api.IPluginManager;
import org.akazukin.loader.api.IPluginMetadata;
import org.akazukin.loader.api.PluginDynamicState;
import org.akazukin.loader.api.PluginState;
import org.akazukin.loader.api.dependency.IDependencyNode;
import org.akazukin.loader.api.dependency.INode;
import org.akazukin.loader.api.dependency.analyze.IAnalyzeResult;
import org.akazukin.loader.api.dependency.analyze.ISuccessResult;
import org.akazukin.loader.api.exception.PluginDependencyLoadException;
import org.akazukin.loader.api.exception.PluginDynamicsLifecycleException;
import org.akazukin.loader.api.exception.PluginLifecycleException;
import org.akazukin.loader.context.PluginClassLoader;
import org.akazukin.loader.context.PluginContext;
import org.akazukin.loader.context.PluginContextManager;
import org.akazukin.loader.context.dependency.DependencyMetadataResolver;
import org.akazukin.loader.event.LifecycleEventManager;
import org.akazukin.loader.event.events.PostPluginDisableEvent;
import org.akazukin.loader.event.events.PostPluginEnableEvent;
import org.akazukin.loader.event.events.PostPluginLoadEvent;
import org.akazukin.loader.event.events.PostPluginRegisterEvent;
import org.akazukin.loader.event.events.PostPluginUnloadEvent;
import org.akazukin.loader.event.events.PostPluginUnregisterEvent;
import org.akazukin.loader.event.events.PrePluginDisableEvent;
import org.akazukin.loader.event.events.PrePluginEnableEvent;
import org.akazukin.loader.event.events.PrePluginLoadEvent;
import org.akazukin.loader.event.events.PrePluginRegisterEvent;
import org.akazukin.loader.event.events.PrePluginUnloadEvent;
import org.akazukin.loader.event.events.PrePluginUnregisterEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * Manages plugin lifecycle and service orchestration with lifecycle listener
 * support.
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PluginManager implements IPluginManager {
    LifecycleEventManager eventMgr;
    PluginResolver pluginResolver;
    PluginContextManager ctxMgr;
    DependencyMetadataResolver depResolver;
    NodeLoader nodeLoader;

    public PluginManager(final LifecycleEventManager eventMgr, final PluginResolver pluginResolver, final PluginContextManager ctxMgr) {
        this.eventMgr = eventMgr;
        this.pluginResolver = pluginResolver;
        this.ctxMgr = ctxMgr;
        this.depResolver = new DependencyMetadataResolver(this.ctxMgr);
        this.nodeLoader = new NodeLoader(16, this, this.ctxMgr);
    }

    public synchronized void disablePluginInternal(final @NotNull String pluginId) throws PluginLifecycleException {
        final PluginContext ctx = this.ctxMgr.getPluginContext(pluginId);
        if (ctx == null) {
            throw new IllegalArgumentException("Plugin not found: " + pluginId);
        }

        final IPluginMetadata meta = ctx.getMetadata();
        synchronized (ctx) {
            if (!ctx.getState().isEnabled()) {
                throw new IllegalStateException("Plugin already disabled: " + meta.getId());
            }

            try {
                log.info("Disabling plugin: " + meta.getId());

                for (final PluginContext pl : this.pluginResolver.getAllPlugins()) {
                    for (final IPluginContext depCtx : pl.getDependencies()) {
                        if (depCtx != ctx) {
                            continue;
                        }
                        if (!depCtx.getState().isEnabled()) {
                            continue;
                        }

                        log.debug("Disabling dependency: " + depCtx.getMetadata().getId());
                        try {
                            this.disablePlugin(pl.getMetadata().getId());
                            log.debug("Disabled dependency: " + depCtx.getMetadata().getId());
                        } catch (final PluginLifecycleException e) {
                            log.debug("Failed to disable dependency: " + depCtx.getMetadata().getId(), e);
                        }
                    }
                }


                ctx.setDynamicState(PluginDynamicState.DISABLING);

                {
                    final PrePluginDisableEvent event = new PrePluginDisableEvent(ctx);
                    this.eventMgr.callEvent(PrePluginDisableEvent.class, event);
                }

                try {
                    ctx.getPlugin().onDisable();
                } catch (final Throwable t) {
                    log.error("Failed to call onDisable() of plugin: " + meta.getId(), t);
                }

                ctx.setState(PluginState.LOADED);
                ctx.setDynamicState(PluginDynamicState.NONE);

                log.info("Plugin disabled successfully: " + meta.getId());

                {
                    final PostPluginDisableEvent event = new PostPluginDisableEvent(ctx);
                    this.eventMgr.callEvent(PostPluginDisableEvent.class, event);
                }
            } catch (final Throwable e) {
                ctx.setDynamicState(PluginDynamicState.NONE);

                throw new PluginDynamicsLifecycleException("Failed to initialize plugin: " + meta.getId(), meta.getId(),
                        PluginState.ENABLED, PluginState.LOADED,
                        e);
            }
        }
    }

    public synchronized void unloadPluginInternal(final @NotNull String pluginId) throws PluginLifecycleException {
        final PluginContext ctx = this.ctxMgr.getPluginContext(pluginId);
        if (ctx == null) {
            throw new IllegalArgumentException("Plugin not found: " + pluginId);
        }

        final IPluginMetadata meta = ctx.getMetadata();
        synchronized (ctx) {
            if (!ctx.getState().isLoaded()) {
                throw new IllegalStateException("Plugin already unloaded: " + meta.getId());
            }
            if (ctx.getState().isEnabled()) {
                this.disablePlugin(pluginId);
            }

            try {
                log.info("Unloading plugin: " + meta.getName());
                ctx.setDynamicState(PluginDynamicState.UNLOADING);

                {
                    final PrePluginUnloadEvent event = new PrePluginUnloadEvent(ctx);
                    this.eventMgr.callEvent(PrePluginUnloadEvent.class, event);
                }

                Throwable t = null;
                if (ctx.getPlugin() != null) {
                    try {
                        ctx.getPlugin().onUnload();
                    } catch (final Throwable t2) {
                        t = t2;
                        log.error("Failed to call onUnload() of plugin: " + meta.getId(), t2);
                    }
                } else {
                    log.debug("Plugin not loaded: " + meta.getId() + ", Skipping onUnload()");
                }

                ctx.getClassLoader().close();
                ctx.setClassLoader(null);


                ctx.setDynamicState(PluginDynamicState.NONE);
                ctx.setState(PluginState.NONE);

                {
                    final PostPluginUnloadEvent event = new PostPluginUnloadEvent(ctx);
                    this.eventMgr.callEvent(PostPluginUnloadEvent.class, event);
                }
            } catch (final Exception e) {
                ctx.setDynamicState(PluginDynamicState.NONE);
                throw new PluginDynamicsLifecycleException("Failed to initialize plugin: " + meta.getId(), meta.getId(),
                        PluginState.LOADED, PluginState.NONE,
                        e);
            }
        }
    }

    public void loadAll() {
        for (final String pluginId : this.getPluginIds()) {
            try {
                this.loadPlugin(pluginId);
            } catch (final PluginLifecycleException e) {
                log.error("Failed to load plugin: " + pluginId, e);
            }
        }
    }

    @Override
    public void loadPlugin(@NotNull final String pluginId) throws PluginLifecycleException {
        final PluginContext ctx = this.ctxMgr.getPluginContext(pluginId);
        if (ctx == null) {
            throw new IllegalArgumentException("Plugin not found: " + pluginId);
        }

        final IPluginMetadata meta = ctx.getMetadata();
        synchronized (ctx) {
            if (ctx.getState().isLoaded()) {
                throw new IllegalStateException("Plugin already loaded: " + meta.getId());
            }

            try {
                log.info("Loading plugin: " + meta.getName());

                ctx.setDynamicState(PluginDynamicState.LOADING);

                {
                    final PrePluginLoadEvent event = new PrePluginLoadEvent(ctx);
                    this.eventMgr.callEvent(PrePluginLoadEvent.class, event);
                }

                final String mainClassName = meta.getMainClass();

                final PluginClassLoader classLoader = new PluginClassLoader(meta, this.ctxMgr.getParentLoader());
                classLoader.addURL(ctx.getUrl());
                ctx.setClassLoader(classLoader);

                final Class<?> mainClz = classLoader.loadClass(mainClassName);

                if (!IPlugin.class.isAssignableFrom(mainClz)) {
                    throw new IllegalArgumentException(
                            "Main class must implement Plugin interface: " + mainClz.getName());
                }

                final Constructor<?> constructor;
                try {
                    constructor = mainClz.getDeclaredConstructor();
                } catch (final NoSuchMethodException e) {
                    throw new IllegalArgumentException(
                            "Main class must have a no-arg constructor: " + mainClz.getName());
                }

                // Init plugin
                final IPlugin plugin = (IPlugin) constructor.newInstance();
                plugin.onLoad();
                ctx.setPlugin(plugin);

                ctx.setState(PluginState.LOADED);
                ctx.setDynamicState(PluginDynamicState.NONE);

                log.info("Plugin loaded successfully: " + meta.getId());

                {
                    final PostPluginLoadEvent event = new PostPluginLoadEvent(ctx);
                    this.eventMgr.callEvent(PostPluginLoadEvent.class, event);
                }
            } catch (final Exception e) {
                log.error("Failed to load plugin: " + meta.getId(), e);

                ctx.setDynamicState(PluginDynamicState.NONE);

                try {
                    this.unloadPlugin(pluginId);
                } catch (final PluginLifecycleException ex) {
                    log.error("Failed to unload plugin, but force unloaded: " + meta.getId(), ex);
                }

                throw new PluginDynamicsLifecycleException("Failed to load plugin: " + meta.getId(), meta.getId(),
                        PluginState.NONE, PluginState.LOADED,
                        e);
            }
        }
    }

    @Override
    public void enablePlugin(@NotNull final String pluginId) throws PluginLifecycleException {
        final INode node = this.depResolver.getLowerNode(pluginId);
        if (!node.getResult().isSuccess()) {
            throw new PluginDependencyLoadException(pluginId, node.getResult());
        }

        this.nodeLoader.enableNode(node, new NodeLoader.LoadCache()).join();
    }

    @Override
    public void disablePlugin(@NotNull final String pluginId) throws PluginLifecycleException {
        final INode node = this.depResolver.getLowerNode(pluginId);
        if (!node.getResult().isSuccess()) {
            throw new PluginDependencyLoadException(pluginId, node.getResult());
        }

        this.nodeLoader.disableNode(node, false, new NodeLoader.LoadCache()).join();
    }

    @Override
    public void unloadPlugin(@NotNull final String pluginId) throws PluginLifecycleException {
        final INode node = this.depResolver.getLowerNode(pluginId);
        if (!node.getResult().isSuccess()) {
            throw new PluginDependencyLoadException(pluginId, node.getResult());
        }

        this.nodeLoader.unloadNode(node, false, new NodeLoader.LoadCache()).join();
    }

    @Override
    public void registerPlugin(final @NotNull URL url, final @NotNull IPluginMetadata meta) {
        synchronized (this.ctxMgr) {
            if (this.ctxMgr.getPluginContext(meta.getId()) != null) {
                throw new IllegalArgumentException("Plugin already registered: " + meta.getId());
            }

            final PluginContext ctx = this.ctxMgr.initPluginContext(meta, url);
            synchronized (ctx) {
                log.info("Loading plugin: " + meta.getName());

                {
                    final PrePluginRegisterEvent event = new PrePluginRegisterEvent(ctx);
                    this.eventMgr.callEvent(PrePluginRegisterEvent.class, event);
                }

                ctx.setState(PluginState.NONE);
                ctx.setDynamicState(PluginDynamicState.NONE);

                log.info("Plugin loaded successfully: " + meta.getId());

                {
                    final PostPluginRegisterEvent event = new PostPluginRegisterEvent(ctx);
                    this.eventMgr.callEvent(PostPluginRegisterEvent.class, event);
                }
            }
        }
    }

    @Override
    public void unregisterPlugin(final @NotNull String pluginId) throws PluginLifecycleException {
        final IPluginContext ctx = this.ctxMgr.getPluginContext(pluginId);
        if (ctx == null) {
            throw new IllegalArgumentException("Plugin not found: " + pluginId);
        }

        final IPluginMetadata meta = ctx.getMetadata();
        synchronized (ctx) {
            if (ctx.getState().isLoaded()) {
                this.unloadPlugin(pluginId);
            }

            log.info("Unregister plugin: " + meta.getName());

            {
                final PrePluginUnregisterEvent event = new PrePluginUnregisterEvent(ctx);
                this.eventMgr.callEvent(PrePluginUnregisterEvent.class, event);
            }

            this.ctxMgr.removeContext(ctx);

            {
                final PostPluginUnregisterEvent event = new PostPluginUnregisterEvent(ctx);
                this.eventMgr.callEvent(PostPluginUnregisterEvent.class, event);
            }

            log.info("Plugin unregistered: " + meta.getId());
        }
    }

    @Override
    @Nullable
    public IPlugin getPlugin(@NotNull final String pluginId) {
        final IPluginContext ctx = this.ctxMgr.getPluginContext(pluginId);
        return ctx != null ? ctx.getPlugin() : null;
    }

    @Override
    public synchronized void unloadAll() {
        final CompletableFuture<?>[] tasks = Arrays.stream(this.getPluginIds())
                .map(this.ctxMgr::getPluginContext)
                .filter(p -> p.getState().isLoaded())
                .map(f -> CompletableFuture.runAsync(() -> {
                    try {
                        this.unloadPlugin(f.getMetadata().getId());
                    } catch (final PluginLifecycleException e) {
                        log.error("Failed to unload plugin: " + f.getMetadata().getId(), e);
                    }
                }))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(tasks).join();
    }

    @Override
    @SneakyThrows
    public void shutdown() {
        this.unloadAll();
    }

    public String[] getPluginIds() {
        return Arrays.stream(this.ctxMgr.getContexts())
                .map(c -> c.getMetadata().getId())
                .toArray(String[]::new);
    }

    public void enablePluginInternal(final String pluginId) throws PluginLifecycleException {
        final PluginContext ctx = this.ctxMgr.getPluginContext(pluginId);
        if (ctx == null) {
            throw new IllegalArgumentException("Plugin not found: " + pluginId);
        }

        final IPluginMetadata meta = ctx.getMetadata();
        synchronized (ctx) {
            if (ctx.getState().isEnabled()) {
                throw new IllegalStateException("Plugin already enabled: " + meta.getId());
            }
            if (ctx.getState() == PluginState.NONE) {
                this.loadPlugin(pluginId);
            }

            try {
                log.info("Enabling plugin: " + meta.getName());
                ctx.setDynamicState(PluginDynamicState.ENABLING);

                {
                    final PrePluginEnableEvent event = new PrePluginEnableEvent(ctx);
                    this.eventMgr.callEvent(PrePluginEnableEvent.class, event);
                }

                {
                    final IAnalyzeResult res = this.depResolver.getLowerNode(meta.getId()).getResult();
                    if (!res.isSuccess()) {
                        throw new PluginDependencyLoadException(meta.getId(), res);
                    }

                    final ISuccessResult res2 = (ISuccessResult) res;
                    for (final IDependencyNode node : res2.getNodes()) {
                        if (node.getResult().isSuccess()) {
                            final IPluginContext dep = this.pluginResolver.findById(node.getPluginId());
                            if (dep != null) {
                                dep.getClassLoader();
                                continue;
                            }
                        }

                        if (node.isRequired()) {
                            throw new PluginDependencyLoadException(meta.getId(), node.getResult());
                        }
                    }
                }

                ctx.getPlugin().onEnable();

                ctx.setState(PluginState.ENABLED);
                log.info("Plugin loaded successfully: " + meta.getId());

                {
                    final PostPluginEnableEvent event = new PostPluginEnableEvent(ctx);
                    this.eventMgr.callEvent(PostPluginEnableEvent.class, event);
                }
            } catch (final Exception e) {
                log.error("Failed to enable plugin: " + meta.getId(), e);
                throw new PluginDynamicsLifecycleException("Failed to enable plugin: " + meta.getId(), meta.getId(),
                        PluginState.LOADED, PluginState.ENABLED,
                        e);
            } finally {
                ctx.setDynamicState(PluginDynamicState.NONE);
            }
        }
    }
}
