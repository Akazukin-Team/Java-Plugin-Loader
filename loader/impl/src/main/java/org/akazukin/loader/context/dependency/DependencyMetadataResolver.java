package org.akazukin.loader.context.dependency;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.context.IPluginContext;
import org.akazukin.loader.api.context.IPluginMetadata;
import org.akazukin.loader.api.context.dependency.IDependencyNode;
import org.akazukin.loader.api.context.dependency.IPluginDependency;
import org.akazukin.loader.api.context.dependency.analyze.IAnalyzeResult;
import org.akazukin.loader.api.exception.PluginNotFoundException;
import org.akazukin.loader.context.PluginContextManager;
import org.akazukin.loader.context.dependency.result.CircularDependencyResult;
import org.akazukin.loader.context.dependency.result.NotFoundResult;
import org.akazukin.loader.context.dependency.result.SuccessResult;
import org.akazukin.loader.context.dependency.result.VersionMismatchResult;
import org.akazukin.semver.range.ISemverRange;
import org.akazukin.semver.range.SemverRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DependencyMetadataResolver {
    PluginContextManager containers;

    public DependencyMetadataResolver(final PluginContextManager containers) {
        this.containers = containers;
    }

    @NotNull
    public Node getUpperNode(final String pluginId) throws PluginNotFoundException {
        final IAnalyzeResult res = this.resolveUpper(pluginId, new ResolverCache());
        if (res instanceof NotFoundResult) {
            throw new PluginNotFoundException(pluginId);
        }

        return new Node(pluginId, res);
    }

    @NotNull
    public Node getLowerNode(final String pluginId) throws PluginNotFoundException {
        final IAnalyzeResult res = this.resolveLower(pluginId, SemverRange.ALL, new ResolverCache()).getResult();
        if (res instanceof NotFoundResult) {
            throw new PluginNotFoundException(pluginId);
        }

        return new Node(pluginId, res);
    }

    // 読み込み時に使う
    @NotNull
    private ResolverResult resolveLower(final String pluginId, final ISemverRange range, final ResolverCache cache) {
        final IPluginMetadata meta;
        {
            final @Nullable IPluginContext context = this.containers.getPluginContext(pluginId);
            if (context == null) {
                final IAnalyzeResult res = new NotFoundResult(pluginId);
                cache.cache.put(pluginId, res);
                return new ResolverResult(res);
            }
            meta = context.getMetadata();
        }

        // Won't cache this result because the required version is different.
        if (!range.isSuitable(meta.getVersion().getVersionCore())) {
            final IAnalyzeResult res = new VersionMismatchResult(pluginId, range, meta.getVersion().getVersionCore());
            return new ResolverResult(res);
        }

        if (cache.visited.contains(pluginId)) {
            final IAnalyzeResult res = new CircularDependencyResult(pluginId);
            cache.cache.put(pluginId, res);
            return new ResolverResult(res);
        }

        cache.visited.add(pluginId);
        final Set<IDependencyNode> nodes = new HashSet<>();
        final Map<String, IAnalyzeResult> visited = new HashMap<>();
        for (final IPluginDependency dep : meta.getDependencies()) {
            final ResolverCache newCache = cache.clone();

            @NotNull final ResolverResult res = this.resolveLower(dep.getId(), dep.getVersionRange(), newCache);

            // For circular dependency
            if (res.visited.containsKey(dep.getId())) {
                return new ResolverResult(res.result);
            }

            visited.putAll(res.visited);
            nodes.add(new DependencyNode(dep.getId(), res.result, dep.isRequired()));
        }

        {
            final IAnalyzeResult res = new SuccessResult(nodes.toArray(IDependencyNode.EMPTY_ARR));
            return new ResolverResult(res, visited);
        }
    }

    @NotNull
    private IAnalyzeResult resolveUpper(final String pluginId, final ResolverCache cache) {
        final IPluginMetadata meta;
        {
            final @Nullable IPluginContext context = this.containers.getPluginContext(pluginId);
            if (context == null) {
                final IAnalyzeResult res = new NotFoundResult(pluginId);
                cache.cache.put(pluginId, res);
                return res;
            }
            meta = context.getMetadata();
        }

        if (cache.visited.contains(pluginId)) {
            final IAnalyzeResult res = new CircularDependencyResult(pluginId);
            cache.cache.put(pluginId, res);
            return res;
        }
        cache.visited.add(pluginId);

        final Set<IDependencyNode> nodes = new HashSet<>();
        for (final IPluginContext ctx : this.containers.getContexts()) {
            for (final IPluginDependency dep : ctx.getMetadata().getDependencies()) {
                if (!pluginId.equals(dep.getId())
                        || !dep.getVersionRange().isSuitable(meta.getVersion().getVersionCore())) {
                    continue;
                }

                final IAnalyzeResult res = this.resolveUpper(ctx.getMetadata().getId(), cache.clone());
                nodes.add(new DependencyNode(ctx.getMetadata().getId(), res, dep.isRequired()));
            }
        }

        return new SuccessResult(nodes.toArray(IDependencyNode.EMPTY_ARR));
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    private static class ResolverCache {
        final Set<String> visited = new HashSet<>();
        Map<String, IAnalyzeResult> cache = new HashMap<>();

        @Override
        public ResolverCache clone() {
            final ResolverCache obj = new ResolverCache();
            obj.cache = this.cache;
            obj.visited.addAll(this.visited);
            return obj;
        }
    }

    @NoArgsConstructor
    private static class ResolverResult {
        @Getter
        IAnalyzeResult result;
        Map<String, IAnalyzeResult> visited = new HashMap<>();

        public ResolverResult(final IAnalyzeResult result, final Map<String, IAnalyzeResult> visited) {
            this.result = result;
            this.visited = visited;
        }

        public ResolverResult(final IAnalyzeResult result) {
            this.result = result;
        }

        @Override
        public ResolverResult clone() {
            final ResolverResult obj = new ResolverResult(this.result);
            obj.visited.putAll(this.visited);
            return obj;
        }
    }
}
