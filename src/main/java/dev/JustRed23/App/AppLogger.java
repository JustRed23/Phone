package dev.JustRed23.App;

import dev.JustRed23.Phone.Phone;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class AppLogger extends Logger {
    private String appName;

    public AppLogger(@NotNull PhoneApp context) {
        super(context.getClass().getCanonicalName(), null);
        appName = "[" + context.getDescription().getAppName() + "] ";
        setParent(Phone.getLogger());
        setLevel(Level.ALL);
    }

    @Override
    public void log(@NotNull LogRecord logRecord) {
        logRecord.setMessage(appName + logRecord.getMessage());
        super.log(logRecord);
    }
}
