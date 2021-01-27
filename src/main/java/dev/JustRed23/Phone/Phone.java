package dev.JustRed23.Phone;

import dev.JustRed23.App.App;
import dev.JustRed23.App.AppManager;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Phone {

    private static final Logger mainLogger = Logger.getLogger(Phone.class.getCanonicalName());
    public static String protectedPackage = "dev.JustRed23.";

    private static AppManager manager = new AppManager();

    public static void main(String[] args) {
        mainLogger.info("Starting up...");
        loadApps();
        mainLogger.info("Loaded " + manager.getApps().length + " app(s) successfully");
    }

    private static void loadApps() {
        File appFolder = new File("D:\\Apps\\");

        if (appFolder.exists()) {
            for (App app : manager.loadApps(appFolder)) {
                try {
                    app.getLogger().info("Loading " + app.getDescription().getFullName());
                    app.onLoad();
                } catch (Throwable e) {
                    mainLogger.log(Level.SEVERE, "An error occurred while initializing " + app.getDescription().getFullName(), e);
                }
            }
        } else {
            mainLogger.warning("Directory " + appFolder + " does not exist. Creating...");
        }
    }

    public static Logger getLogger() {
        return mainLogger;
    }
}
