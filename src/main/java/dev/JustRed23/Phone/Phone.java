package dev.JustRed23.Phone;

import dev.JustRed23.App.App;
import dev.JustRed23.App.AppManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Phone {

    private static final Logger mainLogger = LoggerFactory.getLogger(Phone.class.getCanonicalName());
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
                    mainLogger.info("Loading " + app.getDescription().getFullName());
                    app.onLoad();
                } catch (Throwable e) {
                    mainLogger.error("An error occurred while initializing " + app.getDescription().getFullName(), e);
                }
            }
        } else {
            mainLogger.warn("Directory " + appFolder + " does not exist. Creating...");
        }
    }

    public static Logger getLogger() {
        return mainLogger;
    }
}
