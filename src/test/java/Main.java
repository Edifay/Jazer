import fr.jazer.ThreadManager.ThreadPool;
import fr.jazer.logger.Logger;

public class Main {

    private static Logger logger = Logger.loggerOfStatic(Logger.class);

    private static ThreadPool threadPool = new ThreadPool();


    public static void main(String[] args) {
        for (int i = 0; i < 3; i++) {
            threadPool.exe(() -> {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            threadPool.exe(() -> {});
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            threadPool.exe(() -> {});
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
