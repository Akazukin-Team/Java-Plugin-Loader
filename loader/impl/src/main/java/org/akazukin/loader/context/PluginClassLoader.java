package org.akazukin.loader.context;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

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
    String pluginId;
    PluginClassLoader[] parents;

    public PluginClassLoader(final String pluginId, final URL pluginUrl,
                             final ClassLoader parent, final PluginClassLoader... parents) {
        super(EMPTY_URLS, parent);
        this.addURL(pluginUrl);

        this.pluginId = pluginId;
        this.parents = parents;
    }

    @Override
    protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        log.debug("Loading class: {}, Classloaders: {}", name, (this.parents.length + 1) + ", Name: " + this.pluginId);
        for (final ClassLoader parent : this.parents) {
            if (!(parent instanceof final PluginClassLoader plParent)) {
                continue;
            }

            log.debug("Checking parent: {}, {}", plParent.pluginId, plParent);
            final Class<?> clz = plParent.findLoadedClass(name);
            if (clz != null) {
                return clz;
            }
        }
        {
            log.debug("Checking this, {}, {}", this.pluginId, this);
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
            log.debug("Loading this, {}, {}", this.pluginId, this);
            return super.loadClass(name, resolve);
        } catch (final ClassNotFoundException e) {
            throw new ClassNotFoundException(e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "PluginClassLoader{" +
                "pluginId='" + this.pluginId + '\'' +
                ", urls=" + Arrays.toString(this.getURLs()) +
                '}' + super.toString();
    }
}
