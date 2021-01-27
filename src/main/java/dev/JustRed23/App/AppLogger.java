package dev.JustRed23.App;

import dev.JustRed23.Phone.Phone;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class AppLogger extends Logger {

    private String appName;

    public AppLogger(@NotNull App app) {
        super(app.getClass().getCanonicalName(), null);
        appName = "[" + app.getDescription().getAppName() + "] ";
        setParent(Phone.getLogger());
        setLevel(Level.ALL);
    }

    public void log(@NotNull LogRecord record) {
        record.setMessage(appName + record.getMessage());
        super.log(record);
    }
}
