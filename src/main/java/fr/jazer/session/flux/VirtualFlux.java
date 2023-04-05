package fr.jazer.session.flux;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class VirtualFlux<T> {

    private static final long INFINITE = Long.MAX_VALUE;
    protected final Queue<T> queue;
    protected final ArrayList<Thread> toWakeUp;

    public VirtualFlux() {
        this.queue = new LinkedList<>();
        this.toWakeUp = new ArrayList<>();
    }

    protected synchronized boolean emitValue(final T value) {
        this.queue.add(value);
        this.wakeUp();
        return true;
    }

    public void wakeUp() {
        synchronized (this) {
            toWakeUp.forEach(Thread::interrupt);
        }
    }

    public T readASlash() {
        if (queue.isEmpty()) {
            synchronized (this) {
                toWakeUp.add(Thread.currentThread());
            }
        }
        try {
            Thread.sleep(INFINITE);
        } catch (InterruptedException e) {
        }
        return queue.poll();
    }

}