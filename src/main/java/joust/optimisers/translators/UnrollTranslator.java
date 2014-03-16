package joust.optimisers.translators;

import com.sun.tools.javac.util.List;
import joust.optimisers.evaluation.EvaluationContext;
import joust.optimisers.evaluation.Value;
import joust.treeinfo.EffectSet;
import joust.utils.SymbolSet;
import joust.utils.TreeUtils;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;

import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.utils.StaticCompilerUtils.treeCopier;

/**
 * Loop unrolling funtimes!
 *
 * The visitMethodDef method deploys the UnrollableLoopVisitor to determine which loops should be considered for
 * unrolling.
 */
@Log4j2
public class UnrollTranslator extends BaseTranslator {
    // The number of iterations to attempt to unroll before giving up. Set too low and nothing gets unrolled, too
    // high and too much gets unrolled and the binary becomes huge and the JIT becomes hindered.
    public static final int UNROLL_LIMIT = 16;

    @Override
    public void visitMethodDef(AJCMethodDecl tree) {
        super.visitMethodDef(tree);
        if (mHasMadeAChange) {
            // If we touched anything, it's sort of likely there's new dead assignments to strip...
            UnusedAssignmentStripper stripper;
            do {
                stripper = new UnusedAssignmentStripper();
                stripper.visit(tree);
            } while (stripper.mHasMadeAChange);
        }
    }

    @Override
    public void visitForLoop(AJCForLoop tree) {
        super.visitForLoop(tree);

        log.debug("Unroll consideration for: {}", tree);

        EffectSet condEffects = tree.cond.effects.getEffectSet();
        SymbolSet condReads = condEffects.readInternal;
        SymbolSet condWrites = condEffects.writeInternal;

        // Find the effects for the statements in the repeat steps...
        SymbolSet repeatReads = new SymbolSet();
        SymbolSet repeatWrites = new SymbolSet();
        for (AJCExpressionStatement stat : tree.step) {
            EffectSet stepEffects = stat.effects.getEffectSet();
            repeatReads.addAll(stepEffects.readInternal);
            repeatWrites.addAll(stepEffects.writeInternal);
        }

        // Determine if any of the symbols depended on by the condition or repeat are global.
        if (containsGlobal(condReads) || containsGlobal(condWrites)
         || containsGlobal(repeatReads) || containsGlobal(repeatWrites)) {
            log.debug("Aborting unrolling - global symbol deps!");
            return;
        }

        EffectSet bodyEffects = tree.body.effects.getEffectSet();
        // If the body writes anything read by the cond or repeat, abort. (That shit's complicated.).
        SymbolSet bodyWrites = bodyEffects.writeInternal;

        // TODO: can *sometimes* deal with this. Sort of tricky, and implies very retarded code.
        if (!bodyWrites.intersect(condReads).isEmpty() || !bodyWrites.intersect(repeatReads).isEmpty()) {
            log.debug("Aborting unrolling - body writes to condition/repeat deps!");
            return;
        }

        // Attempt to evaluate the loop management code ahead of time.
        EvaluationContext context = new EvaluationContext();
        context.evaluateStatements(tree.init);
        Value condition = context.evaluate(tree.cond);
        if (condition == Value.UNKNOWN) {
            log.debug("Abort: Condition unknown.");
            return;
        }

        if (!((Boolean) condition.getValue())) {
            log.debug("Instantly false for condition...");
            // Special case - the loop condition is initially false.
            // We can replace the loop with the init statements (Killing any that are variable initialisers or
            // depend thereon.
            AJCBlock block = tree.getEnclosingBlock();
            block.insertBefore(tree, tree.init);
            block.remove(tree);
            return;
        }

        int iterations = 0;
        while (iterations < UNROLL_LIMIT && condition != Value.UNKNOWN && (Boolean) condition.getValue()) {
            context.evaluateExpressionStatements(tree.step);
            log.debug("Status: \n{}", context);
            condition = context.evaluate(tree.cond);
            iterations++;
        }

        if (iterations >= UNROLL_LIMIT || condition == Value.UNKNOWN) {
            log.debug("Abort: Condition unknown or cycles exceeded");
            return;
        }

        // Replace the for loop with its initialiser, then iterations-many repeats of the body inlining variables that
        // are known in the EvaluationContext as we go.

        // Some of these might turn out to be pointless. That's okay - we'll run the UnusedAssignmentStripper over them
        // in a minute.
        List<AJCStatement> statements = tree.init;

        context = new EvaluationContext();
        // Now we replay the loop evaluation that we know terminates nicely and make substitutions as we go...
        context.evaluateStatements(tree.init);

        // The condition is now true. Time for a loop body.
        while (iterations > 0) {
            // Append the loop body with every known value from context substituted.
            statements = statements.appendList(getSubstitutedCopy(tree.body, context));
            context.evaluateExpressionStatements(tree.step);
            iterations--;
        }

        tree.getEnclosingBlock().insertBefore(tree, statements);
        tree.getEnclosingBlock().remove(tree);
    }

    /**
     * Get a copy of the given list of statements with all references to values known in context replaced by the
     * appropriate literal.
     */
    private List<AJCStatement> getSubstitutedCopy(AJCBlock body, EvaluationContext context) {
        List<AJCStatement> ret = treeCopier.copy(body.stats);

        HashMap<VarSymbol, Value> assignments = context.getCurrentAssignments();

        ContextInliningTranslator inliner = new ContextInliningTranslator(assignments);
        inliner.visit(ret);
        return ret;
    }

    private boolean containsGlobal(SymbolSet syms) {
        for (VarSymbol sym : syms) {
            if (!TreeUtils.isLocalVariable(sym)) {
                return true;
            }
        }

        return false;
    }
}
