package dev.JustRed23.App;

import dev.JustRed23.Exceptions.InvalidAppException;
import dev.JustRed23.Exceptions.InvalidDescriptionException;
import dev.JustRed23.Phone.Phone;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class AppLoader {

    private final Pattern[] fileFilters = new Pattern[]{Pattern.compile("\\.jar$")};
    private final Map<String, Class<?>> classes = new ConcurrentHashMap<>();
    private final List<AppClassLoader> loaders = new CopyOnWriteArrayList<>();

    public AppLoader() {}

    public App loadApp(@NotNull final File jarFile) throws InvalidAppException {
        Validate.notNull(jarFile, "Jar file cannot be null");

        if (!jarFile.exists())
            throw new InvalidAppException(new FileNotFoundException(jarFile.getPath() + " does not exist"));

        final AppDescription description;
        try {
            description = getDescription(jarFile);
        } catch (InvalidDescriptionException e) {
            throw new InvalidAppException(e);
        }

        final File parentDir = jarFile.getParentFile();
        final File dataFolder = new File(parentDir, description.getAppName());

        if (dataFolder.exists() && !dataFolder.isDirectory())
            throw new InvalidAppException(String.format("'%s' for %s (%s) exists and is not a directory", dataFolder, description.getFullName(), jarFile));

        final AppClassLoader loader;
        try {
            loader = new AppClassLoader(this, getClass().getClassLoader(), description, dataFolder, jarFile);
        } catch (InvalidAppException e) {
            throw e;
        } catch (Throwable e) {
            throw new InvalidAppException(e);
        }

        loaders.add(loader);

        return loader.app;
    }

    @NotNull
    public AppDescription getDescription(@NotNull File file) throws InvalidDescriptionException {
        Validate.notNull(file, "File cannot be null");

        JarFile jarFile = null;
        InputStream is = null;

        try {
            jarFile = new JarFile(file);
            JarEntry entry = jarFile.getJarEntry("app.yml");

            if (entry == null)
                throw new InvalidDescriptionException(new FileNotFoundException("Jar does not contain app.yml"));

            is = jarFile.getInputStream(entry);

            return new AppDescription(is);
        } catch (IOException e) {
            throw new InvalidDescriptionException(e);
        } finally {
            if (jarFile != null)
                try {
                    jarFile.close();
                } catch (IOException ignored) {}

            if (is != null)
                try {
                    is.close();
                } catch (IOException ignored) {}
        }
    }

    @NotNull
    public Pattern[] getFileFilters() {
        return fileFilters.clone();
    }

    @Nullable
    Class<?> getClassByName(final String name) {
        Class<?> cachedClass = classes.get(name);

        if (cachedClass != null)
            return cachedClass;
        else {
            for (AppClassLoader loader : loaders) {
                try {
                    cachedClass = loader.findTheClass(name);
                } catch (ClassNotFoundException ignored) {}

                if (cachedClass != null)
                    return cachedClass;
            }
        }
        return null;
    }

    public void enableApp(@NotNull final App app) {
        if (!app.isEnabled()) {
            app.getLogger().info("Enabling " + app.getDescription().getFullName());

            AppClassLoader appClassLoader = (AppClassLoader) app.getClassLoader();

            if (!loaders.contains(appClassLoader)) {
                loaders.add(appClassLoader);
                Phone.getLogger().warning("Enabled app with unregistered AppClassLoader " + app.getDescription().getFullName());
            }

            try {
                app.setEnabled(true);
            } catch (Throwable e) {
                Phone.getLogger().log(Level.SEVERE, "An error occurred while enabling " + app.getDescription().getFullName(), e);
            }
        }
    }

    public void disableApp(@NotNull final App app) {
        if (app.isEnabled()) {
            app.getLogger().info("Disabling " + app.getDescription().getFullName());

            try {
                app.setEnabled(false);
            } catch (Throwable e) {
                Phone.getLogger().log(Level.SEVERE, "An error occurred while disabling " + app.getDescription().getFullName(), e);
            }
        }
    }
}
