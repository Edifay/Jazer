package fr.jazer.thread_manager;

import fr.jazer.logger.Logger;

import java.util.ArrayList;

public class ThreadPool {

    private static Logger logger = Logger.loggerOfStatic(ThreadPool.class);

    private static final long DEFAULT_LIVE_TIME = 20000;
    private static int currentPool = 0;

    protected final ArrayList<Slave> slaves;

    protected final int poolNb;
    protected final long liveTime;

    public ThreadPool(final long liveTime) {
        this.slaves = new ArrayList<>();
        this.liveTime = liveTime;
        this.poolNb = currentPool;
        currentPool++;
    }

    public ThreadPool(){
        this(DEFAULT_LIVE_TIME);
    }

    public void exe(final Runnable runnable) {
        synchronized (slaves) {
            for (final Slave slave : slaves) {
                if (slave.isFree()) {
                    logger.log("Executing on " + slave.getName() + ".");
                    slave.workOn(runnable);
                    return;
                }
            }
            final Slave slave = new Slave("Slave " + slaves.size(), liveTime, this);
            logger.log("Creating slave : " + "Slave " + slaves.size() + " executing on " + slave.getName() + ".");
            slaves.add(slave);
            slave.workOn(runnable);
        }
    }

    protected boolean freeSlave(final Slave slave) {
        synchronized (slaves) {
            if (slave.task != null)
                return false;
            slaves.remove(slave);
            logger.log(slave.getName() + " free. Current " + slaves.size() + " slaves alive.");
            return true;
        }
    }

    private static class Slave {

        protected final Thread worker;
        protected boolean alwaysServing;
        protected Runnable task;
        protected final long liveTime;

        public Slave(final String name, final long liveTime, final ThreadPool master) {
            this.alwaysServing = true;
            this.liveTime = liveTime;
            this.worker = new Thread(() -> {
                while (alwaysServing) {
                    try {
                        Thread.sleep(liveTime);
                    } catch (InterruptedException ignored) {
                    }
                    if (task == null && master.freeSlave(this))
                        this.alwaysServing = false;
                    if (task != null)
                        try {
                            this.task.run();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    this.task = null;
                }
            });
            this.worker.setName(name);
            this.worker.start();
        }

        protected void workOn(final Runnable task) {
            if (this.task != null) {
                System.err.println("THREAD AS ALREADY A RUNNING TASK !!");
                return;
            }

            this.task = task;
            this.worker.interrupt();
        }

        protected boolean isFree() {
            return this.task == null;
        }

        protected boolean isWorking() {
            return this.task != null;
        }

        public String getName() {
            return this.worker.getName();
        }

        protected void release() {
            this.alwaysServing = false;
            workOn(() -> {
            });
        }
    }

}
