package fr.jazer.session;

import fr.jazer.logger.Logger;
import fr.jazer.session.stream.Receiver;
import fr.jazer.session.stream.VirtualStream;
import fr.jazer.session.utils.SessionServerStatus;
import fr.jazer.session.utils.SessionType;
import fr.jazer.session.utils.crypted.SSLServerSocketKeystoreFactory;
import fr.jazer.session.utils.crypted.ServerCertConfig;
import fr.jazer.thread_manager.ThreadPool;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class SessionServer implements Receiver<SessionServerStatus> {

    protected ServerSocket server;


    protected SessionServerStatus status = SessionServerStatus.CLOSED;


    private long sessionTimeOut = 20000;
    protected long lastOpen;


    protected final ThreadPool executor = new ThreadPool();
    protected Thread clientListener;


    protected final VirtualStream<Session> sessionsFlux = new VirtualStream<>();
    protected final VirtualStream<SessionServerStatus> statusFlux = new VirtualStream<>();


    protected final Object lockerOpen = new Object();


    protected final Logger logger = Logger.loggerOfStatic(SessionServer.class);

    public SessionServer() {
        this.statusFlux.addReceiver(this);
    }

    public ServerSocket getServer() {
        return this.server;
    }

    public boolean isOpened() {
        synchronized (lockerOpen) {
            return this.status == SessionServerStatus.OPENED;
        }
    }

    public SessionServerStatus openSession(final int port) {
        return openSession(port, null);
    }

    public SessionServerStatus openSession(final int port, @Nullable final ServerCertConfig certConfig) {
        synchronized (lockerOpen) {
            if (this.isOpened() || this.status == SessionServerStatus.DESTROYED)
                return this.status;
            try {
                final ServerSocket server;
                if ((server = constructServer(port, certConfig)).isBound()) {
                    this.server = server;
                    this.setStatus(SessionServerStatus.OPENED);
                }
            } catch (IOException e) {
                logger.err("ServerSocket couldn't be opened : " + e.getMessage());
            } catch (CertificateException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException |
                     KeyManagementException e) {
                logger.err("Couldn't create secured ServerSocket : " + e.getMessage());
            }
            return this.status;
        }
    }


    protected ServerSocket constructServer(final int port, @Nullable final ServerCertConfig certConfig) throws IOException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, CertificateException {
        if (certConfig == null) return new ServerSocket(port);
        else
            return SSLServerSocketKeystoreFactory.getServerSocketWithCert(port, certConfig.input(), certConfig.password(), certConfig.type(), certConfig.certFormat());
    }

    protected void setStatus(final SessionServerStatus status) {
        synchronized (lockerOpen) {
            this.statusFlux.emitValue(status);
            this.status = status;
        }
    }

    public void destroy() {
        setStatus(SessionServerStatus.CLOSED);
        setStatus(SessionServerStatus.DESTROYED);
    }

    public void setSessionTimeOut(final long sessionTimeOut) {
        this.sessionTimeOut = sessionTimeOut;
    }


    public VirtualStream<SessionServerStatus> getStatusFlux() {
        return statusFlux;
    }

    public void addStatusListener(final Receiver<SessionServerStatus> sessionReceiver) {
        this.statusFlux.addReceiver(sessionReceiver);
    }

    public void removeStatusListener(final Receiver<SessionServerStatus> sessionReceiver) {
        this.statusFlux.removeReceiver(sessionReceiver);
    }

    public SessionServerStatus nextStatus() {
        return this.statusFlux.readASlash();
    }

    public void slashStatus(final SessionServerStatus status) {
        this.statusFlux.slash(status);
    }

    public VirtualStream<Session> getSessionsFlux() {
        return sessionsFlux;
    }


    public void addSessionListener(final Receiver<Session> sessionReceiver) {
        this.sessionsFlux.addReceiver(sessionReceiver);
    }

    public void removeSessionListener(final Receiver<Session> sessionReceiver) {
        this.sessionsFlux.removeReceiver(sessionReceiver);
    }

    public Session nextSession() {
        return this.sessionsFlux.readASlash();
    }

    public void slashSession(final Session session) {
        this.sessionsFlux.slash(session);
    }


    @Override
    public void onChanged(SessionServerStatus value) {
        synchronized (this.lockerOpen) {
            if (value == SessionServerStatus.CLOSED) {
                try {
                    this.server.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                this.clientListener.interrupt();
                lastOpen = System.currentTimeMillis();

                executor.exe(() -> {
                    final long lastOpenTemp = lastOpen;
                    try {
                        Thread.sleep(sessionTimeOut);
                    } catch (InterruptedException ignored) {
                    }
                    if (this.status != SessionServerStatus.DESTROYED && lastOpen == lastOpenTemp) {
                        logger.log("Server TIME OUT.");
                        setStatus(SessionServerStatus.DESTROYED);
                    }
                });

            } else if (value == SessionServerStatus.OPENED) {

                if (this.clientListener != null && this.clientListener.isAlive()) {
                    logger.err("COULDN'T CREATE A CLIENT LISTENER. A LISTENER ALREADY EXIST !");
                    return;
                }

                this.clientListener = new Thread(this::internalSessionsListenerLoop);
                this.clientListener.start();

            } else if (status == SessionServerStatus.DESTROYED) {
                logger.log("Server Destroyed.");
                if (!this.statusFlux.isClosed()) {
                    this.statusFlux.removeReceiver(this);
                    this.statusFlux.close(SessionServerStatus.DESTROYED);
                }
                if (!this.sessionsFlux.isClosed())
                    this.sessionsFlux.close(new Session());
                this.executor.destroy();
            }
        }
    }


    private void internalSessionsListenerLoop() {
        logger.log("SESSION-LISTENER STARTED !");

        Socket socket;
        try {
            while (this.isOpened()) {
                socket = this.server.accept();
                this.sessionsFlux.emitValue(new Session(socket, SessionType.SERVER_SIDE));
            }
            logger.log("Session Listener closed normally.");
        } catch (IOException e) {
            if (!this.isOpened())
                logger.log("Session Listener closed normally.");
            else {
                logger.err("Session Listener not normally closed ! " + e.getMessage());
                this.setStatus(SessionServerStatus.CLOSED);
            }
        }
    }

    @Override
    public String toString() {
        return "SessionServer{" +
                "server=" + server +
                ", status=" + status +
                ", sessionTimeOut=" + sessionTimeOut +
                ", lastOpen=" + lastOpen +
                ", executor=" + executor +
                ", clientListener=" + clientListener +
                ", sessionsFlux=" + sessionsFlux +
                ", statusFlux=" + statusFlux +
                ", lockerOpen=" + lockerOpen +
                ", logger=" + logger +
                '}';
    }
}
