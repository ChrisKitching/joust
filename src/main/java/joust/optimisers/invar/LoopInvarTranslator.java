package joust.optimisers.invar;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.util.Name;
import joust.optimisers.invar.ExpressionComplexityClassifier;
import joust.optimisers.invar.InvariantExpressionFinder;
import joust.optimisers.translators.BaseTranslator;
import joust.tree.annotatedtree.AJCComparableExpressionTree;
import joust.tree.annotatedtree.AJCForest;
import joust.tree.annotatedtree.AJCTree;
import joust.tree.annotatedtree.treeinfo.EffectSet;
import joust.utils.data.SetHashMap;
import joust.utils.logging.LogUtils;
import joust.utils.tree.NameFactory;
import joust.utils.data.SymbolSet;
import lombok.AllArgsConstructor;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import static joust.tree.annotatedtree.AJCTree.*;
import static joust.utils.compiler.StaticCompilerUtils.treeCopier;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.utils.compiler.StaticCompilerUtils.treeMaker;

/**
 * Tree translator implementing loop-invariant code motion.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class LoopInvarTranslator extends BaseTranslator {
    // The minimum complexity value for an invariant expression to be moved outside of the loop.
    private static final int INVAR_COMPLEXITY_THRESHOLD = 5;

    private SetHashMap<AJCComparableExpressionTree, AJCTree> getInvariants(AJCEffectAnnotatedTree loop) {
        log.debug("Invar for: {}", loop);

        EffectSet loopEffects = loop.effects.getEffectSet();

        // The local variables written by the loop. Expressions that depend on these aren't loop invariants. (Usually)
        SymbolSet writtenInLoop = loopEffects.writeInternal;
        SymbolSet readInLoop = loopEffects.readInternal;

        InvariantExpressionFinder invariantFinder = new InvariantExpressionFinder(writtenInLoop, readInLoop);
        invariantFinder.visitTree(loop);

        log.debug("Invariant expressions: {}", Arrays.toString(invariantFinder.invariantExpressions.keySet().toArray()));

        Iterator<AJCComparableExpressionTree> iterator = invariantFinder.invariantExpressions.keySet().iterator();
        while (iterator.hasNext()) {
            AJCComparableExpressionTree expr = iterator.next();
            // Discard all expressions that aren't complicated enough to be worth moving..
            ExpressionComplexityClassifier classifier = new ExpressionComplexityClassifier();
            classifier.visitTree(expr.wrappedNode);

            if (classifier.getScore() < INVAR_COMPLEXITY_THRESHOLD) {
                log.info("Ignoring invariant expression {} because score {} is below complexity threshold.", expr.wrappedNode, classifier.getScore());
                iterator.remove();
            }
        }

        return invariantFinder.invariantExpressions;
    }

    @AllArgsConstructor
    private static class InvariantExpressionComparator implements Comparator<AJCComparableExpressionTree> {
        private SetHashMap<AJCComparableExpressionTree, AJCTree> invariants;

        @Override
        public int compare(AJCComparableExpressionTree o1, AJCComparableExpressionTree o2) {
            if (o1.equals(o2)) {
                return 0;
            }

            ExpressionComplexityClassifier classifier1 = new ExpressionComplexityClassifier();
            ExpressionComplexityClassifier classifier2 = new ExpressionComplexityClassifier();

            classifier1.visitTree(o1.wrappedNode);
            classifier2.visitTree(o2.wrappedNode);

            int usagesOne = invariants.get(o1).size();
            int usagesTwo = invariants.get(o2).size();

            int scoreOne = classifier1.getScore() * usagesOne;
            int scoreTwo = classifier2.getScore() * usagesTwo;

            if (scoreOne == scoreTwo) {
                return 0;
            }

            if (scoreOne < scoreTwo) {
                return 1;
            }

            return -1;
        }
    }

    private void extractInvariants(SetHashMap<AJCComparableExpressionTree, AJCTree> invariants, AJCBlock body, AJCStatement loop) {
        if (invariants.isEmpty()) {
            return;
        }

        AJCBlock targetBlock = loop.getEnclosingBlock();
        MethodSymbol owningContext = body.enclosingMethod.getTargetSymbol();

        AJCComparableExpressionTree[] keys = invariants.keySet().toArray(new AJCComparableExpressionTree[invariants.keySet().size()]);
        // Sort keys into descending order of total saving potential (That is, cost multiplied by usage count).

        Arrays.sort(keys, new InvariantExpressionComparator(invariants));

        log.info("Keys: {}", Arrays.toString(keys));
        log.info("Best one: {}", keys[0]);

        AJCComparableExpressionTree key = keys[0];
        Set<AJCTree> usages = invariants.get(key);

        // Unused ones will have a score of zero - so no further ones can be of use.
        if (usages.isEmpty()) {
            return;
        }

        Name tempName = NameFactory.getName();
        VarSymbol newSym = new VarSymbol(Flags.FINAL, tempName, key.wrappedNode.getNodeType(), owningContext);

        // Create a new temporary variable to hold this expression.
        AJCVariableDecl newDecl = treeMaker.VarDef(newSym, (AJCExpressionTree) treeCopier.copy(key.wrappedNode));

        // Insert the new declaration before the for loop.
        targetBlock.insertBefore(loop, newDecl);

        for (AJCTree usage : usages) {
            // Replace each usage with a reference to the new temporary variable.
            AJCIdent ref = treeMaker.Ident(newSym);
            usage.swapFor(ref);
            AJCForest.getInstance().increment("Loop Invariants Hoisted: ");
        }

        mHasMadeAChange = true;
        AJCForest.getInstance().initialAnalysis();

        if (mHasMadeAChange) {
            log.info("After invariant code motion:\n{}", loop.getEnclosingBlock());
        }
    }

    @Override
    public void visitDoWhileLoop(AJCDoWhileLoop doLoop) {
        super.visitDoWhileLoop(doLoop);
        log.debug("Invar for: {}", doLoop);

        extractInvariants(getInvariants(doLoop), doLoop.body, doLoop);
    }

    @Override
    public void visitWhileLoop(AJCWhileLoop whileLoop) {
        super.visitWhileLoop(whileLoop);
        log.debug("Invar for: {}", whileLoop);

        extractInvariants(getInvariants(whileLoop), whileLoop.body, whileLoop);
    }

    @Override
    public void visitForLoop(AJCForLoop forLoop) {
        super.visitForLoop(forLoop);
        log.debug("Invar for: {}", forLoop);

        extractInvariants(getInvariants(forLoop), forLoop.body, forLoop);
    }
}
