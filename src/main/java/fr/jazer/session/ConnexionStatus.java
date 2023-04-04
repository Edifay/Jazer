package fr.jazer.session;

public enum ConnexionStatus {
    CONNECTED,
    DISCONNECTED,
    DESTROYED;

    public static boolean isConnected(final ConnexionStatus status) {
        return status == CONNECTED;
    }
    public static boolean isDisconnected(final ConnexionStatus status){
        return status == DISCONNECTED;
    }
}
