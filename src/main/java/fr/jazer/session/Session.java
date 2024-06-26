package fr.jazer.session;

import fr.jazer.session.utils.crypted.ClientCertConfig;
import fr.jazer.session.utils.crypted.SSLSocketKeystoreFactory;
import fr.jazer.session.utils.ConnectionStatus;
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
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static fr.jazer.session.utils.SessionType.isServerSide;

public class Session implements Receiver<ConnectionStatus> {

    /**
     * The main component of a Session, the embedded Socket.
     */
    protected Socket socket;

    /**
     * SessionType is used to differentiate a Session created by a {@link SessionServer} with a client Session.
     * <p>
     * {@link SessionType#CLIENT_SIDE} or {@link SessionType#SERVER_SIDE}
     */
    protected final SessionType sessionType;
    /**
     * Save the current status of a Session.
     * <p>
     * {@link ConnectionStatus#CONNECTED} the session is currently connected, you be able to receive and send Packets.
     * <p>
     * {@link ConnectionStatus#DISCONNECTED} the session is currently disconnected, you won't be able to receive and send
     * Packets (ou can call {@link Session#reconnect(boolean)} or {@link Session#connect(String, int)} to reconnect the session).
     * But virtual stream are always opened.
     * <p>
     * {@link ConnectionStatus#DESTROYED} the session is destroyed. She will not be able to reconnect. Virtual Stream are destroyed.
     */
    protected ConnectionStatus status = ConnectionStatus.DISCONNECTED;

    /**
     * The time who take a session to auto destroy after be disconnected.
     */
    private long sessionTimeOut = 20000;
    /**
     * Internal temp value.
     * Save the last epoch when was connected.
     */
    private long lastConnected;

    /**
     * The ThreadPool used by this Session. Used to emit event on Receivers.
     */
    protected final ThreadPool executor = new ThreadPool();
    /**
     * The Thread reading continually on Socket for new Packets. He is managed in {@link Session#onChanged(ConnectionStatus)}.
     * His implementation could be find in {@link Session#internalReadLoop()};
     */
    protected Thread reader;

    /**
     * A virtual stream to listen status changes.
     */
    protected final VirtualStream<ConnectionStatus> statusFlux = new VirtualStream<>();
    /**
     * A virtual stream to listen on packet receive.
     */
    protected final PacketVirtualStream<RPacket> packetFlux = new PacketVirtualStream<>();

    /**
     * Locker used in {@link Session#send(SPacket)}
     */
    protected final Object lockerSend = new Object();
    /**
     * Locker used in {@link Session#connect(String, int, ClientCertConfig)}, {@link Session#setStatus(ConnectionStatus)}, {@link Session#onChanged(ConnectionStatus)}.
     */
    protected final Object lockerConnect = new Object();

    /**
     * Log message from Sessions.
     */
    protected Logger logger = Logger.loggerOfObject(this);

    /**
     * Default constructor for clients.
     */
    public Session() {
        this.sessionType = SessionType.CLIENT_SIDE;
        this.statusFlux.addReceiver(this);
    }

    /**
     * Mainly used by {@link SessionServer} to create {@link SessionType#SERVER_SIDE} Session.
     * <p>
     * Constructor can be also used by client with socket already connect using {@link SessionType#CLIENT_SIDE}.
     *
     * @param socket      the socket to embed.
     * @param sessionType the type of the session.
     */
    public Session(final Socket socket, final SessionType sessionType) {
        this.socket = socket;
        this.sessionType = sessionType;
        this.statusFlux.addReceiver(this);
        if (this.socket != null && this.socket.isConnected())
            setStatus(ConnectionStatus.CONNECTED);
    }

    /**
     * @return the embedded socket by the Session.
     */
    public Socket getSocket() {
        return this.socket;
    }

    /**
     * @return true if the current {@link Session#status} is equal to {@link ConnectionStatus#CONNECTED}, false if not.
     */
    public boolean isConnected() {
        return ConnectionStatus.isConnected(this.status);
    }

    /**
     * Try to connect the Session with creating a new Socket.
     * <p>
     * To create a secured Socket use {@link Session#connect(String, int, ClientCertConfig)}.
     *
     * @param address hostname.
     * @param port    port.
     * @return the new status of the Session.
     */
    public ConnectionStatus connect(final String address, final int port) {
        return connect(address, port, null);
    }

