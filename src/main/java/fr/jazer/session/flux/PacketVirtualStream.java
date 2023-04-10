package fr.jazer.session.flux;

import fr.jazer.thread_manager.ThreadPool;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PacketVirtualStream<T extends TaggedEntity> {

    protected final HashMap<Integer, BlockingQueue<T>> queue;
    protected final HashMap<Integer, ArrayList<Receiver<T>>> receivers;

    protected boolean closed = false;
    protected final ThreadPool executor = new ThreadPool(2000);

    public PacketVirtualStream() {
        this.queue = new HashMap<>();
        this.receivers = new HashMap<>();
    }

    public synchronized void emitValue(final T value) {
        if (!this.queue.containsKey(value.getTag()))
            this.queue.put(value.getTag(), new LinkedBlockingQueue<>());

        try {
            this.queue.get(value.getTag()).put(value);
            this.sendToReceivers(value);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void sendToReceivers(final T value) {
        synchronized (this.receivers) {
            if (this.receivers.containsKey(value.getTag()))
                this.receivers.get(value.getTag()).forEach(receiver -> this.executor.exe(() -> receiver.onChanged(value)));
        }
    }

    public T readASlash(final int tag) {
        if (closed)
            return null;
        synchronized (this) {
            if (this.queue.get(tag) == null)
                this.queue.put(tag, new LinkedBlockingQueue<>());
        }
        try {
            return this.queue.get(tag).take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addReceiver(final int tag, final Receiver<T> receiver) {
        synchronized (receivers) {
            if (!this.receivers.containsKey(tag))
                this.receivers.put(tag, new ArrayList<>());
            this.receivers.get(tag).add(receiver);
        }
    }

    public void removeReceiver(final Receiver<T> receiver) {
        synchronized (receivers) {
            this.receivers.values().forEach(each -> each.remove(receiver));
        }
    }

    public boolean slash(final T value) {
        synchronized (this) {
            return queue.get(value.getTag()).remove(value);
        }
    }

    public boolean hasNext(final int tag) {
        synchronized (this) {
            return !this.queue.get(tag).isEmpty();
        }
    }

    public void close(final T terminal) {
        this.closed = true;
        System.out.println("Closing !");
        this.queue.forEach((integer, ts) -> {
            try {
                ts.put(terminal);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public boolean isClosed() {
        return closed;
    }
}
