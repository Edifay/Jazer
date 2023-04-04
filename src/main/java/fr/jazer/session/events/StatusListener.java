package fr.jazer.session.events;

import fr.jazer.session.ConnexionStatus;

public interface StatusListener {
    void onStatusChange(final ConnexionStatus status);
}
