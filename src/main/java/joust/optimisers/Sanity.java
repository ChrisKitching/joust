package joust.optimisers;

import joust.optimisers.translators.TreeSanityInducingTranslator;

public class Sanity extends BluntForceOptimisationRunnable {
    public Sanity() {
        super(TreeSanityInducingTranslator.class);
    }
}