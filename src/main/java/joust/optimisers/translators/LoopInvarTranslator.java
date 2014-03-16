package joust.optimisers.translators;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.List;
import joust.optimisers.avail.Avail;
import joust.optimisers.avail.normalisedexpressions.PossibleSymbol;
import joust.optimisers.avail.normalisedexpressions.PotentiallyAvailableExpression;
import joust.treeinfo.EffectSet;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static joust.tree.annotatedtree.AJCTree.*;
import static joust.utils.StaticCompilerUtils.treeCopier;

/**
 * Tree translator implementing loop-invariant code motion.
 */
@Log4j2
public class LoopInvarTranslator extends BaseTranslator {
    @Override
    public void visitMethodDef(AJCMethodDecl jcMethodDecl) {
        // Run on-demand available expression analysis...
        Avail a  = new Avail();
        //jcMethodDecl.accept(a);
        super.visitMethodDef(jcMethodDecl);
    }

    // TODO: Stop repeating yourself here!
    @Override
    public void visitDoWhileLoop(AJCDoWhileLoop doLoop) {
        log.debug("Invar for: {}", doLoop);
        // Extract statement list from loop body (Which is always a JCBlock).
        List<AJCStatement> loopBody = doLoop.body.stats;

        translateInvariants(doLoop, loopBody, doLoop.getEnclosingBlock());

        //log.info("Modified loop context: \n{}", visitedStack.peek());
        super.visitDoWhileLoop(doLoop);
    }

    @Override
    public void visitWhileLoop(AJCWhileLoop whileLoop) {
        log.debug("Invar for: {}", whileLoop);

        // Extract statement list from loop body.
        List<AJCStatement> loopBody = whileLoop.body.stats;

        translateInvariants(whileLoop, loopBody, whileLoop.getEnclosingBlock());

        //log.info("Modified loop context: \n{}", visitedStack.peek());
        super.visitWhileLoop(whileLoop);
    }

    @Override
    public void visitForLoop(AJCForLoop forLoop) {
        log.debug("Invar for: {}", forLoop);

        // Extract statement list from loop body.
        List<AJCStatement> loopBody = forLoop.body.stats;

        translateInvariants(forLoop, loopBody, forLoop.getEnclosingBlock());
        //log.info("Modified loop context: \n{}", visitedStack.peek());

        super.visitForLoop(forLoop);
    }

    /**
     * Get the last set of PotentiallyAvailableExpressions associated with statements in the given list.
     */
    private Set<PotentiallyAvailableExpression> getLastAvailableSet(List<AJCStatement> statements) {
        int bodyIndex = statements.length();
        Set<PotentiallyAvailableExpression> ret = null;
        // The expressions available on exit will be the expression set attached to the node furthest through the loop
        // body. Not all statements are so tagged, so we iterate backwards through the list until we find one.
        while (ret == null) {
            bodyIndex--;
            if (bodyIndex == -1) {
                return null;
            }

            ret = statements.get(bodyIndex).avail;
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
    private Set<PotentiallyAvailableExpression> findPotentialInvariants(AJCEffectAnnotatedTree loop, List<AJCStatement> loopBody) {
        Set<PotentiallyAvailableExpression> availableAtStart = loop.avail;

        // Find the expressions available at the last statement in the loop.
        Set<PotentiallyAvailableExpression> loopInvariants = getLastAvailableSet(loopBody);
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

            // Avoiding making a new temporary just for an ident or literal by itself...
            // TODO: Find a better way of representing these leaf nodes...
            if (pae.sourceNode instanceof AJCIdent || pae.sourceNode instanceof AJCLiteral) {
                paeIterator.remove();
            }
        }

        return loopInvariants;
    }

    private void translateInvariants(AJCStatement loopNode, List<AJCStatement> loopBody, AJCBlock enclosingBlock) {
        Set<PotentiallyAvailableExpression> loopInvariants = findPotentialInvariants(loopNode, loopBody);

        log.debug("Found invariants for loop:\n{}\nAs:\n{}", loopNode, loopInvariants);

        // Now look at each remaining invariant and move it outside the loop. Use InvarPAEComparator to detect all
        // points where the new temporary should be shoved.
        // TODO: PAEs with sourceNode == null currently must be dropped. Maybe set sourceNode to the AssignOp and do something smart?
        extractLoopInvariants(loopNode, loopInvariants, enclosingBlock);
    }

    /**
     * Given a loop node and a set of loop invariants for that node, move the loop invariants into the enclosing
     * block and mark each element from the loop body which has been moved for replacement.
     */
    private void extractLoopInvariants(AJCStatement loopNode, Set<PotentiallyAvailableExpression> loopInvariants, AJCBlock enclosingBlock) {
        PotentiallyAvailableExpression[] invarArray = loopInvariants.toArray(new PotentiallyAvailableExpression[loopInvariants.size()]);

        for (int i = 0; i < invarArray.length; i++) {
            PotentiallyAvailableExpression pae = invarArray[i];
            if (pae == null) {
                continue;
            }

            List<AJCStatement> newStatements = pae.concretify(enclosingBlock.enclosingMethod.getTargetSymbol());

            log.debug("Introducing new statements: {}", newStatements);

            // Put the new statement into the body of the block containing this loop...
            enclosingBlock.insertBefore(loopNode, newStatements);

            // Now replace references to expressions like this one with references to the newly-created variable.
            AJCExpressionTree tempRef = pae.expressionNode;

            for (int b = 0; b < invarArray.length; b++) {
                PotentiallyAvailableExpression candidate = invarArray[b];
                if (candidate == null) {
                    continue;
                }

                log.debug("Considering:\n{}{}", candidate, pae);
                if (pae.equals(candidate)) {
                    log.debug("Hit!");
                    // If the expressions are equivalent to the thing we just made a temp for, replace!
                    candidate.sourceNode.swapFor(treeCopier.copy(tempRef));

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
    private Set<PossibleSymbol> getKillSet(AJCEffectAnnotatedTree tree) {
        EffectSet treeEffects = tree.effects.getEffectSet();

        HashSet<PossibleSymbol> ret = new HashSet<>();

        // We care only about local variables for this step...
        if (!treeEffects.contains(EffectSet.EffectType.WRITE_INTERNAL)) {
            return ret;
        }
        Set<Symbol.VarSymbol> affectedSymbols = treeEffects.writeInternal;

        for (Symbol.VarSymbol sym : affectedSymbols) {
            ret.add(PossibleSymbol.getConcrete(sym));
        }

        return ret;
    }
}
