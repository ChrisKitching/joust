package joust.optimisers.invar;

import joust.optimisers.evaluation.EvaluationContext;
import joust.optimisers.evaluation.Value;
import joust.tree.annotatedtree.AJCTreeVisitor;

import static joust.tree.annotatedtree.AJCTree.*;


/**
 * Workaround for the poor support of array references in the effect set system. Determines if the input expression
 * contains any array accesses that can't be treated naively by the invariant expression analyser.
 */
public class ArrayAccessDetector extends AJCTreeVisitor {
    boolean failureInducing;

    @Override
    protected void visitArrayAccess(AJCArrayAccess that) {
        super.visitArrayAccess(that);

        // If the index is something we can evaluate at compile-time, we're okay.
        EvaluationContext context = new EvaluationContext();
        Value index = context.evaluate(that.index);

        if (index == Value.UNKNOWN) {
            failureInducing = true;
            return;
        }

        // Might as well optimise it while we're at it...
        if (that.index instanceof AJCLiteral) {
            return;
        }

        that.index.swapFor(index.toLiteral());
    }
}
