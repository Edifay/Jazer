package fr.jazer.logger;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    private static final SimpleDateFormat format = new SimpleDateFormat("[HH:mm:ss dd/MM]");

    protected String className;
    protected Object object;

    protected Logger(final String className, final Object object) {
        this.className = className;
        this.object = object;
    }

    public void log(final String message) {
        final long ms = System.currentTimeMillis();
        final Date date = new Date(ms);
        String logMessage;
        if (this.object != null)
            logMessage = String.format("%s %30s: %-60s       | %s", format.format(date), className, message, this.object.toString());
        else
            logMessage = String.format("%s %30s: %s", format.format(date), className, message);
        System.out.println(logMessage);
    }


    public static Logger loggerOfStatic(final Class<?> component) {
        return new Logger(component.getSimpleName(), null);
    }

    public static Logger loggerOfObject(final Object object) {
        return new Logger(object.getClass().getCanonicalName(), object);
    }

}
