package dev.JustRed23.App;

import com.google.common.collect.ImmutableList;
import dev.JustRed23.Exceptions.InvalidDescriptionException;
import dev.JustRed23.Phone.Phone;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AppDescription {

    private final Pattern VALID_NAME = Pattern.compile("^[A-Za-z0-9 _.-]+$");

    private String appName = null;
    private String appVersion = null;

    private String appAuthor = null;
    private List<String> appContributors = null;

    private String mainClass = null;

    private static final Yaml YAML = new Yaml();

    public AppDescription(InputStream stream) throws InvalidDescriptionException {
        loadMap(asMap(YAML.load(stream)));
    }

    private void loadMap(Map<?, ?> map) throws InvalidDescriptionException {
        String name = getObjectFromMap(map, "name");
        if (!VALID_NAME.matcher(name).matches())
            throw new InvalidDescriptionException("Name '" + name + "' contains invalid characters");
        appName = name;

        appVersion = getObjectFromMap(map, "version");
        appAuthor = getObjectFromMap(map, "author");

        if (map.get("contributors") != null) {
            ImmutableList.Builder<String> contributorsBuilder = ImmutableList.builder();
            try {
                for (Object o : (Iterable<?>) map.get("contributors"))
                    contributorsBuilder.add(o.toString());
            } catch (ClassCastException ex) {
                throw new InvalidDescriptionException(ex, "Contributors are of wrong type");
            }
            appContributors = contributorsBuilder.build();
        } else {
            appContributors = ImmutableList.of();
        }

        String main = getObjectFromMap(map, "mainClass");
        if (main.startsWith(Phone.protectedPackage))
            throw new InvalidDescriptionException("The main class may not be within the dev.JustRed23 namespace");
        mainClass = main;
    }

    @NotNull
    private Map<?, ?> asMap(@NotNull Object object) throws InvalidDescriptionException {
        if (object instanceof Map)
            return (Map<?, ?>) object;

        throw new InvalidDescriptionException(object + " is not properly structured.");
    }

    private String getObjectFromMap(Map<?, ?> map, String objectName) throws InvalidDescriptionException {
        try {
            return map.get(objectName).toString();
        } catch (NullPointerException ex) {
            throw new InvalidDescriptionException(ex, objectName + " is not defined");
        } catch (ClassCastException ex) {
            throw new InvalidDescriptionException(ex, objectName + " is of wrong type");
        }
    }

    public String getAppName() {
        return appName;
    }

    public String getFullName() {
        return appName + " v" + appVersion;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getAppAuthor() {
        return appAuthor;
    }

    public List<String> getAppContributors() {
        return appContributors;
    }

    public String getMainClass() {
        return mainClass;
    }
}
