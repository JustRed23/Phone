package dev.JustRed23.App;

import dev.JustRed23.Exceptions.InvalidAppException;
import dev.JustRed23.Exceptions.InvalidDescriptionException;
import dev.JustRed23.Phone.Phone;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @NotNull
    public PhoneApp loadApp(@NotNull final File file) throws InvalidAppException {
        Validate.notNull(file, "File cannot be null");

        if (!file.exists()) {
            throw new InvalidAppException(new FileNotFoundException(file.getPath() + " does not exist"));
        }

        final AppDescriptionFile description;
        try {
            description = getPluginDescription(file);
        } catch (InvalidDescriptionException ex) {
            throw new InvalidAppException(ex);
        }

        final File parentFile = file.getParentFile();
        final File dataFolder = new File(parentFile, description.getAppName());

        if (dataFolder.exists() && !dataFolder.isDirectory()) {
            throw new InvalidAppException(String.format(
                    "Projected datafolder: `%s' for %s (%s) exists and is not a directory",
                    dataFolder,
                    description.getFullName(),
                    file
            ));
        }

        final AppClassLoader loader;
        try {
            loader = new AppClassLoader(this, getClass().getClassLoader(), description, dataFolder, file);
        } catch (InvalidAppException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new InvalidAppException(ex);
        }

        loaders.add(loader);

        return loader.phoneApp;
    }

    @NotNull
    public AppDescriptionFile getPluginDescription(@NotNull File file) throws InvalidDescriptionException {
        Validate.notNull(file, "File cannot be null");

        JarFile jar = null;
        InputStream stream = null;

        try {
            jar = new JarFile(file);
            JarEntry entry = jar.getJarEntry("app.yml");

            if (entry == null)
                throw new InvalidDescriptionException(new FileNotFoundException("Jar does not contain app.yml"));

            stream = jar.getInputStream(entry);

            return new AppDescriptionFile(stream);

        } catch (IOException ex) {
            throw new InvalidDescriptionException(ex);
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException e) {
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @NotNull
    public Pattern[] getPluginFileFilters() {
        return fileFilters.clone();
    }

    @Nullable
    Class<?> getClassByName(final String name) {
        Class<?> cachedClass = classes.get(name);

        if (cachedClass != null) {
            return cachedClass;
        } else {
            for (AppClassLoader loader : loaders) {
                try {
                    cachedClass = loader.findTheClass(name);
                } catch (ClassNotFoundException cnfe) {}
                if (cachedClass != null)
                    return cachedClass;
            }
        }
        return null;
    }

    public void enableApp(@NotNull final PhoneApp phoneApp) {
        if (!phoneApp.isEnabled()) {
            phoneApp.getLogger().info("Enabling " + phoneApp.getDescription().getFullName());

            PhoneApp phoneApp1 = phoneApp;

            AppClassLoader appClassLoader = (AppClassLoader) phoneApp1.getClassLoader();

            if (!loaders.contains(appClassLoader)) {
                loaders.add(appClassLoader);
                Phone.getLogger().log(Level.WARNING, "Enabled app with unregistered AppClassLoader " + phoneApp.getDescription().getFullName());
            }

            try {
                phoneApp1.setEnabled(true);
            } catch (Throwable ex) {
                Phone.getLogger().log(Level.SEVERE, "Error occurred while enabling " + phoneApp.getDescription().getFullName() + " (Is it up to date?)", ex);
            }
        }
    }

    public void disableApp(@NotNull PhoneApp phoneApp) {
        if (phoneApp.isEnabled()) {
            String message = String.format("Disabling %s", phoneApp.getDescription().getFullName());
            phoneApp.getLogger().info(message);

            PhoneApp phoneApp1 = phoneApp;

            try {
                phoneApp1.setEnabled(false);
            } catch (Throwable ex) {
                Phone.getLogger().log(Level.SEVERE, "Error occurred while disabling " + phoneApp.getDescription().getFullName() + " (Is it up to date?)", ex);
            }
        }
    }
}
