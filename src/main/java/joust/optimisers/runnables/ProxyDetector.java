package joust.optimisers.runnables;

import joust.optimisers.proxydetect.ProxyDetectVisitor;

public class ProxyDetector extends OptimisationRunnable.BluntForce {
    public ProxyDetector() {
        super(new ProxyDetectVisitor());
    }
}
