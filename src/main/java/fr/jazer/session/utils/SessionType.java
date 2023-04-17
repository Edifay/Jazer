package fr.jazer.session.utils;

public enum SessionType {
    SERVER_SIDE,
    CLIENT_SIDE;

    public static boolean isServerSide(final SessionType type) {
        return type == SERVER_SIDE;
    }

    public static boolean isClientSide(final SessionType type) {
        return type == CLIENT_SIDE;
    }
}
