package org.akazukin.loader.context;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.context.IPluginMetadata;

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
        try {
            return super.loadClass(name, resolve);
        } catch (final ClassNotFoundException e) {
            for (final ClassLoader parent : this.parentLoaders) {
                try {
                    return parent.loadClass(name);
                } catch (final ClassNotFoundException ignored) {
                }
            }

            throw new ClassNotFoundException(e.getMessage());
        }
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
