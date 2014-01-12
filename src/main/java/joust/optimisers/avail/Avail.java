package joust.optimisers.avail;

import com.esotericsoftware.minlog.Log;
import com.sun.tools.javac.tree.JCTree;
import joust.treeinfo.TreeInfoManager;
import joust.utils.DepthFirstTreeVisitor;
import lombok.extern.log4j.Log4j2;


import java.util.HashSet;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

/**
 * Perform available expression analysis on methods.
 */
public @Log4j2 class Avail extends DepthFirstTreeVisitor {
    // The symbol of the method currently being processed.
    MethodSymbol enclosingMethod;

    // The current local variable scope.
    AvailScope currentScope;

    // Flag used to indicate if the next block visited should spawn a new scope. Used to handle
    // spontaneous blocks.
    private boolean nextBlockIsNewScope = true;

    private void enterScope() {
        // Create a new local variable scope contained within the current scope and switch to it.
        currentScope = new AvailScope(currentScope, enclosingMethod);
    }

    private void leaveScope() {
        currentScope = currentScope.leave();
    }

    @Override
    public void visitTopLevel(JCCompilationUnit jcCompilationUnit) {
        super.visitTopLevel(jcCompilationUnit);
    }

    @Override
    public void visitImport(JCImport jcImport) {
        super.visitImport(jcImport);
    }

    @Override
    public void visitClassDef(JCClassDecl jcClassDecl) {
        super.visitClassDef(jcClassDecl);
    }

    @Override
    public void visitMethodDef(JCMethodDecl jcMethodDecl) {
        log.debug("Visitng method: "+jcMethodDecl);
        enclosingMethod = jcMethodDecl.sym;
        currentScope = new AvailScope(jcMethodDecl.sym);

        // TODO: Something something nested classes.
        // TODO: Possibly need to prepopulate class scope at this point to avoid collisions...
        super.visitMethodDef(jcMethodDecl);
    }

    @Override
    public void visitVarDef(JCVariableDecl jcVariableDecl) {
        markAvailableExpressions(jcVariableDecl);
        currentScope.enter(jcVariableDecl.sym);

        super.visitVarDef(jcVariableDecl);
        // Enter the symbol.

        // Associate the concrete symbol with the PAE for the initialiser, if any exists.
        JCTree init = jcVariableDecl.getInitializer();
        if (init != null) {
            currentScope.registerSymbolWithExpression(jcVariableDecl.sym, init);
        }
    }

    @Override
    public void visitBlock(JCBlock jcBlock) {
        boolean blockWasNewScope = nextBlockIsNewScope;
        if (nextBlockIsNewScope) {
            log.info("Entering block scope: {}", jcBlock);
            enterScope();
        } else {
            nextBlockIsNewScope = true;
        }

        super.visitBlock(jcBlock);

        if (blockWasNewScope) {
            log.info("Exiting block scope: {}", jcBlock);
            leaveScope();
        }
    }

    @Override
    public void visitDoLoop(JCDoWhileLoop jcDoWhileLoop) {
        // Create a new local variable scope contained within the current scope.
        enterScope();
        nextBlockIsNewScope = false;

        super.visitDoLoop(jcDoWhileLoop);

        leaveScope();
        nextBlockIsNewScope = true;
    }

    @Override
    public void visitWhileLoop(JCWhileLoop jcWhileLoop) {
        log.debug("Entering visitWhileLoop with scope:\n{}", currentScope);


        if (mMarked.contains(jcWhileLoop)) {
            return;
        }

        nextBlockIsNewScope = false;
        visit(jcWhileLoop.cond);
        // Create a new local variable scope contained within the current scope.
        enterScope();


        log.debug("Entered visitWhileLoop scope, now have:\n{}", currentScope);
        nextBlockIsNewScope = false;
        visit(jcWhileLoop.body);

        log.debug("Exiting visitWhileLoop with scope:\n{}", currentScope);

        leaveScope();
        nextBlockIsNewScope = true;
        log.debug("After exit:\n{}", currentScope);

        mMarked.add(jcWhileLoop);
    }

    @Override
    public void visitForLoop(JCForLoop jcForLoop) {
        if (mMarked.contains(jcForLoop)) {
            return;
        }

        enterScope();
        nextBlockIsNewScope = false;
        visit(jcForLoop.init);
        nextBlockIsNewScope = false;
        visit(jcForLoop.step);
        nextBlockIsNewScope = false;
        visit(jcForLoop.cond);
        nextBlockIsNewScope = false;
        visit(jcForLoop.body);

        mMarked.add(jcForLoop);

        leaveScope();
        nextBlockIsNewScope = true;
    }

    @Override
    public void visitForeachLoop(JCEnhancedForLoop jcEnhancedForLoop) {
        if (mMarked.contains(jcEnhancedForLoop)) {
            return;
        }

        enterScope();
        nextBlockIsNewScope = false;
        visit(jcEnhancedForLoop.var);
        nextBlockIsNewScope = false;
        visit(jcEnhancedForLoop.expr);
        nextBlockIsNewScope = false;
        visit(jcEnhancedForLoop.body);

        mMarked.add(jcEnhancedForLoop);
        leaveScope();
        nextBlockIsNewScope = true;
    }

    @Override
    public void visitLabelled(JCLabeledStatement jcLabeledStatement) {
        super.visitLabelled(jcLabeledStatement);
    }

    @Override
    public void visitSwitch(JCSwitch jcSwitch) {
        super.visitSwitch(jcSwitch);
    }

    @Override
    public void visitCase(JCCase jcCase) {
        // Although each case is in the same scope as the other cases, the "Scope" we're using here
        // is a crazy munging of lexical scope and flow analysis. This is safe. Honest.
        enterScope();
        nextBlockIsNewScope = false;
        super.visitCase(jcCase);
        leaveScope();
        nextBlockIsNewScope = true;
    }

    @Override
    public void visitSynchronized(JCSynchronized jcSynchronized) {
        enterScope();
        nextBlockIsNewScope = false;
        super.visitSynchronized(jcSynchronized);
        leaveScope();
        nextBlockIsNewScope = true;
    }

    @Override
    public void visitTry(JCTry jcTry) {
        if (mMarked.contains(jcTry)) {
            return;
        }

        visit(jcTry.resources);

        enterScope();
        nextBlockIsNewScope = false;
        visit(jcTry.body);
        leaveScope();

        // A new scope is needed for *each* catcher.
        visit(jcTry.catchers);

        enterScope();
        nextBlockIsNewScope = false;
        visit(jcTry.finalizer);
        leaveScope();
        nextBlockIsNewScope = true;

        mMarked.add(jcTry);
    }

    @Override
    public void visitCatch(JCCatch jcCatch) {
        enterScope();
        nextBlockIsNewScope = false;
        super.visitCatch(jcCatch);
        leaveScope();
        nextBlockIsNewScope = true;
    }

    // TODO: Something something PolyExpression.
    @Override
    public void visitConditional(JCConditional jcConditional) {
        markAvailableExpressions(jcConditional);
        super.visitConditional(jcConditional);
    }

    @Override
    public void visitIf(JCIf jcIf) {
        if (mMarked.contains(jcIf)) {
            return;
        }

        visit(jcIf.cond);

        enterScope();
        nextBlockIsNewScope = false;
        visit(jcIf.thenpart);
        leaveScope();
        nextBlockIsNewScope = true;

        if (jcIf.elsepart != null) {
            enterScope();
            nextBlockIsNewScope = false;
            visit(jcIf.elsepart);
            leaveScope();
            nextBlockIsNewScope = true;
        }

        mMarked.add(jcIf);
    }

    @Override
    public void visitExec(JCExpressionStatement jcExpressionStatement) {
        markAvailableExpressions(jcExpressionStatement);
        super.visitExec(jcExpressionStatement);
    }

    @Override
    public void visitBreak(JCBreak jcBreak) {
        super.visitBreak(jcBreak);
    }

    @Override
    public void visitContinue(JCContinue jcContinue) {
        super.visitContinue(jcContinue);
    }

    @Override
    public void visitReturn(JCReturn jcReturn) {
        super.visitReturn(jcReturn);
    }

    @Override
    public void visitThrow(JCThrow jcThrow) {
        super.visitThrow(jcThrow);
    }

    @Override
    public void visitAssert(JCAssert jcAssert) {
        super.visitAssert(jcAssert);
    }

    @Override
    public void visitApply(JCMethodInvocation jcMethodInvocation) {
        super.visitApply(jcMethodInvocation);
    }

    @Override
    public void visitNewClass(JCNewClass jcNewClass) {
        super.visitNewClass(jcNewClass);
    }

    @Override
    public void visitNewArray(JCNewArray jcNewArray) {
        super.visitNewArray(jcNewArray);
    }

    @Override
    public void visitLambda(JCLambda jcLambda) {
        super.visitLambda(jcLambda);
    }

    @Override
    public void visitParens(JCParens jcParens) {
        markAvailableExpressions(jcParens);
        super.visitParens(jcParens);
        currentScope.enterExpression(jcParens);
    }

    @Override
    public void visitAssign(JCAssign jcAssign) {
        Log.debug("Visit assign!");

        if (mMarked.contains(jcAssign)) {
            return;
        }

        markAvailableExpressions(jcAssign);
        visit(jcAssign.rhs);
        currentScope.enterExpression(jcAssign);

        mMarked.add(jcAssign);
    }

    @Override
    public void visitAssignop(JCAssignOp jcAssignOp) {
        markAvailableExpressions(jcAssignOp);
        super.visitAssignop(jcAssignOp);
        currentScope.enterExpression(jcAssignOp);
    }

    @Override
    public void visitUnary(JCUnary jcUnary) {
        markAvailableExpressions(jcUnary);
        super.visitUnary(jcUnary);
        currentScope.enterExpression(jcUnary);
    }

    @Override
    public void visitBinary(JCBinary jcBinary) {
        markAvailableExpressions(jcBinary);
        super.visitBinary(jcBinary);
        currentScope.enterExpression(jcBinary);
    }

    @Override
    public void visitTypeCast(JCTypeCast jcTypeCast) {
        markAvailableExpressions(jcTypeCast);
        super.visitTypeCast(jcTypeCast);
        currentScope.enterExpression(jcTypeCast);
    }

    @Override
    public void visitTypeTest(JCInstanceOf jcInstanceOf) {
        markAvailableExpressions(jcInstanceOf);
        super.visitTypeTest(jcInstanceOf);
        currentScope.enterExpression(jcInstanceOf);
    }

    @Override
    public void visitIndexed(JCArrayAccess jcArrayAccess) {
        markAvailableExpressions(jcArrayAccess);
        super.visitIndexed(jcArrayAccess);
        currentScope.enterExpression(jcArrayAccess);
    }

    @Override
    public void visitSelect(JCFieldAccess jcFieldAccess) {
        markAvailableExpressions(jcFieldAccess);
        super.visitSelect(jcFieldAccess);
    }

    @Override
    public void visitReference(JCMemberReference jcMemberReference) {
        markAvailableExpressions(jcMemberReference);
        super.visitReference(jcMemberReference);
    }

    @Override
    public void visitIdent(JCIdent jcIdent) {
        super.visitIdent(jcIdent);
        if (currentScope == null) {
            return;
        }
        markAvailableExpressions(jcIdent);
        currentScope.enterExpression(jcIdent);
    }

    @Override
    public void visitLiteral(JCLiteral jcLiteral) {
        super.visitLiteral(jcLiteral);
        if (currentScope == null) {
            return;
        }
        markAvailableExpressions(jcLiteral);
        currentScope.enterExpression(jcLiteral);
    }

    @Override
    public void visitTypeIdent(JCPrimitiveTypeTree jcPrimitiveTypeTree) {
        super.visitTypeIdent(jcPrimitiveTypeTree);
    }

    @Override
    public void visitTypeArray(JCArrayTypeTree jcArrayTypeTree) {
        super.visitTypeArray(jcArrayTypeTree);
    }

    @Override
    public void visitTypeApply(JCTypeApply jcTypeApply) {
        super.visitTypeApply(jcTypeApply);
    }

    @Override
    public void visitTypeUnion(JCTypeUnion jcTypeUnion) {
        super.visitTypeUnion(jcTypeUnion);
    }

    @Override
    public void visitTypeIntersection(JCTypeIntersection jcTypeIntersection) {
        super.visitTypeIntersection(jcTypeIntersection);
    }

    @Override
    public void visitTypeParameter(JCTypeParameter jcTypeParameter) {
        super.visitTypeParameter(jcTypeParameter);
    }

    @Override
    public void visitWildcard(JCWildcard jcWildcard) {
        super.visitWildcard(jcWildcard);
    }

    @Override
    public void visitTypeBoundKind(TypeBoundKind typeBoundKind) {
        super.visitTypeBoundKind(typeBoundKind);
    }

    @Override
    public void visitModifiers(JCModifiers jcModifiers) {
        super.visitModifiers(jcModifiers);
    }

    @Override
    public void visitAnnotation(JCAnnotation jcAnnotation) {
        super.visitAnnotation(jcAnnotation);
    }

    @Override
    public void visitAnnotatedType(JCAnnotatedType jcAnnotatedType) {
        super.visitAnnotatedType(jcAnnotatedType);
    }

    @Override
    public void visitErroneous(JCErroneous jcErroneous) {
        super.visitErroneous(jcErroneous);
    }

    @Override
    public void visitLetExpr(LetExpr letExpr) {
        super.visitLetExpr(letExpr);
    }

    // Store a copy of the current available expression set in the TreeInfo structure for this node.
    private void markAvailableExpressions(JCTree tree) {
        TreeInfoManager.registerAvailables(tree, new HashSet<>(currentScope.availableExpressions));
    }
}
