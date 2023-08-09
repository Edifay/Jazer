package fr.jazer.session.utils;

public enum ConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    DESTROYED;

    public static boolean isConnected(final ConnectionStatus status) {
        return status == CONNECTED;
    }

    public static boolean isDisconnected(final ConnectionStatus status) {
        return status == DISCONNECTED;
    }

    public static boolean isDestroyed(final ConnectionStatus status) {
        return status == DESTROYED;
    }
}
