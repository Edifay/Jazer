package fr.jazer.logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class is the Logger module of Jazer. By default, she print only errors.
 */
public class Logger {

    /**
     * Disable [OUT] channel {@link Logger#log(String)} from all classes name contained in this list.
     */
    public static final ArrayList<String> disabledOutPut = new ArrayList<>(List.of("ThreadPool", "SessionServer", "fr.jazer.session.Session"));

    /**
     * The format used to date all outputs.
     */
    private static final SimpleDateFormat format = new SimpleDateFormat("[HH:mm:ss dd/MM]");

    /**
     * The name of the class using this Logger.
     */
    protected String className;
    /**
     * If Logger is created with special Object instance.
     */
    protected Object object;

    protected Logger(final String className, final Object object) {
        this.className = className;
        this.object = object;
    }

    /**
     * Used to print a standard message log.
     *
     * @param message the message to print.
     */
    public synchronized void log(final String message) {
        if (disabledOutPut.contains(this.className))
            return;
        final long ms = System.currentTimeMillis();
        final Date date = new Date(ms);
        String logMessage;
        if (this.object != null)
            logMessage = String.format("%s  [OUT]  [%-30s]  %-60s       | %s", format.format(date), StringUtils.center(className, 30), message, this.object.toString());
        else
            logMessage = String.format("%s  [OUT]  [%-30s]  %s", format.format(date), StringUtils.center(className, 30), message);
        System.out.println(logMessage);
    }

    /**
     * Used to print an error.
     *
     * @param message the message to print.
     */
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


    /**
     * Generate a Logger for a Class.
     *
     * @param component the class who need a Logger.
     * @return a Logger.
     */
    public static synchronized Logger loggerOfStatic(final Class<?> component) {
        return new Logger(component.getSimpleName(), null);
    }

    /**
     * Generate a Logger for an Object.
     *
     * @param object the current Object who need a logger.
     * @return a Logger.
     */
    public static synchronized Logger loggerOfObject(final Object object) {
        return new Logger(object.getClass().getCanonicalName(), object);
    }

    /**
     * UTILS.
     */
    static class StringUtils {

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
