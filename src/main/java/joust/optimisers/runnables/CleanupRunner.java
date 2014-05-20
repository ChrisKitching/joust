package joust.optimisers.runnables;

import joust.joustcache.JOUSTCache;

public class CleanupRunner extends OptimisationRunnable {
    @Override
    public void run() {
        JOUSTCache.closeDatabase();
    }
}
