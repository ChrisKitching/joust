package joust.optimisers;

import joust.optimisers.translators.ExpressionNormalisingTranslator;
import joust.optimisers.translators.TreeSanityInducingTranslator;

public class ExpressionNormalise extends BluntForceOptimisationRunnable {
    public ExpressionNormalise() {
        super(ExpressionNormalisingTranslator.class);
    }
}