package fr.jazer.ThreadManager;

import fr.jazer.logger.Logger;

import java.util.ArrayList;

public class ThreadPool {

    private static int currentPool = 0;

    private static Logger logger = Logger.loggerOfStatic(ThreadPool.class);

    protected final ArrayList<Slave> slaves;

    protected final int poolNb;

    public ThreadPool() {
        this.slaves = new ArrayList<>();
        this.poolNb = currentPool;
        currentPool++;
    }

    public void exe(final Runnable runnable) {
        synchronized (slaves) {
            for (final Slave slave : slaves) {
                logger.log("Slave " + slave.getName() + " Working.");
                if (slave.isFree()) {
                    slave.workOn(runnable);
                    logger.log("Slave " + slave.getName() + " executing a task.");
                    return;
                }
            }
            final Slave slave = new Slave("Pool " + this.poolNb + " -> Slave " + slaves.size(), this);
            logger.log("Creating slave : " + "Pool " + this.poolNb + " -> Slave " + slaves.size());
            slaves.add(slave);
            slave.workOn(runnable);
        }
    }

    protected boolean freeSlave(final Slave slave) {
        synchronized (slaves) {
            if (slave.task != null)
                return false;
            slaves.remove(slave);
            logger.log("Free slave : " + slave.getName() + " current threads running : " + slaves.size());
            return true;
        }
    }

    private static class Slave {

        protected final Thread worker;
        protected boolean alwaysServing;

        protected final Object key = new Object();

        protected Runnable task;

        public Slave(final String name, final ThreadPool master) {
            this.alwaysServing = true;
            this.worker = new Thread(() -> {
                synchronized (key) {
                    try {
                        while (alwaysServing) {
                            key.wait(2000);
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
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
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
            synchronized (key) {
                this.key.notify();
            }
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
