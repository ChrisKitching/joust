package joust.optimisers.translators;

import joust.optimisers.visitors.Live;
import joust.treeinfo.EffectSet;
import joust.treeinfo.TreeInfoManager;
import com.sun.tools.javac.code.Symbol;
import joust.utils.TreeUtils;
import lombok.extern.log4j.Log4j2;

import java.util.Set;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.Optimiser.treeMaker;

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
public @Log4j2
class UnusedAssignmentStripper extends MethodsOnlyTreeTranslator {
    private Set<VarSymbol> everLive;

    @Override
    public void visitMethodDef(JCMethodDecl tree) {
        // Run LVA over the method...
        Live live = new Live();
        tree.accept(live);

        everLive = TreeInfoManager.getEverLive(tree.sym);
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
    private boolean shouldCull(EffectSet rhsEffects, Set<Symbol> live) {
        if (rhsEffects.containsAny(EffectSet.EffectType.IO, EffectSet.EffectType.WRITE_ESCAPING)) {
            return false;
        } else if (rhsEffects.contains(EffectSet.EffectType.WRITE_INTERNAL)) {
            // Determine if any of the symbols are interesting. A symbol is interesting if it is live at
            // this point (In which case write side effects to it matter) or if it is global.
            for (VarSymbol sym : rhsEffects.writeInternal) {
                if (!TreeUtils.isLocalVariable(sym) || live.contains(sym)) {
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
    public void visitExec(JCExpressionStatement tree) {
        super.visitExec(tree);

        if (tree.expr == null) {
            mHasMadeAChange = true;
            result = treeMaker.Skip();
        }
    }

    @Override
    public void visitAssign(JCAssign tree) {
        super.visitAssign(tree);

        Symbol target = TreeUtils.getTargetSymbolForAssignment(tree);
        Set<Symbol> live = TreeInfoManager.getLiveVariables(tree);

        if (!live.contains(target)) {
            // Determine if the assignment's RHS has meaningful side-effects...
            EffectSet rhsEffects = TreeInfoManager.getEffects(tree.rhs);

            if (shouldCull(rhsEffects, live)) {
                log.info("Killing redundant assignment: {}", tree);
                mHasMadeAChange = true;
                result = null;
            }
        }
    }

    @Override
    public void visitAssignop(JCAssignOp tree) {
        super.visitAssignop(tree);

        Symbol target = TreeUtils.getTargetSymbolForAssignment(tree);
        Set<Symbol> live = TreeInfoManager.getLiveVariables(tree);

        if (!live.contains(target)) {
            // Determine if the assignment's RHS has meaningful side-effects...
            EffectSet rhsEffects = TreeInfoManager.getEffects(tree.rhs);

            if (shouldCull(rhsEffects, live)) {
                log.info("Killing redundant assignment: {}", tree);
                mHasMadeAChange = true;
                result = null;
            }
        }
    }

    @Override
    public void visitVarDef(JCVariableDecl tree) {
        super.visitVarDef(tree);

        VarSymbol target = tree.sym;

        if (!everLive.contains(target)) {
            log.info("Culling assignment: {}", tree);
            mHasMadeAChange = true;
            result = treeMaker.Skip();
            return;
        }
        Set<Symbol> live = TreeInfoManager.getLiveVariables(tree);

        if (live != null && !live.contains(target)) {
            if (tree.init != null) {
                EffectSet rhsEffects = TreeInfoManager.getEffects(tree.init);
                if (!shouldCull(rhsEffects, live)) {
                    return;
                }
            }

            if (tree.init != null) {
                log.info("Killing redundant assignment: {}", tree);
                mHasMadeAChange = true;

                // So this *assignment* is redundant, but we need the decl. Just null out the init.
                tree.init = null;
            }
        }
    }
}
