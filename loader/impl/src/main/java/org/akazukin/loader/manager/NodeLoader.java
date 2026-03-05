package org.akazukin.loader.manager;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.akazukin.loader.api.IPluginContext;
import org.akazukin.loader.api.PluginState;
import org.akazukin.loader.api.dependency.INode;
import org.akazukin.loader.api.dependency.analyze.ISuccessResult;
import org.akazukin.loader.api.exception.PluginLifecycleException;
import org.akazukin.loader.context.PluginContextManager;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NodeLoader implements Closeable {
    ExecutorService executor;
    final PluginContextManager ctxMgr;
    final PluginManager manager;

    public NodeLoader(final int parallelism, final PluginManager manager, final PluginContextManager ctxMgr) {
        this.executor = Executors.newWorkStealingPool(parallelism);
        this.manager = manager;
        this.ctxMgr = ctxMgr;
    }

    public CompletableFuture<INode> enableNode(final INode node, final LoadCache cache)
            throws PluginLifecycleException {
        if (!cache.success) {
            return CompletableFuture.completedFuture(node);
        }

        if (!node.getResult().isSuccess()
                || !(node.getResult() instanceof final ISuccessResult res)) {
            throw new IllegalArgumentException("Invalid node: " + node);
        }
        if (this.ctxMgr.getPluginContext(node.getPluginId()).getState().isEnabled()) {
            return CompletableFuture.completedFuture(node);
        }

        if (cache.visited.contains(node.getPluginId())) {
            throw new PluginLifecycleException("Circular dependency detected: " + node.getPluginId(), node.getPluginId());
        }
        cache.visited.add(node.getPluginId());

        final Set<CompletableFuture<INode>> futures = new HashSet<>();
        for (final INode dep : res.getNodes()) {
            futures.add(this.enableNode(dep, cache.clone()));
        }
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } catch (final CompletionException e) {
            final CompletableFuture<?>[] safeReverts = futures.stream()
                    .map(f -> f.handle((resNode, ex) -> {
                                if (ex != null) {
                                    return CompletableFuture.completedFuture(null);
                                }
                                try {
                                    return this.unloadNode(resNode, false, cache)
                                            .exceptionally(err -> {
                                                log.error("An exception was thrown while reverting a successfully enabled plugin", err);
                                                return null;
                                            });
                                } catch (final PluginLifecycleException ex2) {
                                    log.error("An unexpected error occurred while scheduling revert", ex2);
                                    return CompletableFuture.completedFuture(null);
                                }
                            })
                            .thenCompose(x -> x))
                    .toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(safeReverts).join();

            final Throwable cause = e.getCause();
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
                    final IPluginContext ctx = this.ctxMgr.getPluginContext(node.getPluginId());
                    if (cache.processed.contains(node.getPluginId())) {
                        if (!ctx.getState().isEnabled()) {
                            throw new PluginLifecycleException("Plugin was tried to enable but already failed: " + node.getPluginId(), node.getPluginId());
                        }
                        log.debug(node.getPluginId() + " was tried to enabling; skipped.");
                    } else {
                        cache.processed.add(node.getPluginId());
                        this.manager.enablePluginInternal(node.getPluginId());
                    }
                }
                return node;
            } catch (final PluginLifecycleException e) {
                throw new RuntimeException(e);
            }
        }, this.executor);
    }

    public CompletableFuture<?> disableNode(final INode node, final boolean ignoreStateSpec, final LoadCache cache)
            throws PluginLifecycleException {
        if (!node.getResult().isSuccess()
                || !(node.getResult() instanceof final ISuccessResult res)) {
            throw new IllegalArgumentException("Invalid node: " + node);
        }

        final IPluginContext ctx = this.ctxMgr.getPluginContext(node.getPluginId());

        final Set<CompletableFuture<?>> futures = new HashSet<>();
        if (ignoreStateSpec || ctx == null || ctx.getStateSpec() == null) {
            for (final INode dep : res.getNodes()) {
                futures.add(this.disableNode(dep, ignoreStateSpec, cache));
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
                if (!state.isEnabled()) {
                    return;
                }

                this.manager.disablePlugin(node.getPluginId());
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

                this.manager.unloadPlugin(node.getPluginId());
            } catch (final PluginLifecycleException e) {
                throw new RuntimeException(e);
            }
        }, this.executor);
    }

    @Override
    public void close() {
        this.executor.shutdown();
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
