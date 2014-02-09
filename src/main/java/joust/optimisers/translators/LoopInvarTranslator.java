package joust.optimisers.translators;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import joust.optimisers.avail.Avail;
import joust.optimisers.avail.normalisedexpressions.PossibleSymbol;
import joust.optimisers.avail.normalisedexpressions.PotentiallyAvailableExpression;
import joust.optimisers.utils.JavacListUtils;
import joust.optimisers.visitors.KillSetVisitor;
import joust.optimisers.visitors.sideeffects.SideEffectVisitor;
import joust.treeinfo.TreeInfoManager;
import joust.utils.LogUtils;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static com.sun.tools.javac.tree.JCTree.*;

/**
 * Tree translator implementing loop-invariant code motion.
 */
public @Log4j2 class LoopInvarTranslator extends TODOTranslator {
    @Override
    public void visitMethodDef(JCMethodDecl jcMethodDecl) {
        // Run on-demand available expression analysis...
        Avail a  = new Avail();
        jcMethodDecl.accept(a);
        super.visitMethodDef(jcMethodDecl);

        if (mHasMadeAChange) {
            // If we touched anything, bring the effect annotations up to date.
            SideEffectVisitor effectVisitor = new SideEffectVisitor();
            jcMethodDecl.accept(effectVisitor);
            effectVisitor.finaliseIncompleteEffectSets();
        }
    }

    @Override
    public void visitDoLoop(JCDoWhileLoop jcDoWhileLoop) {
        log.debug("Invar for: {}", jcDoWhileLoop);
        // Extract statement list from loop body (Which is always a JCBlock).
        List<JCStatement> loopBody = ((JCBlock) jcDoWhileLoop.body).stats;

        translateInvariants(jcDoWhileLoop, loopBody);

        log.info("Modified loop context: \n{}", visitedStack.peek());
        super.visitDoLoop(jcDoWhileLoop);
    }

    @Override
    public void visitWhileLoop(JCWhileLoop jcWhileLoop) {
        log.debug("Invar for: {}", jcWhileLoop);

        // Extract statement list from loop body (Which is always a JCBlock).
        List<JCStatement> loopBody = ((JCBlock) jcWhileLoop.body).stats;

        translateInvariants(jcWhileLoop, loopBody);

        log.info("Modified loop context: \n{}", visitedStack.peek());
        super.visitWhileLoop(jcWhileLoop);
    }

    @Override
    public void visitForLoop(JCForLoop jcForLoop) {
        log.debug("Invar for: {}", jcForLoop);

        // Extract statement list from loop body (Which is always a JCBlock).
        List<JCStatement> loopBody = ((JCBlock) jcForLoop.body).stats;

        translateInvariants(jcForLoop, loopBody);
        log.info("Modified loop context: \n{}", visitedStack.peek());

        super.visitForLoop(jcForLoop);
    }

    @Override
    public void visitForeachLoop(JCEnhancedForLoop jcEnhancedForLoop) {
        super.visitForeachLoop(jcEnhancedForLoop);
        LogUtils.raiseCompilerError("Unexpected EnhancedForEachLoop encountered in LoopInvarTranslator. This should've been desugared by now!");
    }

    /**
     * Get the last set of PotentiallyAvailableExpressions associated with statements in the given list.
     */
    private HashSet<PotentiallyAvailableExpression> getLastAvailableSet(List<JCStatement> statements) {
        int bodyIndex = statements.length();
        HashSet<PotentiallyAvailableExpression> ret = null;
        // The expressions available on exit will be the expression set attached to the node furthest through the loop
        // body. Not all statements are so tagged, so we iterate backwards through the list until we find one.
        while (ret == null) {
            bodyIndex--;
            if (bodyIndex == -1) {
                return null;
            }

            ret = TreeInfoManager.getAvailable(statements.get(bodyIndex));
        }

        return ret;
    }

    /**
     * Find the loop invariant subexpressions for a given loop and its body. General function for any loop -
     * the specialised functions handle extracting the various fields from the actual loop object.
     *
     * @param loop The loop node to find invariants for.
     * @param loopBody The list of statements constituting the body of the given loop.
     * @return A set of PotentiallyAvailableExpressions that are found to be invariant over this loop.
     */
    private Set<PotentiallyAvailableExpression> findPotentialInvariants(JCTree loop, List<JCStatement> loopBody) {
        HashSet<PotentiallyAvailableExpression> availableAtStart = TreeInfoManager.getAvailable(loop);

        // Find the expressions available at the last statement in the loop.
        HashSet<PotentiallyAvailableExpression> loopInvariants = getLastAvailableSet(loopBody);
        if (loopInvariants == null) {
            loopInvariants = availableAtStart;
        }

        log.debug("Available before:\n{}", availableAtStart);
        log.debug("Available after:\n{}", loopInvariants);

        // Keep only things that are new within the loop. (These are the things which are being calculated in
        // the loop and which are available at the end, so are perhaps interesting to move out of the loop).
        loopInvariants.removeAll(availableAtStart);

        // Now throw away everything that is invalivated somewhere during the loop (Not an invariant)...
        // Determine the set of symbols the loop body invalidates at some point...
        Set<PossibleSymbol> bodyKillSet = getKillSet(loop);
        log.debug("Body kill set: {}", Arrays.toString(bodyKillSet.toArray()));

        removeKilled(loopInvariants, bodyKillSet);

        // Finally, throw away all the boring things we don't care about (idents and such).
        Iterator<PotentiallyAvailableExpression> paeIterator = loopInvariants.iterator();
        while (paeIterator.hasNext()) {
            PotentiallyAvailableExpression pae = paeIterator.next();

            // We don't care about JCIdents. They exist just to make our representation match JCTree.
            // We also don't care about JCLiterals.
            if (pae.sourceNode instanceof JCIdent || pae.sourceNode instanceof JCLiteral) {
                paeIterator.remove();
            }
        }

        return loopInvariants;
    }

    private void translateInvariants(JCTree loopNode, List<JCStatement> loopBody) {
        Set<PotentiallyAvailableExpression> loopInvariants = findPotentialInvariants(loopNode, loopBody);

        log.debug("Found invariants for loop:\n{}\nAs:\n{}", loopNode, loopInvariants);

        // Now look at each remaining invariant and move it outside the loop. Use InvarPAEComparator to detect all
        // points where the new temporary should be shoved.
        // TODO: PAEs with sourceNode == null currently must be dropped. Maybe set sourceNode to the AssignOp and do something smart?
        extractLoopInvariants(loopNode, loopInvariants);
    }

    /**
     * Given a loop node and a set of loop invariants for that node, move the loop invariants into the enclosing
     * block and mark each element from the loop body which has been moved for replacement.
     */
    private void extractLoopInvariants(JCTree loopNode, Set<PotentiallyAvailableExpression> loopInvariants) {
        PotentiallyAvailableExpression[] invarArray = loopInvariants.toArray(new PotentiallyAvailableExpression[loopInvariants.size()]);


        for (int i = 0; i < invarArray.length; i++) {
            PotentiallyAvailableExpression pae = invarArray[i];
            if (pae == null) {
                continue;
            }

            List<JCStatement> newStatements = pae.concretify(enclosingMethod.sym);

            log.debug("Introducing new statements: {}", newStatements);

            // Put the new statement into the body of the block containing this loop...
            insertIntoEnclosingBlock(loopNode, newStatements);

            // Now replace references to expressions like this one with references to the newly-created variable.
            JCExpression tempRef = pae.expressionNode;

            for (int b = 0; b < invarArray.length; b++) {
                PotentiallyAvailableExpression candidate = invarArray[b];
                if (candidate == null) {
                    continue;
                }

                log.debug("Considering:\n{}{}", candidate, pae);
                if (pae.equals(candidate)) {
                    log.debug("Hit!");
                    // If the expressions are equivalent to the thing we just made a temp for, mark them for replacement.
                    substitutions.put(candidate.sourceNode, tempRef);

                    invarArray[b] = null;
                }
            }
        }
    }

    /**
     * Remove everything from the candidate set that's invalidated by the provided kill set.
     */
    private void removeKilled(Set<PotentiallyAvailableExpression> candidates, Set<PossibleSymbol> killSet) {
        // Drop everything from the set that depends on something killed in the loop body.
        Iterator<PotentiallyAvailableExpression> i = candidates.iterator();
        while (i.hasNext()) {
            PotentiallyAvailableExpression pae = i.next();
            Set<PossibleSymbol> deps = new HashSet<>(pae.deps);
            deps.retainAll(killSet);

            if (!deps.isEmpty()) {
                log.debug("Dropping {} because {}", pae, Arrays.toString(deps.toArray()));
                i.remove();
            }
        }
    }

    /**
     * Get the set of PossibleSymbols invalidated by the given tree.
     */
    private Set<PossibleSymbol> getKillSet(JCTree tree) {
        KillSetVisitor killVisitor = new KillSetVisitor();
        tree.accept(killVisitor);
        return killVisitor.killSet;
    }
}
