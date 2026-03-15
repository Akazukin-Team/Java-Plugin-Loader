package org.akazukin.loader.context;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.akazukin.loader.api.context.IPluginMetadata;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

/**
 * Isolated ClassLoader for plugins enabling class isolation and package access control.
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PluginClassLoader extends URLClassLoader {
    private static final URL[] EMPTY_URLS = new URL[0];
    IPluginMetadata metadata;
    PluginClassLoader[] parents;

    public PluginClassLoader(final IPluginMetadata metadata, final ClassLoader parent, final PluginClassLoader... parents) {
        super(EMPTY_URLS, parent);
        this.metadata = metadata;
        this.parents = parents;
    }

    @Override
    protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        log.debug("Loading class: {}, Classloaders: {}", name, (this.parents.length + 1) + ", Name: " + this.metadata.getId());
        for (final ClassLoader parent : this.parents) {
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

        for (final ClassLoader parent : this.parents) {
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
