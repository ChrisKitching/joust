package joust.optimisers.runnables;

import joust.joustcache.JOUSTCache;
import joust.tree.annotatedtree.AJCForest;
import joust.utils.logging.LogUtils;

public class CleanupRunner extends OptimisationRunnable {
    @Override
    public void run() {
        JOUSTCache.closeDatabase();
        AJCForest.getInstance().printStatistics();
        LogUtils.unInit();
    }
}
