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
                s = new Scanner(new File("./packet-4.txt")).nextLine();
                //System.out.println(s);
                session.send(new SPacket(4).writeString(s));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });


        Session session = new Session();
        session.connect("localhost", 8888);

        try {
            FileOutputStream output = new FileOutputStream("./receive-4.txt");
            RPacket packet = session.read(4);
            packet.readString();
            output.write(packet.getData());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
