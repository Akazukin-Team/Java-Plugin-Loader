package org.akazukin.loader.manager;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.akazukin.event.EventManager;
import org.akazukin.loader.api.ILoader;
import org.akazukin.loader.api.context.IPlugin;
import org.akazukin.loader.api.context.IPluginMetadata;
import org.akazukin.loader.api.context.PluginDynamicState;
import org.akazukin.loader.api.context.PluginState;
import org.akazukin.loader.api.context.dependency.IDependencyNode;
import org.akazukin.loader.api.context.dependency.INode;
import org.akazukin.loader.api.context.dependency.analyze.IAnalyzeResult;
import org.akazukin.loader.api.context.dependency.analyze.ISuccessResult;
import org.akazukin.loader.api.event.events.IPluginLifecycleEvent;
import org.akazukin.loader.api.exception.PluginDependencyLoadException;
import org.akazukin.loader.api.exception.PluginDynamicsLifecycleException;
import org.akazukin.loader.api.exception.PluginLifecycleException;
import org.akazukin.loader.api.manager.IPluginManager;
import org.akazukin.loader.context.PluginClassLoader;
import org.akazukin.loader.context.PluginContext;
import org.akazukin.loader.context.PluginContextManager;
import org.akazukin.loader.context.dependency.DependencyMetadataResolver;
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
import org.akazukin.util.concurrent.FixedReentrantReadWriteLock;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Manages plugin lifecycle and service orchestration with lifecycle listener
 * support.
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
// TODO ExecutorServiceとCompletableFutureを使って
//  非同期読み込みなどを実装すr
public class PluginManager implements IPluginManager {
    ILoader loader;
    EventManager<IPluginLifecycleEvent> eventMgr;
    PluginContextManager ctxMgr;
    DependencyMetadataResolver depResolver;

    public PluginManager(final ILoader loader, final EventManager<IPluginLifecycleEvent> eventMgr,
                         final PluginContextManager ctxMgr) {
        this.loader = loader;
        this.eventMgr = eventMgr;
        this.ctxMgr = ctxMgr;
        this.depResolver = new DependencyMetadataResolver(this.ctxMgr);
    }

    @Override
    public void close() {
        this.unloadAll();
    }

