package joust.optimisers.visitors.sideeffects;

import com.esotericsoftware.minlog.Log;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import joust.joustcache.data.MethodInfo;
import joust.optimisers.visitors.DepthFirstTreeVisitor;
import joust.treeinfo.EffectSet;
import joust.treeinfo.TreeInfo;
import joust.treeinfo.TreeInfoManager;
import joust.utils.SetHashMap;
import joust.utils.TreeUtils;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.treeinfo.EffectSet.*;
import static joust.Optimiser.methodTable;

public @Log4j2
class SideEffectVisitor extends DepthFirstTreeVisitor {
    // Maps MethodSymbols to the CandidateEffectSets of incomplete methods that depend on those symbols.
    private SetHashMap<MethodSymbol, CandidateEffectSet> incompleteMethods = new SetHashMap<>();

    // Mapping from MethodSymbols to the corresponding CandidateEffectSet for any unresolved methods.
    private HashMap<MethodSymbol, CandidateEffectSet> incompleteMethodIndex = new HashMap<>();

    // Track the method nodes which depend on incomplete methods so we can go back and fix them up when we complete
    // the method in question. Keyed by methods they need completed.
    private SetHashMap<MethodSymbol, JCTree> incompleteNodes = new SetHashMap<>();

    private HashMap<JCTree, CandidateEffectSet> effectSets = new HashMap<>();

    private void registerEffects(JCTree tree, CandidateEffectSet effects) {
        effectSets.put(tree, effects);

        log.debug("Register {} for {}", effects, tree);

        if (effects.needsEffectsFrom.isEmpty()) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("{} is waiting on effects from {}", tree, Arrays.toString(effects.needsEffectsFrom.toArray()));
        }

