package fr.jazer.session;

import java.io.IOException;
import java.net.Socket;

import static fr.jazer.session.SessionType.isServerSide;

public class Session {

    protected Socket socket;
    protected final SessionType sessionType;
    protected ConnexionStatus status = ConnexionStatus.DISCONNECTED;

    protected final Object lockerSend = new Object();
    protected final Object lockerRead = new Object();
    protected final Object lockerConnect = new Object();

    public Session() {
        this.sessionType = SessionType.CLIENT_SIDE;
    }

    public Session(final Socket socket, final SessionType sessionType) {
        this.socket = socket;
        this.sessionType = sessionType;
        if (this.socket != null && this.socket.isConnected())
            setStatus(ConnexionStatus.CONNECTED);
    }

    public ConnexionStatus reconnect() {
        if (socket != null)
            return connect(socket.getInetAddress().getHostAddress(), socket.getPort());
        return this.status;
    }

    public ConnexionStatus connect(final String address, final int port) {
        synchronized (this.lockerConnect) {
            if (isServerSide(this.sessionType) || this.isConnected())
                return this.status;
            try {
                final Socket newSocket;
                if ((newSocket = new Socket(address, port)).isConnected()) {
                    this.socket = newSocket;
                    setStatus(ConnexionStatus.CONNECTED);
                }
            } catch (IOException e) {
                System.err.println("Handled output : " + e.getMessage());
            }
            return this.status;
        }
    }

    public boolean isConnected() {
        return ConnexionStatus.isConnected(this.status);
    }

    public void integrityCheck() {

    }

    public void handShake() {

    }

    public boolean setStatus(final ConnexionStatus status) {
        if (this.status == status)
            return false;
        this.status = status;
        return true;
    }

    public Socket getSocket() {
        return this.socket;
    }

}
