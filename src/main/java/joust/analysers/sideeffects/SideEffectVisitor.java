package joust.analysers.sideeffects;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.List;
import joust.joustcache.JOUSTCache;
import joust.joustcache.data.ClassInfo;
import joust.tree.annotatedtree.AJCForest;
import joust.tree.annotatedtree.AJCTreeVisitor;
import joust.tree.annotatedtree.treeinfo.EffectSet;
import joust.tree.annotatedtree.treeinfo.TreeInfoManager;
import joust.utils.data.SymbolSet;
import joust.utils.logging.LogUtils;
import joust.utils.data.SetHashMap;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.tree.annotatedtree.treeinfo.EffectSet.*;
import static joust.utils.compiler.StaticCompilerUtils.types;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class SideEffectVisitor extends AJCTreeVisitor {
    private boolean saveResults;

    // Track the method calls which depend on incomplete methods so we can go back and fix them up when we complete
    // the method in question. Keyed by MethodSymbol of the incomplete method.
    private final SetHashMap<MethodSymbol, AJCEffectAnnotatedTree> incompleteCalls = new SetHashMap<>();

    // The incomplete Effects objects for methods that have unresolved deps.
    private final HashMap<MethodSymbol, Effects> unfinishedMethodEffects = new HashMap<>();

    private final HashSet<MethodSymbol> calledMethodsWithoutSource = new HashSet<>();

    // The set of methods each method depends upon.
    private final SetHashMap<MethodSymbol, MethodSymbol> methodDeps = new SetHashMap<>();

    // The set of methods depended on by each method.
    private final SetHashMap<MethodSymbol, MethodSymbol> reverseMethodDeps = new SetHashMap<>();

    // The set of methods with a particular name - the set we check to see if an inheritance exists whenever
    // we see a new symbol.
    private final SetHashMap<String, MethodSymbol> possibleInheitors = new SetHashMap<>();

    private MethodSymbol methodBeingVisited;

    public SideEffectVisitor(boolean shouldSave) {
        saveResults = shouldSave;
    }

    @Override
    public void visitMethodDef(AJCMethodDecl that) {
        MethodSymbol sym = that.getTargetSymbol();
        methodBeingVisited = sym;

        log.debug("Visiting method: {}:{}", sym, sym.owner);

        // Create the sets of filtered methods that might inherit from each other.
        // Constructors are omitted, since calls to super() or this() are explicit in the AST of all constructors
        // at this point, so the proper deps will already be present.
        if (!"<init>".equals(sym.toString())) {
            possibleInheitors.listAdd(sym.toString(), sym);
        }

        // Bail out early for abstracts - they just inherit the effects of their implementations, provided
        // they have one.
        if ((sym.flags() & Flags.ABSTRACT) != 0) {
            Effects placeholderEffects = new Effects(NO_EFFECTS, NO_EFFECTS);

            unfinishedMethodEffects.put(sym, placeholderEffects);

            return;
        }

        if ((sym.flags() & Flags.NATIVE) != 0) {
            // Ha. No.
            unfinishedMethodEffects.put(sym, new Effects(ALL_EFFECTS, ALL_EFFECTS));
            methodDeps.ensure(sym);
            return;
        }

        super.visitMethodDef(that);

        unfinishedMethodEffects.put(sym, that.body.effects);
        methodDeps.ensure(sym);
    }

    /**
     * Check all possible overrides for this method and add dependenies as needed.
     */
    private void ensureInheritorLinks() {
        log.debug("Resolving polymorphic effect dependencies...");
        Set<String> inheritorKeys = possibleInheitors.keySet();
        for (String symName : inheritorKeys) {
            Set<MethodSymbol> inheritorSet = possibleInheitors.get(symName);

            // Nothing to do...
            if (inheritorSet.size() < 2) {
                continue;
            }

            log.debug("Checking inheritor links for {}", symName);
            log.debug("Inheritor set: {}", Arrays.toString(inheritorSet.toArray()));

            for (MethodSymbol s : inheritorSet) {
                for (MethodSymbol s2 : inheritorSet) {
                    if (s == s2) {
                        continue;
                    }

                    if (s.overrides(s2, (TypeSymbol) s.owner, types, true)) {
                        log.debug("{}:{} overrides {}:{}!", s, s.owner, s2, s2.owner);

                        Effects e1 = unfinishedMethodEffects.get(s);
                        Effects e2 = unfinishedMethodEffects.get(s2);

                        // s overrides s2, so create a dependency link from s2 to s.
                        e2.deps.add(e1);
                        e1.dependantOnThis.add(e2);

                        // And mark it for resolution...
                        methodDeps.listAdd(s2, s);
                        reverseMethodDeps.listAdd(s, s2);
                    }
                }
            }
        }
    }

    /**
     * After visiting all input nodes, finalise the initial effect tree, ready to handle future incremental
     * updates.
     * It is at this step that cyclic dependencies need to be explicitly resolved.
     */
    public void bootstrap() {
        // Create the missing dependencies, due to polymorphism, between methods...
        ensureInheritorLinks();

        log.debug("Commencing effect dependency resolution.");
        // Ask the cache for information on all called methods that aren't in the method table. These
        // can be resolved right away.
        Set<MethodSymbol> knownSymbols = AJCForest.getInstance().methodTable.keySet();

        log.debug("Keyset: {}", Arrays.toString(methodDeps.keySet().toArray()));
        log.debug("mTable Keyset: {}", Arrays.toString(AJCForest.getInstance().methodTable.keySet().toArray()));
        calledMethodsWithoutSource.removeAll(knownSymbols);
        log.debug("calledCopy: {}", Arrays.toString(calledMethodsWithoutSource.toArray()));

        if (saveResults) {
            for (MethodSymbol mSym : calledMethodsWithoutSource) {
                log.debug("Loading effects for {} from cache... (Owner is {})", mSym, mSym.enclClass());
                JOUSTCache.loadCachedInfoForClass(mSym.enclClass());
            }
        }

        // Now everything we need is loaded, let's start completing methods. Firstly the easy ones...
        for (MethodSymbol mSym : calledMethodsWithoutSource) {
            methodCompleted(mSym, TreeInfoManager.getEffectsForMethod(mSym));
        }

        // Dump the deps...
        for (MethodSymbol sym : methodDeps.keySet()) {
            log.debug("{}:{} depends on: {}", sym, sym.owner, Arrays.toString(methodDeps.get(sym).toArray()));
        }

        for (MethodSymbol sym : reverseMethodDeps.keySet()) {
            log.debug("{}:{} is required by: {}", sym, sym.owner, Arrays.toString(reverseMethodDeps.get(sym).toArray()));
        }

        log.debug("Unfinished: {}", Arrays.toString(unfinishedMethodEffects.keySet().toArray()));

        // As long as there are no cycles in the graph, this will resolve absolutely everything.
        while (resolveMethodsWithoutDeps());

        if (methodDeps.keySet().isEmpty()) {
            return;
        }

        log.debug("After simple resolution have these symbols outstanding: {}", Arrays.toString(methodDeps.keySet().toArray()));

        // Set up unresolved call deps..
        for (MethodSymbol sym : incompleteCalls.keySet()) {
            Set<AJCEffectAnnotatedTree> callsForSym = incompleteCalls.get(sym);
            for (AJCEffectAnnotatedTree tree : callsForSym) {
                Effects callEffects = tree.effects;
                Effects targetEffects = unfinishedMethodEffects.get(sym);

                if (targetEffects == null) {
                    continue;
                }

                callEffects.deps.add(targetEffects);
                targetEffects.dependantOnThis.add(callEffects);
            }
        }

        Set<MethodSymbol> missing = new HashSet<>(methodDeps.keySet());

        // Resolve cycles!
        for (MethodSymbol sym : missing) {
            // Handle concurrent modification.
            if (!methodDeps.containsKey(sym)) {
                continue;
            }

            log.debug("Cycle breaking for: {}", missing);
            Effects missingEff = unfinishedMethodEffects.get(sym);
            log.debug("Before: {}", missingEff);
            missingEff.rebuildFromChildren();
            log.debug("After: {}", missingEff);
            methodCompleted(sym, missingEff);
        }
    }

    /**
     * Find all methods that have no outstanding deps and complete them.
     * @return true if any methods were completed.
     */
    private boolean resolveMethodsWithoutDeps() {
        boolean success = false;

        Set<MethodSymbol> keyCopy = new HashSet<>(methodDeps.keySet());

        for (MethodSymbol sym : keyCopy) {
            // Concurrent modification may occur...
            if (!methodDeps.containsKey(sym)) {
                continue;
            }

            log.debug("{}:{} has deps: {}", sym, sym.owner, methodDeps.get(sym));
            if (methodDeps.get(sym).isEmpty()) {
                methodCompleted(sym, unfinishedMethodEffects.get(sym));
                unfinishedMethodEffects.remove(sym);
                success = true;
            }
        }

        return success;
    }

    /**
     * Called when the effect set for a method becomes available.
     * Used to update all incomplete nodes that rely on this method.
     */
    private void methodCompleted(MethodSymbol completedSym, Effects effects) {
        log.debug("{}:{} completed with {}", completedSym, completedSym.owner, effects);
        methodDeps.remove(completedSym);
        TreeInfoManager.registerMethodEffects(completedSym, effects, saveResults);
        unfinishedMethodEffects.remove(completedSym);

        Set<AJCEffectAnnotatedTree> incompleted = incompleteCalls.get(completedSym);
        if (incompleted != null) {
            for (AJCEffectAnnotatedTree t : incompleted) {
                Effects tEffects = t.effects;
                EffectSet newEffectSet = tEffects.effectSet.union(effects.effectSet);

                // Update dependencies...
                tEffects.deps.add(effects);
                effects.dependantOnThis.add(tEffects);

                log.debug("Setting effects on: {} to: {}", t, newEffectSet);
                tEffects.setEffectSet(newEffectSet);
            }

            incompleteCalls.remove(completedSym);
        }

        // Determine if this completes any methods. If so, recurse!
        Set<MethodSymbol> requireThis = reverseMethodDeps.get(completedSym);
        if (requireThis != null) {
            log.debug("Reverse deps: {}", Arrays.toString(requireThis.toArray()));
            for (MethodSymbol t : requireThis) {
                Set<MethodSymbol> thingsRequiredByT = methodDeps.get(t);
                if (thingsRequiredByT == null) {
                    continue;
                }
                log.debug("t: {}, Required by t: {}", t, thingsRequiredByT);
                thingsRequiredByT.remove(completedSym);

                if (thingsRequiredByT.isEmpty()) {
                    log.debug("Cascading completion of: {}", t, t.owner);
                    methodCompleted(t, unfinishedMethodEffects.get(t));
                    methodDeps.remove(t);
                }
            }

            reverseMethodDeps.remove(completedSym);
        } else {
            log.debug("Reverse deps: []");
        }
    }

    @Override
    public void visitSkip(AJCSkip that) {
        super.visitSkip(that);
        that.effects = new Effects(NO_EFFECTS);
    }

    @Override
    public void visitDoWhileLoop(AJCDoWhileLoop that) {
        super.visitDoWhileLoop(that);

        // The effect set of a do-while loop is the union of its condition with its body.
        that.effects = Effects.unionTrees(that.cond, that.body);
    }

    @Override
    public void visitBlock(AJCBlock that) {
        super.visitBlock(that);

        that.effects = Effects.unionTrees(that.stats);
    }

    @Override
    public void visitWhileLoop(AJCWhileLoop that) {
        super.visitWhileLoop(that);

        // The effect set of a while loop is the union of its condition with its body.
        that.effects = Effects.unionTrees(that.cond, that.body);
    }

    @Override
    public void visitForLoop(AJCForLoop that) {
        super.visitForLoop(that);

        // The effect set of a while loop is the union of its condition with its body.
        Effects initEffects = Effects.unionTrees(that.init);
        Effects stepEffects = Effects.unionTrees(that.step);

        that.effects = Effects.unionOf(initEffects, stepEffects, Effects.unionTrees(that.cond, that.body));
    }

    @Override
    public void visitLabelledStatement(AJCLabeledStatement that) {
        super.visitLabelledStatement(that);

        that.effects = Effects.unionOf(that.body.effects);
    }

    @Override
    public void visitSwitch(AJCSwitch that) {
        super.visitSwitch(that);

        Effects caseEffects = Effects.unionTrees(that.cases);
        that.effects = Effects.unionOf(caseEffects, that.selector.effects);
    }

    @Override
    public void visitCase(AJCCase that) {
        super.visitCase(that);

        Effects bodyEffects = Effects.unionTrees(that.stats);
        that.effects = Effects.unionOf(bodyEffects, that.pat.effects);
    }

    @Override
    public void visitSynchronized(AJCSynchronized that) {
        super.visitSynchronized(that);

        that.effects = Effects.unionTrees(that.lock, that.body);
    }

    @Override
    public void visitTry(AJCTry that) {
        super.visitTry(that);

        Effects catcherEffects = Effects.unionTrees(that.catchers);
        that.effects = Effects.unionOf(catcherEffects, that.body.effects, that.finalizer.effects);
    }

    @Override
    public void visitCatch(AJCCatch that) {
        super.visitCatch(that);

        that.effects = Effects.unionOf(that.body.effects);
    }

    @Override
    public void visitConditional(AJCConditional that) {
        super.visitConditional(that);

        that.effects = Effects.unionTrees(that.cond, that.truepart, that.falsepart);
    }

    @Override
    public void visitIf(AJCIf that) {
        super.visitIf(that);

        that.effects = Effects.unionTrees(that.cond, that.thenpart, that.elsepart);
    }

    @Override
    public void visitExpressionStatement(AJCExpressionStatement that) {
        super.visitExpressionStatement(that);

        that.effects = Effects.unionOf(that.expr.effects);
    }

    @Override
    public void visitBreak(AJCBreak that) {
        super.visitBreak(that);

        that.effects = new Effects(NO_EFFECTS);
    }

    @Override
    public void visitContinue(AJCContinue that) {
        super.visitContinue(that);

        that.effects = new Effects(NO_EFFECTS);
    }

    @Override
    public void visitReturn(AJCReturn that) {
        super.visitReturn(that);

        that.effects = Effects.unionOf(that.expr.effects);
    }

    @Override
    public void visitThrow(AJCThrow that) {
        super.visitThrow(that);

        that.effects = Effects.unionWithDirect(new EffectSet(EffectType.EXCEPTION), that.expr.effects);
    }

    /**
     * In this, the first stage of the bootstrapping pass, we neglect call side effects entirely.
     */
    private void handleCallEffects(List<AJCExpressionTree> args, AJCSymbolRefTree<MethodSymbol> that) {
        MethodSymbol calledMethod = that.getTargetSymbol();

        // The effects of the arguments to the function.
        that.effects = Effects.unionTrees(args);

        // Add to the list of calls needing to be fixed up.
        incompleteCalls.listAdd(calledMethod, that);
        calledMethodsWithoutSource.add(calledMethod);

        // The method being visited requires the called method to complete.
        methodDeps.listAdd(methodBeingVisited, calledMethod);
        methodDeps.ensure(calledMethod);

        // The called method is required by the method being visited.
        reverseMethodDeps.listAdd(calledMethod, methodBeingVisited);
    }

    @Override
    public void visitCall(AJCCall that) {
        super.visitCall(that);
        handleCallEffects(that.args, that);
    }

    @Override
    public void visitNewClass(AJCNewClass that) {
        // This'll be a field initialiser...
        if (methodBeingVisited == null) {
            return;
        }
        super.visitNewClass(that);
        handleCallEffects(that.args, that);
    }

    @Override
    public void visitNewArray(AJCNewArray that) {
        super.visitNewArray(that);

        // A new array operation *by itself* has no side effects (Neglecting ever-present possibilities
        // such as OutOfMemoryException. It is the corresponding assignment (If any exists) which
        // has a side effect.
        // Of course, the argument to the new array call might have side effects, as might the
        // elements of the array (If given explicitly).
        Effects elementEffects = Effects.unionTrees(that.elems);
        Effects dimensionEffects = Effects.unionTrees(that.dims);

        that.effects = Effects.unionOf(elementEffects, dimensionEffects);
    }

    @Override
    public void visitAssign(AJCAssign that) {
        super.visitAssign(that);

        // The direct part of an assignment will be a subset of writes to all of the symbols read by the lhs.
        // *usually* we can narrow it down a lot more than that, but not always. Bridge methods are particularly
        // troublesome in this regard, yeilding constructs like f()[x] = y;, which defy attempts to resolve them to
        // an underlying VarSymbol.

        // Firstly, try the simple way out.
        VarSymbol directSymbol = that.getTargetSymbol();
        if (directSymbol != null) {
            that.effects = Effects.unionWithDirect(write(directSymbol), that.rhs.effects);
            return;
        }

        log.trace("Entering special assignment effects routine for {}", that);
        // TODO: This routine is almost comically inefficient.
        // Construct an EffectSet for "Writes everything read by the lhs".
        EffectSet lhsEffects = that.lhs.effects.getEffectSet();

        SymbolSet readEscaping = lhsEffects.readEscaping == SymbolSet.UNIVERSAL_SET ? SymbolSet.UNIVERSAL_SET : new SymbolSet(lhsEffects.readEscaping);
        SymbolSet readInternal = lhsEffects.readInternal == SymbolSet.UNIVERSAL_SET ? SymbolSet.UNIVERSAL_SET : new SymbolSet(lhsEffects.readInternal);

        EffectSet newEffects = new EffectSet((readEscaping.isEmpty() ? 0 : EffectType.WRITE_ESCAPING.maskValue)
                                           | (readInternal.isEmpty() ? 0 : EffectType.WRITE_INTERNAL.maskValue));

        newEffects.writeEscaping = readEscaping;
        newEffects.writeInternal = readInternal;

        that.effects = Effects.unionWithDirect(newEffects, that.rhs.effects);
    }

    @Override
    public void visitAssignop(AJCAssignOp that) {
        super.visitAssignop(that);

        VarSymbol varSym = that.getTargetSymbol();

        if (varSym != null) {
            that.effects = Effects.unionWithDirect(EffectSet.write(varSym).union(read(varSym)), that.rhs.effects);
            return;
        }


        log.trace("Entering special assignment effects routine for {}", that);
        // TODO: This routine is almost comically inefficient.
        // Similar to assign, but we also immediately *read* everything, too.
        EffectSet lhsEffects = that.lhs.effects.getEffectSet();

        SymbolSet readEscaping = lhsEffects.readEscaping == SymbolSet.UNIVERSAL_SET ? SymbolSet.UNIVERSAL_SET : new SymbolSet(lhsEffects.readEscaping);
        SymbolSet writeEscaping = lhsEffects.readEscaping == SymbolSet.UNIVERSAL_SET ? SymbolSet.UNIVERSAL_SET : new SymbolSet(lhsEffects.readEscaping);
        SymbolSet readInternal = lhsEffects.readInternal == SymbolSet.UNIVERSAL_SET ? SymbolSet.UNIVERSAL_SET : new SymbolSet(lhsEffects.readInternal);
        SymbolSet writeInternal = lhsEffects.readInternal == SymbolSet.UNIVERSAL_SET ? SymbolSet.UNIVERSAL_SET : new SymbolSet(lhsEffects.readInternal);

        EffectSet newEffects = new EffectSet((readEscaping.isEmpty() ? 0 : EffectType.WRITE_ESCAPING.maskValue
                                                                         | EffectType.READ_ESCAPING.maskValue)
                                           | (readInternal.isEmpty() ? 0 : EffectType.WRITE_INTERNAL.maskValue
                                                                         | EffectType.READ_INTERNAL.maskValue));

        newEffects.writeEscaping = writeEscaping;
        newEffects.writeInternal = writeInternal;
        newEffects.readEscaping = readEscaping;
        newEffects.readInternal = readInternal;

        that.effects = Effects.unionWithDirect(newEffects, that.rhs.effects);
    }

    @Override
    public void visitUnary(AJCUnary that) {
        super.visitUnary(that);

        that.effects = Effects.unionOf(that.arg.effects);
    }

    @Override
    public void visitUnaryAsg(AJCUnaryAsg that) {
        super.visitUnaryAsg(that);

        VarSymbol varSym = that.getTargetSymbol();
        that.effects = Effects.unionWithDirect(EffectSet.write(varSym).union(read(varSym)), that.arg.effects);
    }

    @Override
    public void visitBinary(AJCBinary that) {
        super.visitBinary(that);

        that.effects = Effects.unionTrees(that.lhs, that.rhs);
    }

    @Override
    public void visitArrayAccess(AJCArrayAccess that) {
        super.visitArrayAccess(that);

        log.trace("Visiting array access: {}", that);
        // The read effect on the underlying array will be brought in by the ident in indexed.
        // If it's a call or some other crazy thing being used to refer to the array, this also catches that.
        that.effects = Effects.unionTrees(that.indexed, that.index);
    }

    @Override
    public void visitFieldAccess(AJCFieldAccess that) {
        super.visitFieldAccess(that);

        // If it's a field access to a method, we don't care.
        Symbol targetSym  = that.getTargetSymbol();
        if (!(targetSym instanceof VarSymbol)) {
            that.effects = new Effects(NO_EFFECTS);
            return;
        }

        String hash = ClassInfo.getHashForVariable((VarSymbol) targetSym);
        if (!JOUSTCache.varSymbolTable.containsKey(hash)) {
            JOUSTCache.varSymbolTable.put(hash, (VarSymbol) targetSym);
        }

        VarSymbol tSym = (VarSymbol) targetSym;

        that.effects = Effects.unionWithDirect(read(tSym), that.selected.effects);
    }

    @Override
    public void visitIdent(AJCIdent that) {
        super.visitIdent(that);

        Symbol targetSym = that.getTargetSymbol();

        if (targetSym instanceof VarSymbol) {
            String hash = ClassInfo.getHashForVariable((VarSymbol) targetSym);
            if (!JOUSTCache.varSymbolTable.containsKey(hash)) {
                JOUSTCache.varSymbolTable.put(hash, (VarSymbol) targetSym);
            }

            that.effects = new Effects(NO_EFFECTS, read((VarSymbol) targetSym));
            return;
        }

        that.effects = new Effects(NO_EFFECTS);
    }

    @Override
    public void visitLiteral(AJCLiteral that) {
        super.visitLiteral(that);
        that.effects = new Effects(NO_EFFECTS);
    }

    @Override
    public void visitErroneous(AJCErroneous that) {
        super.visitErroneous(that);
        log.error("Encountered erroneous tree: {}", that);
    }

    @Override
    public void visitLetExpr(AJCLetExpr that) {
        super.visitLetExpr(that);

        Effects defEffects = Effects.unionTrees(that.defs);
        that.effects = Effects.unionOf(defEffects, that.expr.effects);
    }

    @Override
    public void visitTypeCast(AJCTypeCast that) {
        super.visitTypeCast(that);
        that.effects = Effects.unionOf(that.expr.effects);
    }

    @Override
    public void visitInstanceOf(AJCInstanceOf that) {
        super.visitInstanceOf(that);
        that.effects = Effects.unionOf(that.expr.effects);
    }

    @Override
    public void visitVariableDecl(AJCVariableDecl that) {
        super.visitVariableDecl(that);

        VarSymbol sym = that.getTargetSymbol();
        JOUSTCache.varSymbolTable.put(ClassInfo.getHashForVariable(sym), sym);

        that.effects = Effects.unionWithDirect(write(that.getTargetSymbol()), that.getInit().effects);
    }

    @Override
    public void visitAnnotation(AJCAnnotation that) {
        super.visitAnnotation(that);
        that.effects = new Effects(NO_EFFECTS);
    }
}
