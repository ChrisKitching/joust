package joust.optimisers.translators;

import joust.optimisers.visitors.Live;
import joust.tree.annotatedtree.AJCForest;
import joust.tree.annotatedtree.AJCTree;
import joust.treeinfo.EffectSet;
import joust.treeinfo.TreeInfoManager;
import com.sun.tools.javac.code.Symbol;
import joust.utils.LogUtils;
import joust.utils.TreeUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;

import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.utils.StaticCompilerUtils.treeMaker;

/**
 * Detects and deletes unused assignments.
 * Backward dataflow: LVA. Kill unused assignments (And, while we're at it, needless temporary variables)
 * Also - count usages of each local variable. If below some threshold, inline it.
 *
 * (Compliments the work of Invar, since it tends to create a large amount of temporaries that
 * are susceptible to this optimisation. Later passes of Invar are improved by running this step
 * in between).
 *
 * Use on one method at a time!
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class UnusedAssignmentStripper extends MethodsOnlyTreeTranslator {
    private Set<VarSymbol> everLive;

    @Override
    public void visitMethodDef(AJCMethodDecl tree) {
        // Run LVA over the method...
        // Actually, use the narking side effects?
        Live live = new Live();
        live.visitTree(tree);

        everLive = tree.everLive;

        // Don't visit the parameters of a method. Deleting parameters isn't allowed.
        // While we're at it, let's gloss over all the crap we don't care about.
        visit(tree.restype);
        visit(tree.mods);
        visit(tree.recvparam);
        visit(tree.thrown);
        visit(tree.body);
        visit(tree.defaultValue);

        log.debug("Result of unused assignment stripping: \n{}", tree);
        // TODO: Datastructure-fu to enable dropping of now-unwanted LVA results here. (To reduce memory footprint).
    }

    @Override
    protected void visitCatch(AJCCatch that) {
        // Don't visit catcher params... (You might try to delete them...)
        visit(that.body);
    }

    /**
     * Check, for a given RHS EffectSet and live variable set, if the current node should be culled.
     * @param rhsEffects The side effects of the RHS of the assignment under consideration.
     * @param live The live variable set applicable at this point.
     * @return true if the assignment under consideration lacks interesting side effects, false otherwise.
     */
    private boolean shouldCull(EffectSet rhsEffects, Set<VarSymbol> live) {
        log.debug(rhsEffects);
        if (rhsEffects.containsAny(EffectSet.EffectType.IO, EffectSet.EffectType.WRITE_ESCAPING)) {
            log.debug("Should not cull because effects.");
            return false;
        } else if (rhsEffects.contains(EffectSet.EffectType.WRITE_INTERNAL)) {
            // Determine if any of the symbols are interesting. A symbol is interesting if it is live at
            // this point (In which case write side effects to it matter) or if it is global.
            for (VarSymbol sym : rhsEffects.writeInternal) {
                if (live.contains(sym)) {
                    log.debug("Should not cull because {} is live.", sym);
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Since Skips aren't expressions, transforming the contents of an expression statement to a skip invalidates
     * the tree. So, use this method to skip-ify an expressionstatement that has its contents skipified.
     */
    @Override
    public void visitExpressionStatement(AJCExpressionStatement tree) {
        super.visitExpressionStatement(tree);

        if (tree.expr.isEmptyExpression()) {
            mHasMadeAChange = true;
            tree.getEnclosingBlock().remove(tree);
            log.info("Removing empty expression.");
        }
    }

    @Override
    public void visitAssign(AJCAssign tree) {
        super.visitAssign(tree);

        VarSymbol target = tree.getTargetSymbol();
        Set<VarSymbol> live = tree.liveVariables;

        if (!live.contains(target) && TreeUtils.isLocalVariable(target)) {
            // TODO: Become cleverer so you can touch array assignments.
            if (tree.lhs instanceof AJCArrayAccess) {
                return;
            }

            // Determine if the assignment's RHS has meaningful side-effects...
            EffectSet rhsEffects = tree.rhs.effects.getEffectSet();

            if (shouldCull(rhsEffects, live)) {
                log.info("Killing redundant assignment: {}", tree);
                log.info("Live: {}", Arrays.toString(live.toArray()));
                log.info("Target: {}", target);
                log.info("rhsEffects: {}", rhsEffects);
                mHasMadeAChange = true;
                tree.swapFor(treeMaker.EmptyExpression());
                AJCForest.getInstance().initialAnalysis();
            }
        }
    }

    @Override
    public void visitAssignop(AJCAssignOp tree) {
        super.visitAssignop(tree);

        VarSymbol target = tree.getTargetSymbol();
        Set<VarSymbol> live = tree.liveVariables;

        if (!live.contains(target) && TreeUtils.isLocalVariable(target)) {
            // TODO: Become cleverer so you can touch array assignments.
            if (tree.lhs instanceof AJCArrayAccess) {
                return;
            }

            // Determine if the assignment's RHS has meaningful side-effects...
            EffectSet rhsEffects = tree.rhs.effects.getEffectSet();

            if (shouldCull(rhsEffects, live)) {
                log.info("Killing redundant assignment: {}", tree);
                log.info("Live: {}", Arrays.toString(live.toArray()));
                log.info("Target: {}", target);
                log.info("rhsEffects: {}", rhsEffects);
                mHasMadeAChange = true;
                tree.swapFor(treeMaker.EmptyExpression());
                AJCForest.getInstance().initialAnalysis();
            }
        }
    }

    @Override
    public void visitVariableDecl(AJCVariableDecl tree) {
        super.visitVariableDecl(tree);

        VarSymbol target = tree.getTargetSymbol();

        if (!TreeUtils.isLocalVariable(target)) {
            return;
        }

        Set<VarSymbol> live = tree.liveVariables;

        if (live != null && !live.contains(target)) {
            // This assignment is certainly not needed.

            if (tree.getInit().isEmptyExpression()) {
                // There's no assignment to remove.
                if (!everLive.contains(target)) {
                    // But even the declaration is pointless.
                    log.info("Binning declaration: {}", tree);
                    log.info("Live: {}", Arrays.toString(live.toArray()));
                    log.info("Target: {}", target);
                    tree.getEnclosingBlock().remove(tree);
                    AJCForest.getInstance().initialAnalysis();
                    mHasMadeAChange = true;
                }

                return;
            }

            // Check if the assignment has interesting side effects.
            EffectSet rhsEffects = tree.getInit().effects.getEffectSet();
            boolean cull = shouldCull(rhsEffects, live);

            if (everLive.contains(target)) {
                // We need the variable declaration.
                if (cull) {
                    // But we can kill the assignment.
                    log.info("Dropping assignment: {}", tree);
                    log.info("Live: {}", Arrays.toString(live.toArray()));
                    log.info("Target: {}", target);
                    log.info("rhsEffects: {}", rhsEffects);
                    tree.setInit(treeMaker.EmptyExpression());
                    AJCForest.getInstance().initialAnalysis();
                    mHasMadeAChange = true;
                }

                return;
            }

            // We don't need the declaration - but do we need the assignment?
            if (cull) {
                // Nope. Bin everything.
                log.info("Binning everything: {}", tree);
                log.info("Live: {}", Arrays.toString(live.toArray()));
                log.info("Target: {}", target);
                log.info("rhsEffects: {}", rhsEffects);
                tree.getEnclosingBlock().remove(tree);
                AJCForest.getInstance().initialAnalysis();
                mHasMadeAChange = true;
                return;
            }

            // We do - so shunt it.
            log.info("Shunting: {}", tree);
            log.info("Live: {}", Arrays.toString(live.toArray()));
            log.info("Target: {}", target);
            log.info("rhsEffects: {}", rhsEffects);
            tree.swapFor(treeMaker.Exec(tree.getInit()));
            AJCForest.getInstance().initialAnalysis();
            mHasMadeAChange = true;
        }
    }
}
