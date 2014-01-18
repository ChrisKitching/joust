package joust.optimisers;

import joust.OptimiserOptions;
import joust.optimisers.translators.AssertionStrippingTranslator;

/**
 * An optimisation to run after annotation processing that strips all assertions from the trees.
 * Default value of the option is to not strip assertions.
 */
public class StripAssertions extends BluntForceOptimisationRunnable {
    public StripAssertions() {
        super(AssertionStrippingTranslator.class);
    }

    @Override
    public void run() {
        if (!OptimiserOptions.stripAssertions) {
            return;
        }

        super.run();
    }
}
