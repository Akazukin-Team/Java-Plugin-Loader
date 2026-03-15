package org.akazukin.loader.context;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

    public synchronized void addParentLoader(final ClassLoader loader) {
        this.parentLoaders.add(loader);
    }

    @Override
    protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        log.debug("Loading class: {}, Classloaders: {}", name, (this.parentLoaders.size() + 1) + ", Name: " + this.metadata.getId());
        for (final ClassLoader parent : this.parentLoaders) {
            if (!(parent instanceof final PluginClassLoader plParent)) {
                continue;
            }

            log.debug("Checking parent: {}, {}", plParent.metadata.getId(), plParent);
            final Class<?> clz = plParent.findLoadedClass(name);
            if (clz != null) {
                return clz;
            }
        }
        {
            log.debug("Checking this, {}, {}", this.metadata.getId(), this);
            final Class<?> clz = super.findLoadedClass(name);
            if (clz != null) {
                return clz;
            }
        }

        for (final ClassLoader parent : this.parentLoaders) {
            try {
                log.debug("Loading parent: {}", parent);
                return parent.loadClass(name);
            } catch (final ClassNotFoundException ignored) {
            }
        }
        try {
            log.debug("Loading this, {}, {}", this.metadata.getId(), this);
            return super.loadClass(name, resolve);
        } catch (final ClassNotFoundException e) {
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
                '}' + super.toString();
    }
}
