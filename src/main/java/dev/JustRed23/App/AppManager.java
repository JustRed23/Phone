package dev.JustRed23.App;

import dev.JustRed23.Exceptions.InvalidAppException;
import dev.JustRed23.Exceptions.InvalidDescriptionException;
import dev.JustRed23.Phone.Phone;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AppManager {

    private final Map<Pattern, AppLoader> fileAssociations = new HashMap<>();
    private final List<App> apps = new ArrayList<>();
    private final Map<String, App> lookupNames = new HashMap<>();

    public AppManager() {
        AppLoader instance = new AppLoader();

        Pattern[] patterns = instance.getFileFilters();

        synchronized (this) {
            for (Pattern pattern : patterns) {
                fileAssociations.put(pattern, instance);
            }
        }
    }

    @NotNull
    public App[] loadApps(@NotNull File directory) {
        Validate.notNull(directory, "Directory cannot be null");
        Validate.isTrue(directory.isDirectory(), "Directory must be a directory");

        List<App> result = new ArrayList<>();
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

            AppDescription description;
            try {
                description = loader.getDescription(file);
            } catch (InvalidDescriptionException e) {
                Phone.getLogger().log(Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'", e);
                continue;
            }

            File replacedFile = apps.put(description.getAppName(), file);
            if (replacedFile != null)
                Phone.getLogger().severe(String.format("Ambiguous app name `%s' for files `%s' and `%s' in `%s'", description.getAppName(), file.getPath(), replacedFile.getPath(), directory.getPath()));
        }

        while (!apps.isEmpty()) {
            Iterator<Map.Entry<String, File>> appIterator = apps.entrySet().iterator();

            while (appIterator.hasNext()) {
                Map.Entry<String, File> entry = appIterator.next();
                String app = entry.getKey();

                File file = apps.get(app);
                appIterator.remove();

                try {
                    App loadedApp = loadApp(file);
                    if (loadedApp != null) {
                        result.add(loadedApp);
                        loadedApps.add(loadedApp.getDescription().getAppName());
                    } else
                        Phone.getLogger().log(Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'");
                } catch (InvalidAppException e) {
                    Phone.getLogger().log(Level.SEVERE, "Could not load '" + file.getPath() + "' in folder '" + directory.getPath() + "'", e);
                }
            }
        }
        return result.toArray(new App[0]);
    }

    @Nullable
    public synchronized App loadApp(@NotNull File file) throws InvalidAppException {
        Validate.notNull(file, "File cannot be null");

        Set<Pattern> filters = fileAssociations.keySet();
        App result = null;

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
    public synchronized App getApp(@NotNull String name) {
        return lookupNames.get(name.replace(' ', '_'));
    }

    @NotNull
    public synchronized App[] getApps() {
        return apps.toArray(new App[0]);
    }

    public boolean isAppEnabled(@NotNull String name) {
        return isAppEnabled(getApp(name));
    }

    public boolean isAppEnabled(@Nullable App app) {
        if ((app != null) && (apps.contains(app))) {
            return app.isEnabled();
        } else {
            return false;
        }
    }

    public void enableApp(@NotNull final App app) {
        if (!app.isEnabled()) {
            try {
                app.getLoader().enableApp(app);
            } catch (Throwable e) {
                Phone.getLogger().log(Level.SEVERE, "An error occurred (in the app loader) while enabling " + app.getDescription().getFullName(), e);
            }
        }
    }

    public void disableApps() {
        for (App app : getApps())
            disableApp(app);
    }

    public void disableApp(@NotNull final App app) {
        if (app.isEnabled())
            try {
                app.getLoader().disableApp(app);
            } catch (Throwable e) {
                Phone.getLogger().log(Level.SEVERE, "An error occurred (in the app loader) while disabling " + app.getDescription().getFullName(), e);
            }
    }
}
