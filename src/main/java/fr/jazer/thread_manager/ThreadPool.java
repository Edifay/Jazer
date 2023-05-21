package fr.jazer.thread_manager;

import fr.jazer.logger.Logger;

import java.util.ArrayList;

public class ThreadPool {

    /**
     * The static logger from ThreadPool, all output are by default disabled.
     * <p>
     * If you want ThreadPool to show output. Remove the "ThreadPool" String in {@link Logger#disabledOutPut}.
     */
    private static Logger logger = Logger.loggerOfStatic(ThreadPool.class);

    /**
     * The default time before a thread is release {@link Slave#release()}.
     */
    private static final long DEFAULT_LIVE_TIME = 20000;
    /**
     * Count the current number of Thread alive in this {@link ThreadPool}.
     */
    private static int currentPool = 0;

    /**
     * The list of all Slave who are serving this {@link ThreadPool}. There is no limit on the number of Threads.
     */
    protected final ArrayList<Slave> slaves;

    /**
     * {@link ThreadPool} get a number at it creation used to identify a Slave.
     */
    protected final int poolNb;
    /**
     * The time used by Slaves from the current {@link ThreadPool} to be release.
     * By default, the value of {@link ThreadPool#DEFAULT_LIVE_TIME} is used.
     */
    protected final long liveTime;

    /**
     * @param liveTime define the time waited by Slaves to be release.
     */
    public ThreadPool(final long liveTime) {
        this.slaves = new ArrayList<>();
        this.liveTime = liveTime;
        this.poolNb = currentPool;
        currentPool++;
    }

    /**
     * Default constructor using the default live_time for Slaves.
     */
    public ThreadPool() {
        this(DEFAULT_LIVE_TIME);
    }

    /**
     * Execute a runnable on a Thread.
     * <p>
     * If any slave owned by the current ThreadPool is available, it will execute the task without creating Thread.
     * If all Slaves owned by the current ThreadPool are working, it will create a new Slave and execute the task on it.
     *
     * @param runnable the task to execute.
     */
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

    /**
     * Interrupt all slaves.
     */
    public void destroy() {
        synchronized (slaves) {
            for (final Slave slave : slaves)
                slave.worker.interrupt();
        }
    }

    /**
     * This method is called by Slaves asking to release.
     * If the slave who's calling this method do not have a task, he is removed from {@link ThreadPool#slaves}.
     *
     * @param slave the slave at remove.
     * @return true if the Slaves was removed, false if not.
     */
    protected boolean freeSlave(final Slave slave) {
        synchronized (slaves) {
            if (slave.task != null)
                return false;
            slaves.remove(slave);
            logger.log(slave.getName() + " free. Current " + slaves.size() + " slaves alive.");
            return true;
        }
    }

    /**
     * The Slave classes own a Thread and a Task (a {@link Runnable}), and execute this task on the owned Thread.
     * The Slave will wait for a new task to execute after finishing the current Task.
     * <p>
     * The Slave wait {@link ThreadPool#liveTime} ms, and ask for freedom to the ThreadPool who's owning it.
     */
    private static class Slave {
        /**
         * The Thread working.
         */
        protected final Thread worker;
        /**
         * Flag the current state of the Slave.
         * true if the slaves always accept work.
         * false if not.
         */
        protected boolean alwaysServing;
        /**
         * The task to execute. If the Slave don't have any task at the moment, task is null.
         */
        protected Runnable task;
        /**
         * The time a Slave is waiting before asking for release.
         */
        protected final long liveTime;

        /**
         * Create a new Thread.
         * The Thread sleep before receiving a new task.
         *
         * @param name     the name of the Slave.
         * @param liveTime the time a Slave is waiting before asking for release.
         * @param master   the ThreadPool owning this Slave.
         */
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

        /**
         * Ask the Slave to work on a task.
         *
         * @param task the task to run.
         */
        protected void workOn(final Runnable task) {
            if (this.task != null) {
                System.err.println("THREAD AS ALREADY A RUNNING TASK !!");
                return;
            }

            this.task = task;
            this.worker.interrupt();
        }

        /**
         * @return if the Slave has been released.
         */
        protected boolean isFree() {
            return this.task == null;
        }

        /**
         * @return if the Slave is currently working.
         */
        protected boolean isWorking() {
            return this.task != null;
        }

        /**
         * @return the name of the Thread owned by the Slave.
         */
        public String getName() {
            return this.worker.getName();
        }

        /**
         * Stop the Thread.
         */
        protected void release() {
            this.alwaysServing = false;
            workOn(() -> {
            });
        }
    }

}
