package org.akazukin.loader.context;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.IPluginMetadata;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Isolated ClassLoader for plugins enabling class isolation and package access control.
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PluginClassLoader extends URLClassLoader {
    private static final URL[] EMPTY_URLS = new URL[0];
    IPluginMetadata metadata;
    Map<String, Class<?>> classCache = new ConcurrentHashMap<>();
    Set<String> loadedClasses = ConcurrentHashMap.newKeySet();
    Set<ClassLoader> parentLoaders = ConcurrentHashMap.newKeySet();

    public PluginClassLoader(final IPluginMetadata metadata, final ClassLoader parent) {
        super(EMPTY_URLS, parent);
        this.metadata = metadata;
    }

    public void addParentLoader(final ClassLoader loader) {
        this.parentLoaders.add(loader);
    }

    @Override
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        // キャッシュを確認
        Class<?> cachedClass = this.classCache.get(name);
        if (cachedClass != null) {
            return cachedClass;
        }

        synchronized (this.getClassLoadingLock(name)) {
            // ダブルチェック
            cachedClass = this.classCache.get(name);
            if (cachedClass != null) {
                return cachedClass;
            }

            try {
                // このClassLoaderで読み込む
                final Class<?> clazz = this.findClass(name);
                this.loadedClasses.add(name);
                this.classCache.put(name, clazz);

                if (resolve) {
                    this.resolveClass(clazz);
                }
                return clazz;
            } catch (final ClassNotFoundException e) {
                // 親ClassLoaderで検索
                for (final ClassLoader parentLoader : this.parentLoaders) {
                    try {
                        final Class<?> clazz = parentLoader.loadClass(name);
                        this.classCache.put(name, clazz);
                        return clazz;
                    } catch (final ClassNotFoundException ignored) {
                    }
                }
                throw new ClassNotFoundException(
                        "Class not found: " + name + " in plugin: " + this.metadata.getId(), e);
            }
        }
    }

    public Set<String> getLoadedClasses() {
        return Set.copyOf(this.loadedClasses);
    }

    @Override
    public void close() throws IOException {
        this.classCache.clear();
        this.loadedClasses.clear();
        super.close();
    }

    @Override
    public void addURL(final URL url) {
        super.addURL(url);
    }

    @Override
    public String toString() {
        return "PluginClassLoader{" +
                "pluginId='" + this.metadata.getId() + '\'' +
                ", urls=" + Arrays.toString(this.getURLs()) +
                '}';
    }
}