    private void unloadPluginInternal(final @NotNull INode upperNode) {
        final PluginContext ctx = this.ctxMgr.getPluginContext(upperNode.getPluginId());
        if (ctx == null) {
            throw new IllegalArgumentException("Plugin not found: " + upperNode.getPluginId());
        }

        final IPluginMetadata meta = ctx.getMetadata();
        try (final FixedReentrantReadWriteLock.ILock l = ctx.getLock().sharedLock()) {
            if (!ctx.getState().isLoaded()) {
                throw new IllegalStateException("Plugin already unloaded: " + meta.getId() + ", state: " + ctx.getState().getName());
            }

            if (ctx.getState().isEnabled()) {
                this.disablePluginInternal(upperNode);
            }

            {
                final IAnalyzeResult res = upperNode.getResult();
                if (!res.isSuccess() || !(res instanceof final ISuccessResult sRes)) {
                    throw new IllegalArgumentException("Invalid node: " + upperNode);
                }

                // Disabling upper nodes
                {
                    final Set<CompletableFuture<Void>> futures = new HashSet<>();
                    for (final IDependencyNode dep : sRes.getNodes()) {
                        futures.add(CompletableFuture.runAsync(() -> {
                            final PluginContext depCtx = this.ctxMgr.getPluginContext(dep.getPluginId());
                            if (depCtx == null) {
                                throw new IllegalStateException("Plugin not found: " + dep.getPluginId());
                            }

                            try (final FixedReentrantReadWriteLock.ILock l2 = depCtx.getLock().sharedLock()) {
                                if (!depCtx.getState().isLoaded()) {
                                    return;
                                }

                                this.unloadPluginInternal(dep);
                            }
                        }));
                    }
                    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
                }
            }

            try (final FixedReentrantReadWriteLock.ILock l2 = ctx.getLock().exclusiveLock()) {
                log.info("Unloading plugin: " + meta.getId());
                ctx.setDynamicState(PluginDynamicState.UNLOADING);

                {
                    final PrePluginUnloadEvent event = new PrePluginUnloadEvent(ctx);
                    this.eventMgr.callEvent(PrePluginUnloadEvent.class, event);
                }

                try {
                    ctx.getPlugin().onUnload();
                } catch (final Throwable t2) {
                    log.error("Failed to call onUnload() of plugin: " + meta.getId(), t2);
                }
                ctx.setPlugin(null);

                try {
                    ctx.getClassLoader().close();
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
                ctx.setClassLoader(null);


                ctx.setDynamicState(PluginDynamicState.NONE);
                ctx.setState(PluginState.NONE);

                {
                    final PostPluginUnloadEvent event = new PostPluginUnloadEvent(ctx);
                    this.eventMgr.callEvent(PostPluginUnloadEvent.class, event);
                }
            }
        }
    }

    @Override
    public void loadPlugin(@NotNull final String pluginId) throws PluginLifecycleException {
        final INode node = this.depResolver.getLowerNode(pluginId);
        if (!node.getResult().isSuccess()) {
            throw new PluginDependencyLoadException(node);
        }

        this.loadPluginInternal(node);
    }

    @Override
    public void enablePlugin(@NotNull final String pluginId) throws PluginLifecycleException {
        final INode node = this.depResolver.getLowerNode(pluginId);
        if (!node.getResult().isSuccess()) {
            throw new PluginDependencyLoadException(node);
        }

        this.enablePluginInternal(node);
    }

    @Override
    public void disablePlugin(@NotNull final String pluginId) throws PluginLifecycleException {
        final INode node = this.depResolver.getUpperNode(pluginId);
        if (!node.getResult().isSuccess()) {
            throw new PluginDependencyLoadException(node);
        }

        this.disablePluginInternal(node);
    }

    @Override
    public void unloadPlugin(@NotNull final String pluginId) throws PluginLifecycleException {
        final INode node = this.depResolver.getUpperNode(pluginId);
        if (!node.getResult().isSuccess()) {
            throw new PluginDependencyLoadException(node);
        }

        this.unloadPluginInternal(node);
    }

    @Override
    public void registerPlugin(final @NotNull URL url, final @NotNull IPluginMetadata meta) {
        synchronized (this.ctxMgr) {
            if (this.ctxMgr.getPluginContext(meta.getId()) != null) {
                throw new IllegalArgumentException("Plugin already registered: " + meta.getId());
            }

            final PluginContext ctx = this.ctxMgr.initPluginContext(meta, url);
            try (final FixedReentrantReadWriteLock.ILock l = ctx.getLock().exclusiveLock()) {
                log.info("Registering plugin: " + meta.getId());

                {
                    final PrePluginRegisterEvent event = new PrePluginRegisterEvent(ctx);
                    this.eventMgr.callEvent(PrePluginRegisterEvent.class, event);
                }

                ctx.setState(PluginState.NONE);
                ctx.setDynamicState(PluginDynamicState.NONE);

                log.info("Plugin registered successfully: " + meta.getId());

                {
                    final PostPluginRegisterEvent event = new PostPluginRegisterEvent(ctx);
                    this.eventMgr.callEvent(PostPluginRegisterEvent.class, event);
                }
            }
        }
    }

    @Override
    public void unregisterPlugin(final @NotNull String pluginId) throws PluginLifecycleException {
        final PluginContext ctx = this.ctxMgr.getPluginContext(pluginId);
        if (ctx == null) {
            throw new IllegalArgumentException("Plugin not found: " + pluginId);
        }

        final IPluginMetadata meta = ctx.getMetadata();
        try (final FixedReentrantReadWriteLock.ILock l = ctx.getLock().exclusiveLock()) {
            if (ctx.getState().isLoaded()) {
                this.unloadPlugin(pluginId);
            }

            log.info("Unregister plugin: " + meta.getId());

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
    public void enableAll() {
        for (final String pluginId : this.getPluginIds()) {
            try {
                final PluginContext ctx = this.ctxMgr.getPluginContext(pluginId);
                if (ctx == null || ctx.getState().isEnabled()) {
                    continue;
                }

                this.enablePlugin(pluginId);
            } catch (final PluginLifecycleException e) {
                log.error("Failed to enable plugin: " + pluginId, e);
            }
        }
    }

    @Override
    public void loadAll() {
        final Set<CompletableFuture<Void>> futures = new HashSet<>();
        for (final String pluginId : this.getPluginIds()) {
            futures.add(CompletableFuture.runAsync(() -> {
                final PluginContext ctx = this.ctxMgr.getPluginContext(pluginId);
                if (ctx == null) {
                    throw new IllegalStateException("Plugin not found: " + pluginId);
                }

                try (final FixedReentrantReadWriteLock.ILock l = ctx.getLock().sharedLock()) {
                    if (ctx.getState().isLoaded()) {
                        return;
                    }

                    l.setLendable(true);
                    this.loadPlugin(pluginId);
                } catch (final Throwable t) {
                    log.error("Failed to load plugin: " + pluginId, t);
                }
            }));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }

    @Override
    public void unloadAll() {
        final Set<CompletableFuture<Void>> futures = new HashSet<>();
        for (final String pluginId : this.getPluginIds()) {
            futures.add(CompletableFuture.runAsync(() -> {
                final PluginContext ctx = this.ctxMgr.getPluginContext(pluginId);
                if (ctx == null) {
                    throw new IllegalStateException("Plugin not found: " + pluginId);
                }

                try (final FixedReentrantReadWriteLock.ILock l = ctx.getLock().sharedLock()) {
                    if (!ctx.getState().isLoaded()) {
                        return;
                    }

                    this.unloadPlugin(pluginId);
                } catch (final Throwable t) {
                    log.error("Failed to unload plugin: " + pluginId, t);
                }
            }));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }

    private void loadPluginInternal(@NotNull final INode node) throws PluginLifecycleException {
        final PluginContext ctx = this.ctxMgr.getPluginContext(node.getPluginId());
        if (ctx == null) {
            throw new IllegalArgumentException("Plugin not found: " + node.getPluginId());
        }

        final IPluginMetadata meta = ctx.getMetadata();
        try (final FixedReentrantReadWriteLock.ILock l = ctx.getLock().sharedLock()) {
            if (ctx.getState().isLoaded()) {
                throw new IllegalStateException("Plugin already loaded: " + meta.getId() + ", state: " + ctx.getState().getName());
            }

            final Collection<ClassLoader> depsLoaders = new HashSet<>();
            {
                final IAnalyzeResult res = node.getResult();
                if (!res.isSuccess() || !(res instanceof final ISuccessResult sRes)) {
                    throw new IllegalArgumentException("Invalid node: " + node);
                }


                final Set<CompletableFuture<Void>> futures = new HashSet<>();

                // TODO CompletableFutureの処理の最初のところでlockを取って
                //  巻き戻し処理まで保持する

                final IDependencyNode[] deps = sRes.getNodes();

                for (final IDependencyNode dep : deps) {
                    futures.add(CompletableFuture.runAsync(() -> {
                        try {
                            final PluginContext depCtx = this.ctxMgr.getPluginContext(dep.getPluginId());
                            if (depCtx == null) {
                                throw new IllegalStateException("Plugin not found: " + dep.getPluginId());
                            }

                            try (final FixedReentrantReadWriteLock.ILock l2 = depCtx.getLock().sharedLock()) {
                                if (!depCtx.getState().isLoaded()) {
                                    this.loadPluginInternal(dep);
                                }

                                synchronized (depsLoaders) {
                                    depsLoaders.add(depCtx.getClassLoader());
                                }
                            }
                        } catch (final PluginLifecycleException e) {
                            throw new RuntimeException(e);
                        }
                    }));
                }

                // TODO 失敗時の巻き戻し処理を描く

                final CompletableFuture<Void> future = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
                try {
                    future.join();
                } catch (final CompletionException e) {
                    throw new PluginDynamicsLifecycleException(meta.getId(), "Failed to load dependent plugin: " + meta.getId(),
                            PluginState.NONE, PluginState.LOADED);
                }
            }

            try (final FixedReentrantReadWriteLock.ILock l2 = ctx.getLock().exclusiveLock()) {
                try {
                    log.info("Loading plugin: " + meta.getId());

                    ctx.setDynamicState(PluginDynamicState.LOADING);

                    {
                        final PrePluginLoadEvent event = new PrePluginLoadEvent(ctx);
                        this.eventMgr.callEvent(PrePluginLoadEvent.class, event);
                    }

                    final String mainClassName = meta.getMainClass();

                    final PluginClassLoader classLoader = new PluginClassLoader(meta.getId(), ctx.getUrl(),
                            this.ctxMgr.getParentLoader(), depsLoaders.toArray(new PluginClassLoader[0]));

                    final Class<?> mainClz = classLoader.loadClass(mainClassName);

                    if (!IPlugin.class.isAssignableFrom(mainClz)) {
                        throw new IllegalArgumentException(
                                "Main class must implement Plugin interface: " + mainClz.getName());
                    }

                    IPlugin plugin;
                    initInstance:
                    {
                        try {
                            final Constructor<?> constructor = mainClz.getDeclaredConstructor(ILoader.class);
                            plugin = (IPlugin) constructor.newInstance(this.loader);
                            break initInstance;
                        } catch (final NoSuchMethodException ignored) {
                        }
                        try {
                            final Constructor<?> constructor = mainClz.getDeclaredConstructor();
                            plugin = (IPlugin) constructor.newInstance();
                            break initInstance;
                        } catch (final NoSuchMethodException ignored) {
                        }

                        throw new IllegalArgumentException("Main class must have a constructor with no args or ILoader: " + mainClz.getName());
                    }

                    // Init plugin
                    plugin.onLoad();

                    ctx.setClassLoader(classLoader);
                    ctx.setPlugin(plugin);

                    ctx.setState(PluginState.LOADED);
                    ctx.setDynamicState(PluginDynamicState.NONE);

                    log.info("Plugin loaded successfully: " + meta.getId());

                    {
                        final PostPluginLoadEvent event = new PostPluginLoadEvent(ctx);
                        this.eventMgr.callEvent(PostPluginLoadEvent.class, event);
                    }
                } catch (final Throwable t) {
                    log.error("Failed to load plugin: " + meta.getId(), t);

                    ctx.setDynamicState(PluginDynamicState.NONE);

                    if (ctx.getState().isLoaded()) {
                        try {
                            this.unloadPlugin(node.getPluginId());
                        } catch (final PluginLifecycleException ex) {
                            log.error("Failed to unload plugin, but force unloaded: " + meta.getId(), ex);
                        }
                    }

                    throw new PluginDynamicsLifecycleException(meta.getId(), "Failed to load plugin: " + meta.getId(),
                            PluginState.NONE, PluginState.LOADED,
                            t);
                }
            }
        }
    }

    private void disablePluginInternal(final INode upperNode) {
        final PluginContext ctx = this.ctxMgr.getPluginContext(upperNode.getPluginId());
        if (ctx == null) {
            throw new IllegalArgumentException("Plugin not found: " + upperNode.getPluginId());
        }

        final IPluginMetadata meta = ctx.getMetadata();
        try (final FixedReentrantReadWriteLock.ILock l = ctx.getLock().exclusiveLock()) {
            if (!ctx.getState().isEnabled()) {
                throw new IllegalStateException("Plugin already disabled: " + meta.getId() + ", state: " + ctx.getState().getName());
            }

            log.info("Disabling plugin: " + meta.getId());

            {
                final IAnalyzeResult res = upperNode.getResult();
                if (!res.isSuccess() || !(res instanceof final ISuccessResult sRes)) {
                    throw new IllegalArgumentException("Invalid node: " + upperNode);
                }

                // Disabling upper nodes
                {
                    final Set<CompletableFuture<Void>> futures = new HashSet<>();
                    for (final IDependencyNode dep : sRes.getNodes()) {
                        futures.add(CompletableFuture.runAsync(() -> {
                            final PluginContext depCtx = this.ctxMgr.getPluginContext(dep.getPluginId());
                            if (depCtx == null) {
                                throw new IllegalStateException("Plugin not found: " + dep.getPluginId());
                            }

                            try (final FixedReentrantReadWriteLock.ILock l2 = depCtx.getLock().sharedLock()) {
                                if (!depCtx.getState().isEnabled()) {
                                    return;
                                }

                                this.disablePluginInternal(dep);
                            }
                        }));
                    }
                    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
                }
            }

            try (final FixedReentrantReadWriteLock.ILock l2 = ctx.getLock().exclusiveLock()) {
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
            }
        }
    }

    public String[] getPluginIds() {
        return Arrays.stream(this.ctxMgr.getContexts())
                .map(c -> c.getMetadata().getId())
                .toArray(String[]::new);
    }

    private void enablePluginInternal(final INode node) throws PluginLifecycleException {
        final PluginContext ctx = this.ctxMgr.getPluginContext(node.getPluginId());
        if (ctx == null) {
            throw new IllegalArgumentException("Plugin not found: " + node.getPluginId());
        }

        final IPluginMetadata meta = ctx.getMetadata();

        try (final FixedReentrantReadWriteLock.ILock l = ctx.getLock().exclusiveLock()) {
            if (ctx.getState().isEnabled()) {
                throw new IllegalStateException("Plugin already enabled: " + meta.getId() + ", state: " + ctx.getState().getName());
            }
            if (!ctx.getState().isLoaded()) {
                this.loadPluginInternal(node);
            }

            {
                final IAnalyzeResult res = node.getResult();
                if (res.isSuccess() && res instanceof final ISuccessResult sRes) {
                    final Set<CompletableFuture<Void>> futures = new HashSet<>();

                    // TODO CompletableFutureの処理の最初のところでlockを取って
                    //  巻き戻し処理まで保持する

                    final IDependencyNode[] deps = sRes.getNodes();
                    for (final IDependencyNode dep : deps) {
                        futures.add(CompletableFuture.runAsync(() -> {
                            try {
                                final PluginContext depCtx = this.ctxMgr.getPluginContext(dep.getPluginId());
                                if (depCtx == null) {
                                    throw new IllegalStateException("Plugin not found: " + dep.getPluginId());
                                }

                                try (final FixedReentrantReadWriteLock.ILock l2 = depCtx.getLock().sharedLock()) {
                                    if (!depCtx.getState().isEnabled()) {
                                        this.enablePluginInternal(dep);
                                    }
                                }
                            } catch (final PluginLifecycleException e) {
                                throw new RuntimeException(e);
                            }
                        }));
                    }

                    // TODO 失敗時の巻き戻し処理を描く
                    final CompletableFuture<Void> future = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
                    try {
                        future.join();
                    } catch (final CompletionException e) {
                        throw new PluginDynamicsLifecycleException(meta.getId(), "Failed to load dependent plugin: " + meta.getId(),
                                PluginState.NONE, PluginState.LOADED);
                    }
                }
            }

            try (final FixedReentrantReadWriteLock.ILock l2 = ctx.getLock().exclusiveLock()) {
                try {
                    log.info("Enabling plugin: " + meta.getId());
                    ctx.setDynamicState(PluginDynamicState.ENABLING);

                    {
                        final PrePluginEnableEvent event = new PrePluginEnableEvent(ctx);
                        this.eventMgr.callEvent(PrePluginEnableEvent.class, event);
                    }

                    ctx.getPlugin().onEnable();

                    ctx.setState(PluginState.ENABLED);
                    log.info("Plugin enabled successfully: " + meta.getId());

                    {
                        final PostPluginEnableEvent event = new PostPluginEnableEvent(ctx);
                        this.eventMgr.callEvent(PostPluginEnableEvent.class, event);
                    }
                } catch (final Throwable t) {
                    log.error("Failed to enable plugin: " + meta.getId(), t);
                    throw new PluginDynamicsLifecycleException(meta.getId(), "Failed to enable plugin: " + meta.getId(),
                            PluginState.LOADED, PluginState.ENABLED,
                            t);
                } finally {
                    ctx.setDynamicState(PluginDynamicState.NONE);
                }
            }
        }
    }
}
