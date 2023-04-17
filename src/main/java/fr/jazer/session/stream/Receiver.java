package fr.jazer.session.stream;

public interface Receiver<T> {
    void onChanged(final T value);
}