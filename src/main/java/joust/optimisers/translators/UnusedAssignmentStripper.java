package joust.optimisers.translators;

import joust.optimisers.visitors.Live;
import joust.treeinfo.EffectSet;
import joust.treeinfo.TreeInfoManager;
import com.sun.tools.javac.code.Symbol;
import joust.utils.TreeUtils;
import lombok.extern.log4j.Log4j2;

import java.util.Set;

import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.utils.StaticCompilerUtils.treeMaker;

/**
 * Detects and deletes unused
 * Backward dataflow: LVA. Kill unused assignments (And, while we're at it, needless temporary variables)
 * Also - count usages of each local variable. If below some threshold, inline it.
 *
 * (Compliments the work of Invar, since it tends to create a large amount of temporaries that
 * are susceptible to this optimisation. Later passes of Invar are improved by running this step
 * in between).
 *
 * Use on one method at a time!
 */
@Log4j2
public
class UnusedAssignmentStripper extends MethodsOnlyTreeTranslator {
    private Set<VarSymbol> everLive;

    @Override
    public void visitMethodDef(AJCMethodDecl tree) {
        // Run LVA over the method...
        // Actually, use the narking side effects?
        Live live = new Live();
        live.visit(tree);

        everLive = tree.everLive;
        super.visitMethodDef(tree);

        log.debug("Result of unused assignment stripping: \n{}", tree);
        // TODO: Datastructure-fu to enable dropping of now-unwanted LVA results here. (To reduce memory footprint).
    }

    /**
     * Check, for a given RHS EffectSet and live variable set, if the current node should be culled.
     * @param rhsEffects The side effects of the RHS of the assignment under consideration.
     * @param live The live variable set applicable at this point.
     * @return true if the assignment under consideration lacks interesting side effects, false otherwise.
     */
    private boolean shouldCull(EffectSet rhsEffects, Set<VarSymbol> live) {
        if (rhsEffects.containsAny(EffectSet.EffectType.IO, EffectSet.EffectType.WRITE_ESCAPING)) {
            return false;
        } else if (rhsEffects.contains(EffectSet.EffectType.WRITE_INTERNAL)) {
            // Determine if any of the symbols are interesting. A symbol is interesting if it is live at
            // this point (In which case write side effects to it matter) or if it is global.
            for (VarSymbol sym : rhsEffects.writeInternal) {
                if (live.contains(sym)) {
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
        }
    }

    @Override
    public void visitAssign(AJCAssign tree) {
        super.visitAssign(tree);

        VarSymbol target = tree.getTargetSymbol();
        Set<VarSymbol> live = tree.liveVariables;

        if (!live.contains(target)) {
            // Determine if the assignment's RHS has meaningful side-effects...
            EffectSet rhsEffects = tree.rhs.effects.getEffectSet();

            if (shouldCull(rhsEffects, live)) {
                log.info("Killing redundant assignment: {}", tree);
                mHasMadeAChange = true;
                tree.swapFor(treeMaker.EmptyExpression());
            }
        }
    }

    @Override
    public void visitAssignop(AJCAssignOp tree) {
        super.visitAssignop(tree);

        VarSymbol target = tree.getTargetSymbol();
        Set<VarSymbol> live = tree.liveVariables;

        if (!live.contains(target)) {
            // Determine if the assignment's RHS has meaningful side-effects...
            EffectSet rhsEffects = tree.rhs.effects.getEffectSet();

            if (shouldCull(rhsEffects, live)) {
                log.info("Killing redundant assignment: {}", tree);
                mHasMadeAChange = true;
                tree.swapFor(treeMaker.EmptyExpression());
            }
        }
    }

    @Override
    public void visitVariableDecl(AJCVariableDecl tree) {
        super.visitVariableDecl(tree);

        VarSymbol target = tree.getTargetSymbol();

        if (!everLive.contains(target)) {
            log.info("Culling assignment: {}", tree);
            mHasMadeAChange = true;
            tree.getEnclosingBlock().remove(tree);
            return;
        }
        Set<VarSymbol> live = tree.liveVariables;

        if (live != null && !live.contains(target)) {
            if (tree.getInit() != null) {
                EffectSet rhsEffects = tree.getInit().effects.getEffectSet();
                if (!shouldCull(rhsEffects, live)) {
                    return;
                }
            }

            if (tree.getInit() != null) {
                log.info("Killing redundant assignment: {}", tree);
                mHasMadeAChange = true;

                // So this *assignment* is redundant, but we need the decl.
                tree.setInit(treeMaker.EmptyExpression());
            }
        }
    }
}
