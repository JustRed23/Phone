package dev.JustRed23.App;

import com.google.common.base.Preconditions;
import dev.JustRed23.Exceptions.InvalidAppException;
import dev.JustRed23.Exceptions.InvalidDescriptionException;
import dev.JustRed23.Phone.Phone;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Constructor;
import java.security.Permission;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SimpleAppManager {
    private final Map<Pattern, AppLoader> fileAssociations = new HashMap<>();
    private final List<PhoneApp> apps = new ArrayList<>();
    private final Map<String, PhoneApp> lookupNames = new HashMap<>();

    public SimpleAppManager() {}

    public void registerInterface(@NotNull Class<? extends AppLoader> loader) throws IllegalArgumentException {
        AppLoader instance;

        if (AppLoader.class.isAssignableFrom(loader)) {
            Constructor<? extends AppLoader> constructor;

            try {
                constructor = loader.getConstructor();
                instance = constructor.newInstance();
            } catch (Exception ex) {
                throw new IllegalArgumentException(String.format("Unexpected exception %s while attempting to construct a new instance of %s", ex.getClass().getName(), loader.getName()), ex);
            }
        } else {
            throw new IllegalArgumentException(String.format("Class %s does not implement interface AppLoader", loader.getName()));
        }

        Pattern[] patterns = instance.getPluginFileFilters();

        synchronized (this) {
            for (Pattern pattern : patterns) {
                fileAssociations.put(pattern, instance);
            }
        }
    }

    @NotNull
    public PhoneApp[] loadApps(@NotNull File directory) {
        Validate.notNull(directory, "Directory cannot be null");
        Validate.isTrue(directory.isDirectory(), "Directory must be a directory");

        List<PhoneApp> result = new ArrayList<>();
        Set<Pattern> filters = fileAssociations.keySet();

        Map<String, File> apps = new HashMap<>();
        Set<String> loadedApps = new HashSet<>();

        for (File file : directory.listFiles()) {
            AppLoader loader = null;
            for (Pattern filter : filters) {
                Matcher match = filter.matcher(file.getName());
                if (match.find()) {
                    loader = fileAssociations.get(filter);
                }
            }

            if (loader == null) continue;

            AppDescriptionFile description;
            try {
                description = loader.getPluginDescription(file);
            } catch (InvalidDescriptionException ex) {
                Phone.getLogger().log(Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'", ex);
                continue;
            }

            File replacedFile = apps.put(description.getAppName(), file);
            if (replacedFile != null) {
                Phone.getLogger().severe(String.format(
                        "Ambiguous app name `%s' for files `%s' and `%s' in `%s'",
                        description.getAppName(),
                        file.getPath(),
                        replacedFile.getPath(),
                        directory.getPath()
                ));
            }
        }

        while (!apps.isEmpty()) {
            Iterator<Map.Entry<String, File>> pluginIterator = apps.entrySet().iterator();

            while (pluginIterator.hasNext()) {
                Map.Entry<String, File> entry = pluginIterator.next();
                String phoneApp = entry.getKey();

                File file = apps.get(phoneApp);
                pluginIterator.remove();

                try {
                    PhoneApp loadedApp = loadApp(file);
                    if (loadedApp != null) {
                        result.add(loadedApp);
                        loadedApps.add(loadedApp.getDescription().getAppName());
                    } else {
                        Phone.getLogger().log(Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'");
                    }
                    continue;
                } catch (InvalidAppException ex) {
                    Phone.getLogger().log(Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'", ex);
                }
            }
        }

        return result.toArray(new PhoneApp[result.size()]);
    }

    @Nullable
    public synchronized PhoneApp loadApp(@NotNull File file) throws InvalidAppException {
        Validate.notNull(file, "File cannot be null");

        Set<Pattern> filters = fileAssociations.keySet();
        PhoneApp result = null;

        for (Pattern filter : filters) {
            String name = file.getName();
            Matcher match = filter.matcher(name);

            if (match.find()) {
                AppLoader loader = fileAssociations.get(filter);
                result = loader.loadApp(file);
            }
        }

        if (result != null) {
            apps.add(result);
            lookupNames.put(result.getDescription().getAppName(), result);
        }

        return result;
    }

    @Nullable
    public synchronized PhoneApp getApp(@NotNull String name) {
        return lookupNames.get(name.replace(' ', '_'));
    }

    @NotNull
    public synchronized PhoneApp[] getApps() {
        return apps.toArray(new PhoneApp[apps.size()]);
    }

    public boolean isAppEnabled(@NotNull String name) {
        PhoneApp app = getApp(name);
        return isAppEnabled(app);
    }

    public boolean isAppEnabled(@Nullable PhoneApp app) {
        if ((app != null) && (apps.contains(app))) {
            return app.isEnabled();
        } else {
            return false;
        }
    }

    public void enableApp(@NotNull final PhoneApp app) {
        if (!app.isEnabled()) {
            try {
                app.getAppLoader().enableApp(app);
            } catch (Throwable ex) {
                Phone.getLogger().log(Level.SEVERE, "Error occurred (in the app loader) while enabling " + app.getDescription().getFullName() + " (Is it up to date?)", ex);
            }
        }
    }

    public void disableApps() {
        PhoneApp[] apps = getApps();
        for (int i = apps.length - 1; i >= 0; i--) {
            disableApp(apps[i]);
        }
    }

    public void disableApp(@NotNull final PhoneApp app) {
        if (app.isEnabled()) {
            try {
                app.getAppLoader().disableApp(app);
            } catch (Throwable ex) {
                Phone.getLogger().log(Level.SEVERE, "Error occurred (in the app loader) while disabling " + app.getDescription().getFullName() + " (Is it up to date?)", ex);
            }
        }
    }
}