        // If necessary, register this as an incomplete node.
        for (MethodSymbol sym : effects.needsEffectsFrom) {
            incompleteNodes.listAdd(sym, tree);
        }
    }

    /**
     * Compute the union of the EffectSets of the given JCTree elements. Each individual tree must
     * have had its EffectSet registered previously.
     *
     * @param trees Trees to find the union EffectSet for.
     * @return An EffectSet representing the union of the effect sets of the given trees.
     */
    private CandidateEffectSet unionNodeEffects(List<? extends JCTree> trees) {
        final int numTrees = trees.size();
        if (numTrees == 0) {
            return CandidateEffectSet.NO_EFFECTS;
        }

        CandidateEffectSet effects = effectSets.get(trees.get(0));
        for (int i = 1; i < numTrees; i++) {
            effects = effects.union(effectSets.get(trees.get(i)));
        }

        return effects;
    }

    /**
     * Called by the user after every input tree has been passed through this visitor. After this method returns,
     * every node shall have an EffectSet. In the case that we have been unable to find more useful information about
     * a method, we mark it here as having every effect (If we have not done so already).
     * Prior to that, however, this step needs to resolve cyclic dependencies in the call graph - those aren't handled
     * as we went along, so potentially a great many of the incomplete entries at this point are caused by such cycles.
     *
     *
     */
    public void finaliseIncompleteEffectSets() {
        Log.info("Finalising effect sets.");
        // Build a set of all desired methods and all methods currently unresolved.
        // Desired methods that are not currently unresolved are not in the analysable set. If they got to
        // this point they weren't caught by JOUSTCache, either, so we're stuck.
        HashSet<MethodSymbol> affectedMethods = new HashSet<>();
        HashSet<MethodSymbol> unresolveableMethods = new HashSet<>();
        for (HashSet<CandidateEffectSet> sets : incompleteMethods.values()) {
            for (CandidateEffectSet cae : sets) {
                affectedMethods.add(cae.targetMethod);
                unresolveableMethods.addAll(cae.needsEffectsFrom);
            }
        }

        // Everything required but not in affectedMethods is unresolvable.
        unresolveableMethods.removeAll(affectedMethods);

        // Complete all unresolvable methods with the set of all side effects.
        for (MethodSymbol victim : unresolveableMethods) {
            log.warn("Unresolvable method effect set for: {}", victim);
            // Of course, no declaration to annotate directly in these cases - if there was it would be resolvable!
            methodCompleted(victim);
        }

        // Anything that survived that is part of a cycle.
        // Build a set of all incomplete sets.
        HashSet<CandidateEffectSet> incompleteSets = new HashSet<>();
        for (HashSet<CandidateEffectSet> sets : incompleteMethods.values()) {
            incompleteSets.addAll(sets);
        }

        log.debug("Incomplete sets:\n{}", Arrays.toString(incompleteSets.toArray()));

        // Take each incomplete set in turn and explore its inclusions, merging the partial results as you go.
        // When you reach a dependency on yourself, or a dependency on something already included, ignore it.
        // This procedure will resolve cycles in the call graph. Once a method completes, call methodCompleted -
        // hopefully much of the remaining work will recursively resolve.
        for (CandidateEffectSet set : incompleteSets) {
            if (!incompleteMethodIndex.containsKey(set.targetMethod)) {
                log.info("{} is no longer incomplete - skip.", set);
                continue;
            }
            log.debug("Incomplete: {}", Arrays.toString(incompleteMethodIndex.keySet().toArray()));
            log.debug("Resolving: {}", set);
            CandidateEffectSet resolved = explore(set, new HashSet<CandidateEffectSet>());

            log.debug("Resolved to: {}", resolved);

            TreeInfoManager.registerEffects(methodTable.get(MethodInfo.getHashForMethod(set.targetMethod)), resolved.concreteEffects);
            methodCompleted(set.targetMethod);
        }

        // Finally, register all the EffectSets. Panic if any remain unresolved.
        // TODO: Suitable datastructure-fu will allow the omission of this copying step.
        for (JCTree tree : effectSets.keySet()) {
            CandidateEffectSet cand = effectSets.get(tree);
            if (!cand.needsEffectsFrom.isEmpty()) {
                log.error("Unresolved CandidateEffectSet: {}. Panic.", cand);
                TreeInfoManager.registerEffects(tree, EffectSet.ALL_EFFECTS);
            } else {
                TreeInfoManager.registerEffects(tree, cand.concreteEffects);
            }
        }
        effectSets.clear();
    }

    private CandidateEffectSet explore(CandidateEffectSet set, Set<CandidateEffectSet> visited) {
        visited.add(set);
        CandidateEffectSet resolved = set;

        log.trace("Exploring: {}", set);

        for (MethodSymbol dep : set.needsEffectsFrom) {
            // Get the CandidateEffectSet for this dependency. If there is no such set, panic.
            CandidateEffectSet depSet = incompleteMethodIndex.get(dep);
            if (depSet == null) {
                log.error("Unable to find CandidateEffectSet for {}! Panic!", dep);
                // Attempt to recover by returning ALL_EFFECTS.
                resolved = resolved.union(EffectSet.ALL_EFFECTS);
                break;
            }

            if (visited.contains(depSet)) {
                log.trace("Already considered: {}", depSet);
                // We've seen this one before, so we don't care about it again.
                continue;
            }

            resolved = resolved.union(explore(depSet, visited), true);
            log.trace("Final visited: {}", Arrays.toString(visited.toArray()));
        }

        // Drop merged sets from the returned dependency list. If the ensuing list is not the empty set - panic.
        for (CandidateEffectSet visitedSet : visited) {
            resolved.needsEffectsFrom.remove(visitedSet.targetMethod);
        }

        if (!resolved.needsEffectsFrom.isEmpty()) {
            log.error("Nonempty resolved dependency set for: {}. PANIC.", resolved);
            resolved = resolved.union(EffectSet.ALL_EFFECTS);
        }

        return resolved;
    }

    /**
     * Called when the effect set for the given method has been completed. Update all work-in-progress methods
     * and complete any further methods/calls where possible.
     * @param sym MethodSymbol for which a complete EffectSet is now available.
     */
    private void methodCompleted(MethodSymbol sym) {
        incompleteMethodIndex.remove(sym);

        EffectSet victory = TreeInfoManager.getEffectsForMethod(sym);

        log.debug("Completing: {} with {}", sym, victory);

        // Get the set of candidate effect sets that depend on the method we just completed.
        Set<CandidateEffectSet> dependees = incompleteMethods.get(sym);
        if (dependees != null) {
            // Copy the set to prevent ConcurrentModificationExceptions.
            Set<CandidateEffectSet> dependenciesCopy = new HashSet<>(dependees);
            for (CandidateEffectSet wip : dependenciesCopy) {
                // For each CES, add the effects from the completed method to it and remove the dependency
                // on this method. If this action completes the method, recurse!
                log.trace("Affects candidate set: {}", wip);
                wip.concreteEffects = wip.concreteEffects.unionEscaping(victory);
                log.trace("Now has: {}", wip.concreteEffects);

                wip.needsEffectsFrom.remove(sym);
                incompleteMethods.listRemove(sym, wip);

                if (wip.needsEffectsFrom.isEmpty()) {
                    methodCompleted(sym);
                }
            }
        }

        // Advance nodes that relied on this method being completed...
        Set<JCTree> incompleted = incompleteNodes.get(sym);
        if (incompleted == null) {
            return;
        }

        for (JCTree node : incompleted) {
            CandidateEffectSet candidate = effectSets.get(node);
            candidate.concreteEffects = candidate.concreteEffects.unionEscaping(victory);
            candidate.needsEffectsFrom.remove(sym);
        }

        incompleteNodes.remove(sym);
    }

    @Override
    public void visitMethodDef(JCMethodDecl that) {
        super.visitMethodDef(that);

        if ((that.sym.flags() & Flags.ABSTRACT) != 0) {
            log.warn("Panic: Abstract method encountered. How to handle the effects?!");
            // TODO: Funky system for resolving polymorphic calls to the superset of all possible
            // call targets. For now, though, Universe!
            TreeInfoManager.registerEffects(that, EffectSet.ALL_EFFECTS);
            return;
        }

        List<JCExpression> thrownExceptions = that.thrown;
        for (JCExpression e : thrownExceptions) {
            log.debug("Thrown: {} : {}", e, e.getClass());
        }

        CandidateEffectSet effects = effectSets.get(that.body);
        effects.targetMethod = that.sym;
        if (!thrownExceptions.isEmpty()) {
            effects.concreteEffects = effects.concreteEffects.union(EffectType.EXCEPTION);
        }

        // If there are no unresolved deps for this method, register the final effect set and complete the method.
        if (effects.needsEffectsFrom.isEmpty()) {
            TreeInfoManager.registerEffects(that, effects.concreteEffects);
            methodCompleted(that.sym);
        } else {
            // Put a reference from each unresolved dependency to this candidate effect set.
            for (MethodSymbol dep : effects.needsEffectsFrom) {
                incompleteMethods.listAdd(dep, effects);
            }

            // And add this incomplete method to the index.
            incompleteMethodIndex.put(that.sym, effects);
        }
    }

    @Override
    public void visitSkip(JCSkip that) {
        super.visitSkip(that);

        registerEffects(that, CandidateEffectSet.NO_EFFECTS);
    }

    @Override
    public void visitDoLoop(JCDoWhileLoop that) {
        super.visitDoLoop(that);

        // The effect set of a do-while loop is the union of its condition with its body.
        CandidateEffectSet condEffects = effectSets.get(that.cond);
        CandidateEffectSet bodyEffects = effectSets.get(that.body);

        registerEffects(that, condEffects.union(bodyEffects));
    }

    @Override
    public void visitBlock(JCBlock that) {
        super.visitBlock(that);

        registerEffects(that, unionNodeEffects(that.stats));
    }

    @Override
    public void visitWhileLoop(JCWhileLoop that) {
        super.visitWhileLoop(that);

        // The effect set of a while loop is the union of its condition with its body.
        CandidateEffectSet condEffects = effectSets.get(that.cond);
        CandidateEffectSet bodyEffects = effectSets.get(that.body);

        registerEffects(that, condEffects.union(bodyEffects));
    }

    @Override
    public void visitForLoop(JCForLoop that) {
        super.visitForLoop(that);

        // The effect set of a while loop is the union of its condition with its body.
        CandidateEffectSet initEffects = unionNodeEffects(that.init);
        CandidateEffectSet stepEffects = unionNodeEffects(that.step);
        CandidateEffectSet condEffects = effectSets.get(that.cond);
        CandidateEffectSet bodyEffects = effectSets.get(that.body);

        registerEffects(that, condEffects.union(bodyEffects, initEffects, stepEffects));
    }

    @Override
    public void visitForeachLoop(JCEnhancedForLoop that) {
        super.visitForeachLoop(that);

        CandidateEffectSet exprEffects = effectSets.get(that.body);
        CandidateEffectSet bodyEffects = effectSets.get(that.expr);

        registerEffects(that, exprEffects.union(bodyEffects));
    }

    @Override
    public void visitLabelled(JCLabeledStatement that) {
        super.visitLabelled(that);

        registerEffects(that, effectSets.get(that.body));
    }

    @Override
    public void visitSwitch(JCSwitch that) {
        super.visitSwitch(that);

        CandidateEffectSet condEffects = effectSets.get(that.selector);
        CandidateEffectSet caseEffects = unionNodeEffects(that.cases);

        registerEffects(that, condEffects.union(caseEffects));
    }

    @Override
    public void visitCase(JCCase that) {
        super.visitCase(that);

        CandidateEffectSet bodyEffects = unionNodeEffects(that.stats);

        // The default case...
        if (that.pat == null) {
            registerEffects(that, bodyEffects);
        } else {
            CandidateEffectSet condEffects = effectSets.get(that.pat);
            registerEffects(that, condEffects.union(bodyEffects));
        }
    }

    @Override
    public void visitSynchronized(JCSynchronized that) {
        super.visitSynchronized(that);

        CandidateEffectSet lockEffects = effectSets.get(that.lock);
        CandidateEffectSet bodyEffects = effectSets.get(that.body);

        registerEffects(that, lockEffects.union(bodyEffects));
    }

    @Override
    public void visitTry(JCTry that) {
        super.visitTry(that);

        // Union ALL the things.
        CandidateEffectSet bodyEffects = effectSets.get(that.body);
        CandidateEffectSet catcherEffects = unionNodeEffects(that.catchers);
        CandidateEffectSet finalizerEffects = effectSets.get(that.finalizer);
        CandidateEffectSet resourceEffects = unionNodeEffects(that.resources);

        registerEffects(that, bodyEffects.union(catcherEffects, finalizerEffects, resourceEffects));
    }

    @Override
    public void visitCatch(JCCatch that) {
        super.visitCatch(that);
        registerEffects(that, effectSets.get(that.body));
    }

    @Override
    public void visitConditional(JCConditional that) {
        super.visitConditional(that);

        CandidateEffectSet condEffects = effectSets.get(that.cond);
        CandidateEffectSet trueEffects = effectSets.get(that.truepart);
        CandidateEffectSet falseEffects = effectSets.get(that.falsepart);

        registerEffects(that, condEffects.union(trueEffects, falseEffects));
    }

    @Override
    public void visitIf(JCIf that) {
        super.visitIf(that);

        CandidateEffectSet condEffects = effectSets.get(that.cond);
        CandidateEffectSet trueEffects = effectSets.get(that.thenpart);
        if (that.elsepart != null) {
            CandidateEffectSet falseEffects = effectSets.get(that.elsepart);
            registerEffects(that, condEffects.union(trueEffects, falseEffects));
        }  else {
            registerEffects(that, condEffects.union(trueEffects));
        }
    }

    @Override
    public void visitExec(JCExpressionStatement that) {
        super.visitExec(that);

        registerEffects(that, effectSets.get(that.expr));
    }

    @Override
    public void visitBreak(JCBreak that) {
        super.visitBreak(that);

        registerEffects(that, CandidateEffectSet.NO_EFFECTS);
    }

    @Override
    public void visitContinue(JCContinue that) {
        super.visitContinue(that);

        registerEffects(that, CandidateEffectSet.NO_EFFECTS);
    }

    @Override
    public void visitReturn(JCReturn that) {
        super.visitReturn(that);

        CandidateEffectSet returneeEffects = effectSets.get(that.expr).union(CandidateEffectSet.NO_EFFECTS);

        registerEffects(that, returneeEffects);
    }

    @Override
    public void visitThrow(JCThrow that) {
        super.visitThrow(that);

        // Unioning for copy...
        CandidateEffectSet exprEffects = effectSets.get(that.expr).union(CandidateEffectSet.NO_EFFECTS);
        exprEffects.concreteEffects = exprEffects.concreteEffects.union(new EffectSet(EffectType.EXCEPTION));

        registerEffects(that, exprEffects);
    }

    @Override
    public void visitAssert(JCAssert that) {
        super.visitAssert(that);

        // Hopefully the empty set...
        CandidateEffectSet condEffects = effectSets.get(that.cond).union(CandidateEffectSet.NO_EFFECTS);
        condEffects.concreteEffects = condEffects.concreteEffects.union(new EffectSet(EffectType.EXCEPTION));

        registerEffects(that, condEffects);
    }

    /**
     * Get the CandidateEffectSet for a particular call (constructor or regular) given args and a MethodSymbol.
     */
    private CandidateEffectSet getCallEffects(List<JCExpression> args, MethodSymbol methodSym) {
        // The effects of the arguments to the function.
        CandidateEffectSet callEffects = unionNodeEffects(args);

        // Determine if we have final effects for the method being called yet (From the cache or from earlier analysis.
        EffectSet methodEffects = TreeInfoManager.getEffectsForMethod(methodSym);
        if (methodEffects == EffectSet.ALL_EFFECTS) {
            // We found none - add the dependancy and hope we manage to resolve it later...
            log.info("No effects for {} found. Adding dep.", methodSym);

            callEffects = callEffects.alsoRequires(methodSym);
        } else {
            // We found some! Union them and continue with joy.
            callEffects = callEffects.union(methodEffects);
        }

        return callEffects;
    }

    @Override
    public void visitApply(JCMethodInvocation that) {
        super.visitApply(that);

        MethodSymbol methodSym = TreeUtils.getTargetSymbolForCall(that);

        registerEffects(that, getCallEffects(that.args, methodSym));
    }

    @Override
    public void visitNewClass(JCNewClass that) {
        super.visitNewClass(that);

        registerEffects(that, getCallEffects(that.args, (MethodSymbol) that.constructor));
    }

    @Override
    public void visitNewArray(JCNewArray that) {
        super.visitNewArray(that);

        // A new array operation *by itself* has no side effects (Neglecting ever-present possibilities
        // such as OutOfMemoryException. It is the corresponding assignment (If any exists) which
        // has a side effect.
        // Of course, the argument to the new array call might have side effects, as might the
        // elements of the array (If given explicitly).
        CandidateEffectSet elementEffects = unionNodeEffects(that.elems);
        CandidateEffectSet dimensionEffects = unionNodeEffects(that.dims);

        registerEffects(that, elementEffects.union(dimensionEffects));
    }

    @Override
    public void visitParens(JCParens that) {
        super.visitParens(that);

        registerEffects(that, effectSets.get(that.expr));
    }

    @Override
    public void visitAssign(JCAssign that) {
        super.visitAssign(that);

        CandidateEffectSet rhsEffects = effectSets.get(that.rhs);

        VarSymbol varSym = TreeUtils.getTargetSymbolForAssignment(that);

        registerEffects(that, rhsEffects.union(EffectSet.write(varSym)));
    }

    @Override
    public void visitAssignop(JCAssignOp that) {
        super.visitAssignop(that);

        // Assignment with an operator, such as +=. Same side effect profile as visitAssign?
        CandidateEffectSet rhsEffects = effectSets.get(that.rhs);

        VarSymbol varSym = TreeUtils.getTargetSymbolForAssignment(that);

        registerEffects(that, rhsEffects.union(EffectSet.write(varSym).union(EffectSet.read(varSym))));
    }

    @Override
    public void visitUnary(JCUnary that) {
        super.visitUnary(that);

        CandidateEffectSet argEffects = effectSets.get(that.arg);

        // If the operation is a ++ or --, it has write side effects on the target. Otherwise, it has
        // no side effects.
        final Tag nodeTag = that.getTag();
        if (nodeTag == Tag.PREINC
         || nodeTag == Tag.PREDEC
         || nodeTag == Tag.POSTINC
         || nodeTag == Tag.POSTDEC) {
            VarSymbol varSym = null;

            // It is irritating how little javac uses its own type system...
            if (that.arg instanceof JCIdent) {
                varSym = (VarSymbol) ((JCIdent) that.arg).sym;
            } else if (that.arg instanceof JCFieldAccess) {
                varSym = (VarSymbol) ((JCFieldAccess) that.arg).sym;
            }

            registerEffects(that, CandidateEffectSet.wrap(EffectSet.write(varSym).union(EffectSet.read(varSym))));
        } else {
            registerEffects(that, argEffects);
        }
    }

    @Override
    public void visitBinary(JCBinary that) {
        super.visitBinary(that);

        CandidateEffectSet lhsEffects = effectSets.get(that.lhs);
        CandidateEffectSet rhsEffects = effectSets.get(that.rhs);

        registerEffects(that, lhsEffects.union(rhsEffects));
    }

    @Override
    public void visitIndexed(JCArrayAccess that) {
        super.visitIndexed(that);

        CandidateEffectSet nodeEffects = effectSets.get(that.index);

        registerEffects(that, nodeEffects.union(EffectSet.read(TreeUtils.getTargetSymbolForExpression(that.indexed))));
    }

    @Override
    public void visitSelect(JCFieldAccess that) {
        super.visitSelect(that);

        CandidateEffectSet argEffects = effectSets.get(that.selected);

        if (that.selected instanceof JCIdent) {
            registerEffects(that, argEffects.union(EffectSet.read(TreeUtils.getTargetSymbolForExpression(that.selected))));
        }
    }

    @Override
    public void visitIdent(JCIdent that) {
        super.visitIdent(that);

        Symbol sym = that.sym;

        if (sym instanceof VarSymbol) {
            registerEffects(that, CandidateEffectSet.wrap(EffectSet.read((VarSymbol) sym)));

            return;
        }

        registerEffects(that, CandidateEffectSet.NO_EFFECTS);
    }

    @Override
    public void visitLiteral(JCLiteral that) {
        super.visitLiteral(that);
        registerEffects(that, CandidateEffectSet.NO_EFFECTS);
    }

    @Override
    public void visitErroneous(JCErroneous that) {
        super.visitErroneous(that);

        log.error("Encountered errorneous tree: {}", that);
        registerEffects(that, CandidateEffectSet.wrap(EffectSet.ALL_EFFECTS));
    }

    @Override
    public void visitLetExpr(LetExpr that) {
        super.visitLetExpr(that);

        // Hopefully the empty set...
        CandidateEffectSet defEffects = unionNodeEffects(that.defs);
        CandidateEffectSet condEffects = effectSets.get(that.expr);

        registerEffects(that, defEffects.union(condEffects));
    }

    @Override
    public void visitTypeCast(JCTypeCast jcTypeCast) {
        super.visitTypeCast(jcTypeCast);
        registerEffects(jcTypeCast, CandidateEffectSet.NO_EFFECTS);
    }

    @Override
    public void visitTypeTest(JCInstanceOf jcInstanceOf) {
        super.visitTypeTest(jcInstanceOf);
        registerEffects(jcInstanceOf, CandidateEffectSet.NO_EFFECTS);
    }

    @Override
    public void visitReference(JCMemberReference jcMemberReference) {
        super.visitReference(jcMemberReference);
        Symbol referenced = jcMemberReference.sym;

        CandidateEffectSet exprEffects = effectSets.get(jcMemberReference.expr);

        if (referenced instanceof VarSymbol) {
            registerEffects(jcMemberReference, exprEffects.union(EffectSet.read((VarSymbol) referenced)));
        } else {
            // It could be a reference to a method, for example. These have no side-effects. (But the
            // associated call might.)
            registerEffects(jcMemberReference, exprEffects.union(CandidateEffectSet.NO_EFFECTS));
        }
    }

    @Override
    public void visitClassDef(JCClassDecl jcClassDecl) {
        super.visitClassDef(jcClassDecl);
        registerEffects(jcClassDecl, CandidateEffectSet.NO_EFFECTS);
    }

    @Override
    public void visitVarDef(JCVariableDecl jcVariableDecl) {
        super.visitVarDef(jcVariableDecl);
        CandidateEffectSet initEffects;

        if (jcVariableDecl.init != null) {
            initEffects = effectSets.get(jcVariableDecl.init);
        } else {
            initEffects = CandidateEffectSet.NO_EFFECTS;
        }

        registerEffects(jcVariableDecl, initEffects.union(EffectSet.write(jcVariableDecl.sym)));
    }

    @Override
    public void visitTypeIdent(JCPrimitiveTypeTree jcPrimitiveTypeTree) {
        super.visitTypeIdent(jcPrimitiveTypeTree);
        registerEffects(jcPrimitiveTypeTree, CandidateEffectSet.NO_EFFECTS);
    }

    @Override
    public void visitTypeArray(JCArrayTypeTree jcArrayTypeTree) {
        super.visitTypeArray(jcArrayTypeTree);
        registerEffects(jcArrayTypeTree, CandidateEffectSet.NO_EFFECTS);
    }

    @Override
    public void visitTypeApply(JCTypeApply jcTypeApply) {
        super.visitTypeApply(jcTypeApply);
        registerEffects(jcTypeApply, CandidateEffectSet.NO_EFFECTS);
    }

    @Override
    public void visitTypeUnion(JCTypeUnion jcTypeUnion) {
        super.visitTypeUnion(jcTypeUnion);
        registerEffects(jcTypeUnion, CandidateEffectSet.NO_EFFECTS);
    }

    @Override
    public void visitTypeIntersection(JCTypeIntersection jcTypeIntersection) {
        super.visitTypeIntersection(jcTypeIntersection);
        registerEffects(jcTypeIntersection, CandidateEffectSet.NO_EFFECTS);
    }

    @Override
    public void visitTypeParameter(JCTypeParameter jcTypeParameter) {
        super.visitTypeParameter(jcTypeParameter);
        registerEffects(jcTypeParameter, CandidateEffectSet.NO_EFFECTS);
    }

    @Override
    public void visitTypeBoundKind(TypeBoundKind typeBoundKind) {
        super.visitTypeBoundKind(typeBoundKind);
        registerEffects(typeBoundKind, CandidateEffectSet.NO_EFFECTS);
    }

    @Override
    public void visitWildcard(JCWildcard jcWildcard) {
        super.visitWildcard(jcWildcard);
        registerEffects(jcWildcard, CandidateEffectSet.NO_EFFECTS);
    }

    @Override
    public void visitAnnotation(JCAnnotation jcAnnotation) {
        super.visitAnnotation(jcAnnotation);
        registerEffects(jcAnnotation, CandidateEffectSet.NO_EFFECTS);
    }
}
