package dev.JustRed23.App;

import com.google.common.io.ByteStreams;
import dev.JustRed23.Exceptions.InvalidAppException;
import dev.JustRed23.Phone.Phone;
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
    private final AppDescription description;
    private final File dataFolder;

    private final File jarFile;
    private final JarFile jar;
    private final Manifest manifest;
    private final URL url;

    final App app;
    private boolean isInitialized;

    AppClassLoader(@NotNull final AppLoader loader, @Nullable final ClassLoader parent, @Nullable final AppDescription description, @NotNull final File dataFolder, @NotNull final File jarFile) throws IOException, InvalidAppException {
        super(new URL[] {jarFile.toURI().toURL()}, parent);

        this.loader = loader;
        this.description = description;
        this.dataFolder = dataFolder;

        this.jarFile = jarFile;
        this.jar = new JarFile(jarFile);
        this.manifest = jar.getManifest();
        this.url = jarFile.toURI().toURL();

        this.app = performChecks();
    }

    private App performChecks() throws InvalidAppException {
        try {
            Class<?> jarClass;
            try {
                jarClass = Class.forName(description.getMainClass(), true, this);
            } catch (ClassNotFoundException e) {
                throw new InvalidAppException(String.format("Cannot find main class %s", description.getMainClass()), e);
            }

            Class<? extends App> appClass;
            try {
                appClass = jarClass.asSubclass(App.class);
            } catch (ClassCastException e) {
                throw new InvalidAppException(String.format("Main class %s does not extend %s", description.getMainClass(), App.class.getCanonicalName()), e);
            }

            return appClass.getDeclaredConstructor().newInstance();
        } catch (IllegalAccessException e) {
            throw new InvalidAppException("Constructor is not public", e);
        } catch (InstantiationException e) {
            throw new InvalidAppException("Abnormal app type", e);
        } catch (NoSuchMethodException | InvocationTargetException e) {
            throw new InvalidAppException(e);
        }
    }

    public URL getResource(String name) {
        return findResource(name);
    }

    public Enumeration<URL> getResources(String name) throws IOException {
        return findResources(name);
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return findTheClass(name);
    }

    Class<?> findTheClass(@NotNull String name) throws ClassNotFoundException {
        if (name.startsWith(Phone.protectedPackage))
            throw new ClassNotFoundException(name);

        Class<?> result = classes.get(name);

        if (result == null) {
            String path = name.replace('.', '/').concat(".class");
            JarEntry entry = jar.getJarEntry(path);

            if (entry != null) {
                byte[] classBytes;

                try (InputStream is = jar.getInputStream(entry)) {
                    classBytes = ByteStreams.toByteArray(is);
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }

                int dot = name.lastIndexOf('.');
                if (dot != -1) {
                    String packageName = name.substring(0, dot);
                    if (getPackage(packageName) == null) {
                        try {
                            if (manifest != null)
                                definePackage(packageName, manifest, url);
                            else
                                definePackage(packageName, null, null);
                        } catch (IllegalArgumentException e) {
                            if (getPackage(packageName) == null)
                                throw new IllegalStateException(String.format("Cannot find package %s", packageName));
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

    synchronized void init(@NotNull App app) {
        Validate.notNull(app, "App cannot be null");
        Validate.isTrue(app.getClass().getClassLoader() == this, "Cannot init app outside loader");

        if (this.app != null || this.isInitialized)
            throw new IllegalArgumentException("App already initialized!");

        app.init(loader, description, dataFolder, jarFile, this);
        this.isInitialized = true;
    }
}
