import fr.jazer.session.RPacket;
import fr.jazer.session.SPacket;
import fr.jazer.session.Session;
import fr.jazer.session.SessionServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

public class Main2 {
    public static void main(String[] args) {
        SessionServer server = new SessionServer();
        server.openSession(8888);

        server.addSessionListener(session -> {
            String s = null;
            try {
                s = s = new Scanner(new File("./packet-4.txt")).nextLine();

                for (int i = 0; i < 100; i++) {
                    session.send(new SPacket(4).writeString(s));

                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });


        Session session = new Session();
        session.connect("192.168.1.37", 8888);


        for (int i = 0; i < 100; i++) {
            long ms = System.currentTimeMillis();
            RPacket packet = session.read(4);
            packet.readString();
            System.out.println("Took : " + (System.currentTimeMillis() - ms) + "ms");
        }

    }
}
