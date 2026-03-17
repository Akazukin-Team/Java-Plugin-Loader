package org.akazukin.loader.context;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

/**
 * Isolated ClassLoader for plugins enabling class isolation and package access control.
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
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
        for (final ClassLoader parent : this.parents) {
            if (!(parent instanceof final PluginClassLoader plParent)) {
                continue;
            }

            final Class<?> clz = plParent.findLoadedClass(name);
            if (clz != null) {
                return clz;
            }
        }
        {
            final Class<?> clz = super.findLoadedClass(name);
            if (clz != null) {
                return clz;
            }
        }

        for (final ClassLoader parent : this.parents) {
            try {
                return parent.loadClass(name);
            } catch (final ClassNotFoundException ignored) {
            }
        }
        try {
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
