package fr.jazer.session;

import fr.jazer.session.utils.crypted.ClientCertConfig;
import fr.jazer.session.utils.crypted.SSLSocketKeystoreFactory;
import fr.jazer.session.utils.ConnexionStatus;
import fr.jazer.session.utils.SessionType;
import fr.jazer.thread_manager.ThreadPool;
import fr.jazer.logger.Logger;
import fr.jazer.session.stream.PacketVirtualStream;
import fr.jazer.session.stream.Receiver;
import fr.jazer.session.stream.VirtualStream;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static fr.jazer.session.utils.SessionType.isServerSide;

public class Session implements Receiver<ConnexionStatus> {

    protected Socket socket;


    protected final SessionType sessionType;
    protected ConnexionStatus status = ConnexionStatus.DISCONNECTED;


    private long sessionTimeOut = 20000;
    private long lastConnected;


    protected final ThreadPool executor = new ThreadPool();
    protected Thread reader;


    protected final VirtualStream<ConnexionStatus> statusFlux = new VirtualStream<>();
    protected final PacketVirtualStream<RPacket> packetFlux = new PacketVirtualStream<>();


    protected final Object lockerSend = new Object();
    protected final Object lockerConnect = new Object();

    protected Logger logger = Logger.loggerOfObject(this);

    public Session() {
        this.sessionType = SessionType.CLIENT_SIDE;
        this.statusFlux.addReceiver(this);
    }

    public Session(final Socket socket, final SessionType sessionType) {
        this.socket = socket;
        this.sessionType = sessionType;
        this.statusFlux.addReceiver(this);
        if (this.socket != null && this.socket.isConnected())
            setStatus(ConnexionStatus.CONNECTED);
    }

    public Socket getSocket() {
        return this.socket;
    }

    public boolean isConnected() {
        return ConnexionStatus.isConnected(this.status);
    }

    public ConnexionStatus connect(final String address, final int port) {
        return connect(address, port, null);
    }

