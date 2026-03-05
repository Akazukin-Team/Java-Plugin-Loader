package org.akazukin.loader.manager;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.akazukin.loader.api.ILoaderConfig;
import org.akazukin.loader.api.context.IPluginMetadata;
import org.akazukin.loader.api.manager.IPluginLoader;
import org.akazukin.loader.api.manager.IPluginManager;
import org.akazukin.loader.context.PluginMetadataLoader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PluginLoader implements IPluginLoader {
    private static final String PROPERTIES = "META-INF/plugin.properties";
    ILoaderConfig cfg;
    IPluginManager pluginMgr;
    PluginMetadataLoader metadataLoader;

    public PluginLoader(@NotNull final ILoaderConfig cfg, final PluginManager pluginMgr) {
        this.cfg = cfg;
        this.pluginMgr = pluginMgr;
        this.metadataLoader = new PluginMetadataLoader();
    }

    public void loadPlugins() {
        for (final String path : this.cfg.getPluginPaths()) {
            final File file = new File(path);
            if (!file.exists()) {
                return;
            }

            if (file.isDirectory()) {
                this.loadPluginFromFolder(path);
            } else {
                this.loadPluginFromZip(path);
            }
        }

        for (final String path : this.cfg.getPluginDirectories()) {
            final File file = new File(path);
            if (!file.exists() || !file.isDirectory()) {
                return;
            }

            this.loadPluginsInDirectory(path);
        }

        if (this.cfg.isLoadFromClassPath()) {
            this.loadPluginsFromClasspath();
        }
    }

    private void loadPluginFromZip(final String path) {
        try (final ZipFile zipFile = new ZipFile(path)) {
            final ZipEntry entry = zipFile.getEntry(PROPERTIES);

            if (entry != null) {
                try (final InputStream is = zipFile.getInputStream(entry)) {
                    final IPluginMetadata meta = this.metadataLoader.loadFromResource(is);
                    this.pluginMgr.registerPlugin(URI.create(path).toURL(), meta);
                }
            } else {
                throw new RuntimeException("Entry not found: " + PROPERTIES);
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadPluginsFromClasspath() {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            final Enumeration<URL> resources = loader.getResources(PROPERTIES);
            while (resources.hasMoreElements()) {
                final URL res = resources.nextElement();
                final String protocol = res.getProtocol();
                if ("jar".equals(protocol)) {
                    final String path = res.getPath();
                    final int idx = path.indexOf('!');
                    if (idx <= 0) {
                        continue;
                    }
                    final String jarUrlStr = path.substring(0, idx);
                    try {
                        final URI jarUri = URI.create(jarUrlStr);
                        final Path jarPath = Paths.get(jarUri);
                        this.loadPluginFromZip(jarPath.toString());
                    } catch (final Exception ex) {
                        log.error("Failed to load plugin from jar: " + jarUrlStr, ex);
                    }
                } else if ("file".equals(protocol)) {
                    try {
                        final URI uri = res.toURI();
                        final Path p = Paths.get(uri);
                        final Path root = p.getParent().getParent();
                        this.loadPluginFromFolder(root.toString());
                    } catch (final Exception ex) {
                        log.error("Failed to load plugin from file: " + res, ex);
                    }
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadPluginFromFolder(final String path) {
        final File folder = new File(path);
        if (!folder.exists()) {
            throw new IllegalArgumentException("Path does not exist: " + path);
        }

        final File meta = new File(folder, PROPERTIES);
        if (!meta.exists() || !meta.isFile()) {
            throw new IllegalArgumentException("File not found under: " + folder.getAbsolutePath() + ", file: " + PROPERTIES);
        }

        try (final InputStream is = Files.newInputStream(meta.toPath())) {
            final IPluginMetadata metaData = this.metadataLoader.loadFromResource(is);
            this.pluginMgr.registerPlugin(folder.toURI().toURL(), metaData);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadPluginsInDirectory(final String path) {
        final File folder = new File(path);
        if (!folder.exists() || !folder.isDirectory()) {
            log.error("Path does not exist: " + path);
        }

        final File[] files = Objects.requireNonNull(folder.listFiles());
        for (final File f : files) {
            if (f.isFile()) {
                if (this.hasMetadataInZip(f)) {
                    try {
                        this.loadPluginFromZip(f.getAbsolutePath());
                    } catch (final Exception e) {
                        log.error("Failed to load plugin: " + f.getAbsolutePath(), e);
                    }
                }
            }
        }
    }

    private boolean hasMetadataInZip(final File jarFile) {
        if (jarFile == null || !jarFile.exists() || !jarFile.isFile()) {
            return false;
        }
        try (final ZipFile zip = new ZipFile(jarFile)) {
            final ZipEntry entry = zip.getEntry(PROPERTIES);
            return entry != null;
        } catch (final IOException e) {
            return false;
        }
    }

    private boolean hasMetadataInDirectory(final File root) {
        if (root == null) {
            return false;
        }
        final File meta = new File(root, PROPERTIES);
        return meta.exists() && meta.isFile();
    }
}
