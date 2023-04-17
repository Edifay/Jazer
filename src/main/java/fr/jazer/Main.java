package fr.jazer;

import fr.jazer.session.utils.ConnexionStatus;
import fr.jazer.session.utils.SessionType;
import fr.jazer.thread_manager.ThreadPool;
import fr.jazer.logger.Logger;
import fr.jazer.session.*;
import fr.jazer.session.stream.Receiver;

import java.io.IOException;
import java.net.ServerSocket;

public class Main implements Receiver<ConnexionStatus> {

    private static Logger logger = Logger.loggerOfStatic(Main.class);

    public static ThreadPool executor = new ThreadPool(2000);


    public static void main(String[] args) throws IOException {
        SessionServer server = new SessionServer();
        server.openSession(5555);

        executor.exe(() -> {
            try {
                Session client = server.nextSession();
                client.send(new SPacket(5).writeString("Hello World !"));
                RPacket packet = client.read(5);
                logger.log(packet.readString());
                while (client.isConnected()) {
                    RPacket pack = client.read(5);
                    logger.log(pack.readString());
                    logger.log("------------------");
                }
                server.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Session client = new Session();
        client.addStatusListener(new Main());
        client.connect("localhost", 5555);
        RPacket packet = client.read(5);
        logger.log(packet.readString());
        client.send(new SPacket(5).writeString("5/5"));

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
       /* for (int i = 0; i < 1000; i++) {
            client.send(new SPacket(5).writeString(i + " DKFJQ SFLJS K"));
        }*/
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        client.destroy();
    }

    @Override
    public void onChanged(ConnexionStatus value) {
        logger.log("Status changed to : " + value.name());
    }
}
