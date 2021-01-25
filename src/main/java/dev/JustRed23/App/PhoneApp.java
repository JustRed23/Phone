package dev.JustRed23.App;

import com.google.common.base.Charsets;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

public class PhoneApp {

    private boolean isEnabled = false;
    private AppLoader loader = null;
    private File file = null;
    private AppDescriptionFile description = null;
    private File dataFolder = null;
    private ClassLoader classLoader = null;
    private AppLogger logger = null;

    public PhoneApp() {
        final ClassLoader classLoader = this.getClass().getClassLoader();
        if (!(classLoader instanceof AppClassLoader)) {
            throw new IllegalStateException("JavaPlugin requires " + AppClassLoader.class.getName());
        }
        ((AppClassLoader) classLoader).initialize(this);
    }

    protected PhoneApp(@NotNull final AppLoader loader, @NotNull final AppDescriptionFile description, @NotNull final File dataFolder, @NotNull final File file) {
        final ClassLoader classLoader = this.getClass().getClassLoader();

        if (classLoader instanceof AppClassLoader)
            throw new IllegalStateException("Cannot use initialization constructor at runtime");

        init(loader, description, dataFolder, file, classLoader);
    }

    @NotNull
    public final File getDataFolder() {
        return dataFolder;
    }

    @NotNull
    public final AppLoader getAppLoader() {
        return loader;
    }

    public final boolean isEnabled() {
        return isEnabled;
    }

    @NotNull
    protected File getFile() {
        return file;
    }

    @NotNull
    public final AppDescriptionFile getDescription() {
        return description;
    }

    @Nullable
    protected final Reader getTextResource(@NotNull String file) {
        final InputStream in = getResource(file);
        return in == null ? null : new InputStreamReader(in, Charsets.UTF_8);
    }

    @Nullable
    public InputStream getResource(@NotNull String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }

        try {
            URL url = getClassLoader().getResource(filename);

            if (url == null) {
                return null;
            }

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }

    @NotNull
    protected final ClassLoader getClassLoader() {
        return classLoader;
    }

    protected final void setEnabled(final boolean enabled) {
        if (isEnabled != enabled) {
            isEnabled = enabled;

            if (isEnabled) {
                onEnable();
            } else {
                onDisable();
            }
        }
    }

    final void init(@NotNull AppLoader loader, @NotNull AppDescriptionFile description, @NotNull File dataFolder, @NotNull File file, @NotNull ClassLoader classLoader) {
        this.loader = loader;
        this.file = file;
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
    public static <T extends PhoneApp> T getApp(@NotNull Class<T> clazz) {
        Validate.notNull(clazz, "Null class cannot have a plugin");

        if (!PhoneApp.class.isAssignableFrom(clazz))
            throw new IllegalArgumentException(clazz + " does not extend " + PhoneApp.class);

        final ClassLoader cl = clazz.getClassLoader();
        if (!(cl instanceof AppClassLoader))
            throw new IllegalArgumentException(clazz + " is not initialized by " + AppClassLoader.class);

        PhoneApp phoneApp = ((AppClassLoader) cl).phoneApp;
        if (phoneApp == null)
            throw new IllegalStateException("Cannot get plugin for " + clazz + " from a static initializer");

        return clazz.cast(phoneApp);
    }

    @NotNull
    public static PhoneApp getProvidingApp(@NotNull Class<?> clazz) {
        Validate.notNull(clazz, "Null class cannot have a plugin");

        final ClassLoader cl = clazz.getClassLoader();
        if (!(cl instanceof AppClassLoader))
            throw new IllegalArgumentException(clazz + " is not provided by " + AppClassLoader.class);

        PhoneApp phoneApp = ((AppClassLoader) cl).phoneApp;
        if (phoneApp == null)
            throw new IllegalStateException("Cannot get plugin for " + clazz + " from a static initializer");

        return phoneApp;
    }
}