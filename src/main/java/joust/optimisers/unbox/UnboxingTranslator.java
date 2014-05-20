package joust.optimisers.unbox;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.List;
import joust.analysers.TouchedSymbolLocator;
import joust.tree.annotatedtree.AJCForest;
import joust.utils.tree.functiontemplates.FunctionTemplate;
import joust.optimisers.translators.BaseTranslator;
import joust.tree.annotatedtree.AJCTree;
import joust.utils.data.SetHashMap;
import joust.utils.logging.LogUtils;
import joust.utils.tree.TreeUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;

import static com.sun.tools.javac.code.Symbol.*;
import static com.sun.tools.javac.code.Type.*;
import static joust.tree.annotatedtree.AJCTree.*;
import static joust.utils.compiler.StaticCompilerUtils.*;
import static joust.optimisers.unbox.UnboxingFunctionTemplates.functionTemplates;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class UnboxingTranslator extends BaseTranslator {
    // The usages of each boxed VarSymbol that we can safely handle if it weren't boxed.
    // If empty when the symbol is dropped, no action is taken. Otherwise, these are fixed up
    // and the boxed variable declared at a later point (if necessary).
    SetHashMap<VarSymbol, AJCTree> fixableUsages = new SetHashMap<VarSymbol, AJCTree>();

    // Maps VarSymbols to the VarSymbols that they depend upon.
    SetHashMap<VarSymbol, VarSymbol> deps = new SetHashMap<VarSymbol, VarSymbol>();

    Set<AJCLetExpr> letExpressions = new HashSet<AJCLetExpr>();

    private boolean scanningArgs;

    // The target of the assignment nodes being explored.
    private Stack<VarSymbol> assignmentStack = new Stack<VarSymbol>();
    private VarSymbol currentAssignmentTarget;

    private void pushAssignment(VarSymbol sym) {
        assignmentStack.push(sym);
        currentAssignmentTarget = sym;
    }
    private void popAssignment() {
        currentAssignmentTarget = assignmentStack.pop();
    }

    @Override
    protected void visitMethodDef(AJCMethodDecl that) {
        // Don't visit the arguments!
        visit(that.body);

        log.debug("Unbox scan finished for:\n{}", that);
        log.debug("Found: {}", Arrays.toString(fixableUsages.keySet().toArray()));

        boolean changedMethod = false;

        for (VarSymbol sym : fixableUsages.keySet()) {
            mHasMadeAChange = true;
            AJCForest.getInstance().increment("Variables Unboxed:");
            changedMethod = true;
            log.info("Fixing: {}", sym);
            Set<AJCTree> fixableHere = fixableUsages.get(sym);
            log.info("Usages: {}", Arrays.toString(fixableHere.toArray()));

            AJCVariableDecl decl = null;
            // Find the declaration. Should be first.
            Iterator<AJCTree> usageIterator = fixableHere.iterator();
            while (usageIterator.hasNext()) {
                AJCTree usage = usageIterator.next();
                if (usage instanceof AJCVariableDecl) {
                    decl = (AJCVariableDecl) usage;
                    usageIterator.remove();
                    break;
                }
            }

            if (decl == null) {
                log.fatal("Unable to find decl for {}!", sym);
                return;
            }

            UnboxMapper mapper = new UnboxMapper(sym);

            // Fix up all usages, including the init...
            for (AJCTree usage : fixableHere) {
                AJCForest.getInstance().increment("Boxed Usages Removed:");
                log.info("Fixing usage: {}", usage);
                AJCTree replacement = mapper.replacementTree(usage);
                log.info("Swapping {} for {}", usage, replacement);
                log.debug("Parent: {}:{}", usage.mParentNode, usage.mParentNode.getClass().getCanonicalName());
                usage.swapFor(replacement);
            }

            AJCVariableDecl newDecl = mapper.replacementVarDef(decl);

            log.debug("Parent: {}:{}", decl.mParentNode, decl.mParentNode.getClass().getCanonicalName());
            log.info("NewDecl: {}", newDecl);
            decl.swapFor(newDecl);

            log.info("After fixing that one:\n{}", that);
        }

        for (AJCLetExpr letExpr : letExpressions) {
            log.debug("Final letExpr type: {}", letExpr.expr.getNodeType());
            log.debug("Final letExpr body: {}", letExpr.expr);
            log.debug("Final letExpr up type: {}", letExpr.getNodeType());
        }

        log.debug("BAZINGA");

        if (changedMethod) {
            LetExprHack hack = new LetExprHack();
            hack.visitTree(that);
        }

        for (AJCLetExpr letExpr : letExpressions) {
            log.debug("Final letExpr type: {}", letExpr.expr.getNodeType());
            log.debug("Final letExpr body: {}", letExpr.expr);
            log.debug("Final letExpr up type: {}", letExpr.getNodeType());
        }


        fixableUsages = new SetHashMap<VarSymbol, AJCTree>();
        deps = new SetHashMap<VarSymbol, VarSymbol>();
        letExpressions = new HashSet<AJCLetExpr>();
        assignmentStack = new Stack<VarSymbol>();
        currentAssignmentTarget = null;
        scanningArgs = false;

        if (changedMethod) {
            log.info("After unboxing:\n{}", that);
            AJCForest.getInstance().initialAnalysis();
        }
    }

    @Override
    protected void visitVariableDecl(AJCVariableDecl that) {
        super.visitVariableDecl(that);

        VarSymbol sym = that.getTargetSymbol();
        if (!TreeUtils.isLocalVariable(sym)) {
            return;
        }

        Type boxedType = types.unboxedType(sym.type);
        if (boxedType == noType) {
            return;
        }

        log.debug("For {} have {} of type {} boxing {}", that, sym, sym.type, boxedType);

        // Ensure the initialiser is of a form we can handle.
        AJCExpressionTree init = that.getInit();

        // If it's a literal, it must be a null literal, which is not okay.
        if (init instanceof AJCLiteral) {
            return;
        }

        // We're okay if it's a function call - a function that returns a boxed value, and it satisfies our other rules
        // about acceptable use, then we can replace it with f().intValue() or such without the need for a null check.

        fixableUsages.listAdd(sym, that);

        super.visitVariableDecl(that);
    }

    /**
     * If the given SymbolRef refers to a tracked boxed symbol, fail it.
     */
    private void killInstanceReferencedByTree(AJCSymbolRef refTree) {
        Symbol refdTree = refTree.getTargetSymbol();
        if (!(refdTree instanceof VarSymbol)) {
            return;
        }

        log.debug("Killing: {}", refdTree);
        if (fixableUsages.containsKey(refdTree)) {
            failSymbol((VarSymbol) refdTree);
        }
    }

    @Override
    protected void visitBinary(AJCBinary that) {
        log.debug("Visiting binary: {}", that);
        super.visitBinary(that);

        // TODO: Can special-case things like BOOLEAN.TRUE here...
        // Any *direct* use of a boxed instance in a binary expression prevents us from unboxing it.
        log.debug("Killing...");
        if (that.rhs instanceof AJCSymbolRef) {
            killInstanceReferencedByTree((AJCSymbolRef) that.rhs);
        }

        if (that.lhs instanceof AJCSymbolRef) {
            killInstanceReferencedByTree((AJCSymbolRef) that.lhs);
        }
    }

    @Override
    protected void visitAssign(AJCAssign that) {
        super.visitAssign(that);
        log.debug("Assignment: {}", that);
        VarSymbol assignee = that.lhs.getTargetSymbol();
        // Bloody unconventional array accesses.
        if (assignee == null) {
            return;
        }

        // If we're assigning to something that isn't a boxed type itself, we're not going to do any harm.
        Type boxedType = types.unboxedType(assignee.type);
        if (boxedType == noType) {
            log.debug("Not a boxed type.");
            // We still need to visit the rhs, though.
            return;
        }

        boolean containsAssignee = fixableUsages.containsKey(assignee);
        log.debug("containsAssignee: {}", containsAssignee);

        // If the assignee is being transformed away and the rhs refers to something that isn't, we fail the assignee and
        // every candidate referred to on the RHS.
        // If the assignee is not being transformed away, and the rhs refers to things that aren't, we fail every candidate
        // referred to on the rhs.
        TouchedSymbolLocator locator = new TouchedSymbolLocator();
        locator.visitTree(that.rhs);
        Set<VarSymbol> touchedSymbols = locator.touched;
        log.debug("touched syms: {}", Arrays.toString(touchedSymbols.toArray()));
        // Strip out all the symbols that aren't boxed.
        Iterator<VarSymbol> i = touchedSymbols.iterator();
        while (i.hasNext()) {
            VarSymbol vSym = i.next();
            if (types.unboxedType(vSym.type) == noType) {
                i.remove();
            }
        }

        log.debug("touched boxed syms: {}", Arrays.toString(touchedSymbols.toArray()));

        // Now we're left with the set of symbols of a boxed type touched in the rhs. If any of these aren't due for
        // transformation, all that are must be failed.
        int beforeSize = touchedSymbols.size();
        touchedSymbols.retainAll(fixableUsages.keySet());
        log.debug("touched tracked boxed syms: {}", Arrays.toString(touchedSymbols.toArray()));
        if (beforeSize != touchedSymbols.size()) {
            log.debug("Fail: symbols were lost!");
            // At least one symbol was dropped from the set - the one we're not transforming. Fail them all!
            failSymbols(touchedSymbols);

            // We're assigning something we're not transforming away, so it's got to go...
            if (containsAssignee) {
                failSymbol(assignee);
            }

            return;
        }

        // We only care if the assignment is *to* a tracked boxed symbol.
        if (!containsAssignee) {
            log.debug("Finished: Not assigning to a tracked boxed sym");
            super.visitAssign(that);
            return;
        }

        if (that.rhs instanceof AJCLiteral) {
            // It's being assigned a null literal - that's not acceptable.
            log.debug("(Asg) Killing {} because {}", ((AJCSymbolRef) that.lhs).getTargetSymbol(), that);
            killInstanceReferencedByTree(that.lhs);
            return;
        }


        // So all symbols in the rhs that are of boxed types are being transformed. These ones need to be added to the
        // dependency set for the assignee. If you kill the things referenced in the rhs, you kill the assignee.
        for (VarSymbol dep : touchedSymbols) {
            deps.listAdd(dep, assignee);
        }

        fixableUsages.listAdd(assignee, that.rhs);
        fixableUsages.listAdd(assignee, that);

        pushAssignment(assignee);
        // TODO: Something something new Integer(3);
        super.visitAssign(that);
        popAssignment();
    }

    @Override
    protected void visitLetExpr(AJCLetExpr that) {
        log.debug("Let expr type: {}", that.expr.getNodeType());
        log.debug("Let expr body: {}", that.expr);
        log.debug("Let expr up type: {}", that.getNodeType());
        letExpressions.add(that);
        super.visitLetExpr(that);
    }

    @Override
    protected void visitCall(AJCCall that) {
        super.visitCall(that);
        // TODO: If the call doesn't have read or write side effects, we can unbox it if we like, but need to put a call
        // to *.valueOf in the right place when we do.

        log.debug("Visit call: {}", that);

        // Is this a method we know about?
        MethodSymbol calledMethod = that.getTargetSymbol();
        log.debug("Called: {}", calledMethod);


        if (!functionTemplates.containsKey(calledMethod)) {
            log.debug("Dropped - no template.");

            scanUnfixableMethod(that);
            return;
        }

        // Is the call a static one?
        FunctionTemplate template = functionTemplates.get(calledMethod);
        if (template.isStatic) {
            log.debug("Static!");
            // If you're inside an assignment, then this one needs to be added to the list for that assignee.
            if (currentAssignmentTarget != null) {
                log.info("Attaching to: {}", currentAssignmentTarget);
                fixableUsages.listAdd(currentAssignmentTarget, that);
            }

            return;
        }

        // Is this a call to a method on a boxed symbol?
        VarSymbol callee = TreeUtils.getCalledObjectForCall(that);

        // We can't identify the callee, so continue as if it were an unknown method.
        if (callee == null || !fixableUsages.containsKey(callee)) {
            log.debug("Dropped - callee unidentified.");
            scanUnfixableMethod(that);
            return;
        }

        // If it's a call on a tracked boxed instance to a method that takes another boxed instance as an argument, we
        // can only transform it (and hence this tracked boxed instance) if the argument contains no reference to a
        // non-tracked boxed instance.
        if (UnboxingFunctionTemplates.functionTemplatesNeedingArgCheck.contains(template)) {
            log.debug("Needs args check....");
            // This one has been marked as requiring the argument check.
            // This means it's a template taking a reference type as an argument which cannot safely be ignored by the
            // template. If any of the argument expressions can possibly be null, the symbol must be failed.
            // If any of the argument expressions are objects other than tracked boxed instances, the symbol must also
            // be failed.

            // TODO: More sophisticated nullity-inference!

            List<VarSymbol> argPrototypes = calledMethod.params;
            Iterator<VarSymbol> argIterator = argPrototypes.iterator();

            for (AJCExpressionTree tree : that.args) {
                // The type of this parameter as declared.
                Type argType = argIterator.next().type;

                // Primitive types cannot cause the manner of failure we are checking for.
                if (argType.isPrimitive()) {
                    continue;
                }

                // If the type of the argument is not ostensibly a boxed type, we also don't care.
                // Presumably the template has a plan for that...
                if (types.unboxedType(argType) == noType) {
                    continue;
                }

                // Remove a cast, if there is any.
                tree = TreeUtils.removeCast(tree);

                if (tree instanceof AJCLiteral) {
                    // The dreaded null literal!
                    failSymbol(callee);
                    scanUnfixableMethod(that);
                    return;
                }

                if (tree instanceof AJCIdent) {
                    AJCIdent cast = (AJCIdent) tree;
                    Symbol targetSym = cast.getTargetSymbol();

                    // A reference to some other kind of symbol? Nope.
                    if (!(targetSym instanceof VarSymbol)) {
                        failSymbol(callee);
                        scanUnfixableMethod(that);
                        return;
                    }

                    VarSymbol castSym = (VarSymbol) targetSym;

                    // Not a tracked boxed instance? Nope.
                    if (!fixableUsages.containsKey(castSym)) {
                        failSymbol(callee);
                        scanUnfixableMethod(that);
                        return;
                    }
                }

                // TODO: Handle calls here.

                failSymbol(callee);
                scanUnfixableMethod(that);
                return;
            }


            // If you survived that, you now need to add every referenced boxed symbol in the arguments to the dependencies of
            // the callee (As it may only be transformed if the arguments to all functions needing the argument check are also
            // being transformed).
            TouchedSymbolLocator locator = new TouchedSymbolLocator();
            for (AJCExpressionTree arg : that.args) {
                locator.visitTree(arg);
            }

            Set<VarSymbol> touchedSyms = locator.touched;
            touchedSyms.retainAll(fixableUsages.keySet());
            for (VarSymbol touchedSym : touchedSyms) {
                deps.listAdd(touchedSym, callee);
            }
        }

        // So it's a call on a tracked boxed instance. Add it to the fixable set for that instance.
        log.debug("Adding to {}", callee);

        fixableUsages.listAdd(callee, that);
    }

    private void scanUnfixableMethod(AJCCall that) {
        scanningArgs = true;

        super.visitCall(that);

        scanningArgs = false;
    }


    @Override
    protected void visitIdent(AJCIdent that) {
        super.visitIdent(that);

        Symbol tSym = that.getTargetSymbol();
        if (!(tSym instanceof VarSymbol)) {
            return;
        }

        if (!fixableUsages.containsKey(tSym)) {
            return;
        }

        if (scanningArgs) {
            // It escapes. TODO: Follow the variable's usages across borders!
            failSymbol((VarSymbol) tSym);
        } else {
            fixableUsages.listAdd((VarSymbol) tSym, that);
        }
    }

    private void failSymbols(Set<VarSymbol> sym) {
        for (VarSymbol varSymbol : sym) {
            failSymbol(varSymbol);
        }
    }
    private void failSymbol(VarSymbol sym) {
        fixableUsages.remove(sym);
        if (!deps.containsKey(sym)) {
            return;
        }

        Set<VarSymbol> alsoFailed = deps.get(sym);
        for (VarSymbol fail : alsoFailed) {
            failSymbol(fail);
        }

        deps.remove(sym);
    }
}
