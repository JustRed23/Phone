package dev.JustRed23.App;

import com.google.common.base.Charsets;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

public class App {

    private boolean enabled;
    private AppLoader loader;
    private File jarFile;
    private AppDescription description;
    private File dataFolder;
    private ClassLoader classLoader;
    private AppLogger logger;

    public App() {
        final ClassLoader classLoader = this.getClass().getClassLoader();
        if (!(classLoader instanceof AppClassLoader))
            throw new IllegalStateException(String.format("App requires %s as loader", AppClassLoader.class.getName()));
        ((AppClassLoader) classLoader).init(this);
    }

    protected App(@NotNull final AppLoader loader, @NotNull final AppDescription description, @NotNull final File dataFolder, @NotNull final File file) {
        final ClassLoader classLoader = this.getClass().getClassLoader();
        if (classLoader instanceof AppClassLoader)
            throw new IllegalStateException("Cannot use initialization constructor at runtime");
        init(loader, description, dataFolder, file, classLoader);
    }

    @NotNull
    public File getDataFolder() {
        return dataFolder;
    }

    @NotNull
    public AppLoader getLoader() {
        return loader;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @NotNull
    public File getJarFile() {
        return jarFile;
    }

    @NotNull
    public AppDescription getDescription() {
        return description;
    }

    @Nullable
    protected final Reader getTextResource(@NotNull String file) {
        final InputStream in = getResource(file);
        return in == null ? null : new InputStreamReader(in, Charsets.UTF_8);
    }

    @Nullable
    public InputStream getResource(@NotNull String filename) {
        Validate.notNull(filename, "File name cannot be null");

        try {
            URL url = getClassLoader().getResource(filename);

            if (url == null)
                return null;

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException e) {
            return null;
        }
    }

    @NotNull
    protected final ClassLoader getClassLoader() {
        return classLoader;
    }

    protected final void setEnabled(final boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;

            if (enabled)
                onEnable();
            else
                onDisable();
        }
    }

    final void init(@NotNull AppLoader loader, @NotNull AppDescription description, @NotNull File dataFolder, @NotNull File jarFile, @NotNull ClassLoader classLoader) {
        this.loader = loader;
        this.jarFile = jarFile;
        this.description = description;
        this.dataFolder = dataFolder;
        this.classLoader = classLoader;
        this.logger = new AppLogger(this);
    }

    public void onLoad() {}

    public void onDisable() {}

    public void onEnable() {}

    @NotNull
    public Logger getLogger() {
        return logger;
    }

    @NotNull
    public String toString() {
        return description.getFullName();
    }

    @NotNull
    public static <T extends App> T getApp(@NotNull Class<T> clazz) {
        Validate.notNull(clazz, "Class cannot be null");

        if (!App.class.isAssignableFrom(clazz))
            throw new IllegalArgumentException(String.format("%s does not extend %s", clazz, App.class));

        final ClassLoader classLoader = clazz.getClassLoader();
        if (!(classLoader instanceof AppClassLoader))
            throw new IllegalArgumentException(String.format("%s is not initialized by %s", clazz, AppLoader.class));

        App app = ((AppClassLoader) classLoader).app;
        if (app == null)
            throw new IllegalStateException(String.format("Cannot get app for %s from a static initializer", clazz));

        return clazz.cast(app);
    }
}
