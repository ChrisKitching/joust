package joust.optimisers.translators;

import com.sun.tools.javac.util.List;
import joust.optimisers.evaluation.EvaluationContext;
import joust.optimisers.evaluation.Value;
import joust.optimisers.visitors.sideeffects.SideEffectVisitor;
import joust.treeinfo.EffectSet;
import joust.treeinfo.TreeInfoManager;
import joust.utils.SymbolSet;
import joust.utils.TreeUtils;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

/**
 * Loop unrolling funtimes!
 *
 * The visitMethodDef method deploys the UnrollableLoopVisitor to determine which loops should be considered for
 * unrolling.
 */
public @Log4j2
class UnrollTranslator extends ParentTrackingTreeTranslator {
    // The number of iterations to attempt to unroll before giving up. Set too low and nothing gets unrolled, too
    // high and too much gets unrolled and the binary becomes huge and the JIT becomes hindered.
    public static final int UNROLL_LIMIT = 16;

    @Override
    public void visitMethodDef(JCMethodDecl tree) {
        super.visitMethodDef(tree);
        if (mHasMadeAChange) {
            // If we touched anything, it's sort of likely there's new dead assignments to strip...
            SideEffectVisitor effectVisitor = new SideEffectVisitor();
            tree.accept(effectVisitor);
            effectVisitor.finaliseIncompleteEffectSets();
            UnusedAssignmentStripper stripper;
            do {
                stripper = new UnusedAssignmentStripper();
                tree.accept(stripper);
            } while (stripper.mHasMadeAChange);
            log.info("After unrolling and stripping: \n{}", visitedStack.peek());
        }
    }

    @Override
    public void visitForLoop(JCForLoop tree) {
        super.visitForLoop(tree);

        log.debug("Unroll consideration for: {}", tree);

        EffectSet condEffects = TreeInfoManager.getEffects(tree.cond);
        SymbolSet condReads = condEffects.readInternal;
        SymbolSet condWrites = condEffects.writeInternal;

        EffectSet repeatEffects = TreeInfoManager.getEffects(tree.cond);
        SymbolSet repeatReads = repeatEffects.readInternal;
        SymbolSet repeatWrites = repeatEffects.writeInternal;

        // Determine if any of the symbols depended on by the condition or repeat are global.
        if (containsGlobal(condReads) || containsGlobal(condWrites)
         || containsGlobal(repeatReads) || containsGlobal(repeatWrites)) {
            log.debug("Aborting unrolling - global symbol deps!");
            return;
        }

        EffectSet bodyEffects = TreeInfoManager.getEffects(tree.body);
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
            insertIntoEnclosingBlock(tree, tree.init);
            result = treeMaker.Skip();
            return;
        }

        int iterations = 0;
        while (iterations < UNROLL_LIMIT && condition != Value.UNKNOWN && ((Boolean) condition.getValue())) {
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
        List<JCStatement> statements = tree.init;

        context = new EvaluationContext();
        // Now we replay the loop evaluation that we know terminates nicely and make substitutions as we go...
        context.evaluateStatements(tree.init);

        // The condition is now true. Time for a loop body.
        while (iterations > 0) {
            // Append the loop body with every known value from context substituted.
            statements = statements.appendList(getSubstitutedCopy((JCBlock) tree.body, context));
            context.evaluateExpressionStatements(tree.step);
            iterations--;
        }

        insertIntoEnclosingBlock(tree, statements);

        // Finally, delete the loop.
        result = treeMaker.Skip();
    }

    /**
     * Get a copy of the given list of statements with all references to values known in context replaced by the
     * appropriate literal.
     */
    private List<JCStatement> getSubstitutedCopy(JCBlock body, EvaluationContext context) {
        List<JCStatement> ret = treeCopier.copy(body.stats);

        HashMap<VarSymbol, Value> assignments = context.getCurrentAssignments();

        ContextInliningTranslator inliner = new ContextInliningTranslator(assignments);
        ret = inliner.translate(ret);
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
