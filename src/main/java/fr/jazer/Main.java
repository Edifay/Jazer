package fr.jazer;

import fr.jazer.ThreadManager.ThreadPool;
import fr.jazer.logger.Logger;

public class Main {

    private static Logger logger = Logger.loggerOfStatic(Logger.class);

    private static ThreadPool threadPool = new ThreadPool();

    private static int count;
    private static final Object key = new Object();

    public static void main(String[] args) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < 100; i++) {
            threadPool.exe(() -> {
                synchronized (key) {
                    count++;
                }
            });
            threadPool.exe(() -> {
                synchronized (key) {
                    count++;
                }
            });
            threadPool.exe(() -> {
                synchronized (key) {
                    count++;
                }
            });
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Count : " + count);
    }
}
