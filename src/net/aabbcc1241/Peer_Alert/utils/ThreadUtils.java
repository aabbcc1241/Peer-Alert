package net.aabbcc1241.Peer_Alert.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by beenotung on 1/10/16.
 */
public class ThreadUtils {

    public static class ThreadWorker {
        final Iterator<Runnable> runnableIterator;
        private HashMap<Long, Thread> mThreads = new HashMap<>();
        private LongUIDGenerator uidGenerator = new LongUIDGenerator();

        public ThreadWorker(Iterator<Runnable> runnableIterator) {
            this.runnableIterator = runnableIterator;
        }

        public void start() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (runnableIterator.hasNext())
                        runnableIterator.next().run();
                }
            });
        }

        /**
         * for debug only
         */
        @Deprecated
        public void run() {
            while (runnableIterator.hasNext())
                runnableIterator.next().run();
        }
    }

    public static class LoopWorker {
        private final Runnable runnable;
        private final AtomicBoolean shouldStop = new AtomicBoolean(true);
        private boolean running = false;

        public LoopWorker(Runnable runnable) {
            this.runnable = runnable;
        }

        public void stop() {
            shouldStop.set(true);
        }

        public boolean isRunning() {
            return running;
        }

        public void start() {
            shouldStop.set(false);
            running = true;
            new ThreadWorker(new Iterator<Runnable>() {
                @Override
                public boolean hasNext() {
                    return !shouldStop.get();
                }

                @Override
                public Runnable next() {
                    return new Runnable() {
                        @Override
                        public void run() {
                            runnable.run();
                            if (shouldStop.get())
                                running = false;
                        }
                    };
                }

                @Override
                public void remove() {

                }
            }).start();
        }
    }
}
