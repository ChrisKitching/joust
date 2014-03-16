package joust.optimisers.visitors.sideeffects;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.List;
import joust.tree.annotatedtree.AJCTreeVisitorImpl;
import joust.treeinfo.EffectSet;
import joust.treeinfo.TreeInfoManager;
import joust.utils.SetHashMap;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Set;

import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.treeinfo.EffectSet.*;

@Log4j2
public class SideEffectVisitor extends AJCTreeVisitorImpl {
    // Track the method calls which depend on incomplete methods so we can go back and fix them up when we complete
    // the method in question. Keyed by MethodSymbol of the incomplete method.
    private final SetHashMap<MethodSymbol, AJCEffectAnnotatedTree> incompleteNodes = new SetHashMap<>();

    // Maps MethodSymbols to sets of MethodSymbols that depend on the key MethodSymbol and are thus incomplete.
    private final SetHashMap<MethodSymbol, MethodSymbol> incompleteMethods = new SetHashMap<>();

    // The incomplete Effects objects for methods that have unresolved deps.
    private final HashMap<MethodSymbol, Effects> workInProgressMethodEffects = new HashMap<>();

    @Override
    public void visitMethodDef(AJCMethodDecl that) {
        super.visitMethodDef(that);

        Effects methodEffects = that.body.effects;

        MethodSymbol sym = that.getTargetSymbol();

        if ((sym.flags() & Flags.ABSTRACT) != 0) {
            log.warn("Panic: Abstract method encountered. How to handle the effects?!");
            // TODO: Funky system for resolving polymorphic calls to the superset of all possible
            // call targets. For now, though, Universe!
            methodCompleted(sym, ALL_EFFECTS);
            return;
        }

        // If there are no unresolved deps for this method, register the final effect set and complete the method.
        if (methodEffects.needsEffectsFrom.isEmpty()) {
            methodCompleted(sym, methodEffects.effectSet);
        } else {
            // Put a reference from each unresolved dependency to this.
            for (MethodSymbol dep : methodEffects.needsEffectsFrom) {
                incompleteMethods.listAdd(dep, sym);
                workInProgressMethodEffects.put(sym, methodEffects);
            }
        }
    }

    /**
     * Called when the effect set for a method becomes available.
     * Used to update all incomplete nodes that rely on this method.
     */
    private void methodCompleted(MethodSymbol sym, EffectSet effects) {
        effects = effects.dropUnescaping();
        log.debug("{} completed with {}", sym, effects);
        TreeInfoManager.registerMethodEffects(sym, effects);

        Set<AJCEffectAnnotatedTree> incompleted = incompleteNodes.get(sym);
        if (incompleted != null) {
            for (AJCEffectAnnotatedTree t : incompleted) {
                Effects tEffects = t.effects;

                EffectSet newEffectSet = tEffects.effectSet.union(effects);
                tEffects.needsEffectsFrom.remove(sym);
                tEffects.setEffectSet(newEffectSet);
            }

            incompleteNodes.remove(sym);
        }

        // Determine if this completes any methods. If so, recurse!
        Set<MethodSymbol> affectedMethods = incompleteMethods.get(sym);
        if (affectedMethods != null) {
            for (MethodSymbol t : affectedMethods) {
                Effects methodEffects = workInProgressMethodEffects.get(sym);
                if (methodEffects.needsEffectsFrom.isEmpty()) {
                    workInProgressMethodEffects.remove(sym);
                    methodCompleted(t, methodEffects.effectSet);
                }
            }

            incompleteMethods.remove(sym);
        }

    }

    @Override
    public void visitSkip(AJCSkip that) {
        super.visitSkip(that);
        that.effects = new Effects(NO_EFFECTS);
    }

    @Override
    public void visitEmptyExpression(AJCEmptyExpression that) {
        super.visitEmptyExpression(that);
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

        that.effects = that.body.effects;
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

        that.effects = that.body.effects;
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

        that.effects = that.expr.effects;
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

        that.effects = that.expr.effects;
    }

    @Override
    public void visitThrow(AJCThrow that) {
        super.visitThrow(that);

        that.effects = Effects.unionWithDirect(new EffectSet(EffectType.EXCEPTION), that.expr.effects);
    }

    /**
     * Get the CandidateEffectSet for a particular call (constructor or regular) given args and a MethodSymbol.
     */
    private void handleCallEffects(List<AJCExpressionTree> args, AJCSymbolRefTree<MethodSymbol> that) {
        MethodSymbol methodSym = that.getTargetSymbol();

        // The effects of the arguments to the function.
        Effects argEffects = Effects.unionTrees(args);

        // Determine if we have final effects for the method being called yet (From the cache or from earlier analysis.
        EffectSet methodEffects = TreeInfoManager.getEffectsForMethod(methodSym);
        if (methodEffects == null) {
            // We found none - add the dependancy and hope we manage to resolve it later...
            log.info("No effects for {} found. Adding dep.", methodSym);

            that.effects = argEffects;
            incompleteNodes.listAdd(methodSym, that);
        } else {
            // We found some! Union them and continue with joy.
            that.effects = Effects.unionOf(argEffects, new Effects(methodEffects));
        }
    }

    @Override
    public void visitCall(AJCCall that) {
        super.visitCall(that);
        handleCallEffects(that.args, that);
    }

    @Override
    public void visitNewClass(AJCNewClass that) {
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

        that.effects = Effects.unionWithDirect(write(that.getTargetSymbol()), that.rhs.effects);
    }

    @Override
    public void visitAssignop(AJCAssignOp that) {
        super.visitAssignop(that);

        VarSymbol varSym = that.getTargetSymbol();
        that.effects = Effects.unionWithDirect(EffectSet.write(varSym).union(read(varSym)), that.rhs.effects);
    }

    @Override
    public void visitUnary(AJCUnary that) {
        super.visitUnary(that);

        that.effects = that.arg.effects;
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

        that.effects = Effects.unionWithDirect(read(that.indexed.getTargetSymbol()), that.index.effects);
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

        VarSymbol tSym = (VarSymbol) targetSym;

        that.effects = Effects.unionWithDirect(read(tSym), that.selected.effects);
    }

    @Override
    public void visitIdent(AJCIdent that) {
        super.visitIdent(that);

        Symbol sym = that.getTargetSymbol();

        if (sym instanceof VarSymbol) {
            that.effects = new Effects(NO_EFFECTS, read((VarSymbol) sym));
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
        log.error("Encountered errorneous tree: {}", that);
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
        that.effects = that.expr.effects;
    }

    @Override
    public void visitInstanceOf(AJCInstanceOf that) {
        super.visitInstanceOf(that);
        that.effects = that.expr.effects;
    }

    @Override
    public void visitVariableDecl(AJCVariableDecl that) {
        super.visitVariableDecl(that);

        that.effects = Effects.unionWithDirect(write(that.getTargetSymbol()), that.getInit().effects);
    }

    @Override
    public void visitAnnotation(AJCAnnotation that) {
        super.visitAnnotation(that);
        that.effects = new Effects(NO_EFFECTS);
    }
}
