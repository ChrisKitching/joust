package joust.optimisers.runnables;

import joust.optimisers.translators.ExpressionNormalisingTranslator;

/**
 * Normalise expressions. In cases where doing so does not affect the semantics, reorder operations into an
 * arbitrary but consistent ordering. This allows us to neglect (costly) considerations of the many arrangements
 * of commutative operations in later steps (as all such operations for which it is safe to do so will by then
 * have been converted into a standardised format).
 */
public class ExpressionNormalise extends OptimisationRunnable.BluntForce {
    public ExpressionNormalise() {
        super(ExpressionNormalisingTranslator.class);
    }
}