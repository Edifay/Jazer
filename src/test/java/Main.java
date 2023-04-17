public class Main {
    public static void main(String[] args) {

        Thread Thread_B = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    int i = 0;
                    while (i < 100)
                        i++;
                    //libérer le thread A
                    notify();
                }
            }
        });
        Thread_B.start();

        synchronized (Thread_B) {
            try {
                System.out.println("Thread A est bloqué - En attente de thread B" +
                                   "qu'il termine");
                //mettre en attente le thread A
                Thread_B.wait();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println("le thread B a terminé - Le thread A est relaché");
        }
    }
}
