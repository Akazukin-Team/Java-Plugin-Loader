package org.akazukin.loader.manager;

import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.akazukin.loader.api.context.IPlugin;
import org.akazukin.loader.api.context.IPluginContext;
import org.akazukin.loader.api.context.IPluginMetadata;
import org.akazukin.loader.api.context.PluginDynamicState;
import org.akazukin.loader.api.context.PluginState;
import org.akazukin.loader.api.context.dependency.IDependencyNode;
import org.akazukin.loader.api.context.dependency.INode;
import org.akazukin.loader.api.context.dependency.analyze.IAnalyzeResult;
import org.akazukin.loader.api.context.dependency.analyze.ISuccessResult;
import org.akazukin.loader.api.exception.PluginDependencyLoadException;
import org.akazukin.loader.api.exception.PluginDynamicsLifecycleException;
import org.akazukin.loader.api.exception.PluginLifecycleException;
import org.akazukin.loader.api.manager.IPluginManager;
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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    ExecutorService executor;

    public PluginManager(final LifecycleEventManager eventMgr, final PluginResolver pluginResolver, final PluginContextManager ctxMgr) {
        this.eventMgr = eventMgr;
        this.pluginResolver = pluginResolver;
        this.ctxMgr = ctxMgr;
        this.depResolver = new DependencyMetadataResolver(this.ctxMgr);
        this.executor = Executors.newWorkStealingPool(16);
    }

    public CompletableFuture<?> disableNode(final PluginContext ctx, final boolean ignoreStateSpec, final LoadCache cache)
            throws PluginLifecycleException {
        final Set<CompletableFuture<?>> futures = new HashSet<>();
        for (final IPluginContext depCtx : ctx.getDependencies()) {
            futures.add(this.disableNode((PluginContext) depCtx, ignoreStateSpec, cache));
        }

        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } catch (final CompletionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof final PluginLifecycleException e2) {
                throw e2;
            }
            if (cause instanceof final RuntimeException e2) {
                throw e2;
            }
            throw new RuntimeException(cause);
        }

        if (!cache.processed.contains(ctx.getMetadata().getId())) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                final PluginState state = ctx.getState();
                if (!state.isEnabled()) {
                    return;
                }

                this.disablePluginInternal(ctx);
            } catch (final PluginLifecycleException e) {
                throw new RuntimeException(e);
            }
        }, this.executor);
    }

    public CompletableFuture<?> unloadNode(final INode node, final boolean ignoreStateSpec, final LoadCache cache)
            throws PluginLifecycleException {
        if (!node.getResult().isSuccess()
                || !(node.getResult() instanceof final ISuccessResult res)) {
            throw new IllegalArgumentException("Invalid node: " + node);
        }

        final IPluginContext ctx = this.ctxMgr.getPluginContext(node.getPluginId());

        final Set<CompletableFuture<?>> futures = new HashSet<>();
        if (ctx == null || ctx.getStateSpec() == null || ignoreStateSpec) {
            for (final INode dep : res.getNodes()) {
                futures.add(this.unloadNode(dep, ignoreStateSpec, cache));
            }
        }

        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } catch (final CompletionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof final PluginLifecycleException e2) {
                throw e2;
            }
            if (cause instanceof final RuntimeException e2) {
                throw e2;
            }
            throw new RuntimeException(cause);
        }

        if (!cache.processed.contains(node.getPluginId())) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                final PluginState state = this.ctxMgr.getPluginContext(node.getPluginId()).getState();
                if (state == null || !state.isLoaded()) {
                    return;
                }

                this.unloadPlugin(node.getPluginId());
            } catch (final PluginLifecycleException e) {
                throw new RuntimeException(e);
            }
        }, this.executor);
    }

    public synchronized void unloadPluginInternal(final @NotNull PluginContext ctx) throws PluginLifecycleException {
        final IPluginMetadata meta = ctx.getMetadata();
        synchronized (ctx) {
            if (!ctx.getState().isLoaded()) {
                throw new IllegalStateException("Plugin already unloaded: " + meta.getId());
            }
            if (ctx.getState().isEnabled()) {
                try {
                    this.disablePluginInternal(ctx);
                } catch (final PluginLifecycleException e) {
                    log.error("Failed to disable plugin, but ignored: " + meta.getId(), e);
                }
            }

            log.info("Unloading plugin: " + meta.getName());
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

        this.enableNode(node, new LoadCache()).join();
    }

    @Override
    public void disablePlugin(@NotNull final String pluginId) throws PluginLifecycleException {
        final PluginContext ctx = this.ctxMgr.getPluginContext(pluginId);
        if (ctx == null) {
            throw new IllegalArgumentException("Plugin not found: " + pluginId);
        }

        synchronized (ctx) {
            ctx.setStateSpec(PluginState.LOADED);
            try {
                this.disableNode(ctx, false, new LoadCache()).join();
            } catch (final PluginLifecycleException e) {
                throw e;
            }
        }
    }

    @Override
    public void unloadPlugin(@NotNull final String pluginId) throws PluginLifecycleException {
        final PluginContext ctx = this.ctxMgr.getPluginContext(pluginId);
        this.unloadPluginInternal(ctx);
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

    public synchronized void disablePluginInternal(final PluginContext ctx) throws PluginLifecycleException {
        final IPluginMetadata meta = ctx.getMetadata();
        synchronized (ctx) {
            if (!ctx.getState().isEnabled()) {
                throw new IllegalStateException("Plugin already disabled: " + meta.getId());
            }

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
        }
    }

    public String[] getPluginIds() {
        return Arrays.stream(this.ctxMgr.getContexts())
                .map(c -> c.getMetadata().getId())
                .toArray(String[]::new);
    }

    public CompletableFuture<INode> enableNode(final INode node, final LoadCache cache)
            throws PluginLifecycleException {
        if (!cache.success) {
            return CompletableFuture.completedFuture(node);
        }

        if (!node.getResult().isSuccess() || !(node.getResult() instanceof final ISuccessResult res)) {
            throw new IllegalArgumentException("Invalid node: " + node);
        }

        final PluginContext ctx = this.ctxMgr.getPluginContext(node.getPluginId());
        if (ctx == null) {
            throw new IllegalArgumentException("Plugin not found: " + node.getPluginId());
        }
        if (ctx.getState().isEnabled()) {
            return CompletableFuture.completedFuture(node);
        }

        final Map<INode, CompletableFuture<INode>> futures = new HashMap<>();
        for (final INode dep : res.getNodes()) {
            futures.put(dep, this.enableNode(dep, cache.clone()));
        }

        try {
            CompletableFuture.allOf(futures.values().toArray(CompletableFuture[]::new)).join();
        } catch (final CompletionException ex) {
            // On failure, attempt to unload any implicitly-loaded dependency plugins
            for (final INode dep : futures.keySet()) {
                try {
                    final IPluginContext depCtx = this.ctxMgr.getPluginContext(dep.getPluginId());
                    if (depCtx != null && depCtx.getStateSpec() == null && depCtx.getState() != null && depCtx.getState().isLoaded()) {
                        try {
                            this.unloadPlugin(dep.getPluginId());
                        } catch (final PluginLifecycleException e) {
                            log.error("Failed to unload dependency after enable failure: {}", dep.getPluginId(), e);
                        }
                    }
                } catch (final RuntimeException ignored) {
                }
            }

            final Throwable cause = ex.getCause();
            if (cause instanceof final PluginLifecycleException e2) {
                throw e2;
            }
            if (cause instanceof final RuntimeException e2) {
                throw e2;
            }
            throw new RuntimeException(cause);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                synchronized (cache.processed) {
                    if (cache.processed.contains(node.getPluginId())) {
                        if (!ctx.getState().isEnabled()) {
                            throw new PluginLifecycleException("Plugin was tried to enable but already failed: " + node.getPluginId(), node.getPluginId());
                        }
                        log.debug(node.getPluginId() + " was tried to enabling; skipped.");
                    } else {
                        cache.processed.add(node.getPluginId());
                        this.enablePluginInternal(ctx);
                    }
                }
                return node;
            } catch (final PluginLifecycleException e) {
                // cleanup: unload any implicitly-loaded dependencies and possibly this plugin
                for (final INode d : res.getNodes()) {
                    final IPluginContext dctx = this.ctxMgr.getPluginContext(d.getPluginId());
                    if (dctx != null && dctx.getStateSpec() == null && dctx.getState().isLoaded()) {
                        try {
                            this.unloadPlugin(d.getPluginId());
                        } catch (final PluginLifecycleException ex2) {
                            log.error("Failed to unload dependency after enable failure: {}", d.getPluginId(), ex2);
                        }
                    }
                }

                if (ctx.getStateSpec() == null && ctx.getState().isLoaded()) {
                    try {
                        this.unloadPlugin(node.getPluginId());
                    } catch (final PluginLifecycleException ex2) {
                        log.error("Failed to unload plugin after enable failure: {}", node.getPluginId(), ex2);
                    }
                }

                throw new RuntimeException(e);
            }
        }, this.executor);
    }

    public void enablePluginInternal(final PluginContext ctx) throws PluginLifecycleException {
        final IPluginMetadata meta = ctx.getMetadata();

        synchronized (ctx) {
            if (ctx.getState().isEnabled()) {
                throw new IllegalStateException("Plugin already enabled: " + meta.getId());
            }
            if (ctx.getState() == PluginState.NONE) {
                this.loadPlugin(ctx.getMetadata().getId());
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

    public static class LoadCache {
        final Set<String> visited = new HashSet<>();
        final Set<String> processed = new HashSet<>();
        boolean success = true;

        @Override
        public LoadCache clone() {
            final LoadCache cache = new LoadCache();
            cache.visited.addAll(this.visited);
            cache.processed.addAll(this.processed);
            cache.success = this.success;
            return cache;
        }
    }
}
