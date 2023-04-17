package fr.jazer.session.stream;

import fr.jazer.thread_manager.ThreadPool;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class VirtualStream<T> {

    protected boolean closed = false;
    protected final BlockingQueue<T> queue;
    protected final ArrayList<Receiver<T>> receivers;
    protected final ThreadPool executor = new ThreadPool(2000);

    public VirtualStream() {
        this.queue = new LinkedBlockingQueue<>();
        this.receivers = new ArrayList<>();
    }

    public synchronized void emitValue(final T value) {
        try {
            this.queue.put(value);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        this.sendToReceivers(value);
    }

    protected void sendToReceivers(final T value) {
        synchronized (receivers) {
            this.receivers.forEach(receiver -> this.executor.exe(() -> receiver.onChanged(value)));
        }
    }

    public T readASlash() {
        if (this.closed)
            return null;
        try {
            return queue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void addReceiver(final Receiver<T> receiver) {
        synchronized (receivers) {
            this.receivers.add(receiver);
        }
    }

    public void removeReceiver(final Receiver<T> receiver) {
        synchronized (receivers) {
            this.receivers.remove(receiver);
        }
    }

    public boolean slash(final T value) {
        synchronized (this) {
            return queue.remove(value);
        }
    }

    public boolean hasNext() {
        synchronized (this) {
            return !this.queue.isEmpty();
        }
    }

    public void close(final T value) {
        this.emitValue(value);
        this.closed = true;
    }

    public boolean isClosed() {
        return this.closed;
    }

}