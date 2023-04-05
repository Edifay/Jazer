package fr.jazer.session;

import fr.jazer.ThreadManager.ThreadPool;

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

    protected final ThreadPool executor = new ThreadPool();
    

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

    public ConnexionStatus connect(final String address, final int port, final CertConfig certConfig) {
        synchronized (this.lockerConnect) {
            if (isServerSide(this.sessionType) || this.isConnected())
                return this.status;
            try {
                final Socket newSocket;
                if ((newSocket = constructSocket(address, port, certConfig)).isConnected() && integrityCheck(newSocket)) {
                    this.socket = newSocket;
                    setStatus(ConnexionStatus.CONNECTED);
                }
            } catch (IOException e) {
                System.err.println("Handled output : " + e.getMessage());
            }
            return this.status;
        }
    }

    private ConnexionStatus connect(final String address, final int port) {
        return connect(address, port, null);
    }

    public boolean isConnected() {
        return ConnexionStatus.isConnected(this.status);
    }

    public boolean integrityCheck(final Socket socket) {
        // TODO make the handshake exchange a client key.
        return true;
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

    private Socket constructSocket(final String address, final int port, final CertConfig certConfig) throws IOException {
        if (certConfig == null)
            return new Socket(address, port);
        else
            throw new IOException("NOT IMPLEMENTED METHOD !");
    }



}
