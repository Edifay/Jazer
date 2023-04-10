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

    public synchronized void log(final String message) {
        final long ms = System.currentTimeMillis();
        final Date date = new Date(ms);
        String logMessage;
        if (this.object != null)
            logMessage = String.format("%s  [OUT]  [%-30s]  %-60s       | %s", format.format(date), StringUtils.center(className, 30), message, this.object.toString());
        else
            logMessage = String.format("%s  [OUT]  [%-30s]  %s", format.format(date), StringUtils.center(className, 30), message);
        System.out.println(logMessage);
    }

    public synchronized void err(final String message) {
        final long ms = System.currentTimeMillis();
        final Date date = new Date(ms);
        String logMessage;
        if (this.object != null)
            logMessage = String.format("%s  [ERR]  [%-30s]  %-60s       | %s", format.format(date), StringUtils.center(className, 30), message, this.object.toString());
        else
            logMessage = String.format("%s  [ERR]  [%-30s]  %s", format.format(date), StringUtils.center(className, 30), message);
        System.out.println(logMessage);
    }


    public static synchronized Logger loggerOfStatic(final Class<?> component) {
        return new Logger(component.getSimpleName(), null);
    }

    public static synchronized Logger loggerOfObject(final Object object) {
        return new Logger(object.getClass().getCanonicalName(), object);
    }

    class StringUtils {

        public static String center(String s, int size) {
            return center(s, size, ' ');
        }

        public static String center(String s, int size, char pad) {
            if (s == null || size <= s.length())
                return s;

            StringBuilder sb = new StringBuilder(size);
            for (int i = 0; i < (size - s.length()) / 2; i++) {
                sb.append(pad);
            }
            sb.append(s);
            while (sb.length() < size) {
                sb.append(pad);
            }
            return sb.toString();
        }
    }

}