    /**
     * Try to connect the Session with creating a new Socket.
     * <p>
     * If the Session is {@link SessionType#SERVER_SIDE} or {@link ConnectionStatus#CONNECTED} or {@link ConnectionStatus#DESTROYED}, this method will just return the current {@link Session#status}.
     * <p>
     * If the session is currently {@link ConnectionStatus#DISCONNECTED}, this method will create a new Socket and try to connect it to a Server.
     * <p>
     * If the socket connect successfully he is assigned to the {@link Session#socket} and a new {@link Session#status} is emitted.
     * <p>
     * If the socket couldn't connect successfully, this method just return the current {@link Session#status} without changing the {@link Session#socket}.
     *
     * @param address          hostname.
     * @param port             port.
     * @param clientCertConfig the CertConfig, can be null.
     * @return the new status of the Session.
     */
    public ConnectionStatus connect(final String address, final int port, @Nullable final ClientCertConfig clientCertConfig) {
        synchronized (this.lockerConnect) {
            if (isServerSide(this.sessionType) || this.isConnected() || this.status == ConnectionStatus.DESTROYED)
                return this.status;
            try {
                final Socket newSocket;
                if ((newSocket = constructSocket(address, port, clientCertConfig)).isConnected() && integrityCheck(newSocket)) {
                    this.socket = newSocket;
                    setStatus(ConnectionStatus.CONNECTED);
                }
            } catch (IOException e) {
                logger.err("Handled output : " + e.getMessage());
            } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                logger.err("Couldn't create secured socket : " + e.getMessage());
            }
            return this.status;
        }
    }

    /**
     * Try to reconnect the Socket using data's of the current disconnected {@link Session#socket}.
     * This method don't reconnect secured socket.
     * <p>
     * To prevent to reconnect secured session you need to validate that it's not a secured session. Using secured param.
     * <p>
     * This method call {@link Session#connect(String, int)} go check how it works !
     *
     * @param secured if the socket is secured.
     * @return the new status of the Session.
     */
    public ConnectionStatus reconnect(final boolean secured) {
        if (secured)
            new IllegalStateException("Session.reconnect() method cannot be used on SECURED Session.").printStackTrace();
        if (!secured && socket != null)
            return connect(socket.getInetAddress().getHostAddress(), socket.getPort());
        return this.status;
    }

    /**
     * Construct a Socket using params and the optional {@link ClientCertConfig}.
     * <p>
     * This method is used in {@link Session#connect(String, int, ClientCertConfig)}.
     *
     * @param address          hostname.
     * @param port             port.
     * @param clientCertConfig cert config.
     * @return the constructed socket.
     * @throws IOException              errors...
     * @throws CertificateException     errors...
     * @throws NoSuchAlgorithmException errors...
     * @throws KeyStoreException        errors...
     * @throws KeyManagementException   errors...
     */
    protected Socket constructSocket(final String address, final int port, @Nullable final ClientCertConfig clientCertConfig) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        if (clientCertConfig == null)
            return new Socket(address, port);
        else
            return SSLSocketKeystoreFactory.getSocketWithCert(address, port, clientCertConfig.input(), clientCertConfig.password(), clientCertConfig.type(), clientCertConfig.format());
    }

    protected boolean integrityCheck(final Socket socket) {
        // TODO make the handshake exchange a client key.
        return true;
    }

    /**
     * Internal method used to change the current {@link Session#status}.
     * <p>
     * This method emit a new value on the {@link Session#statusFlux}.
     *
     * @param status the new value of status.
     * @return false if status was changed, true if status was changed.
     */
    protected boolean setStatus(final ConnectionStatus status) {
        synchronized (this.lockerConnect) {
            if (this.status == status)
                return false;
            this.statusFlux.emitValue(status);
            this.status = status;
            return true;
        }
    }

    /**
     * This method destroy this session, with closing {@link Session#socket} and destroying {@link Session#statusFlux} and {@link Session#packetFlux}.
     */
    public void destroy() {
        if (this.status == ConnectionStatus.DESTROYED)
            return;
        setStatus(ConnectionStatus.DISCONNECTED);
        setStatus(ConnectionStatus.DESTROYED);
        try {
            this.socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSessionTimeOut(long sessionTimeOut) {
        this.sessionTimeOut = sessionTimeOut;
    }

    public VirtualStream<ConnectionStatus> getStatusFlux() {
        return this.statusFlux;
    }

    public void addStatusListener(final Receiver<ConnectionStatus> statusReceiver) {
        this.statusFlux.addReceiver(statusReceiver);
    }

    public void removeStatusListener(final Receiver<ConnectionStatus> statusReceiver) {
        this.statusFlux.removeReceiver(statusReceiver);
    }

    public ConnectionStatus nextStatus() {
        return this.statusFlux.readASlash();
    }

    public void slashStatus(final ConnectionStatus status) {
        this.statusFlux.slash(status);
    }

    /**
     * Send a packed throw {@link Session#socket}.
     *
     * @param packet the packet at send.
     * @return true if the packet was sent, false if not.
     */
    public boolean send(final SPacket packet) {
        synchronized (this.lockerSend) {
            try {
                socket.getOutputStream().write(ByteBuffer.allocate(4).putInt(packet.data.length).array());
                socket.getOutputStream().write(ByteBuffer.allocate(4).putInt(packet.packetNumber).array());
                socket.getOutputStream().write(packet.data);
                socket.getOutputStream().flush();

                return true;
            } catch (SocketException e) {
                this.setStatus(ConnectionStatus.DISCONNECTED);
                return false;
            } catch (IOException e) {
                e.printStackTrace();
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
    public void onChanged(ConnectionStatus value) {
        synchronized (this.lockerConnect) {
            if (value == ConnectionStatus.DISCONNECTED) {

                this.reader.interrupt();
                lastConnected = System.currentTimeMillis();

                executor.exe(() -> {
                    final long lastConnectedTemp = lastConnected;
                    try {
                        Thread.sleep(sessionTimeOut);
                    } catch (InterruptedException ignored) {
                    }
                    if (this.status != ConnectionStatus.DESTROYED && lastConnected == lastConnectedTemp) {
                        logger.log("Session TIME OUT.");
                        setStatus(ConnectionStatus.DESTROYED);
                    }
                });

            } else if (value == ConnectionStatus.CONNECTED) {

                if (this.reader != null && this.reader.isAlive()) {
                    logger.err("COULDN'T CREATE A READER. A READER ALREADY EXIST !");
                    return;
                }

                this.reader = new Thread(this::internalReadLoop);
                this.reader.start();

            } else if (status == ConnectionStatus.DESTROYED) {

                logger.log("Session Destroyed.");
                if (!this.statusFlux.isClosed()) {
                    this.statusFlux.removeReceiver(this);
                    this.statusFlux.close(ConnectionStatus.DESTROYED);
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
            int read_byte;
            
            while (this.isConnected()) {
                read_byte = readNBytes(in, intBuffer, 0, intBuffer.length);
                if (read_byte == 0)
                    break;
                packetSize = ByteBuffer.wrap(intBuffer).getInt();
                readNBytes(in, intBuffer, 0, intBuffer.length);
                packetNumber = ByteBuffer.wrap(intBuffer).getInt();
                byte[] datas = new byte[packetSize];
                readNBytes(in, datas, 0, packetSize);
                this.packetFlux.emitValue(new RPacket(packetNumber, datas));
            }

            logger.log("Reader closing, other part closed the Session.");
            setStatus(ConnectionStatus.DISCONNECTED);

        } catch (Exception e) {
            if (!this.isConnected())
                logger.log("Reader stopped by closing the socket. " + e.getMessage());
            else
                e.printStackTrace();
        }
    }

    /**
     * Reimplementation of InputStream.readNBytes to be supported by Java 8.
     * --> {@link InputStream#readNBytes(byte[], int, int)}
     */
    public int readNBytes(InputStream stream, byte[] b, int off, int len) {
        try {
            int n = 0;
            while (n < len) {
                int count = 0;
                count = stream.read(b, off + n, len - n);
                if (count < 0)
                    break;
                n += count;
            }
            return n;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getStringID() {
        return "Session{" +
               "id=" + socket.getInetAddress()
               + ", port=" + socket.getPort() + "}";
    }

    @Override
    public String toString() {
        return "Session{" +
               "socket=" + socket +
               ", sessionType=" + sessionType +
               ", status=" + status +
               ", sessionTimeOut=" + sessionTimeOut +
               ", lastConnected=" + lastConnected +
               ", executor=" + executor +
               ", reader=" + reader +
               ", statusFlux=" + statusFlux +
               ", packetFlux=" + packetFlux +
               ", lockerSend=" + lockerSend +
               ", lockerConnect=" + lockerConnect +
               ", logger=" + logger +
               '}';
    }


}