    public ConnexionStatus connect(final String address, final int port, @Nullable final ClientCertConfig clientCertConfig) {
        synchronized (this.lockerConnect) {
            if (isServerSide(this.sessionType) || this.isConnected())
                return this.status;
            try {
                final Socket newSocket;
                if ((newSocket = constructSocket(address, port, clientCertConfig)).isConnected() && integrityCheck(newSocket)) {
                    this.socket = newSocket;
                    setStatus(ConnexionStatus.CONNECTED);
                }
            } catch (IOException e) {
                logger.err("Handled output : " + e.getMessage());
            } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                logger.err("Couldn't create secured socket : " + e.getMessage());
            }
            return this.status;
        }
    }

    public ConnexionStatus reconnect(final boolean secured) {
        if (secured)
            new IllegalStateException("Session.reconnect() method cannot be used on SECURED Session.").printStackTrace();
        if (!secured && socket != null)
            return connect(socket.getInetAddress().getHostAddress(), socket.getPort());
        return this.status;
    }

    protected Socket constructSocket(final String address, final int port, @Nullable final ClientCertConfig clientCertConfig) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        if (clientCertConfig == null)
            return new Socket(address, port);
        else
            return SSLSocketKeystoreFactory.getSocketWithCert(address, port, clientCertConfig.input(), clientCertConfig.password(), clientCertConfig.secureType(), clientCertConfig.format());
    }

    protected boolean integrityCheck(final Socket socket) {
        // TODO make the handshake exchange a client key.
        return true;
    }

    protected boolean setStatus(final ConnexionStatus status) {
        synchronized (this.lockerConnect) {
            if (this.status == status)
                return false;
            this.statusFlux.emitValue(status);
            this.status = status;
            return true;
        }
    }

    public void destroy() {
        setStatus(ConnexionStatus.DISCONNECTED);
        setStatus(ConnexionStatus.DESTROYED);
        try {
            this.socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSessionTimeOut(long sessionTimeOut) {
        this.sessionTimeOut = sessionTimeOut;
    }

    public VirtualStream<ConnexionStatus> getStatusFlux() {
        return this.statusFlux;
    }

    public void addStatusListener(final Receiver<ConnexionStatus> statusReceiver) {
        this.statusFlux.addReceiver(statusReceiver);
    }

    public void removeStatusListener(final Receiver<ConnexionStatus> statusReceiver) {
        this.statusFlux.removeReceiver(statusReceiver);
    }

    public ConnexionStatus nextStatus() {
        return this.statusFlux.readASlash();
    }

    public void slashStatus(final ConnexionStatus status) {
        this.statusFlux.slash(status);
    }

    public boolean send(final SPacket packet) {
        synchronized (this.lockerSend) {
            try {
                final byte[] buf = ByteBuffer.allocate(8 + packet.data.length)
                        .putInt(packet.data.length)
                        .putInt(packet.packetNumber)
                        .put(packet.data)
                        .array();
                // TODO may be use a BufferedOutputStream to speed up the transfer !
                socket.getOutputStream().write(buf);
                socket.getOutputStream().flush();
                return true;
            } catch (IOException e) {
                logger.err("COULD'T SEND RETURN FALSE.");
                return false;
            }
        }
    }


    public PacketVirtualStream<RPacket> getPacketFlux() {
        return this.packetFlux;
    }

    public void addPacketListener(final int tag, final Receiver<RPacket> receiver) {
        this.packetFlux.addReceiver(tag, receiver);
    }

    public void removePacketListener(final Receiver<RPacket> receiver) {
        this.packetFlux.removeReceiver(receiver);
    }

    public RPacket read(final int tag) {
        return this.packetFlux.readASlash(tag);
    }


    public void slashPacket(final RPacket packet) {
        this.packetFlux.slash(packet);
    }

    @Override
    public void onChanged(ConnexionStatus value) {
        synchronized (this.lockerConnect) {
            if (value == ConnexionStatus.DISCONNECTED) {

                this.reader.interrupt();
                lastConnected = System.currentTimeMillis();

                executor.exe(() -> {
                    final long lastConnectedTemp = lastConnected;
                    try {
                        Thread.sleep(sessionTimeOut);
                    } catch (InterruptedException ignored) {
                    }
                    if (this.status != ConnexionStatus.DESTROYED && lastConnected == lastConnectedTemp) {
                        logger.log("Session TIME OUT.");
                        setStatus(ConnexionStatus.DESTROYED);
                    }
                });

            } else if (value == ConnexionStatus.CONNECTED) {

                if (this.reader != null && this.reader.isAlive()) {
                    logger.err("COULDN'T CREATE A READER. A READER ALREADY EXIST !");
                    return;
                }

                this.reader = new Thread(this::internalReadLoop);
                this.reader.start();

            } else if (status == ConnexionStatus.DESTROYED) {

                logger.log("Session Destroyed.");
                if (!this.statusFlux.isClosed()) {
                    this.statusFlux.removeReceiver(this);
                    this.statusFlux.close(ConnexionStatus.DESTROYED);
                }
                if (!this.packetFlux.isClosed())
                    this.packetFlux.close(new RPacket(0, new byte[0]));

                this.executor.destroy();

            }
        }
    }

    private void internalReadLoop() {
        try {
            logger.log("READER STARTED !");

            InputStream in = new BufferedInputStream(socket.getInputStream());
            byte[] intBuffer = new byte[4];
            int packetSize;
            int packetNumber;

            while (this.isConnected() && in.read(intBuffer) != -1) {
                packetSize = ByteBuffer.wrap(intBuffer).getInt();
                in.read(intBuffer);
                packetNumber = ByteBuffer.wrap(intBuffer).getInt();
                byte[] datas = new byte[packetSize];
                in.read(datas);
                this.packetFlux.emitValue(new RPacket(packetNumber, datas));
            }

            logger.log("Reader closing, other part closed the Session.");
            setStatus(ConnexionStatus.DISCONNECTED);

        } catch (Exception e) {
            if (!this.isConnected())
                logger.log("Reader stopped by closing the socket. " + e.getMessage());
            else
                e.printStackTrace();
        }
    }

}
