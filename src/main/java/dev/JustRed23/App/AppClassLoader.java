package dev.JustRed23.App;

import com.google.common.io.ByteStreams;
import dev.JustRed23.Exceptions.InvalidAppException;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class AppClassLoader extends URLClassLoader {

    private final AppLoader loader;
    private final Map<String, Class<?>> classes = new ConcurrentHashMap<>();
    private final AppDescriptionFile description;
    private final File dataFolder;
    private final File file;
    private final JarFile jar;
    private final Manifest manifest;
    private final URL url;
    final PhoneApp phoneApp;
    private PhoneApp appInit;
    private IllegalStateException appState;

    static {
        ClassLoader.registerAsParallelCapable();
    }

    AppClassLoader(@NotNull final AppLoader loader, @Nullable final ClassLoader parent, @NotNull final AppDescriptionFile description, @NotNull final File dataFolder, @NotNull final File file) throws IOException, InvalidAppException {
        super(new URL[] {file.toURI().toURL()}, parent);
        Validate.notNull(loader, "Loader cannot be null");

        this.loader = loader;
        this.description = description;
        this.dataFolder = dataFolder;
        this.file = file;
        this.jar = new JarFile(file);
        this.manifest = jar.getManifest();
        this.url = file.toURI().toURL();

        try {
            Class<?> jarClass;
            try {
                jarClass = Class.forName(description.getMainClass(), true, this);
            } catch (ClassNotFoundException ex) {
                throw new InvalidAppException("Cannot find main class `" + description.getMainClass() + "'", ex);
            }

            Class<? extends PhoneApp> pluginClass;
            try {
                pluginClass = jarClass.asSubclass(PhoneApp.class);
            } catch (ClassCastException ex) {
                throw new InvalidAppException("main class `" + description.getMainClass() + "' does not extend JavaPlugin", ex);
            }

            phoneApp = pluginClass.getDeclaredConstructor().newInstance();
        } catch (IllegalAccessException ex) {
            throw new InvalidAppException("No public constructor", ex);
        } catch (InstantiationException ex) {
            throw new InvalidAppException("Abnormal plugin type", ex);
        } catch (NoSuchMethodException | InvocationTargetException ex) {
            throw new InvalidAppException(ex);
        }
    }

    @Override
    public URL getResource(String name) {
        return findResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return findResources(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return findTheClass(name);
    }

    Class<?> findTheClass(@NotNull String name) throws ClassNotFoundException {
        if (name.startsWith("dev.JustRed23."))
            throw new ClassNotFoundException(name);

        Class<?> result = classes.get(name);

        if (result == null) {
            String path = name.replace('.', '/').concat(".class");
            JarEntry entry = jar.getJarEntry(path);

            if (entry != null) {
                byte[] classBytes;

                try (InputStream is = jar.getInputStream(entry)) {
                    classBytes = ByteStreams.toByteArray(is);
                } catch (IOException ex) {
                    throw new ClassNotFoundException(name, ex);
                }

                int dot = name.lastIndexOf('.');
                if (dot != -1) {
                    String pkgName = name.substring(0, dot);
                    if (getPackage(pkgName) == null) {
                        try {
                            if (manifest != null)
                                definePackage(pkgName, manifest, url);
                            else
                                definePackage(pkgName, null, null, null, null, null, null, null);
                        } catch (IllegalArgumentException ex) {
                            if (getPackage(pkgName) == null)
                                throw new IllegalStateException("Cannot find package " + pkgName);
                        }
                    }
                }

                CodeSigner[] signers = entry.getCodeSigners();
                CodeSource source = new CodeSource(url, signers);

                result = defineClass(name, classBytes, 0, classBytes.length, source);
            }

            if (result == null)
                result = super.findClass(name);

            classes.put(name, result);
        }

        return result;
    }

    @NotNull
    Set<String> getClasses() {
        return classes.keySet();
    }

    synchronized void initialize(@NotNull PhoneApp phoneApp) {
        Validate.notNull(phoneApp, "Initializing app cannot be null");
        Validate.isTrue(phoneApp.getClass().getClassLoader() == this, "Cannot initialize plugin outside of this class loader");

        if (this.phoneApp != null || this.appInit != null)
            throw new IllegalArgumentException("Plugin already initialized!", appState);

        appState = new IllegalStateException("Initial initialization");
        this.appInit = phoneApp;

        phoneApp.init(loader, description, dataFolder, file, this);
    }
}
