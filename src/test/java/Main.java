import fr.jazer.session.utils.ConnectionStatus;
import fr.jazer.thread_manager.ThreadPool;
import fr.jazer.logger.Logger;
import fr.jazer.session.*;
import fr.jazer.session.stream.Receiver;

import java.io.IOException;

public class Main implements Receiver<ConnectionStatus> {

    private static Logger logger = Logger.loggerOfStatic(Main.class);

    public static ThreadPool executor = new ThreadPool(2000);


    public static void main(String[] args) throws IOException {
        SessionServer server = new SessionServer();
        server.openSession(5555);

        server.addSessionListener(client -> {
            try {
                if (!client.isConnected())
                    return;
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
        for (int i = 0; i < 1000; i++) {
            client.send(new SPacket(5).writeString(i + " DKFJQ SFLJS K"));
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        client.destroy();

    }

    @Override
    public void onChanged(ConnectionStatus value) {
        logger.log("Status changed to : " + value.name());
    }
}
