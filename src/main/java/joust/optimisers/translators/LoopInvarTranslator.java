package joust.optimisers.translators;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.util.Name;
import joust.optimisers.avail.NameFactory;
import joust.optimisers.invar.ExpressionComplexityClassifier;
import joust.optimisers.invar.InvariantExpressionFinder;
import joust.tree.annotatedtree.AJCForest;
import joust.treeinfo.EffectSet;
import joust.utils.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;

import static joust.tree.annotatedtree.AJCTree.*;
import static joust.utils.StaticCompilerUtils.treeCopier;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.utils.StaticCompilerUtils.treeMaker;

/**
 * Tree translator implementing loop-invariant code motion.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class LoopInvarTranslator extends BaseTranslator {
    // The minimum complexity value for an invariant expression to be moved outside of the loop.
    private static final int INVAR_COMPLEXITY_THRESHOLD = 0;

    private Set<AJCExpressionTree> getInvariants(AJCEffectAnnotatedTree loop) {
        log.debug("Invar for: {}", loop);

        EffectSet loopEffects = loop.effects.getEffectSet();

        // The local variables written by the loop. Expressions that depend on these aren't loop invariants. (Usually)
        Set<VarSymbol> writtenInLoop = loopEffects.writeInternal;
        Set<VarSymbol> readInLoop = loopEffects.readInternal;

        InvariantExpressionFinder invariantFinder = new InvariantExpressionFinder(writtenInLoop, readInLoop);
        invariantFinder.visitTree(loop);

        log.debug("Invariant expressions: {}", Arrays.toString(invariantFinder.invariantExpressions.toArray()));

        return invariantFinder.invariantExpressions;
    }

    private void extractInvariants(Set<AJCExpressionTree> invariants, AJCBlock body, AJCStatement loop) {
        AJCBlock targetBlock = loop.getEnclosingBlock();
        MethodSymbol owningContext = body.enclosingMethod.getTargetSymbol();

        for (AJCExpressionTree expr : invariants) {
            // Ignore all expressions that aren't complicated enough to be worth moving..
            ExpressionComplexityClassifier classifier = new ExpressionComplexityClassifier();
            classifier.visitTree(expr);

            if (classifier.getScore() < INVAR_COMPLEXITY_THRESHOLD) {
                log.info("Ignoring invariant expression {} because score {} is below complexity threshold.", expr, classifier.getScore());
                continue;
            }

            Name tempName = NameFactory.getName();
            VarSymbol newSym = new VarSymbol(Flags.FINAL, tempName, expr.getNodeType(), owningContext);

            // Create a new temporary variable to hold this expression.
            AJCVariableDecl newDecl = treeMaker.VarDef(newSym, treeCopier.copy(expr));

            // Insert the new declaration before the for loop.
            targetBlock.insertBefore(loop, newDecl);

            // Replace the expression with a reference to the new temporary variable.
            AJCIdent ref = treeMaker.Ident(newSym);
            expr.swapFor(ref);

            mHasMadeAChange = true;
            AJCForest.getInstance().initialAnalysis();
        }

        if (mHasMadeAChange) {
            log.info("After invariant code motion:\n{}", loop.getEnclosingBlock());
        }
    }

    @Override
    public void visitDoWhileLoop(AJCDoWhileLoop doLoop) {
        log.debug("Invar for: {}", doLoop);

        Set<AJCExpressionTree> invariantExpressions = getInvariants(doLoop);
        extractInvariants(invariantExpressions, doLoop.body, doLoop);

        super.visitDoWhileLoop(doLoop);
    }

    @Override
    public void visitWhileLoop(AJCWhileLoop whileLoop) {
        log.debug("Invar for: {}", whileLoop);

        Set<AJCExpressionTree> invariantExpressions = getInvariants(whileLoop);
        extractInvariants(invariantExpressions, whileLoop.body, whileLoop);

        super.visitWhileLoop(whileLoop);
    }

    @Override
    public void visitForLoop(AJCForLoop forLoop) {
        log.debug("Invar for: {}", forLoop);

        Set<AJCExpressionTree> invariantExpressions = getInvariants(forLoop);
        extractInvariants(invariantExpressions, forLoop.body, forLoop);

        super.visitForLoop(forLoop);
    }
}
