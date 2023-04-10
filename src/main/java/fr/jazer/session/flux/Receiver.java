package fr.jazer.session.flux;

public interface Receiver<T> {
    void onChanged(final T value);
}