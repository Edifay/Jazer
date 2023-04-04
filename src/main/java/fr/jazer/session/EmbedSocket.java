package fr.jazer.session;

import java.net.Socket;

public class EmbedSocket {

    protected Socket socket;

    public EmbedSocket(final Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket(){
        return this.socket;
    }
}