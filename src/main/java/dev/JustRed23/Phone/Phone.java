package dev.JustRed23.Phone;

import dev.JustRed23.App.AppLoader;
import dev.JustRed23.App.PhoneApp;
import dev.JustRed23.App.SimpleAppManager;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Phone {

    private static final Logger mainLogger = Logger.getLogger(Phone.class.getCanonicalName());
    private static SimpleAppManager simpleAppManager = new SimpleAppManager();

    public static void main(String[] args) {
        mainLogger.info("Starting up...");
        loadApps();
    }

    private static void loadApps() {
        simpleAppManager.registerInterface(AppLoader.class);

        File appFolder = new File("D:\\Apps\\");

        if (appFolder.exists()) {
            PhoneApp[] apps = simpleAppManager.loadApps(appFolder);
            for (PhoneApp app : apps) {
                try {
                    String message = String.format("Loading %s", app.getDescription().getFullName());
                    app.getLogger().info(message);
                    app.onLoad();
                } catch (Throwable ex) {
                    mainLogger.log(Level.SEVERE, ex.getMessage() + " initializing " + app.getDescription().getFullName() + " (Is it up to date?)", ex);
                }
            }
        } else {
            appFolder.mkdir();
        }
    }

    public static Logger getLogger() {
        return mainLogger;
    }
}
